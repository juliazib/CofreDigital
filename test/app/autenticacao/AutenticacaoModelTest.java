// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.app.autenticacao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import app.autenticacao.AutenticacaoModel;

public class AutenticacaoModelTest {

  @Test
  public void deveArmazenarDadosDeLoginEPermitirLimparSenha() {
    AutenticacaoModel model = new AutenticacaoModel();
    char[][] pares = {
        { '1', '9' },
        { '2', '8' }
    };

    model.preencherLogin("usuario@example.com");
    model.preencherParesSenha(pares);
    model.preencherCodigoTotp("123456");
    pares[0][0] = '\0';

    assertEquals("usuario@example.com", model.getEmail());
    assertEquals('1', model.getParesSenha()[0][0]);
    assertEquals("123456", model.getCodigoTotp());

    model.limparSenha();
    model.limparCodigoTotp();

    assertNull(model.getParesSenha());
    assertNull(model.getCodigoTotp());
  }
}
