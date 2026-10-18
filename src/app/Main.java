package app;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import app.cadastro.CadastroModel;
import app.cadastro.CadastroPresenter;
import database.Database;
import database.DatabaseSchema;
import database.UsuarioDAO;
import model.Usuario;
import service.ContextoAdministrador;

public class Main {
  public static void main(String[] args) {
    Connection connection = null;
    try {
      connection = Database.getConnection();
      DatabaseSchema.createTables(connection);
      UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
      int totalUsuarios = usuarioDAO.contarUsuarios();
      boolean administradorCadastrado = usuarioDAO.existeAdministrador();
      Connection conexaoAplicacao = connection;

      SwingUtilities.invokeLater(() -> {
        if (administradorCadastrado) {
          iniciarComAdministrador(conexaoAplicacao);
          return;
        }

        CadastroPresenter cadastro = new CadastroPresenter(
            conexaoAplicacao,
            CadastroModel.cadastroInicial(totalUsuarios));
        cadastro.abrir(null, () -> fecharConexao(conexaoAplicacao));
      });
    } catch (SQLException e) {
      System.err.println(
          "Erro ao inicializar o banco de dados: " + e.getMessage());
      fecharConexao(connection);
    }
  }

  private static void iniciarComAdministrador(Connection connection) {
    InicializacaoPresenter presenter = new InicializacaoPresenter(
        connection,
        new SwingInicializacaoView(null));
    ContextoAdministrador contexto = presenter.validarAdministrador();
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
      SwingAutenticacaoView view = new SwingAutenticacaoView(null);
      AutenticacaoPresenter autenticacao =
          new AutenticacaoPresenter(connection, view);
      Usuario usuario = autenticacao.autenticar();
      if (usuario != null) {
        JanelaPrincipal janela =
            new JanelaPrincipal(usuario, connection, encerrarSessao);
        janela.setVisible(true);
        sessaoIniciada = true;
      }
    } catch (SQLException erro) {
      JOptionPane.showMessageDialog(
          null,
          "Erro durante a autenticação: " + erro.getMessage(),
          "Erro de autenticação",
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
