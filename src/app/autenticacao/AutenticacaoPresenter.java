package app.autenticacao;

import java.awt.Window;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import database.UsuarioDAO;
import model.Usuario;
import service.AutenticacaoService;

public final class AutenticacaoPresenter {

  public interface View {
    boolean solicitarLogin(AutenticacaoModel model);
    boolean solicitarSenha(AutenticacaoModel model);
    boolean solicitarCodigoTotp(AutenticacaoModel model);
    void exibirAviso(String mensagem);
  }

  private final AutenticacaoService service;
  private final UsuarioDAO usuarioDAO;
  private final AutenticacaoModel model;
  private View view;

  public AutenticacaoPresenter(
      Connection connection,
      AutenticacaoModel model) {
    this(connection, model, null);
  }

  public AutenticacaoPresenter(
      Connection connection,
      AutenticacaoModel model,
      View view) {
    usuarioDAO = new UsuarioDAO(connection);
    service = new AutenticacaoService(usuarioDAO);
    this.model = model;
    this.view = view;
  }

  public Usuario abrir(Window owner) throws SQLException {
    if (view != null) {
      throw new IllegalStateException(
          "A janela de autenticação já foi aberta.");
    }
    view = new AutenticacaoWindow(owner);
    try {
      return autenticar();
    } finally {
      limpar();
    }
  }

  public Usuario autenticar() throws SQLException {
    if (view == null) {
      throw new IllegalStateException(
          "A janela de autenticação ainda não foi criada.");
    }
    while (true) {
      if (!view.solicitarLogin(model)) {
        return null;
      }
      String email = model.getEmail() == null
          ? ""
          : model.getEmail().trim();
      model.preencherLogin(email);
      if (!service.iniciarLogin(email)) {
        if (exibirSeBloqueado(email)) {
          return null;
        }
        view.exibirAviso("Usuário não identificado.");
        continue;
      }

      String senha = validarSenha(email);
      if (senha == null) {
        return null;
      }

      while (true) {
        if (!view.solicitarCodigoTotp(model)) {
          model.limparCodigoTotp();
          return null;
        }
        String codigo = model.getCodigoTotp();
        boolean tokenValido = service.validarToken(
            email,
            senha,
            codigo == null ? "" : codigo.trim());
        model.limparCodigoTotp();

        if (tokenValido) {
          return usuarioDAO.buscarPorEmail(email);
        }
        if (exibirSeBloqueado(email)) {
          return null;
        }
        view.exibirAviso("Código TOTP incorreto.");
      }
    }
  }

  private String validarSenha(String email) throws SQLException {
    while (true) {
      if (!view.solicitarSenha(model)) {
        model.limparSenha();
        return null;
      }

      char[][] paresSenha = model.getParesSenha();
      model.limparSenha();
      if (paresSenha == null) {
        return null;
      }

      String senha;
      try {
        senha = service.validarSenhaPorPares(email, paresSenha);
      } finally {
        for (char[] par : paresSenha) {
          if (par != null) {
            Arrays.fill(par, '\0');
          }
        }
      }

      if (senha != null) {
        return senha;
      }
      if (exibirSeBloqueado(email)) {
        return null;
      }
      view.exibirAviso("Senha pessoal incorreta. Tente novamente.");
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

  private void limpar() {
    model.limparSenha();
    model.limparCodigoTotp();
  }
}
