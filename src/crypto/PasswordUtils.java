
package crypto;

import java.security.SecureRandom;

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

public final class PasswordUtils {

  private static final int COST = 8;

  private PasswordUtils() {
  }

  public static String gerarHash(String senha) {
    if (senha == null) {
      throw new IllegalArgumentException("A senha não pode ser nula.");
    }

    byte[] salt = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(salt);

    return OpenBSDBCrypt.generate(
        "2y",
        senha.toCharArray(),
        salt,
        COST);
  }

  public static boolean verificarSenha(String senha, String hash) {
    if (senha == null || hash == null) {
      return false;
    }

    try {
      return OpenBSDBCrypt.checkPassword(hash, senha.toCharArray());
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
