package app;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
import service.AuditoriaService;
import service.ContextoAdministrador;

public final class CofreDigital {

  public void iniciar() {
    Connection connection = null;
    try {
      connection = Database.getConnection();
      DatabaseSchema.createTables(connection);
      registrarEvento(connection, 1001, null);
      UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
      int totalUsuarios = usuarioDAO.contarUsuarios();
      if (usuarioDAO.existeAdministrador()) {
        registrarEvento(connection, 1006, null);
        iniciarComAdministrador(connection);
      } else {
        registrarEvento(connection, 1005, null);
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
      encerrarSistema(connection);
    }
  }

  private void iniciarCadastroInicial(
      Connection connection,
      int totalUsuarios) {
    CadastroPresenter cadastro = new CadastroPresenter(
        connection,
        CadastroModel.cadastroInicial(totalUsuarios));
    cadastro.abrir(null, () -> encerrarSistema(connection));
  }

  private void iniciarComAdministrador(Connection connection) {
    InicializacaoPresenter inicializacao = new InicializacaoPresenter(
        connection,
        new InicializacaoModel());
    ContextoAdministrador contexto =
        inicializacao.validarAdministrador(null);
    if (contexto == null) {
      encerrarSistema(connection);
      return;
    }

    AtomicBoolean recursosEncerrados = new AtomicBoolean();
    Runnable encerrarSistemaSeNecessario = () -> {
      if (recursosEncerrados.compareAndSet(false, true)) {
        contexto.close();
        encerrarSistema(connection);
      }
    };
    Consumer<Boolean> aoEncerrarSessao = encerrarSistemaSelecionado -> {
      if (recursosEncerrados.compareAndSet(false, true)) {
        contexto.close();
        if (encerrarSistemaSelecionado) {
          encerrarSistema(connection);
        } else {
          SwingUtilities.invokeLater(
              () -> iniciarComAdministrador(connection));
        }
      }
    };
    boolean sessaoIniciada = false;
    try {
      AutenticacaoPresenter autenticacao = new AutenticacaoPresenter(
          connection,
          new AutenticacaoModel());
      Usuario usuario = autenticacao.abrir(null);
      if (usuario != null) {
        try {
          AuditoriaService.getInstancia().registrar(
              connection,
              1003,
              usuario.getUid(),
              null);
        } catch (SQLException erro) {
          System.err.println(
              "Erro ao registrar o início da sessão: " + erro.getMessage());
          JOptionPane.showMessageDialog(
              null,
              "Não foi possível registrar o acesso. A sessão não será iniciada.",
              "Auditoria indisponível",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        PrincipalPresenter janelaPrincipal =
            new PrincipalPresenter(
                connection,
                new PrincipalModel(usuario),
                contexto);
        janelaPrincipal.abrir(null, aoEncerrarSessao);
        sessaoIniciada = true;
      }
    } catch (SQLException erro) {
      JOptionPane.showMessageDialog(
          null,
          MensagensErro.erroAplicacao(),
          "Não foi possível autenticar",
          JOptionPane.ERROR_MESSAGE);
    } catch (IllegalStateException erro) {
      JOptionPane.showMessageDialog(
          null,
          erro.getMessage(),
          "Falha de auditoria",
          JOptionPane.ERROR_MESSAGE);
    } finally {
      if (!sessaoIniciada) {
        encerrarSistemaSeNecessario.run();
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

  private static void registrarEvento(
      Connection connection,
      int codigo,
      Integer uid) throws SQLException {
    AuditoriaService.getInstancia().registrar(
        connection,
        codigo,
        uid,
        null);
  }

  private static void encerrarSistema(Connection connection) {
    if (connection == null) {
      return;
    }
    try {
      registrarEvento(connection, 1002, null);
    } catch (SQLException erro) {
      System.err.println(
          "Erro ao registrar o encerramento do sistema: "
              + erro.getMessage());
      JOptionPane.showMessageDialog(
          null,
          "Não foi possível registrar o encerramento do sistema: "
              + erro.getMessage(),
          "Falha de auditoria",
          JOptionPane.ERROR_MESSAGE);
    } finally {
      fecharConexao(connection);
    }
  }
}
