package service;

import java.sql.SQLException;
import java.sql.Connection;
import java.util.Arrays;

import crypto.PasswordUtils;
import crypto.TOTP;
import crypto.TokenUtils;
import database.UsuarioDAO;
import model.Usuario;

public class AutenticacaoService {

    private final UsuarioDAO usuarioDAO;
    private final Connection connection;
    private final AuditoriaService auditoria;

    public AutenticacaoService(
            UsuarioDAO usuarioDAO,
            Connection connection) {
        if (usuarioDAO == null || connection == null) {
            throw new IllegalArgumentException(
                "O DAO e a conexão são obrigatórios.");
        }
        this.usuarioDAO = usuarioDAO;
        this.connection = connection;
        auditoria = AuditoriaService.getInstancia();
    }

    public boolean iniciarLogin(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            registrar(2005, null);
            return false;
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim());

        if (usuario == null) {
            registrar(2005, null);
            return false;
        }

        long agora = System.currentTimeMillis();

        if (usuario.estaBloqueado(agora)) {
            registrar(2004, usuario);
            return false;
        }

        usuarioDAO.limparBloqueioExpirado(usuario.getUid(), agora);
        registrar(2003, usuario);
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
            registrar(3007, usuario);
            return false;
        }

        usuarioDAO.limparBloqueioExpirado(usuario.getUid(), agora);

        if (!PasswordUtils.verificarSenha(senha, usuario.getSenhaHash())) {
            registrarFalhaDeSenha(usuario, agora);
            return false;
        }

        usuarioDAO.limparFalhasDeSenha(usuario.getUid());
        registrar(3003, usuario);
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
            registrar(3007, usuario);
            return null;
        }
        usuarioDAO.limparBloqueioExpirado(usuario.getUid(), agora);

        if (!paresValidos(pares)) {
            registrarFalhaDeSenha(usuario, agora);
            return null;
        }

        String senhaValida = buscarSequencialmente(
            1 << pares.length,
            pares,
            usuario.getSenhaHash());
        if (senhaValida == null) {
            registrarFalhaDeSenha(usuario, agora);
            return null;
        }
        usuarioDAO.limparFalhasDeSenha(usuario.getUid());
        registrar(3003, usuario);
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
            registrar(4007, usuario);
            return false;
        }

        usuarioDAO.limparBloqueioExpirado(
            usuario.getUid(),
            agoraMillis
        );

        // Só valida o TOTP se a senha pessoal estiver correta.
        if (!PasswordUtils.verificarSenha(senha, usuario.getSenhaHash())) {
            registrarFalhaDeSenha(usuario, agoraMillis);
            return false;
        }

        usuarioDAO.limparFalhasDeSenha(usuario.getUid());

        String segredoBase32;
        TOTP totp;
        try {
            segredoBase32 = TokenUtils.decifrar(
                usuario.getTokenCifrado(),
                senha);
            totp = new TOTP(segredoBase32, 30);
        } catch (Exception erroCriptografico) {
            registrarFalhaDeToken(usuario, agoraMillis);
            return false;
        }

        if (!totp.validateCode(codigoTOTP)) {
            registrarFalhaDeToken(usuario, agoraMillis);
            return false;
        }

        usuarioDAO.limparFalhasDeToken(usuario.getUid());
        registrar(4003, usuario);
        return true;
    }

    private void registrarFalhaDeSenha(Usuario usuario, long agoraMillis)
            throws SQLException {
        int falhasAntes = usuario.getFalhasSenha();
        usuarioDAO.registrarFalhaDeSenha(usuario.getUid(), agoraMillis);
        registrar(3004 + Math.min(falhasAntes, 2), usuario);
        if (falhasAntes >= 2) {
            registrar(3007, usuario);
        }
    }

    private void registrarFalhaDeToken(Usuario usuario, long agoraMillis)
            throws SQLException {
        int falhasAntes = usuario.getFalhasToken();
        usuarioDAO.registrarFalhaDeToken(usuario.getUid(), agoraMillis);
        registrar(4004 + Math.min(falhasAntes, 2), usuario);
        if (falhasAntes >= 2) {
            registrar(4007, usuario);
        }
    }

    private void registrar(int codigo, Usuario usuario) throws SQLException {
        auditoria.registrar(connection, codigo, usuario, null);
    }

}