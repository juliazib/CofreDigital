package test.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.DatabaseSchema;
import database.RegistroDAO;
import model.RegistroAuditoria;

public class RegistroDAOTest {

  private Connection connection;

  @Before
  public void setUp() throws SQLException {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (java.sql.Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }
    DatabaseSchema.createTables(connection);
    connection.createStatement().executeUpdate(
        "INSERT INTO Usuarios " +
            "(nome, email, senha_hash, token_cifrado, KID, GID) " +
            "VALUES ('Usuário', 'usuario@example.com', 'hash', X'01', NULL, 2)");
  }

  @After
  public void tearDown() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  private RegistroDAO daoNoInstante(String instante) {
    return new RegistroDAO(
        connection,
        Clock.fixed(Instant.parse(instante), ZoneOffset.UTC));
  }

  @Test
  public void deveRegistrarEventoComCodigoUsuarioDataEArquivo() throws SQLException {
    daoNoInstante("2026-10-18T19:39:06Z")
        .registrar(7001, "Arquivo descriptografado.", 1, "XXYYZZ11");

    List<RegistroAuditoria> registros = new RegistroDAO(connection).listar();

    assertEquals(1, registros.size());
    RegistroAuditoria registro = registros.get(0);
    assertEquals(1, registro.getRid());
    assertEquals(Instant.parse("2026-10-18T19:39:06Z"), registro.getDataHora());
    assertEquals(Integer.valueOf(1), registro.getUid());
    assertEquals("usuario@example.com", registro.getLoginName());
    assertEquals(7001, registro.getCodigoMensagem());
    assertEquals("Arquivo descriptografado.", registro.getMensagem());
    assertEquals("XXYYZZ11", registro.getArquivo());
  }

  @Test
  public void deveReutilizarMensagemEPermitirUsuarioEArquivoOpcionais()
      throws SQLException {
    RegistroDAO dao = daoNoInstante("2026-10-18T19:39:06Z");
    dao.registrar(1003, "Acesso realizado.", 1, null);
    dao.registrar(1003, "Acesso realizado.", null, null);

    List<RegistroAuditoria> registros = new RegistroDAO(connection).listar();

    assertEquals(2, registros.size());
    assertEquals(Integer.valueOf(1), registros.get(0).getUid());
    assertNull(registros.get(0).getArquivo());
    assertNull(registros.get(1).getUid());
    assertEquals(1003, registros.get(1).getCodigoMensagem());
    assertEquals(1, contarLinhas("Mensagens"));
  }

  @Test
  public void deveListarRegistrosEmOrdemCronologica() throws SQLException {
    daoNoInstante("2026-10-18T19:40:00Z")
        .registrar(7001, "Evento posterior.", 1, null);
    daoNoInstante("2026-10-18T19:39:00Z")
        .registrar(7002, "Evento anterior.", 1, null);

    List<RegistroAuditoria> registros = new RegistroDAO(connection).listar();

    assertEquals(2, registros.size());
    assertEquals(7002, registros.get(0).getCodigoMensagem());
    assertEquals(7001, registros.get(1).getCodigoMensagem());
  }

  @Test
  public void naoDeveAssociarDescricaoDiferenteAoMesmoCodigo()
      throws SQLException {
    RegistroDAO dao = daoNoInstante("2026-10-18T19:39:06Z");
    dao.registrar(1003, "Acesso realizado.", 1, null);

    try {
      dao.registrar(1003, "Descrição conflitante.", 1, null);
      fail("Era esperado erro para descrição conflitante do mesmo código.");
    } catch (SQLException esperado) {
      assertTrue(esperado.getMessage().contains("1003"));
    }

    assertEquals(1, new RegistroDAO(connection).listar().size());
  }

  @Test
  public void naoDeveArmazenarTextoDaMensagemEmRegistros() throws SQLException {
    boolean temColunaMensagem = false;
    boolean temColunaCodigo = false;

    try (java.sql.Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("PRAGMA table_info(Registros)")) {
      while (result.next()) {
        String coluna = result.getString("name");
        temColunaMensagem |= "mensagem".equalsIgnoreCase(coluna);
        temColunaCodigo |= "codigo".equalsIgnoreCase(coluna);
      }
    }

    assertFalse(temColunaMensagem);
    assertFalse(temColunaCodigo);
  }

  private int contarLinhas(String tabela) throws SQLException {
    try (java.sql.Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(
            "SELECT COUNT(*) FROM " + tabela)) {
      assertTrue(result.next());
      return result.getInt(1);
    }
  }
}
