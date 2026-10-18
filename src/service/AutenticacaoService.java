package service;

import java.sql.SQLException;
import java.util.Arrays;

import crypto.PasswordUtils;
import crypto.TOTP;
import crypto.TokenUtils;
import database.UsuarioDAO;
import model.Usuario;

public class AutenticacaoService {

    private final UsuarioDAO usuarioDAO;

    public AutenticacaoService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public boolean iniciarLogin(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim());

        if (usuario == null) {
            return false;
        }

        long agora = System.currentTimeMillis();

        if (usuario.estaBloqueado(agora)) {
            return false;
        }

        usuarioDAO.limparBloqueioExpirado(usuario.getUid(), agora);
        return true;
    }

    public boolean validarSenha(String email, String senha) throws SQLException {
        if (email == null || email.trim().isEmpty() || senha == null) {
            return false;
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim());

        if (usuario == null) {
            return false;
        }

        long agora = System.currentTimeMillis();

        if (usuario.estaBloqueado(agora)) {
            return false;
        }

        usuarioDAO.limparBloqueioExpirado(usuario.getUid(), agora);

        if (!PasswordUtils.verificarSenha(senha, usuario.getSenhaHash())) {
            usuarioDAO.registrarFalhaDeSenha(usuario.getUid(), agora);
            return false;
        }

        usuarioDAO.limparFalhasDeSenha(usuario.getUid());
        return true;
    }

    public String validarSenhaPorPares(
            String email,
            char[][] pares) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim());
        if (usuario == null) {
            return null;
        }

        long agora = System.currentTimeMillis();
        if (usuario.estaBloqueado(agora)) {
            return null;
        }
        usuarioDAO.limparBloqueioExpirado(usuario.getUid(), agora);

        if (!paresValidos(pares)) {
            usuarioDAO.registrarFalhaDeSenha(usuario.getUid(), agora);
            return null;
        }

        String senhaValida = buscarSequencialmente(
            1 << pares.length,
            pares,
            usuario.getSenhaHash());
        if (senhaValida == null) {
            usuarioDAO.registrarFalhaDeSenha(usuario.getUid(), agora);
            return null;
        }
        usuarioDAO.limparFalhasDeSenha(usuario.getUid());
        return senhaValida;
    }

    private static boolean paresValidos(char[][] pares) {
        if (pares == null || pares.length < 8 || pares.length > 10) {
            return false;
        }
        for (char[] par : pares) {
            if (par == null || par.length != 2
                    || par[0] < '0' || par[0] > '9'
                    || par[1] < '0' || par[1] > '9'
                    || par[0] == par[1]) {
                return false;
            }
        }
        return true;
    }

    private static String buscarSequencialmente(
            int quantidadeCandidatos,
            char[][] pares,
            String hash) {
        char[] candidato = new char[pares.length];
        try {
            for (int indice = 0; indice < quantidadeCandidatos; indice++) {
                for (int posicao = 0; posicao < pares.length; posicao++) {
                    int bit = (indice >> (pares.length - posicao - 1)) & 1;
                    candidato[posicao] = pares[posicao][bit];
                }
                if (PasswordUtils.verificarSenha(candidato, hash)) {
                    return new String(candidato);
                }
            }
            return null;
        } finally {
            Arrays.fill(candidato, '\0');
        }
    }

    public boolean validarToken(
            String email,
            String senha,
            String codigoTOTP) throws SQLException {

        if (email == null || email.trim().isEmpty()
                || senha == null || codigoTOTP == null) {
            return false;
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim());

        if (usuario == null) {
            return false;
        }

        long agoraMillis = System.currentTimeMillis();

        if (usuario.estaBloqueado(agoraMillis)) {
            return false;
        }

        usuarioDAO.limparBloqueioExpirado(
            usuario.getUid(),
            agoraMillis
        );

        // Só valida o TOTP se a senha pessoal estiver correta.
        if (!PasswordUtils.verificarSenha(senha, usuario.getSenhaHash())) {
            usuarioDAO.registrarFalhaDeSenha(
                usuario.getUid(),
                agoraMillis
            );
            return false;
        }

        usuarioDAO.limparFalhasDeSenha(usuario.getUid());

        try {
            String segredoBase32 = TokenUtils.decifrar(
                usuario.getTokenCifrado(),
                senha
            );

            TOTP totp = new TOTP(segredoBase32, 30);

            if (!totp.validateCode(codigoTOTP)) {
                usuarioDAO.registrarFalhaDeToken(
                    usuario.getUid(),
                    agoraMillis
                );
                return false;
            }

            usuarioDAO.limparFalhasDeToken(usuario.getUid());
            return true;

        } catch (Exception e) {
            // Falha ao decifrar ou interpretar o segredo.
            return false;
        }
    }

}