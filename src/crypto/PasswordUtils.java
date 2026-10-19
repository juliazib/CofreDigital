// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)


package crypto;

import java.security.SecureRandom;
import java.util.Arrays;

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

    char[] caracteres = senha.toCharArray();
    try {
      return verificarSenha(caracteres, hash);
    } finally {
      Arrays.fill(caracteres, '\0');
    }
  }

  public static boolean verificarSenha(char[] senha, String hash) {
    if (senha == null || hash == null) {
      return false;
    }

    try {
      return OpenBSDBCrypt.checkPassword(hash, senha);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
