
package test.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import crypto.PasswordUtils;
import database.DatabaseSchema;
import database.UsuarioDAO;
import model.Usuario;
import service.AutenticacaoService;

public class AutenticacaoServiceTest {

  private Connection connection;
  private AutenticacaoService autenticacao;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");

    try (java.sql.Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }

    DatabaseSchema.createTables(connection);

    UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
    autenticacao = new AutenticacaoService(usuarioDAO);
  }

  @After
  public void tearDown() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  public void deveRecusarEmailNaoCadastrado() throws Exception {
    assertFalse(
        autenticacao.iniciarLogin("inexistente@example.com"));
  }

  
    @Test
    public void deveRecusarLoginDeUsuarioBloqueado() throws Exception {
    UsuarioDAO usuarioDAO = new UsuarioDAO(connection);

    int uid = usuarioDAO.inserir(
        "Usuário de Teste",
        "teste@example.com",
        "hash-ficticio",
        new byte[] { 1, 2, 3 },
        2
    );

    long agora = System.currentTimeMillis();

    usuarioDAO.registrarFalhaDeSenha(uid, agora);
    usuarioDAO.registrarFalhaDeSenha(uid, agora);
    usuarioDAO.registrarFalhaDeSenha(uid, agora);

    autenticacao = new AutenticacaoService(usuarioDAO);

    assertFalse(autenticacao.iniciarLogin("teste@example.com"));
    }

    @Test
    public void deveAceitarSenhaCorreta() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        String hash = PasswordUtils.gerarHash("12345678");

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            new byte[] { 1, 2, 3 },
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertTrue(
            autenticacao.validarSenha("teste@example.com", "12345678")
        );
    }

    @Test
    public void deveRecusarSenhaIncorreta() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        String hash = PasswordUtils.gerarHash("12345678");

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            new byte[] { 1, 2, 3 },
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertFalse(
            autenticacao.validarSenha("teste@example.com", "senhaerrada")
        );

        Usuario usuario = usuarioDAO.buscarPorEmail("teste@example.com");
        org.junit.Assert.assertEquals(1, usuario.getFalhasSenha());
    }

    @Test
    public void deveBloquearAposTresSenhasIncorretas() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        String hash = PasswordUtils.gerarHash("12345678");

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            new byte[] { 1, 2, 3 },
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertFalse(autenticacao.validarSenha("teste@example.com", "errada1"));
        assertFalse(autenticacao.validarSenha("teste@example.com", "errada2"));
        assertFalse(autenticacao.validarSenha("teste@example.com", "errada3"));

        assertFalse(
            "O login deve ser recusado enquanto o usuário estiver bloqueado.",
            autenticacao.validarSenha("teste@example.com", "12345678")
        );
    }

}
