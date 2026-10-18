package test.app.ui.componentes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;

import org.junit.Test;

import app.ui.componentes.CampoSenha;
import app.ui.componentes.CampoTexto;
import app.ui.componentes.EntradaSenhaVirtual;

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
    assertEquals(0, entrada.getPassword().length);
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
    assertEquals(12, botoes.size());
    assertEquals(5, contarPares(entrada));
    validarDigitosUnicos(botoes);

    JButton digito = botoes.stream()
        .filter(botao -> botao.getActionCommand().matches("\\d"))
        .findFirst()
        .orElseThrow(AssertionError::new);
    char esperado = digito.getActionCommand().charAt(0);
    digito.doClick();

    assertEquals(esperado, entrada.getPassword()[0]);
    assertTrue(encontrarMascara(entrada).getText().contains("\u2022"));

    JButton limpar = botoes.stream()
        .filter(botao -> botao.getText().equals("LIMPAR"))
        .findFirst()
        .orElseThrow(AssertionError::new);
    JLabel mascara = encontrarMascara(entrada);
    assertTrue(mascara.getText().contains("\u2022"));
    limpar.doClick();

    assertEquals(0, entrada.getPassword().length);
    assertEquals("", mascara.getText());
    validarDigitosUnicos(botoes);
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
      if (valor != null && valor.matches("\\d")) {
        digitos.add(valor);
      }
    }
    assertEquals(10, digitos.size());
  }

  private static int contarPares(Container container) {
    int quantidade = container instanceof JComponent
        ? contarBordaPar(((JComponent) container).getBorder())
        : 0;
    for (Component componente : container.getComponents()) {
      if (componente instanceof Container) {
        quantidade += contarPares((Container) componente);
      }
    }
    return quantidade;
  }

  private static int contarBordaPar(javax.swing.border.Border border) {
    if (border instanceof EtchedBorder) {
      return 1;
    }
    if (border instanceof CompoundBorder) {
      CompoundBorder composta = (CompoundBorder) border;
      return contarBordaPar(composta.getOutsideBorder())
          + contarBordaPar(composta.getInsideBorder());
    }
    return 0;
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
