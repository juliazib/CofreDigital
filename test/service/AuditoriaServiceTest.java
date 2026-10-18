package test.service;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.DatabaseSchema;
import database.RegistroDAO;
import database.UsuarioDAO;
import model.RegistroAuditoria;
import service.AuditoriaService;

public class AuditoriaServiceTest {

  private Connection connection;
  private int uid;

  @Before
  public void setUp() throws SQLException {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (java.sql.Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }
    DatabaseSchema.createTables(connection);
    uid = new UsuarioDAO(connection).inserir(
        "Usuário",
        "usuario@example.com",
        "hash",
        new byte[] { 1 },
        2);
  }

  @After
  public void tearDown() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  public void registrarAcessoDeveCriarEventoQueAlimentaContadorDeAcessos()
      throws SQLException {
    RegistroDAO registroDAO = new RegistroDAO(
        connection,
        Clock.fixed(
            Instant.parse("2026-10-18T19:39:06Z"),
            ZoneOffset.UTC));

    new AuditoriaService(registroDAO).registrarAcesso(uid);

    assertEquals(1, new UsuarioDAO(connection).contarAcessos(uid));
    RegistroAuditoria registro = registroDAO.listar().get(0);
    assertEquals(1003, registro.getCodigoMensagem());
    assertEquals(
        "Sessão iniciada para <login_name>.",
        registro.getMensagem());
    assertEquals(Integer.valueOf(uid), registro.getUid());
  }
}
