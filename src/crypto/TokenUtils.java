// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public final class TokenUtils {

    private static final String ALGORITMO = "AES/ECB/PKCS5Padding";
    private static final int TAMANHO_CHAVE = 32;

    private TokenUtils() {
    }

    private static SecretKeySpec derivarChave(String senha)
            throws Exception {

        if (senha == null || senha.isEmpty()) {
            throw new IllegalArgumentException(
                "A senha não pode ser vazia."
            );
        }

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(senha.getBytes(StandardCharsets.UTF_8));

        byte[] chave = new byte[TAMANHO_CHAVE];
        random.nextBytes(chave);

        return new SecretKeySpec(chave, "AES");
    }

    public static byte[] cifrar(String segredoBase32, String senha)
            throws Exception {

        if (segredoBase32 == null || segredoBase32.isEmpty()) {
            throw new IllegalArgumentException(
                "O segredo Base32 não pode ser vazio."
            );
        }

        SecretKeySpec chave = derivarChave(senha);

        Cipher cipher = Cipher.getInstance(ALGORITMO);
        cipher.init(Cipher.ENCRYPT_MODE, chave);

        return cipher.doFinal(
            segredoBase32.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String decifrar(byte[] tokenCifrado, String senha)
            throws Exception {

        if (tokenCifrado == null || tokenCifrado.length == 0) {
            throw new IllegalArgumentException(
                "O token cifrado não pode ser vazio."
            );
        }

        SecretKeySpec chave = derivarChave(senha);

        Cipher cipher = Cipher.getInstance(ALGORITMO);
        cipher.init(Cipher.DECRYPT_MODE, chave);

        byte[] segredo = cipher.doFinal(tokenCifrado);

        return new String(segredo, StandardCharsets.UTF_8);
    }
}