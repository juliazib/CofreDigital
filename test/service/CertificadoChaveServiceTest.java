package test.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Test;

import crypto.CertificadoUtils;
import service.CertificadoChaveService;

public class CertificadoChaveServiceTest {

  private static final String DIR_CERTIFICADOS =
      "test/resources/certificados/";

  @Test
  public void testValidarCertificadoAdmin() throws Exception {
    String certificadoPem = new String(
        Files.readAllBytes(Paths.get(
            DIR_CERTIFICADOS + "admin-x509.crt")),
        StandardCharsets.UTF_8);
    X509Certificate certificado =
        CertificadoChaveService.validarCertificadoPem(certificadoPem);

    assertNotNull(certificado);
    assertNotNull(CertificadoUtils.extrairNome(certificado));
    assertTrue(CertificadoUtils.extrairEmail(certificado).contains("@"));
  }

  @Test
  public void testDescriptografarChaveSenhaIncorreta() throws Exception {
    byte[] chaveCifrada = Files.readAllBytes(Paths.get(
        DIR_CERTIFICADOS + "admin-pkcs8-aes.key"));

    assertThrows(
        Exception.class,
        () -> CertificadoChaveService.descriptografarChavePrivada(
            chaveCifrada,
            "senhaIncorretaXYZ"));
  }

  @Test
  public void deveDescriptografarChavePkcs8Binaria() throws Exception {
    char[] fraseSecreta = "frase-de-teste".toCharArray();
    KeyPair parChaves = gerarParChaves();
    byte[] conteudoPem = criarPemPkcs8(parChaves);
    byte[] chaveCifrada = cifrarChaveDeTeste(conteudoPem, fraseSecreta);

    try {
      assertArrayEquals(
          parChaves.getPrivate().getEncoded(),
          CertificadoChaveService.descriptografarChavePrivada(
              chaveCifrada,
              fraseSecreta).getEncoded());
    } finally {
      Arrays.fill(fraseSecreta, '\0');
      Arrays.fill(conteudoPem, (byte) 0);
      Arrays.fill(chaveCifrada, (byte) 0);
    }
  }

  @Test
  public void deveRejeitarFraseIncorretaParaChaveBinaria() throws Exception {
    char[] fraseCorreta = "frase-de-teste".toCharArray();
    char[] fraseIncorreta = "outra-frase".toCharArray();
    byte[] conteudoPem = criarPemPkcs8(gerarParChaves());
    byte[] chaveCifrada = cifrarChaveDeTeste(conteudoPem, fraseCorreta);

    try {
      assertThrows(
          Exception.class,
          () -> CertificadoChaveService.descriptografarChavePrivada(
              chaveCifrada,
              fraseIncorreta));
    } finally {
      Arrays.fill(fraseCorreta, '\0');
      Arrays.fill(fraseIncorreta, '\0');
      Arrays.fill(conteudoPem, (byte) 0);
      Arrays.fill(chaveCifrada, (byte) 0);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidarCertificadoNulo() throws Exception {
    CertificadoChaveService.validarCertificadoPem(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidarCertificadoEmBranco() throws Exception {
    CertificadoChaveService.validarCertificadoPem("   ");
  }

  @Test
  public void certificadoAleatorioRetornaMensagemAmigavelSemDetalhesTecnicos() {
    try {
      CertificadoChaveService.validarCertificadoPem(
          "texto aleatório que não é certificado");
    } catch (IllegalArgumentException erro) {
      assertEquals(
          "O certificado selecionado é inválido, expirou ou ainda não é "
              + "válido.",
          erro.getMessage());
      return;
    } catch (Exception erro) {
      throw new AssertionError(
          "A validação deveria falhar com mensagem controlada.", erro);
    }
    throw new AssertionError("Era esperado que o certificado fosse rejeitado.");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDescriptografarChaveNula() throws Exception {
    CertificadoChaveService.descriptografarChavePrivada(null, "senha");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDescriptografarChaveVazia() throws Exception {
    CertificadoChaveService.descriptografarChavePrivada(new byte[0], "senha");
  }

  private static KeyPair gerarParChaves() throws Exception {
    KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
    gerador.initialize(2048);
    return gerador.generateKeyPair();
  }

  private static byte[] criarPemPkcs8(KeyPair parChaves) {
    String codificada = Base64.getMimeEncoder(
        64,
        new byte[] { '\n' }).encodeToString(
            parChaves.getPrivate().getEncoded());
    return ("-----BEGIN PRIVATE KEY-----\n"
        + codificada
        + "\n-----END PRIVATE KEY-----\n")
            .getBytes(StandardCharsets.US_ASCII);
  }

  private static byte[] cifrarChaveDeTeste(
      byte[] texto,
      char[] fraseSecreta) throws Exception {
    ByteBuffer codificado =
        StandardCharsets.UTF_8.encode(CharBuffer.wrap(fraseSecreta));
    byte[] semente = new byte[codificado.remaining()];
    codificado.get(semente);
    try {
      SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
      random.setSeed(semente);
      KeyGenerator geradorChave = KeyGenerator.getInstance("AES");
      geradorChave.init(256, random);
      SecretKey chave = geradorChave.generateKey();

      Cipher cifra = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cifra.init(Cipher.ENCRYPT_MODE, chave);
      return cifra.doFinal(texto);
    } finally {
      Arrays.fill(semente, (byte) 0);
      if (codificado.hasArray()) {
        Arrays.fill(codificado.array(), (byte) 0);
      }
    }
  }
}
