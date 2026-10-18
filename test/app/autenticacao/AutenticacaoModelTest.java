package test.app.autenticacao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import app.autenticacao.AutenticacaoModel;

public class AutenticacaoModelTest {

  @Test
  public void deveArmazenarDadosDeLoginEPermitirLimparSenha() {
    AutenticacaoModel model = new AutenticacaoModel();
    char[] senha = "12345678".toCharArray();

    model.preencherLogin("usuario@example.com");
    model.preencherSenha(senha);
    model.preencherCodigoTotp("123456");
    senha[0] = '\0';

    assertEquals("usuario@example.com", model.getEmail());
    assertEquals('1', model.getSenha()[0]);
    assertEquals("123456", model.getCodigoTotp());

    model.limparSenha();
    model.limparCodigoTotp();

    assertNull(model.getSenha());
    assertNull(model.getCodigoTotp());
  }
}
