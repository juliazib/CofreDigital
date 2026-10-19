// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.app.cadastro;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;

import org.junit.Test;

import app.cadastro.CadastroWindow;

public class CadastroWindowTest {

  @Test
  public void tamanhoInicialDeveCaberNoConteudoSemExcederAreaUtil()
      {
    Dimension preferido = new Dimension(920, 740);

    assertEquals(
        preferido,
        CadastroWindow.calcularTamanhoInicial(
            preferido,
            new Dimension(1600, 900)));
  }

  @Test
  public void tamanhoInicialDeveSerLimitadoPelaAreaUtilDoMonitor() {
    assertEquals(
        new Dimension(1200, 700),
        CadastroWindow.calcularTamanhoInicial(
            new Dimension(1400, 800),
            new Dimension(1200, 700)));
  }
}
