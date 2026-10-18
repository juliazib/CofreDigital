
package test.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import crypto.PasswordUtils;

public class PasswordUtilsTest {

  @Test
  public void hashDeveTerFormatoEsperado() {
    String hash = PasswordUtils.gerarHash("12345678");

    assertEquals(60, hash.length());
    assertTrue(hash.startsWith("$2y$08$"));
  }

  @Test
  public void deveAceitarSenhaCorreta() {
    String senha = "12345678";
    String hash = PasswordUtils.gerarHash(senha);

    assertTrue(PasswordUtils.verificarSenha(senha, hash));
  }

  @Test
  public void deveVerificarSenhaEmCharArraySemAlterarConteudo() {
    char[] senha = "12345678".toCharArray();
    String hash = PasswordUtils.gerarHash("12345678");

    assertTrue(PasswordUtils.verificarSenha(senha, hash));
    assertEquals("12345678", new String(senha));
  }

  @Test
  public void deveRejeitarSenhaIncorreta() {
    String hash = PasswordUtils.gerarHash("12345678");

    assertFalse(PasswordUtils.verificarSenha("87654321", hash));
  }

  @Test
  public void hashesDaMesmaSenhaDevemSerDiferentes() {
    String hash1 = PasswordUtils.gerarHash("12345678");
    String hash2 = PasswordUtils.gerarHash("12345678");

    assertNotEquals(hash1, hash2);
    assertTrue(PasswordUtils.verificarSenha("12345678", hash1));
    assertTrue(PasswordUtils.verificarSenha("12345678", hash2));
  }

  @Test
  public void deveRejeitarHashInvalido() {
    assertFalse(PasswordUtils.verificarSenha("12345678", "hash-invalido"));
  }
}
