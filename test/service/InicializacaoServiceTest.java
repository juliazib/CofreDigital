// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.ChaveiroDAO;
import database.DatabaseSchema;
import database.UsuarioDAO;
import service.ContextoAdministrador;
import service.InicializacaoService;

public class InicializacaoServiceTest {

  private Connection connection;
  private UsuarioDAO usuarioDAO;
  private InicializacaoService service;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }
    DatabaseSchema.createTables(connection);
    usuarioDAO = new UsuarioDAO(connection);
    service = new InicializacaoService(connection);
  }

  @After
  public void tearDown() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  public void deveSolicitarCadastroQuandoAdministradorNaoExiste()
      throws Exception {
    assertTrue(service.deveCadastrarAdministrador());
  }

  @Test
  public void usuarioComumNaoImpedeCadastroInicialDoAdministrador()
      throws Exception {
    usuarioDAO.inserir(
        "Usuário",
        "usuario@example.com",
        "hash",
        new byte[] { 1 },
        2);

    assertTrue(service.deveCadastrarAdministrador());
  }

  @Test
  public void deveValidarFraseEManterContextoEmMemoria() throws Exception {
    inserirAdministrador();
    char[] frase = "admin".toCharArray();

    try (ContextoAdministrador contexto = service.validarAdministrador(frase)) {
      Arrays.fill(frase, '\0');

      assertFalse(service.deveCadastrarAdministrador());
      assertNotNull(contexto.getChavePrivada());
      assertNotNull(contexto.getCertificado());
    }
  }

  @Test
  public void deveRejeitarFraseIncorreta() throws Exception {
    inserirAdministrador();

    assertThrows(
        Exception.class,
        () -> service.validarAdministrador("incorreta".toCharArray()));
  }

  @Test
  public void deveRejeitarFraseVazia() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validarAdministrador(new char[0]));
  }

  @Test
  public void deveEncerrarContextoEAfastarAcessoPosterior()
      throws Exception {
    inserirAdministrador();
    ContextoAdministrador contexto = service.validarAdministrador("admin".toCharArray());

    contexto.close();

    assertThrows(
        IllegalStateException.class,
        contexto::getChavePrivada);
    assertThrows(
        IllegalStateException.class,
        contexto::getCertificado);
  }

  private void inserirAdministrador() throws Exception {
    byte[] chaveCifrada = Files.readAllBytes(Paths.get(
        "test/resources/certificados/admin-pkcs8-aes.key"));
    String certificadoPem = new String(
        Files.readAllBytes(Paths.get(
            "test/resources/certificados/admin-x509.crt")),
        StandardCharsets.UTF_8);
    int uid = usuarioDAO.inserir(
        "Administrador",
        "admin@inf1416.puc-rio.br",
        "hash",
        new byte[] { 1 },
        1);
    new ChaveiroDAO(connection).inserir(
        uid,
        certificadoPem,
        chaveCifrada);
  }
}
