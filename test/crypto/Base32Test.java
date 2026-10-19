// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import crypto.Base32;

public class Base32Test {

  private final Base32 base32 = new Base32(
      Base32.Alphabet.BASE32,
      true,
      false);

  @Test
  public void deveCodificarTextoConformeRFC4648() {
    assertEquals("MY======", base32.toString(
        "f".getBytes(StandardCharsets.US_ASCII)));

    assertEquals("MZXQ====", base32.toString(
        "fo".getBytes(StandardCharsets.US_ASCII)));

    assertEquals("MZXW6===", base32.toString(
        "foo".getBytes(StandardCharsets.US_ASCII)));

    assertEquals("MZXW6YQ=", base32.toString(
        "foob".getBytes(StandardCharsets.US_ASCII)));

    assertEquals("MZXW6YTB", base32.toString(
        "fooba".getBytes(StandardCharsets.US_ASCII)));

    assertEquals("MZXW6YTBOI======", base32.toString(
        "foobar".getBytes(StandardCharsets.US_ASCII)));
  }

  @Test
  public void deveDecodificarTextoConformeRFC4648() {
    assertEquals("f", new String(
        base32.fromString("MY======"), StandardCharsets.US_ASCII));

    assertEquals("fo", new String(
        base32.fromString("MZXQ===="), StandardCharsets.US_ASCII));

    assertEquals("foo", new String(
        base32.fromString("MZXW6==="), StandardCharsets.US_ASCII));

    assertEquals("foob", new String(
        base32.fromString("MZXW6YQ="), StandardCharsets.US_ASCII));

    assertEquals("fooba", new String(
        base32.fromString("MZXW6YTB"), StandardCharsets.US_ASCII));

    assertEquals("foobar", new String(
        base32.fromString("MZXW6YTBOI======"),
        StandardCharsets.US_ASCII));
  }

  @Test
  public void devePreservarBytesAoCodificarEDecodificar() {
    byte[] original = {
        0, 1, 2, 127, (byte) 128, (byte) 255
    };

    assertArrayEquals(
        original,
        base32.fromString(base32.toString(original)));
  }

  @Test
  public void deveCodificarEntradaVazia() {
    assertEquals("", base32.toString(new byte[0]));
  }

  @Test
  public void deveDecodificarEntradaVazia() {
    assertArrayEquals(new byte[0], base32.fromString(""));
  }
}
