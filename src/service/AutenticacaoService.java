package service;

import java.sql.SQLException;

import crypto.PasswordUtils;
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
}