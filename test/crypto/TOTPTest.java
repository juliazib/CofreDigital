// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import crypto.Base32;
import crypto.TOTP;

public class TOTPTest {

  private final Base32 base32 = new Base32(
      Base32.Alphabet.BASE32,
      true,
      false
  );

  private final String segredo = base32.toString(
      new byte[] {
          1, 2, 3, 4, 5,
          6, 7, 8, 9, 10,
          11, 12, 13, 14, 15,
          16, 17, 18, 19, 20
      }
  );

  @Test
  public void deveGerarCodigoComSeisDigitos() throws Exception {
    TOTP totp = new TOTP(segredo, 30);

    String codigo = totp.generateCode();

    assertNotNull(codigo);
    assertTrue(codigo.matches("[0-9]{6}"));
  }

  @Test
  public void deveValidarCodigoAtual() throws Exception {
    TOTP totp = new TOTP(segredo, 30);

    String codigo = totp.generateCode();

    assertTrue(totp.validateCode(codigo));
  }

  @Test
  public void deveRejeitarCodigoIncorreto() throws Exception {
    TOTP totp = new TOTP(segredo, 30);

    assertFalse(totp.validateCode("000000"));
  }

  @Test
  public void deveRejeitarCodigoComFormatoInvalido() throws Exception {
    TOTP totp = new TOTP(segredo, 30);

    assertFalse(totp.validateCode("12345"));
    assertFalse(totp.validateCode("abcdef"));
    assertFalse(totp.validateCode(null));
  }

  @Test
  public void deveRejeitarSegredoVazio() {
    try {
      new TOTP("", 30);
    } catch (Exception e) {
      return;
    }

    throw new AssertionError("Deveria rejeitar um segredo vazio.");
  }

  @Test
  public void deveRejeitarIntervaloInvalido() {
    try {
      new TOTP(segredo, 0);
    } catch (Exception e) {
      return;
    }

    throw new AssertionError("Deveria rejeitar intervalo zero.");
  }

  @Test
  public void deveRejeitarSegredoComTamanhoInvalido() {
    String segredoCurto = base32.toString(new byte[] { 1, 2, 3 });

    try {
      new TOTP(segredoCurto, 30);
    } catch (Exception e) {
      return;
    }

    throw new AssertionError(
        "Deveria rejeitar segredo diferente de 20 bytes."
    );
  }

  @Test
  public void deveGerarCodigosConformeRFC6238() throws Exception {
    byte[] segredoRFC =
        "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    String segredoBase32 = base32.toString(segredoRFC);
    TOTP totp = new TOTP(segredoBase32, 30);

    assertEquals("287082", totp.generateCode(59));
    assertEquals("081804", totp.generateCode(1111111109));
    assertEquals("050471", totp.generateCode(1111111111));
    assertEquals("005924", totp.generateCode(1234567890));
    assertEquals("279037", totp.generateCode(2000000000));
    assertEquals("353130", totp.generateCode(20000000000L));
  }

  @Test
  public void deveAceitarCodigoDoIntervaloAnterior() throws Exception {
    TOTP totp = new TOTP(segredo, 30);

    String codigoAnterior = totp.generateCode(0);

    assertTrue(totp.validateCode(codigoAnterior, 30));
  }

  @Test
  public void deveAceitarCodigoDoProximoIntervalo() throws Exception {
    TOTP totp = new TOTP(segredo, 30);

    String codigoSeguinte = totp.generateCode(60);

    assertTrue(totp.validateCode(codigoSeguinte, 30));
  }

  @Test
  public void deveRejeitarCodigoForaDaJanelaDeTolerancia()
      throws Exception {

    TOTP totp = new TOTP(segredo, 30);

    String codigoDistante = totp.generateCode(120);

    assertFalse(totp.validateCode(codigoDistante, 30));
  }
}