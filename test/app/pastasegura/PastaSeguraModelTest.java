// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.app.pastasegura;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;

import app.pastasegura.PastaSeguraModel;
import model.Usuario;

public class PastaSeguraModelTest {

  @Test
  public void deveArmazenarConsultaComCopiaDefensivaDaFraseSecreta() {
    PastaSeguraModel model = new PastaSeguraModel(usuario());
    char[] frase = "frase-secreta".toCharArray();

    model.registrarConsulta(
        Paths.get("pasta"),
        frase,
        Collections.emptyList());
    frase[0] = '\0';
    char[] fraseLida = model.obterCopiaDaFraseSecreta();

    assertEquals(Paths.get("pasta"), model.getPasta());
    assertEquals('f', fraseLida[0]);
    assertEquals(0, model.getArquivos().size());
    java.util.Arrays.fill(fraseLida, '\0');

    model.limparConsulta();

    assertNull(model.getPasta());
    assertNull(model.obterCopiaDaFraseSecreta());
  }

  @Test
  public void deveRejeitarUsuarioNulo() {
    try {
      new PastaSeguraModel(null);
      throw new AssertionError("Esperava rejeição de usuário nulo.");
    } catch (IllegalArgumentException esperado) {
      assertEquals(
          "A pasta segura exige um usuário autenticado.",
          esperado.getMessage());
    }
  }

  private static Usuario usuario() {
    return new Usuario(
        1,
        "Usuário",
        "usuario@example.com",
        "hash",
        null,
        null,
        2,
        "Usuário",
        0,
        0,
        null);
  }
}
