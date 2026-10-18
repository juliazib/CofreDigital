package test.app.ui.componentes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.Test;

import app.ui.componentes.CampoTexto;
import app.ui.componentes.EntradaSenhaVirtual;
import app.ui.componentes.CampoSenha;

public class CamposSwingTest {

  static {
    System.setProperty("javax.accessibility.assistive_technologies", "");
  }

  @Test
  public void campoTextoDeveRespeitarLimiteDeCaracteres() {
    CampoTexto campo = new CampoTexto(10, 5);
    campo.setText("12345");
    campo.setCaretPosition(campo.getDocument().getLength());
    campo.replaceSelection("6");

    assertEquals("12345", campo.getText());
  }

  @Test
  public void campoSenhaDeveRespeitarLimiteELimparConteudo() {
    CampoSenha campo = new CampoSenha(10, 4);
    campo.setText("1234");
    campo.setCaretPosition(campo.getDocument().getLength());
    campo.replaceSelection("5");
    assertEquals(4, campo.getPassword().length);

    campo.limpar();

    assertTrue(campo.getPassword().length == 0);
  }

  @Test
  public void senhaVirtualDeveImpedirDigitacaoPeloTecladoFisico() {
    EntradaSenhaVirtual entrada = new EntradaSenhaVirtual(8, 10);

    assertFalse(contemCampoTexto(entrada));
    assertEquals(0, entrada.getParesSelecionados().length);
  }

  @Test
  public void senhaVirtualDeveRejeitarLimitesInvalidos() {
    try {
      new EntradaSenhaVirtual(11, 10);
      fail("Esperava rejeição de limites inválidos.");
    } catch (IllegalArgumentException esperado) {
      assertEquals(
          "Os limites da senha virtual são inválidos.",
          esperado.getMessage());
    }
  }

  @Test
  public void tecladoVirtualDeveExibirParesUnicosEAtualizarMascara() {
    EntradaSenhaVirtual entrada = new EntradaSenhaVirtual(1, 2);
    List<JButton> botoes = new ArrayList<>();
    coletarBotoes(entrada, botoes);
    assertEquals(7, botoes.size());
    assertEquals(5, contarPares(botoes));
    validarDigitosUnicos(botoes);

    JButton primeiroPar = primeiroBotaoPar(botoes);
    String esperado = primeiroPar.getActionCommand();
    primeiroPar.doClick();
    assertEquals(
        esperado.replace(" ", ""),
        new String(entrada.getParesSelecionados()[0]));
    assertTrue(encontrarMascara(entrada).getText().contains("\u2022"));
    validarDigitosUnicos(botoes);

    JButton segundoPar = primeiroBotaoPar(botoes);
    String segundoEsperado = segundoPar.getActionCommand();
    segundoPar.doClick();
    char[][] pares = entrada.getParesSelecionados();
    assertEquals(2, pares.length);
    assertEquals(
        segundoEsperado.replace(" ", ""),
        new String(pares[1]));

    JButton limpar = botoes.stream()
        .filter(botao -> botao.getText().equals("LIMPAR"))
        .findFirst()
        .orElseThrow(AssertionError::new);
    JLabel mascara = encontrarMascara(entrada);
    assertTrue(mascara.getText().contains("\u2022"));
    limpar.doClick();

    assertEquals(0, entrada.getParesSelecionados().length);
    assertEquals("", mascara.getText());
    validarDigitosUnicos(botoes);
  }

  private static JButton primeiroBotaoPar(List<JButton> botoes) {
    return botoes.stream()
        .filter(CamposSwingTest::ehPar)
        .findFirst()
        .orElseThrow(AssertionError::new);
  }

  @Test
  public void tecladoVirtualDevePreservarCadaParSelecionado() {
    EntradaSenhaVirtual entrada = new EntradaSenhaVirtual(8, 10);
    List<JButton> botoes = new ArrayList<>();
    coletarBotoes(entrada, botoes);
    List<String> paresEsperados = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      JButton botaoPar = primeiroBotaoPar(botoes);
      paresEsperados.add(botaoPar.getActionCommand());
      botaoPar.doClick();
    }

    char[][] obtidos = entrada.getParesSelecionados();
    assertEquals(8, obtidos.length);
    for (int i = 0; i < obtidos.length; i++) {
      assertEquals(
          paresEsperados.get(i).replace(" ", ""),
          new String(obtidos[i]));
    }
    entrada.limpar();
  }

  private static boolean contemCampoTexto(Component componente) {
    if (componente instanceof JTextField) {
      return true;
    }
    if (componente instanceof Container) {
      for (Component filho : ((Container) componente).getComponents()) {
        if (contemCampoTexto(filho)) {
          return true;
        }
      }
    }
    return false;
  }

  private static void coletarBotoes(
      Component componente,
      List<JButton> botoes) {
    if (componente instanceof JButton) {
      botoes.add((JButton) componente);
    }
    if (componente instanceof Container) {
      for (Component filho : ((Container) componente).getComponents()) {
        coletarBotoes(filho, botoes);
      }
    }
  }

  private static void validarDigitosUnicos(List<JButton> botoes) {
    Set<String> digitos = new HashSet<>();
    for (JButton botao : botoes) {
      String valor = botao.getActionCommand();
      if (ehPar(botao)) {
        digitos.addAll(java.util.Arrays.asList(valor.split(" ")));
      }
    }
    assertEquals(10, digitos.size());
  }

  private static int contarPares(Container container) {
    List<JButton> botoes = new ArrayList<>();
    coletarBotoes(container, botoes);
    return contarPares(botoes);
  }

  private static int contarPares(List<JButton> botoes) {
    return (int) botoes.stream()
        .filter(CamposSwingTest::ehPar)
        .count();
  }

  private static boolean ehPar(JButton botao) {
    String acao = botao.getActionCommand();
    return acao != null && acao.matches("\\d \\d");
  }

  private static JLabel encontrarMascara(Container container) {
    for (Component componente : container.getComponents()) {
      if (componente instanceof JLabel
          && "senhaMascarada".equals(componente.getName())) {
        return (JLabel) componente;
      }
      if (componente instanceof Container) {
        JLabel encontrada = encontrarMascara((Container) componente);
        if (encontrada != null) {
          return encontrada;
        }
      }
    }
    return null;
  }
}
