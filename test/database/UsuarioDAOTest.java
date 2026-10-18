
package test.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.DatabaseSchema;
import database.UsuarioDAO;
import model.Usuario;

public class UsuarioDAOTest {

  private Connection connection;
  private UsuarioDAO dao;

  @Before
  public void setUp() throws SQLException {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");

    try (java.sql.Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }

    DatabaseSchema.createTables(connection);
    dao = new UsuarioDAO(connection);
  }

  @After
  public void tearDown() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  private int inserirUsuarioDeTeste(String email) throws SQLException {
    return dao.inserir(
        "Usuário de Teste",
        email,
        "$2y$08$hashficticioparateste123456789012345678901234567890",
        new byte[] { 1, 2, 3 },
        2);
  }

  @Test
  public void deveIniciarSemUsuarios() throws SQLException {
    assertEquals(0, dao.contarUsuarios());
  }

  @Test
  public void deveInserirEBuscarUsuarioPorEmail() throws SQLException {
    int uid = inserirUsuarioDeTeste("teste@example.com");

    Usuario usuario = dao.buscarPorEmail("teste@example.com");

    assertNotNull(usuario);
    assertEquals(uid, usuario.getUid());
    assertEquals("Usuário de Teste", usuario.getNome());
    assertEquals("teste@example.com", usuario.getEmail());
    assertEquals("Usuário", usuario.getGrupo());
    assertNull(usuario.getKid());
  }

  @Test
  public void deveBuscarUsuarioPorId() throws SQLException {
    int uid = inserirUsuarioDeTeste("teste@example.com");

    Usuario usuario = dao.buscarPorId(uid);

    assertNotNull(usuario);
    assertEquals(uid, usuario.getUid());
  }

  @Test
  public void deveRetornarNullQuandoUsuarioNaoExiste() throws SQLException {
    assertNull(dao.buscarPorEmail("inexistente@example.com"));
    assertNull(dao.buscarPorId(999));
  }

  @Test
  public void deveDetectarEmailExistente() throws SQLException {
    inserirUsuarioDeTeste("teste@example.com");

    assertTrue(dao.existeEmail("teste@example.com"));
    assertFalse(dao.existeEmail("outro@example.com"));
  }

  @Test
  public void deveContarUsuarios() throws SQLException {
    inserirUsuarioDeTeste("um@example.com");
    inserirUsuarioDeTeste("dois@example.com");

    assertEquals(2, dao.contarUsuarios());
  }

  @Test
  public void deveBloquearAposTresFalhasDeSenha() throws SQLException {
    int uid = inserirUsuarioDeTeste("teste@example.com");
    long agora = 1_800_000_000_000L;

    dao.registrarFalhaDeSenha(uid, agora);
    Usuario primeiraFalha = dao.buscarPorId(uid);
    assertEquals(1, primeiraFalha.getFalhasSenha());
    assertNull(primeiraFalha.getBloqueadoAte());

    dao.registrarFalhaDeSenha(uid, agora);
    Usuario segundaFalha = dao.buscarPorId(uid);
    assertEquals(2, segundaFalha.getFalhasSenha());
    assertNull(segundaFalha.getBloqueadoAte());

    dao.registrarFalhaDeSenha(uid, agora);
    Usuario terceiraFalha = dao.buscarPorId(uid);

    assertEquals(0, terceiraFalha.getFalhasSenha());
    assertEquals(Long.valueOf(agora + 120_000L),
        terceiraFalha.getBloqueadoAte());
    assertTrue(terceiraFalha.estaBloqueado(agora));
    assertFalse(terceiraFalha.estaBloqueado(agora + 120_000L));
  }

  @Test
  public void deveLimparFalhasDeToken() throws SQLException {
    int uid = inserirUsuarioDeTeste("teste@example.com");
    long agora = 1_800_000_000_000L;

    dao.registrarFalhaDeToken(uid, agora);
    dao.limparFalhasDeToken(uid);

    assertEquals(0, dao.buscarPorId(uid).getFalhasToken());
  }

  @Test
  public void deveLimparBloqueioExpirado() throws SQLException {
    int uid = inserirUsuarioDeTeste("teste@example.com");
    long agora = 1_800_000_000_000L;

    dao.registrarFalhaDeSenha(uid, agora);
    dao.registrarFalhaDeSenha(uid, agora);
    dao.registrarFalhaDeSenha(uid, agora);

    dao.limparBloqueioExpirado(uid, agora + 120_000L);

    assertNull(dao.buscarPorId(uid).getBloqueadoAte());
  }

  @Test
  public void deveContarAcessosRegistrados() throws SQLException {
    int uid = inserirUsuarioDeTeste("teste@example.com");
    int outroUid = inserirUsuarioDeTeste("outro@example.com");

    try (PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO Mensagens (codigo, mensagem) VALUES (?, ?), (?, ?)")) {
      statement.setInt(1, 1003);
      statement.setString(2, "Sessão iniciada.");
      statement.setInt(3, 5001);
      statement.setString(4, "Tela principal apresentada.");
      statement.executeUpdate();
    }

    try (PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO Registros (data_hora, UID, MID) VALUES " +
            "(?, ?, (SELECT MID FROM Mensagens WHERE codigo = 1003)), " +
            "(?, ?, (SELECT MID FROM Mensagens WHERE codigo = 1003)), " +
            "(?, ?, (SELECT MID FROM Mensagens WHERE codigo = 5001))")) {
      statement.setString(1, "2026-10-16T20:00:00");
      statement.setInt(2, uid);
      statement.setString(3, "2026-10-16T21:00:00");
      statement.setInt(4, uid);
      statement.setString(5, "2026-10-16T22:00:00");
      statement.setInt(6, outroUid);
      statement.executeUpdate();
    }

    assertEquals(2, dao.contarAcessos(uid));
    assertEquals(0, dao.contarAcessos(outroUid));
  }
 
  @Test
  public void existeAdministradorDeveRetornarFalseQuandoNaoExistirAdministrador()
          throws SQLException {
      assertFalse(dao.existeAdministrador());
  }

  @Test
  public void existeAdministradorDeveRetornarTrueQuandoExistirAdministrador()
          throws SQLException {
      dao.inserir(
          "Administrador",
          "admin@example.com",
          "hash-teste",
          new byte[] { 1, 2, 3 },
          1
      );

      assertTrue(dao.existeAdministrador());
  }

}
