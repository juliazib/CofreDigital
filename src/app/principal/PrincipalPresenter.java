package app.principal;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

import app.cadastro.CadastroModel;
import app.cadastro.CadastroPresenter;
import app.pastasegura.PastaSeguraWindow;
import database.UsuarioDAO;
import model.Usuario;
import service.AuditoriaService;
import service.ContextoAdministrador;

public final class PrincipalPresenter {

  private final Connection connection;
  private final PrincipalModel model;
  private final ContextoAdministrador contextoAdministrador;
  private final AuditoriaService auditoria;
  private final int totalAcessos;
  private PrincipalWindow janela;
  private boolean encerrarSistemaSelecionado = true;

  public PrincipalPresenter(
      Connection connection,
      PrincipalModel model) throws SQLException {
    this(connection, model, null);
  }

  public PrincipalPresenter(
      Connection connection,
      PrincipalModel model,
      ContextoAdministrador contextoAdministrador) throws SQLException {
    this.connection = connection;
    this.model = model;
    this.contextoAdministrador = contextoAdministrador;
    auditoria = AuditoriaService.getInstancia();
    totalAcessos = new UsuarioDAO(connection).contarAcessos(
        model.getUsuarioAutenticado().getUid());
  }

  public void abrir(
      Window owner,
      Consumer<Boolean> aoEncerrar) {
    if (janela != null) {
      throw new IllegalStateException(
          "A janela principal já foi aberta.");
    }
    janela = new PrincipalWindow(owner, model, this);
    janela.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent evento) {
        if (aoEncerrar != null) {
          aoEncerrar.accept(encerrarSistemaSelecionado);
        }
      }
    });
    registrarEvento(5001);
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
    registrarEvento(5002);
    int totalUsuarios;
    try {
      totalUsuarios = new UsuarioDAO(connection).contarUsuarios();
    } catch (SQLException erro) {
      throw new IllegalStateException(
          "Não foi possível consultar o total de usuários.",
          erro);
    }
    CadastroPresenter cadastro = new CadastroPresenter(
        connection,
        CadastroModel.cadastroDeUsuario(
            model.getUsuarioAutenticado(),
            totalUsuarios));
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
    registrarEvento(5003);
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

  int getTotalAcessos() {
    return totalAcessos;
  }

  public void iniciarSaida() {
    registrarEvento(5004);
    registrarEvento(8001);
  }

  public void encerrarSessao() {
    registrarEvento(8002);
    registrarEvento(1004);
    encerrarSistemaSelecionado = false;
    fecharJanela();
  }

  public void encerrarSistema() {
    registrarEvento(8003);
    registrarEvento(1004);
    encerrarSistemaSelecionado = true;
    fecharJanela();
  }

  public void voltarDaSaida() {
    registrarEvento(8004);
  }

  private void fecharJanela() {
    if (janela != null) {
      janela.dispose();
    }
  }

  private void registrarEvento(int codigo) {
    Usuario usuario = model.getUsuarioAutenticado();
    try {
      auditoria.registrar(
          connection,
          codigo,
          usuario,
          null);
    } catch (SQLException erro) {
      throw new IllegalStateException(
          "Não foi possível registrar o evento " + codigo + ".",
          erro);
    }
  }
}
