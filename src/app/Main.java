package app;

import java.sql.Connection;
import java.sql.SQLException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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

        CadastroWindow janela =
            new CadastroWindow(conexaoAplicacao, totalUsuarios);
        janela.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosed(WindowEvent evento) {
            fecharConexao(conexaoAplicacao);
          }
        });
        janela.setVisible(true);
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
    try (ContextoAdministrador contexto = presenter.validarAdministrador()) {
      if (contexto == null) {
        return;
      }

      SwingAutenticacaoView view = new SwingAutenticacaoView(null);
      AutenticacaoPresenter autenticacao =
          new AutenticacaoPresenter(connection, view);
      try {
        Usuario usuario = autenticacao.autenticar();
        if (usuario != null) {
          view.exibirAutenticacaoConcluida(usuario.getNome());
        }
      } catch (SQLException erro) {
        JOptionPane.showMessageDialog(
            null,
            "Erro durante a autenticação: " + erro.getMessage(),
            "Erro de autenticação",
            JOptionPane.ERROR_MESSAGE);
      }
    } finally {
      fecharConexao(connection);
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
