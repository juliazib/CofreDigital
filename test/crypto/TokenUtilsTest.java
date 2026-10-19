// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import crypto.TokenUtils;

public class TokenUtilsTest {

    @Test
    public void deveCifrarEDecifrarSegredo() throws Exception {
        String segredo = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        String senha = "12345678";

        byte[] cifrado = TokenUtils.cifrar(segredo, senha);
        String decifrado = TokenUtils.decifrar(cifrado, senha);

        assertEquals(segredo, decifrado);
    }

    @Test
    public void senhasDiferentesDevemGerarCifrasDiferentes()
            throws Exception {

        String segredo = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

        byte[] cifra1 = TokenUtils.cifrar(segredo, "12345678");
        byte[] cifra2 = TokenUtils.cifrar(segredo, "87654321");

        assertNotEquals(
            java.util.Arrays.toString(cifra1),
            java.util.Arrays.toString(cifra2)
        );
    }

    @Test(expected = Exception.class)
    public void deveFalharAoDecifrarComSenhaErrada() throws Exception {
        String segredo = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

        byte[] cifrado = TokenUtils.cifrar(segredo, "12345678");

        TokenUtils.decifrar(cifrado, "87654321");
    }
}