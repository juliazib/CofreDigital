package test.vault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import vault.IndexEntry;
import vault.IndexShareService;
import vault.ShareEntry;

public class IndexShareServiceTest {

  @Test
  public void deveLerIndexEShareValidos() {
    List<IndexEntry> index = IndexShareService.parseIndex(ascii(
        "a1b1c1 nome1doc userA@domain grupoA\n"
            + "x1y1z1 nome9doc userZ@domain grupoA\n"));
    List<ShareEntry> share = IndexShareService.parseShare(ascii(
        "a1b1c1 1 userB@domain\n"
            + "a1b1c1 2 userC@domain\n"
            + "x1y1z1 1 userX@domain\n"));

    IndexShareService.validateReferences(index, share);

    assertEquals(2, index.size());
    assertEquals("nome1doc", index.get(0).getNomeSecreto());
    assertEquals("userA@domain", index.get(0).getDono());
    assertEquals(3, share.size());
    assertEquals(2L, share.get(1).getIdCompartilhamento());
    assertEquals("userC@domain", share.get(1).getUsuarioAutorizado());
  }

  @Test
  public void aceitaArquivosVazios() {
    assertEquals(0, IndexShareService.parseIndex(new byte[0]).size());
    assertEquals(0, IndexShareService.parseShare(new byte[0]).size());
  }

  @Test
  public void rejeitaCamposAusentesExtrasEEspacosDuplicados() {
    assertInvalidIndex("codigo nome dono\n");
    assertInvalidIndex("codigo nome dono grupo extra\n");
    assertInvalidIndex("codigo  nome dono grupo\n");
    assertInvalidShare("codigo 1\n");
    assertInvalidShare("codigo  1 usuario\n");
  }

  @Test
  public void rejeitaCaracteresForaDoFormatoAscii() {
    assertInvalid(
        "cód nome dono grupo\n".getBytes(StandardCharsets.UTF_8),
        true);
    assertInvalid(
        "codigo 1 usu\u00e1rio\n".getBytes(StandardCharsets.UTF_8),
        false);
    assertInvalidShare("codigo 1 usuario\r\n");
  }

  @Test
  public void rejeitaRegistroSemFinalDeLinha() {
    assertInvalidIndex("codigo nome dono grupo");
    assertInvalidShare("codigo 1 usuario");
  }

  @Test
  public void rejeitaCodigoDuplicadoNoIndex() {
    assertInvalidIndex("codigo nome dono grupo\ncodigo outro dono grupo\n");
  }

  @Test
  public void rejeitaIdDeCompartilhamentoInvalidoOuDuplicado() {
    assertInvalidShare("codigo 0 usuario\n");
    assertInvalidShare("codigo -1 usuario\n");
    assertInvalidShare("codigo 1 usuario\ncodigo 1 outraPessoa\n");
  }

  @Test
  public void rejeitaReferenciaDeShareSemArquivoNoIndex() {
    List<IndexEntry> index = IndexShareService.parseIndex(ascii("codigo nome dono grupo\n"));
    List<ShareEntry> share = IndexShareService.parseShare(ascii("outro 1 usuario\n"));

    assertThrows(
        IllegalArgumentException.class,
        () -> IndexShareService.validateReferences(index, share));
  }

  private static byte[] ascii(String texto) {
    return texto.getBytes(StandardCharsets.US_ASCII);
  }

  private static void assertInvalidIndex(String texto) {
    assertInvalid(ascii(texto), true);
  }

  private static void assertInvalidShare(String texto) {
    assertInvalid(ascii(texto), false);
  }

  private static void assertInvalid(byte[] conteudo, boolean index) {
    try {
      if (index) {
        IndexShareService.parseIndex(conteudo);
      } else {
        IndexShareService.parseShare(conteudo);
      }
      fail("Era esperada uma IllegalArgumentException.");
    } catch (IllegalArgumentException expected) {
      // Esperado: o conteúdo viola o formato especificado.
    }
  }
}
