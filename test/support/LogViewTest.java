package test.support;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.junit.Test;

import service.CertificadoChaveService;
import support.LogView;

public class LogViewTest {

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
