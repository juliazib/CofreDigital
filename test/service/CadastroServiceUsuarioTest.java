package test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.ChaveiroDAO;
import database.DatabaseSchema;
import database.UsuarioDAO;
import model.Usuario;
import service.CadastroService;

public class CadastroServiceUsuarioTest {

  private Connection connection;
  private UsuarioDAO usuarioDAO;
  private CadastroService service;
  private int uidAdministrador;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }
    DatabaseSchema.createTables(connection);
    usuarioDAO = new UsuarioDAO(connection);
    service = new CadastroService(connection);
    uidAdministrador = usuarioDAO.inserir(
        "Administrador",
        "admin@example.com",
        "hash",
        new byte[] { 1 },
        1);
    usuarioDAO.inserir(
        "Usuário comum",
        "comum@example.com",
        "hash",
        new byte[] { 2 },
        2);
  }

  @After
  public void tearDown() throws Exception {
    connection.close();
  }

  @Test
  public void deveCadastrarUsuarioComumComCertificadoEChave()
      throws Exception {
    String certificado = new String(
        Files.readAllBytes(Paths.get(
            "test/resources/certificados/user01-x509.crt")),
        StandardCharsets.UTF_8);
    byte[] chave = Files.readAllBytes(Paths.get(
        "test/resources/certificados/user01-pkcs8-aes.key"));

    String segredo = service.cadastrarUsuario(
        uidAdministrador,
        "12345678".toCharArray(),
        "12345678".toCharArray(),
        certificado,
        chave,
        "user01".toCharArray());

    Usuario cadastrado = usuarioDAO.buscarPorEmail(
        "user01@inf1416.puc-rio.br");
    assertNotNull(cadastrado);
    assertEquals(2, cadastrado.getGid());
    assertEquals("Usuário", cadastrado.getGrupo());
    assertNotNull(cadastrado.getKid());
    assertNotNull(new ChaveiroDAO(connection).buscarPorUsuario(
        cadastrado.getUid()));
    assertEquals(20, new crypto.Base32(
        crypto.Base32.Alphabet.BASE32,
        true,
        false).fromString(segredo).length);
  }

  @Test
  public void deveRecusarCadastroSolicitadoPorUsuarioComum()
      throws Exception {
    Usuario usuarioComum = usuarioDAO.buscarPorEmail("comum@example.com");
    String certificado = new String(
        Files.readAllBytes(Paths.get(
            "test/resources/certificados/user01-x509.crt")),
        StandardCharsets.UTF_8);
    byte[] chave = Files.readAllBytes(Paths.get(
        "test/resources/certificados/user01-pkcs8-aes.key"));

    assertThrows(
        SecurityException.class,
        () -> service.cadastrarUsuario(
            usuarioComum.getUid(),
            "12345678".toCharArray(),
            "12345678".toCharArray(),
            certificado,
            chave,
            "user01".toCharArray()));
    assertEquals(2, usuarioDAO.contarUsuarios());
  }
}
