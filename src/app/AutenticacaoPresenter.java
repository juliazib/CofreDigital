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
    char[] solicitarSenha(String email);
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
      String login = view.solicitarLogin();
      if (login == null) {
        return null;
      }
      String email = login.trim();
      if (!service.iniciarLogin(email)) {
        if (exibirSeBloqueado(email)) {
          return null;
        }
        view.exibirAviso("Usuário não identificado.");
        continue;
      }

      String senha = null;
      boolean senhaValida = false;
      while (!senhaValida) {
        char[] senhaCaracteres = view.solicitarSenha(email);
        if (senhaCaracteres == null) {
          return null;
        }

        try {
          senha = new String(senhaCaracteres);
        } finally {
          Arrays.fill(senhaCaracteres, '\0');
        }

        senhaValida = service.validarSenha(email, senha);
        if (!senhaValida) {
          if (exibirSeBloqueado(email)) {
            return null;
          }
          view.exibirAviso("Senha pessoal incorreta. Tente novamente.");
        }
      }

      while (true) {
        String codigo = view.solicitarCodigoTotp();
        if (codigo == null) {
          return null;
        }

        if (service.validarToken(email, senha, codigo.trim())) {
          return usuarioDAO.buscarPorEmail(email);
        }

        if (exibirSeBloqueado(email)) {
          return null;
        }
        view.exibirAviso("Código TOTP incorreto.");
      }
    }
  }

  private boolean exibirSeBloqueado(String email) throws SQLException {
    Usuario usuario = usuarioDAO.buscarPorEmail(email);
    if (usuario != null
        && usuario.estaBloqueado(System.currentTimeMillis())) {
      view.exibirAviso(
          "Acesso bloqueado por dois minutos após três tentativas incorretas.");
      return true;
    }
    return false;
  }
}
