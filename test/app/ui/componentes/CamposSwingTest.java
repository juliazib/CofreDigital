package test.app.ui.componentes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.ui.componentes.CampoSenha;
import app.ui.componentes.CampoTexto;

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
}
