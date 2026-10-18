package app.mainWindow;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;

import app.cadastro.CadastroModel;
import app.cadastro.CadastroPresenter;

public final class MainWindowPresenter {

  private final Connection connection;
  private final MainWindowModel model;
  private MainWindow janela;

  public MainWindowPresenter(
      Connection connection,
      MainWindowModel model) {
    this.connection = connection;
    this.model = model;
  }

  public void abrir(Window owner, Runnable aoEncerrarSessao) {
    if (janela != null) {
      throw new IllegalStateException(
          "A janela principal já foi aberta.");
    }
    janela = new MainWindow(owner, model, this);
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

  public void encerrarSessao() {
    if (janela != null) {
      janela.dispose();
    }
  }
}
