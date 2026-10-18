package test.app.mainWindow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import app.mainWindow.MainWindowModel;
import model.Usuario;

public class MainWindowModelTest {

  @Test
  public void deveExporDadosEPermissoesDoUsuarioAutenticado() {
    MainWindowModel administrador =
        new MainWindowModel(usuario(1, "Administrador"));
    MainWindowModel usuario =
        new MainWindowModel(usuario(2, "Usuário"));

    assertEquals("Nome", administrador.getNomeUsuario());
    assertEquals("nome@example.com", administrador.getEmailUsuario());
    assertEquals("Administrador", administrador.getGrupoUsuario());
    assertTrue(administrador.podeGerenciarUsuarios());
    assertFalse(usuario.podeGerenciarUsuarios());
  }

  @Test
  public void deveRejeitarUsuarioNulo() {
    try {
      new MainWindowModel(null);
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
