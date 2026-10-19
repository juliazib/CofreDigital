// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.app.principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import app.principal.PrincipalModel;
import model.Usuario;

public class PrincipalModelTest {

  @Test
  public void deveExporDadosEPermissoesDoUsuarioAutenticado() {
    PrincipalModel administrador =
        new PrincipalModel(usuario(1, "Administrador"));
    PrincipalModel usuario =
        new PrincipalModel(usuario(2, "Usuário"));

    assertEquals("Nome", administrador.getNomeUsuario());
    assertEquals("nome@example.com", administrador.getEmailUsuario());
    assertEquals("Administrador", administrador.getGrupoUsuario());
    assertTrue(administrador.podeGerenciarUsuarios());
    assertFalse(usuario.podeGerenciarUsuarios());
  }

  @Test
  public void deveRejeitarUsuarioNulo() {
    try {
      new PrincipalModel(null);
      fail("Esperava rejeição de usuário nulo.");
    } catch (IllegalArgumentException esperado) {
      assertEquals(
          "A janela principal exige um usuário autenticado.",
          esperado.getMessage());
    }
  }

  private static Usuario usuario(int gid, String grupo) {
    return new Usuario(
        1,
        "Nome",
        "nome@example.com",
        "hash",
        null,
        null,
        gid,
        grupo,
        0,
        0,
        null);
  }
}
