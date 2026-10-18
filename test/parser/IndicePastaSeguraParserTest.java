package test.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import model.ArquivoSeguro;
import parser.IndicePastaSeguraParser;

public class IndicePastaSeguraParserTest {

  @Test
  public void deveInterpretarLinhasDoIndice() throws Exception {
    List<ArquivoSeguro> arquivos = IndicePastaSeguraParser.interpretarIndice(
        ascii("COD001 relatorio.pdf dono@example.com GrupoA\n"
            + "COD002 imagem.png outro@example.com GrupoB\n"));

    assertEquals(2, arquivos.size());
    assertEquals("COD001", arquivos.get(0).getCodigo());
    assertEquals("relatorio.pdf", arquivos.get(0).getNome());
    assertEquals("dono@example.com", arquivos.get(0).getDono());
    assertEquals("GrupoA", arquivos.get(0).getGrupo());
    assertEquals("COD002", arquivos.get(1).getCodigo());
  }

  @Test
  public void deveAceitarIndiceVazio() throws Exception {
    assertTrue(IndicePastaSeguraParser.interpretarIndice(ascii("")).isEmpty());
  }

  @Test
  public void deveRejeitarIndiceComCodigoDuplicado() {
    assertIndiceInvalido(
        "COD001 a.txt dono@example.com Grupo\n"
            + "COD001 b.txt dono@example.com Grupo");
  }

  @Test
  public void deveRejeitarIndiceComLinhaOuNomeInvalido() {
    assertIndiceInvalido("COD001 ../segredo dono@example.com Grupo");
    assertIndiceInvalido("COD001 arquivo.txt dono@example.com");
    assertIndiceInvalido("COD001  dono@example.com Grupo");
  }

  @Test
  public void deveRejeitarIndiceForaDeAscii() {
    assertGeneralSecurityException(
        () -> IndicePastaSeguraParser.interpretarIndice(
            new byte[] { (byte) 0xFF }));
  }

  @Test
  public void deveRetornarSomenteCompartilhamentosDoUsuario()
      throws Exception {
    List<ArquivoSeguro> arquivos = Arrays.asList(
        new ArquivoSeguro("COD001", "a.txt", "dono@example.com", "Grupo"),
        new ArquivoSeguro("COD002", "b.txt", "dono@example.com", "Grupo"));

    Map<String, Integer> compartilhamentos =
        IndicePastaSeguraParser.interpretarCompartilhamentos(
            ascii("COD001 7 user@example.com\n"
                + "COD002 8 outro@example.com\n"),
            arquivos,
            "USER@example.com");

    assertEquals(1, compartilhamentos.size());
    assertEquals(Integer.valueOf(7), compartilhamentos.get("COD001"));
    assertNull(compartilhamentos.get("COD002"));
  }

  @Test
  public void devePreservarPrimeiroIdQuandoShareSeRepete()
      throws Exception {
    List<ArquivoSeguro> arquivos = Arrays.asList(
        new ArquivoSeguro("COD001", "a.txt", "dono@example.com", "Grupo"));

    Map<String, Integer> compartilhamentos =
        IndicePastaSeguraParser.interpretarCompartilhamentos(
            ascii("COD001 7 user@example.com\n"
                + "COD001 8 user@example.com\n"),
            arquivos,
            "user@example.com");

    assertEquals(Integer.valueOf(7), compartilhamentos.get("COD001"));
  }

  @Test
  public void deveRejeitarShareReferenciandoCodigoAusenteOuIdInvalido()
      throws Exception {
    List<ArquivoSeguro> arquivos = Arrays.asList(
        new ArquivoSeguro("COD001", "a.txt", "dono@example.com", "Grupo"));

    assertShareInvalido(
        "COD002 7 user@example.com",
        arquivos,
        "user@example.com");
    assertShareInvalido(
        "COD001 0 user@example.com",
        arquivos,
        "user@example.com");
    assertShareInvalido(
        "COD001 999999999999999999999 user@example.com",
        arquivos,
        "user@example.com");
  }

  @Test
  public void deveRejeitarShareComLinhaMalformada() throws Exception {
    List<ArquivoSeguro> arquivos = Arrays.asList(
        new ArquivoSeguro("COD001", "a.txt", "dono@example.com", "Grupo"));

    assertShareInvalido(
        "COD001 1 user@example.com campo-extra",
        arquivos,
        "user@example.com");
  }

  @Test
  public void deveNormalizarNomesDeGrupoSemAcentosEAoIgnorarCaixa() {
    assertEquals(
        IndicePastaSeguraParser.normalizar("Usuário"),
        IndicePastaSeguraParser.normalizar("USUARIO"));
  }

  private static void assertIndiceInvalido(String indice) {
    assertGeneralSecurityException(
        () -> IndicePastaSeguraParser.interpretarIndice(ascii(indice)));
  }

  private static void assertShareInvalido(
      String share,
      List<ArquivoSeguro> arquivos,
      String emailUsuario) throws Exception {
    assertGeneralSecurityException(
        () -> IndicePastaSeguraParser.interpretarCompartilhamentos(
            ascii(share),
            arquivos,
            emailUsuario));
  }

  private static void assertGeneralSecurityException(
      AcaoComErro acao) {
    try {
      acao.executar();
      fail("Era esperada uma GeneralSecurityException.");
    } catch (GeneralSecurityException esperado) {
      assertTrue(esperado.getMessage() != null);
    } catch (Exception inesperado) {
      throw new AssertionError(inesperado);
    }
  }

  private static byte[] ascii(String valor) {
    return valor.getBytes(StandardCharsets.US_ASCII);
  }

  private interface AcaoComErro {
    void executar() throws Exception;
  }
}
