// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import model.RegistroAuditoria;
import service.CertificadoChaveService;
import support.LogView;

public class LogViewTest {

  @Test
  public void tabelaDeveExibirDataCodigoMensagemUsuarioEArquivo() {
    RegistroAuditoria registro = new RegistroAuditoria(
        1,
        Instant.parse("2026-10-18T19:39:06Z"),
        42,
        7018,
        "Arquivo <arq_name> descriptografado para <login_name>.",
        "usuario@example.com",
        "relatorio.txt");
    LogView.RegistroTableModel tabela =
        new LogView.RegistroTableModel(Collections.singletonList(registro));

    assertEquals(5, tabela.getColumnCount());
    assertEquals("Data/hora (UTC)", tabela.getColumnName(0));
    assertEquals("UID", tabela.getColumnName(1));
    assertEquals("Código", tabela.getColumnName(2));
    assertEquals("Mensagem", tabela.getColumnName(3));
    assertEquals("Arquivo selecionado", tabela.getColumnName(4));
    assertEquals("2026-10-18 19:39:06.000 UTC", tabela.getValueAt(0, 0));
    assertEquals("42", tabela.getValueAt(0, 1));
    assertEquals(7018, tabela.getValueAt(0, 2));
    assertEquals(
        "Arquivo relatorio.txt descriptografado para usuario@example.com.",
        tabela.getValueAt(0, 3));
    assertEquals("relatorio.txt", tabela.getValueAt(0, 4));
  }

  @Test
  public void tabelaDeveIndicarQuandoUsuarioOuArquivoNaoSeAplicam() {
    RegistroAuditoria registro = new RegistroAuditoria(
        1,
        Instant.parse("2026-10-18T19:39:06Z"),
        null,
        1001,
        "Sistema iniciado.",
        null,
        null);
    LogView.RegistroTableModel tabela =
        new LogView.RegistroTableModel(Collections.singletonList(registro));

    assertEquals("—", tabela.getValueAt(0, 1));
    assertEquals("—", tabela.getValueAt(0, 4));
  }

  @Test
  public void mensagemDeveExibirTracoParaMarcadoresSemDados() {
    RegistroAuditoria registro = new RegistroAuditoria(
        1,
        Instant.parse("2026-10-18T19:39:06Z"),
        null,
        7001,
        "Arquivo <arq_name> selecionado por <login_name>.",
        null,
        null);
    LogView.RegistroTableModel tabela =
        new LogView.RegistroTableModel(Collections.singletonList(registro));

    assertEquals("Arquivo — selecionado por —.", tabela.getValueAt(0, 3));
  }

  @Test
  public void deveValidarChavePrivadaDoAdministrador()
      throws Exception {
    String certificadoPem = new String(
        Files.readAllBytes(Paths.get(
            "test/resources/certificados/admin-x509.crt")),
        StandardCharsets.UTF_8);
    X509Certificate certificado =
        CertificadoChaveService.validarCertificadoPem(certificadoPem);
    byte[] chaveCifrada = Files.readAllBytes(Paths.get(
        "test/resources/certificados/admin-pkcs8-aes.key"));

    try {
      PrivateKey chavePrivada =
          CertificadoChaveService.descriptografarChavePrivada(
              chaveCifrada,
              "admin".toCharArray());

      assertTrue(LogView.validarChavePrivada(chavePrivada, certificado));
    } finally {
      Arrays.fill(chaveCifrada, (byte) 0);
    }
  }

  @Test
  public void deveRejeitarChaveQueNaoCorrespondeAoCertificado()
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

    assertFalse(LogView.validarChavePrivada(
        parDiferente.getPrivate(),
        certificado));
  }
}
