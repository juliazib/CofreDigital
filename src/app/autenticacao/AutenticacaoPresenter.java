package app.autenticacao;

import java.awt.Window;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import database.UsuarioDAO;
import model.Usuario;
import service.AuditoriaService;
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
  private final AuditoriaService auditoria;
  private final Connection connection;
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
    this.connection = connection;
    auditoria = AuditoriaService.getInstancia();
    service = new AutenticacaoService(usuarioDAO, connection);
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
      registrarEvento(2001, null);
      if (!view.solicitarLogin(model)) {
        registrarEvento(2002, null);
        return null;
      }
      String email = model.getEmail() == null
          ? ""
          : model.getEmail().trim();
      model.preencherLogin(email);
      boolean loginPermitido = service.iniciarLogin(email);
      Usuario usuarioIdentificado = usuarioDAO.buscarPorEmail(email);
      registrarEvento(2002, usuarioIdentificado);
      if (!loginPermitido) {
        if (exibirSeBloqueado(email)) {
          return null;
        }
        view.exibirAviso("Usuário não identificado.");
        continue;
      }

      String senha = validarSenha(email);
      if (senha == null) {
        Usuario usuarioAposSenha = usuarioDAO.buscarPorEmail(email);
        if (usuarioAposSenha != null
            && usuarioAposSenha.estaBloqueado(System.currentTimeMillis())) {
          continue;
        }
        return null;
      }

      Usuario usuarioAutenticado = usuarioDAO.buscarPorEmail(email);
      registrarEvento(4001, usuarioAutenticado);
      while (true) {
        if (!view.solicitarCodigoTotp(model)) {
          model.limparCodigoTotp();
          registrarEvento(4002, usuarioAutenticado);
          return null;
        }
        String codigo = model.getCodigoTotp();
        boolean tokenValido = service.validarToken(
            email,
            senha,
            codigo == null ? "" : codigo.trim());
        model.limparCodigoTotp();

        if (tokenValido) {
          registrarEvento(4002, usuarioAutenticado);
          return usuarioDAO.buscarPorEmail(email);
        }
        if (exibirSeBloqueado(email)) {
          registrarEvento(4002, usuarioAutenticado);
          return null;
        }
        view.exibirAviso("Código TOTP incorreto.");
      }
    }
  }

  private String validarSenha(String email) throws SQLException {
    Usuario usuario = usuarioDAO.buscarPorEmail(email);
    registrarEvento(3001, usuario);
    try {
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
    } finally {
      registrarEvento(3002, usuario);
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

  private void registrarEvento(int codigo, Usuario usuario)
      throws SQLException {
    auditoria.registrar(connection, codigo, usuario, null);
  }
}
