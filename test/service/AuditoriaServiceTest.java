package test.service;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
    AuditoriaService auditoria = AuditoriaService.getInstancia();
    auditoria.registrar(connection, 1003, uid, null);

    assertEquals(1, new UsuarioDAO(connection).contarAcessos(uid));
    RegistroAuditoria registro =
        new RegistroDAO(connection).listar().get(0);
    assertEquals(1003, registro.getCodigoMensagem());
    assertEquals(
        "Sessão iniciada para <login_name>.",
        registro.getMensagem());
    assertEquals(Integer.valueOf(uid), registro.getUid());
  }

  @Test
  public void contarConsultasDeveConsiderarApenasListagensDoUsuario()
      throws SQLException {
    int outroUid = new UsuarioDAO(connection).inserir(
        "Outro usuário",
        "outro@example.com",
        "hash",
        new byte[] { 2 },
        2);
    AuditoriaService auditoria = AuditoriaService.getInstancia();
    auditoria.registrar(connection, 7001, uid, null);
    auditoria.registrar(connection, 7003, uid, null);
    auditoria.registrar(connection, 7003, uid, null);
    auditoria.registrar(connection, 7003, outroUid, null);
    auditoria.registrar(connection, 7004, uid, null);

    assertEquals(2, auditoria.contarEventos(connection, uid, 7003));
  }

  @Test
  public void todosOsCodigosDoEnunciadoDevemEstarDisponiveis()
      throws SQLException {
    int[] codigos = {
        1001, 1002, 1003, 1004, 1005, 1006,
        2001, 2002, 2003, 2004, 2005,
        3001, 3002, 3003, 3004, 3005, 3006, 3007,
        4001, 4002, 4003, 4004, 4005, 4006, 4007,
        5001, 5002, 5003, 5004,
        6001, 6002, 6003, 6004, 6005, 6006, 6007, 6008, 6009, 6010,
        7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008, 7009, 7010,
        7011, 7012, 7013, 7014, 7015, 7016, 7017, 7018, 7019, 7020,
        7021,
        8001, 8002, 8003, 8004
    };
    AuditoriaService auditoria = AuditoriaService.getInstancia();

    for (int codigo : codigos) {
      auditoria.registrar(connection, codigo, uid, null);
    }

    assertEquals(codigos.length, new RegistroDAO(connection).listar().size());
  }
}
