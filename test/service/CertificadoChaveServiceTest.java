package test.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;

import org.junit.Test;

import crypto.CertificadoUtils;
import service.CertificadoChaveService;

public class CertificadoChaveServiceTest {

  private static final String DIR_CERTIFICADOS = "resources/certificados/";

  @Test
  public void testValidarCertificadoAdmin() throws Exception {
    Path caminhoCert = Paths.get(DIR_CERTIFICADOS, "admin-x509.crt");

    if (!Files.exists(caminhoCert)) {
      caminhoCert = Paths.get("../resources/certificados/admin-x509.crt");
      if (!Files.exists(caminhoCert)) {
        caminhoCert = Paths.get("src/test/resources/certificados/admin-x509.crt");
        if (!Files.exists(caminhoCert)) {
          return; // Ignora se o ficheiro não estiver presente no ambiente atual
        }
      }
    }

    // Compatível com Java 8 para ler o ficheiro PEM como String
    String certificadoPem = new String(Files.readAllBytes(caminhoCert), StandardCharsets.UTF_8);
    X509Certificate certificado = CertificadoChaveService.validarCertificadoPem(certificadoPem);

    assertNotNull("O certificado não deve ser nulo.", certificado);

    String nome = CertificadoUtils.extrairNome(certificado);
    String email = CertificadoUtils.extrairEmail(certificado);

    assertNotNull("O nome (CN) do certificado deve ser extraído.", nome);
    assertNotNull("O e-mail do certificado deve ser extraído.", email);
  }

  @Test
  public void testDescriptografarChaveSenhaIncorreta() throws Exception {
    Path caminhoChave = Paths.get(DIR_CERTIFICADOS, "admin-pkcs8-aes.key");

    if (!Files.exists(caminhoChave)) {
      caminhoChave = Paths.get("../resources/certificados/admin-pkcs8-aes.key");
      if (!Files.exists(caminhoChave)) {
        caminhoChave = Paths.get("src/test/resources/certificados/admin-pkcs8-aes.key");
        if (!Files.exists(caminhoChave)) {
          return;
        }
      }
    }

    byte[] chaveCifrada = Files.readAllBytes(caminhoChave);

    // Tentativa de descriptografia com senha inválida deve lançar exceção
    assertThrows(Exception.class, () -> {
      CertificadoChaveService.descriptografarChavePrivada(chaveCifrada, "senhaIncorretaXYZ");
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidarCertificadoNulo() throws Exception {
    CertificadoChaveService.validarCertificadoPem(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidarCertificadoEmBranco() throws Exception {
    CertificadoChaveService.validarCertificadoPem("   ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDescriptografarChaveNula() throws Exception {
    CertificadoChaveService.descriptografarChavePrivada(null, "senha");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDescriptografarChaveVazia() throws Exception {
    CertificadoChaveService.descriptografarChavePrivada(new byte[0], "senha");
  }
}