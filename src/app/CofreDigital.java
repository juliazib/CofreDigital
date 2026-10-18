package app;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import app.MensagensErro;
import app.autenticacao.AutenticacaoModel;
import app.autenticacao.AutenticacaoPresenter;
import app.cadastro.CadastroModel;
import app.cadastro.CadastroPresenter;
import app.inicializacao.InicializacaoModel;
import app.inicializacao.InicializacaoPresenter;
import app.principal.PrincipalModel;
import app.principal.PrincipalPresenter;
import database.Database;
import database.DatabaseSchema;
import database.UsuarioDAO;
import model.Usuario;
import service.ContextoAdministrador;

public final class CofreDigital {

  public void iniciar() {
    Connection connection = null;
    try {
      connection = Database.getConnection();
      DatabaseSchema.createTables(connection);
      UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
      int totalUsuarios = usuarioDAO.contarUsuarios();
      if (usuarioDAO.existeAdministrador()) {
        iniciarComAdministrador(connection);
      } else {
        iniciarCadastroInicial(connection, totalUsuarios);
      }
    } catch (SQLException erro) {
      System.err.println(
          "Erro ao inicializar o banco de dados: " + erro.getMessage());
      JOptionPane.showMessageDialog(
          null,
          MensagensErro.erroAplicacao(),
          "Não foi possível iniciar",
          JOptionPane.ERROR_MESSAGE);
      fecharConexao(connection);
    }
  }

  private void iniciarCadastroInicial(
      Connection connection,
      int totalUsuarios) {
    CadastroPresenter cadastro = new CadastroPresenter(
        connection,
        CadastroModel.cadastroInicial(totalUsuarios));
    cadastro.abrir(null, () -> fecharConexao(connection));
  }

  private void iniciarComAdministrador(Connection connection) {
    InicializacaoPresenter inicializacao = new InicializacaoPresenter(
        connection,
        new InicializacaoModel());
    ContextoAdministrador contexto =
        inicializacao.validarAdministrador(null);
    if (contexto == null) {
      fecharConexao(connection);
      return;
    }

    AtomicBoolean recursosEncerrados = new AtomicBoolean();
    Runnable encerrarSessao = () -> {
      if (recursosEncerrados.compareAndSet(false, true)) {
        contexto.close();
        fecharConexao(connection);
      }
    };
    boolean sessaoIniciada = false;
    try {
      AutenticacaoPresenter autenticacao = new AutenticacaoPresenter(
          connection,
          new AutenticacaoModel());
      Usuario usuario = autenticacao.abrir(null);
      if (usuario != null) {
        PrincipalPresenter janelaPrincipal =
            new PrincipalPresenter(
                connection,
                new PrincipalModel(usuario));
        janelaPrincipal.abrir(null, encerrarSessao);
        sessaoIniciada = true;
      }
    } catch (SQLException erro) {
      JOptionPane.showMessageDialog(
          null,
          MensagensErro.erroAplicacao(),
          "Não foi possível autenticar",
          JOptionPane.ERROR_MESSAGE);
    } finally {
      if (!sessaoIniciada) {
        encerrarSessao.run();
      }
    }
  }

  private static void fecharConexao(Connection connection) {
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (SQLException erro) {
      System.err.println(
          "Erro ao fechar a conexão com o banco de dados: "
              + erro.getMessage());
    }
  }
}
