package app.principal;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;

import app.cadastro.CadastroModel;
import app.cadastro.CadastroPresenter;
import app.pastasegura.PastaSeguraWindow;
import service.ContextoAdministrador;

public final class PrincipalPresenter {

  private final Connection connection;
  private final PrincipalModel model;
  private final ContextoAdministrador contextoAdministrador;
  private PrincipalWindow janela;

  public PrincipalPresenter(
      Connection connection,
      PrincipalModel model) {
    this(connection, model, null);
  }

  public PrincipalPresenter(
      Connection connection,
      PrincipalModel model,
      ContextoAdministrador contextoAdministrador) {
    this.connection = connection;
    this.model = model;
    this.contextoAdministrador = contextoAdministrador;
  }

  public void abrir(Window owner, Runnable aoEncerrarSessao) {
    if (janela != null) {
      throw new IllegalStateException(
          "A janela principal já foi aberta.");
    }
    janela = new PrincipalWindow(owner, model, this);
    janela.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent evento) {
        if (aoEncerrarSessao != null) {
          aoEncerrarSessao.run();
        }
      }
    });
    janela.setVisible(true);
  }

  public void abrirCadastroUsuarios() {
    if (!model.podeGerenciarUsuarios()) {
      throw new IllegalStateException(
          "Somente administradores podem gerenciar usuários.");
    }
    if (janela == null) {
      throw new IllegalStateException(
          "A janela principal ainda não foi aberta.");
    }
    CadastroPresenter cadastro = new CadastroPresenter(
        connection,
        CadastroModel.cadastroDeUsuario(
            model.getUsuarioAutenticado()));
    cadastro.abrir(janela, null);
  }

  public void consultarPastaSegura() {
    if (janela == null) {
      throw new IllegalStateException(
          "A janela principal ainda não foi aberta.");
    }
    if (contextoAdministrador == null) {
      throw new IllegalStateException(
          "O contexto do administrador não está disponível.");
    }
    PastaSeguraWindow consulta = new PastaSeguraWindow(
        janela,
        connection,
        model.getUsuarioAutenticado(),
        contextoAdministrador);
    consulta.setVisible(true);
  }

  boolean podeConsultarPastaSegura() {
    return contextoAdministrador != null;
  }

  public void encerrarSessao() {
    if (janela != null) {
      janela.dispose();
    }
  }
}
