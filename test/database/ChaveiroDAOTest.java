
package test.database;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.ChaveiroDAO;
import database.DatabaseSchema;

public class ChaveiroDAOTest {

  private Connection connection;
  private ChaveiroDAO chaveiroDAO;

  @Before
  public void setUp() throws SQLException {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    DatabaseSchema.createTables(connection);
    chaveiroDAO = new ChaveiroDAO(connection);

    // O Chaveiro referencia Usuarios; criamos um usuário de teste.
    connection.createStatement().executeUpdate(
        "INSERT INTO Usuarios " +
        "(nome, email, senha_hash, token_cifrado, KID, GID) " +
        "VALUES ('Teste', 'teste@example.com', 'hash', X'01', NULL, 2)");
  }

  @After
  public void tearDown() throws SQLException {
    connection.close();
  }

  @Test
  public void inserirEConsultarChaveiro() throws SQLException {
    int uid = 1;
    String certificado = "certificado PEM de teste";
    byte[] chaveCifrada = new byte[] { 10, 20, 30 };

    int kid = chaveiroDAO.inserir(uid, certificado, chaveCifrada);

    assertTrue(kid > 0);

    ChaveiroDAO.ChaveiroRegistro registro =
        chaveiroDAO.buscarPorUsuario(uid);

    assertNotNull(registro);
    assertEquals(kid, registro.getKid());
    assertEquals(certificado, registro.getCertificadoPem());
    assertArrayEquals(chaveCifrada, registro.getChavePrivadaCifrada());
  }

  @Test
  public void buscarUsuarioSemChaveRetornaNull() throws SQLException {
    assertNull(chaveiroDAO.buscarPorUsuario(999));
  }

  
    @Test
    public void inserirParaUsuarioInexistenteNaoDeveDeixarRegistro()
        throws SQLException {

    try {
        chaveiroDAO.inserir(
            999,
            "certificado PEM de teste",
            new byte[] { 1, 2, 3 });

        fail("Era esperado um erro para um usuário inexistente.");
    } catch (SQLException expected) {
    }

    assertNull(chaveiroDAO.buscarPorUsuario(999));
    }

}
