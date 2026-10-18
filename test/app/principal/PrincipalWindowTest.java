package test.app.principal;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.junit.Test;

import app.principal.PrincipalWindow;

public class PrincipalWindowTest {

  @Test
  public void botoesDoMenuDevemTerDimensoesFixasECentralizadas() {
    JPanel menu = new JPanel(new GridBagLayout());
    JButton botao = new JButton("Acessar pasta secreta");

    PrincipalWindow.adicionarBotaoMenu(menu, botao, 0);

    Dimension tamanhoEsperado = new Dimension(420, 42);
    assertEquals(tamanhoEsperado, botao.getPreferredSize());
    assertEquals(tamanhoEsperado, botao.getMinimumSize());
    assertEquals(tamanhoEsperado, botao.getMaximumSize());
    assertEquals(GridBagConstraints.CENTER,
        ((GridBagLayout) menu.getLayout())
            .getConstraints(botao).anchor);
  }
}
