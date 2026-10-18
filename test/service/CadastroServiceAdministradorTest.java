package test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.DatabaseSchema;
import database.UsuarioDAO;
import service.CadastroService;
import service.CertificadoChaveService;
import service.ErroDeValidacao;

public class CadastroServiceAdministradorTest {

  private Connection connection;
  private CadastroService service;
  private UsuarioDAO usuarioDAO;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }
    DatabaseSchema.createTables(connection);
    service = new CadastroService(connection);
    usuarioDAO = new UsuarioDAO(connection);
  }

  @After
  public void tearDown() throws Exception {
    connection.close();
  }

  @Test
  public void deveRejeitarSenhaComSequenciaNumericaRepetida()
      throws Exception {
    ErroDeValidacao erro = assertThrows(
        ErroDeValidacao.class,
        () -> service.cadastrarAdministrador(
            "12341234".toCharArray(),
            "12341234".toCharArray(),
            null,
            null,
            "frase".toCharArray()));
    assertEquals(6003, erro.getCodigoAuditoria());
    assertEquals(0, usuarioDAO.contarUsuarios());
  }

  @Test
  public void deveRejeitarSenhaQueNaoSejaNumerica() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.cadastrarAdministrador(
            "1234abcd".toCharArray(),
            "1234abcd".toCharArray(),
            null,
            null,
            "frase".toCharArray()));
    assertEquals(0, usuarioDAO.contarUsuarios());
  }

  @Test
  public void deveRejeitarCadastroQuandoJaExisteAdministrador()
      throws Exception {
    usuarioDAO.inserir(
        "Admin existente",
        "existente@example.com",
        "hash",
        new byte[] { 1 },
        1);

    assertThrows(
        IllegalStateException.class,
        () -> service.cadastrarAdministrador(
            "12345678".toCharArray(),
            "12345678".toCharArray(),
            null,
            null,
            "frase".toCharArray()));
    assertEquals(1, usuarioDAO.contarUsuarios());
  }

  @Test
  public void deveRejeitarChavePrivadaQueNaoCorrespondeAoCertificado()
      throws Exception {
    String certificadoPem = new String(
        Files.readAllBytes(Paths.get(
            "test/resources/certificados/admin-x509.crt")),
        StandardCharsets.UTF_8);
    X509Certificate certificado =
        CertificadoChaveService.validarCertificadoPem(certificadoPem);
    KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
    gerador.initialize(2048);
    KeyPair parDiferente = gerador.generateKeyPair();

    assertFalse(CertificadoChaveService.validarParChaves(
        parDiferente.getPrivate(),
        certificado));
  }
}
