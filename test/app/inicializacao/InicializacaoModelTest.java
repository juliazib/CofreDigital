package test.app.inicializacao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import app.inicializacao.InicializacaoModel;

public class InicializacaoModelTest {

  @Test
  public void deveConsumirFraseSecretaUmaUnicaVez() {
    InicializacaoModel model = new InicializacaoModel();
    char[] frase = "frase".toCharArray();

    model.preencherFraseSecreta(frase);
    frase[0] = '\0';

    char[] consumida = model.consumirFraseSecreta();

    assertEquals('f', consumida[0]);
    assertNull(model.consumirFraseSecreta());
    model.limpar();
  }
}
