// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.app.cadastro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import app.cadastro.CadastroModel;
import model.Usuario;

public class CadastroModelTest {

  @Test
  public void deveRepresentarCadastroInicialDoAdministrador() {
    CadastroModel model = CadastroModel.cadastroInicial(0);

    assertTrue(model.isCadastroInicial());
    assertEquals("Total de usuários do sistema: 0", model.getDescricao());
    assertEquals("Administrador", model.getGrupoNovoUsuario());
    assertThrows(IllegalStateException.class, model::getUidAdministrador);
  }

  @Test
  public void deveRepresentarCadastroDeUsuarioPorAdministrador() {
    Usuario administrador = new Usuario(
        7,
        "Admin",
        "admin@example.com",
        "hash",
        new byte[] { 1 },
        1,
        1,
        "Administrador",
        0,
        0,
        null);

    CadastroModel model = CadastroModel.cadastroDeUsuario(administrador, 4);

    assertFalse(model.isCadastroInicial());
    assertEquals(7, model.getUidAdministrador());
    assertEquals("Total de usuários do sistema: 4", model.getDescricao());
    assertEquals("admin@example.com", model.getLoginAdministrador());
    assertEquals("Admin", model.getNomeAdministrador());
    assertEquals("Administrador", model.getGrupoAdministrador());
    assertEquals(4, model.getTotalUsuarios());
    assertEquals("Usuário", model.getGrupoNovoUsuario());
    model.registrarUsuarioCadastrado();
    assertEquals("Total de usuários do sistema: 5", model.getDescricao());
  }

  @Test
  public void devePermitirSelecionarGrupoNoCadastroDeUsuario() {
    Usuario administrador = new Usuario(
        7,
        "Admin",
        "admin@example.com",
        "hash",
        new byte[] { 1 },
        1,
        1,
        "Administrador",
        0,
        0,
        null);
    CadastroModel model = CadastroModel.cadastroDeUsuario(administrador, 2);

    model.setGrupoNovoUsuario("Administrador");

    assertEquals("Administrador", model.getGrupoNovoUsuario());
    assertEquals(1, model.getGidNovoUsuario());
    model.setGrupoNovoUsuario("Usuário");
    assertEquals(2, model.getGidNovoUsuario());
  }

  @Test
  public void deveRejeitarGrupoDesconhecidoNoCadastro() {
    CadastroModel model = CadastroModel.cadastroInicial(0);

    assertThrows(
        IllegalArgumentException.class,
        () -> model.setGrupoNovoUsuario("Outro"));
  }

  @Test
  public void deveRejeitarCadastroEmNomeDeUsuarioComum() {
    Usuario usuario = new Usuario(
        8,
        "Usuário",
        "user@example.com",
        "hash",
        new byte[] { 1 },
        2,
        2,
        "Usuário",
        0,
        0,
        null);

    assertThrows(
        IllegalArgumentException.class,
        () -> CadastroModel.cadastroDeUsuario(usuario, 2));
  }

  @Test
  public void deveArmazenarCredenciaisEmCopiaEPermitirLimpeza() {
    CadastroModel model = CadastroModel.cadastroInicial(0);
    char[] senha = "12345678".toCharArray();
    char[] confirmacao = "12345678".toCharArray();
    char[] fraseSecreta = "frase".toCharArray();

    model.preencherFormulario(
        "/tmp/certificado.crt",
        "/tmp/chave.key",
        senha,
        confirmacao,
        fraseSecreta);

    senha[0] = '\0';
    char[] senhaLida = model.getSenha();
    assertEquals('1', senhaLida[0]);
    senhaLida[0] = '\0';
    assertEquals('1', model.getSenha()[0]);

    model.limparFormulario();

    assertNull(model.getCaminhoCertificado());
    assertNull(model.getCaminhoChavePrivada());
    assertNull(model.getSenha());
    assertNull(model.getConfirmacaoSenha());
    assertNull(model.getFraseSecreta());
  }
}
