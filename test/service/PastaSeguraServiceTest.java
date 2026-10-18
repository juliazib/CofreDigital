package test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.ChaveiroDAO;
import database.DatabaseSchema;
import database.RegistroDAO;
import database.UsuarioDAO;
import model.ArquivoSeguro;
import model.Usuario;
import service.ContextoAdministrador;
import service.InicializacaoService;
import service.PastaSeguraService;

public class PastaSeguraServiceTest {

  private static final Path ARQUIVOS = Paths.get("test/resources/files");
  private static final Path CERTIFICADOS =
      Paths.get("test/resources/certificados");

  private Connection connection;
  private UsuarioDAO usuarioDAO;
  private ContextoAdministrador contextoAdministrador;
  private PastaSeguraService service;
  private Usuario administrador;
  private Usuario usuario01;
  private Usuario usuario02;
  private Path pastaTemporaria;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }
    DatabaseSchema.createTables(connection);
    usuarioDAO = new UsuarioDAO(connection);

    cadastrarUsuario(
        "Administrador",
        "admin@inf1416.puc-rio.br",
        "admin",
        1);
    cadastrarUsuario(
        "Usuario 01",
        "user01@inf1416.puc-rio.br",
        "user01",
        2);
    cadastrarUsuario(
        "Usuario 02",
        "user02@inf1416.puc-rio.br",
        "user02",
        2);
    administrador =
        usuarioDAO.buscarPorEmail("admin@inf1416.puc-rio.br");
    usuario01 = usuarioDAO.buscarPorEmail("user01@inf1416.puc-rio.br");
    usuario02 = usuarioDAO.buscarPorEmail("user02@inf1416.puc-rio.br");

    contextoAdministrador = new InicializacaoService(connection)
        .validarAdministrador("admin".toCharArray());
    service = new PastaSeguraService(connection, contextoAdministrador);
    pastaTemporaria = copiarArquivosDeIndice();
  }

  @After
  public void tearDown() throws Exception {
    if (contextoAdministrador != null) {
      contextoAdministrador.close();
    }
    if (connection != null) {
      connection.close();
    }
    if (pastaTemporaria != null) {
      try (java.util.stream.Stream<Path> caminhos =
          Files.walk(pastaTemporaria)) {
        caminhos.sorted(Comparator.reverseOrder())
            .forEach(caminho -> {
              try {
                Files.delete(caminho);
              } catch (java.io.IOException erro) {
                throw new java.io.UncheckedIOException(erro);
              }
            });
      }
    }
  }

  @Test
  public void deveListarArquivosDoProprioGrupoECompartilhados()
      throws Exception {
    List<ArquivoSeguro> arquivos =
        service.listarArquivos(pastaTemporaria, usuario01, "user01".toCharArray());

    assertEquals(2, arquivos.size());
    assertTrue(contemCodigo(arquivos, "XXYYZZ11"));
    assertTrue(contemCodigo(arquivos, "XXYYZZ22"));
    assertFalse(contemCodigo(arquivos, "XXYYZZ00"));
    assertEquals(
        Integer.valueOf(1),
        idDoCompartilhamento(arquivos, "XXYYZZ22"));
  }

  @Test
  public void deveRegistrarEventosDeSucessoAoListarArquivos()
      throws Exception {
    service.listarArquivos(
        pastaTemporaria,
        usuario01,
        "user01".toCharArray());

    assertEquals(1, contarEvento(usuario01, 7005));
    assertEquals(1, contarEvento(usuario01, 7006));
    assertEquals(1, contarEvento(usuario01, 7010));
    assertEquals(1, contarEvento(usuario01, 7011));
    assertEquals(1, contarEvento(usuario01, 7009));
    assertEquals(1, contarEvento(usuario01, 7014));
  }

  @Test
  public void deveRegistrarFalhaDeVerificacaoDoIndiceAdulterado()
      throws Exception {
    Path assinatura = pastaTemporaria.resolve("index.asd");
    byte[] original = Files.readAllBytes(assinatura);
    Files.write(assinatura, new byte[] { 1, 2, 3 });

    assertThrows(
        GeneralSecurityException.class,
        () -> service.listarArquivos(
            pastaTemporaria,
            usuario01,
            "user01".toCharArray()));

    Files.write(assinatura, original);
    assertEquals(1, contarEvento(usuario01, 7005));
    assertEquals(1, contarEvento(usuario01, 7008));
    assertEquals(0, contarEvento(usuario01, 7006));
  }

  @Test
  public void deveRegistrarEventosDeAcessoEDescriptografiaDoArquivo()
      throws Exception {
    service.descriptografarArquivo(
        pastaTemporaria,
        usuario01,
        "user01".toCharArray(),
        "XXYYZZ22");

    assertEquals(1, contarEvento(usuario01, 7016));
    assertEquals(1, contarEvento(usuario01, 7018));
    assertEquals(1, contarEvento(usuario01, 7019));
  }

  @Test
  public void deveRegistrarAcessoNegadoParaArquivoSemEnvelopeDoUsuario()
      throws Exception {
    assertThrows(
        SecurityException.class,
        () -> service.descriptografarArquivo(
            pastaTemporaria,
            usuario02,
            "user02".toCharArray(),
            "XXYYZZ11"));

    assertEquals(1, contarEvento(usuario02, 7017));
    assertEquals(0, contarEvento(usuario02, 7016));
  }

  @Test
  public void deveRegistrarCaminhoInvalidoDaPastaSegura() throws Exception {
    assertThrows(
        IOException.class,
        () -> service.listarArquivos(
            pastaTemporaria.resolve("inexistente"),
            usuario01,
            "user01".toCharArray()));

    assertEquals(1, contarEvento(usuario01, 7004));
  }

  private int contarEvento(Usuario usuario, int codigo) throws Exception {
    return new RegistroDAO(connection).contarEventos(
        usuario.getUid(),
        codigo);
  }

  @Test
  public void deveRejeitarFraseSecretaIncorretaAntesDaListagem() {
    assertThrows(
        GeneralSecurityException.class,
        () -> service.listarArquivos(
            pastaTemporaria,
            usuario01,
            "frase-errada".toCharArray()));
  }

  @Test
  public void deveRejeitarIndiceComAssinaturaAdulterada() throws Exception {
    Path assinatura = pastaTemporaria.resolve("index.asd");
    byte[] bytes = Files.readAllBytes(assinatura);
    bytes[0] ^= 0x01;
    Files.write(assinatura, bytes);

    assertThrows(
        GeneralSecurityException.class,
        () -> service.listarArquivos(
            pastaTemporaria,
            usuario01,
            "user01".toCharArray()));
  }

  @Test
  public void deveRejeitarShareComAssinaturaAdulterada() throws Exception {
    Path assinatura = pastaTemporaria.resolve("share.asd");
    byte[] bytes = Files.readAllBytes(assinatura);
    bytes[0] ^= 0x01;
    Files.write(assinatura, bytes);

    assertThrows(
        GeneralSecurityException.class,
        () -> service.listarArquivos(
            pastaTemporaria,
            usuario01,
            "user01".toCharArray()));

    assertEquals(1, contarEvento(usuario01, 7010));
    assertEquals(1, contarEvento(usuario01, 7013));
    assertEquals(0, contarEvento(usuario01, 7011));
  }

  @Test
  public void deveDescriptografarArquivoCompartilhadoComUsuario()
      throws Exception {
    Path arquivo = service.descriptografarArquivo(
        pastaTemporaria,
        usuario01,
        "user01".toCharArray(),
        "XXYYZZ22");

    assertEquals("teste02.docx", arquivo.getFileName().toString());
    assertTrue(Files.size(arquivo) > 0);
  }

  @Test
  public void deveDescriptografarArquivoProprioComEnvelopePessoal()
      throws Exception {
    Path arquivo = service.descriptografarArquivo(
        pastaTemporaria,
        usuario01,
        "user01".toCharArray(),
        "XXYYZZ11");

    assertEquals("teste01.docx", arquivo.getFileName().toString());
    assertTrue(Files.size(arquivo) > 0);
  }

  @Test
  public void deveDescriptografarArquivoDoAdministrador()
      throws Exception {
    Path arquivo = service.descriptografarArquivo(
        pastaTemporaria,
        administrador,
        "admin".toCharArray(),
        "XXYYZZ00");

    assertEquals("teste00.docx", arquivo.getFileName().toString());
    assertTrue(Files.size(arquivo) > 0);
  }

  @Test
  public void deveNegarDescriptografiaSemEnvelopeDoUsuario()
      throws Exception {
    List<ArquivoSeguro> arquivosDoGrupo =
        service.listarArquivos(
            pastaTemporaria,
            usuario02,
            "user02".toCharArray());
    assertTrue(contemCodigo(arquivosDoGrupo, "XXYYZZ11"));

    assertThrows(
        SecurityException.class,
        () -> service.descriptografarArquivo(
            pastaTemporaria,
            usuario02,
            "user02".toCharArray(),
            "XXYYZZ11"));
  }

  @Test
  public void deveRejeitarArquivoComAssinaturaAdulterada()
      throws Exception {
    Path assinatura = pastaTemporaria.resolve("XXYYZZ22.asd");
    byte[] bytes = Files.readAllBytes(assinatura);
    bytes[0] ^= 0x01;
    Files.write(assinatura, bytes);

    assertThrows(
        PastaSeguraService.FalhaIntegridade.class,
        () -> service.descriptografarArquivo(
            pastaTemporaria,
            usuario01,
            "user01".toCharArray(),
            "XXYYZZ22"));

    assertEquals(1, contarEvento(usuario01, 7021));
    assertEquals(0, contarEvento(usuario01, 7019));
  }

  @Test
  public void deveRejeitarCriptogramaAdulterado() throws Exception {
    Path criptograma = pastaTemporaria.resolve("XXYYZZ22.enc");
    byte[] bytes = Files.readAllBytes(criptograma);
    bytes[0] ^= 0x01;
    Files.write(criptograma, bytes);

    assertThrows(
        GeneralSecurityException.class,
        () -> service.descriptografarArquivo(
            pastaTemporaria,
            usuario01,
            "user01".toCharArray(),
            "XXYYZZ22"));

    assertEquals(1, contarEvento(usuario01, 7021));
    assertEquals(1, contarEvento(usuario01, 7018));
  }

  @Test
  public void deveRegistrarFalhaQuandoCriptogramaNaoPodeSerDecriptado()
      throws Exception {
    Files.write(
        pastaTemporaria.resolve("XXYYZZ22.enc"),
        new byte[] { 1 });

    assertThrows(
        PastaSeguraService.FalhaDescriptografia.class,
        () -> service.descriptografarArquivo(
            pastaTemporaria,
            usuario01,
            "user01".toCharArray(),
            "XXYYZZ22"));

    assertEquals(1, contarEvento(usuario01, 7020));
    assertEquals(0, contarEvento(usuario01, 7018));
  }

  @Test
  public void deveClassificarEnvelopeAusenteComoFalhaDeSigilo()
      throws Exception {
    Files.delete(pastaTemporaria.resolve("XXYYZZ11.env"));

    assertThrows(
        PastaSeguraService.FalhaDescriptografia.class,
        () -> service.descriptografarArquivo(
            pastaTemporaria,
            usuario01,
            "user01".toCharArray(),
            "XXYYZZ11"));

    assertEquals(1, contarEvento(usuario01, 7020));
    assertEquals(0, contarEvento(usuario01, 7017));
  }

  @Test
  public void naoDeveSobrescreverArquivoDescriptografadoExistente()
      throws Exception {
    Path destino = pastaTemporaria.resolve("teste02.docx");
    byte[] conteudoExistente = new byte[] { 4, 5, 6 };
    Files.write(destino, conteudoExistente);

    assertThrows(
        IOException.class,
        () -> service.descriptografarArquivo(
            pastaTemporaria,
            usuario01,
            "user01".toCharArray(),
            "XXYYZZ22"));
    assertTrue(java.util.Arrays.equals(
        conteudoExistente,
        Files.readAllBytes(destino)));
  }

  @Test
  public void deveRejeitarCertificadoQueNaoPertenceAoUsuario()
      throws Exception {
    cadastrarUsuario(
        "Identidade incorreta",
        "outro@example.com",
        "user01",
        2);
    Usuario usuarioComCertificadoIncorreto =
        usuarioDAO.buscarPorEmail("outro@example.com");

    assertThrows(
        GeneralSecurityException.class,
        () -> service.listarArquivos(
            pastaTemporaria,
            usuarioComCertificadoIncorreto,
            "user01".toCharArray()));
  }

  private int cadastrarUsuario(
      String nome,
      String email,
      String prefixoCertificado,
      int grupo) throws Exception {
    String hash = crypto.PasswordUtils.gerarHash("12345678");
    int uid = usuarioDAO.inserir(
        nome,
        email,
        hash,
        new byte[] { 1 },
        grupo);
    String certificado = new String(Files.readAllBytes(
        CERTIFICADOS.resolve(prefixoCertificado + "-x509.crt")),
        java.nio.charset.StandardCharsets.UTF_8);
    byte[] chave = Files.readAllBytes(
        CERTIFICADOS.resolve(prefixoCertificado + "-pkcs8-aes.key"));
    new ChaveiroDAO(connection).inserir(uid, certificado, chave);
    return uid;
  }

  private Path copiarArquivosDeIndice() throws Exception {
    Path destino = Files.createTempDirectory("cofre-indice-");
    for (String nome : new String[] {
        "index.enc", "index.env", "index.asd",
        "share.enc", "share.env", "share.asd",
        "XXYYZZ00.enc", "XXYYZZ00.env", "XXYYZZ00.asd",
        "XXYYZZ11.enc", "XXYYZZ11.env", "XXYYZZ11.asd",
        "XXYYZZ22.enc", "XXYYZZ22.asd", "XXYYZZ22.1.shr"
    }) {
      Files.copy(ARQUIVOS.resolve(nome), destino.resolve(nome));
    }
    return destino;
  }

  private static boolean contemCodigo(
      List<ArquivoSeguro> arquivos,
      String codigo) {
    return arquivos.stream().anyMatch(
        arquivo -> codigo.equals(arquivo.getCodigo()));
  }

  private static Integer idDoCompartilhamento(
      List<ArquivoSeguro> arquivos,
      String codigo) {
    return arquivos.stream()
        .filter(arquivo -> codigo.equals(arquivo.getCodigo()))
        .findFirst()
        .orElseThrow(AssertionError::new)
        .getIdCompartilhamento();
  }
}
