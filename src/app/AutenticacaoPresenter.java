package app;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import database.UsuarioDAO;
import model.Usuario;
import service.AutenticacaoService;

public final class AutenticacaoPresenter {

  public interface View {
    String solicitarLogin();
    char[] solicitarSenha();
    String solicitarCodigoTotp();
    void exibirAviso(String mensagem);
  }

  private final AutenticacaoService service;
  private final UsuarioDAO usuarioDAO;
  private final View view;

  public AutenticacaoPresenter(Connection connection, View view) {
    this.usuarioDAO = new UsuarioDAO(connection);
    this.service = new AutenticacaoService(usuarioDAO);
    this.view = view;
  }

  public Usuario autenticar() throws SQLException {
    while (true) {
      String email = view.solicitarLogin();
      if (email == null) {
        return null;
      }

      if (!service.iniciarLogin(email)) {
        view.exibirAviso(
            "Usuário não identificado ou acesso temporariamente bloqueado.");
        continue;
      }

      char[] senhaCaracteres = view.solicitarSenha();
      if (senhaCaracteres == null) {
        return null;
      }

      String senha;
      try {
        senha = new String(senhaCaracteres);
      } finally {
        Arrays.fill(senhaCaracteres, '\0');
      }

      if (!service.validarSenha(email, senha)) {
        view.exibirAviso("Senha pessoal incorreta.");
        continue;
      }

      while (true) {
        String codigo = view.solicitarCodigoTotp();
        if (codigo == null) {
          return null;
        }

        if (service.validarToken(email, senha, codigo.trim())) {
          return usuarioDAO.buscarPorEmail(email.trim());
        }

        if (!service.iniciarLogin(email)) {
          view.exibirAviso(
              "O acesso foi bloqueado por dois minutos após falhas no TOTP.");
          break;
        }

        view.exibirAviso("Código TOTP incorreto.");
      }
    }
  }
}
