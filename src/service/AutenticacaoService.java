package service;

import java.sql.SQLException;

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