// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
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
import service.InicializacaoService;

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
        CadastroModel.cadastroInicial(totalUsuarios),
        fraseSecreta -> SwingUtilities.invokeLater(
            () -> iniciarAposCadastroInicial(connection, fraseSecreta)));
    cadastro.abrir(null, () -> encerrarSistema(connection));
  }

  private void iniciarAposCadastroInicial(
      Connection connection,
      char[] fraseSecreta) {
    ContextoAdministrador contexto = null;
    try {
      contexto =
          new InicializacaoService(connection).validarAdministrador(
              fraseSecreta);
    } catch (Exception erro) {
      JOptionPane.showMessageDialog(
          null,
          "O administrador foi cadastrado, mas não foi possível "
              + "iniciar sua sessão: "
              + erro.getMessage(),
          "Falha ao iniciar sessão",
          JOptionPane.ERROR_MESSAGE);
      encerrarSistema(connection);
    } finally {
      Arrays.fill(fraseSecreta, '\0');
    }
    if (contexto != null) {
      iniciarAutenticacao(connection, contexto);
    }
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

    iniciarAutenticacao(connection, contexto);
  }

  private void iniciarAutenticacao(
      Connection connection,
      ContextoAdministrador contexto) {
    Consumer<Boolean> aoEncerrarSessao = encerrarSistemaSelecionado -> {
      if (encerrarSistemaSelecionado) {
        contexto.close();
        encerrarSistema(connection);
      } else {
        SwingUtilities.invokeLater(
            () -> iniciarAutenticacao(connection, contexto));
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
        contexto.close();
        encerrarSistema(connection);
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
