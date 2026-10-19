// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.SQLException;

import org.junit.Test;

import app.MensagensErro;
import service.ErroDeValidacao;

public class MensagensErroTest {

  @Test
  public void cadastroPreservaValidacoesConhecidasESuprimeErrosTecnicos() {
    assertEquals(
        "O certificado selecionado é inválido.",
        MensagensErro.cadastro(
            new ErroDeValidacao("O certificado selecionado é inválido.")));

    String mensagem = MensagensErro.cadastro(
        new IllegalArgumentException("ASN.1 parsing error: secret"));
    assertEquals(
        "Não foi possível concluir o cadastro. Confira os dados e os "
            + "arquivos selecionados e tente novamente.",
        mensagem);
    assertFalse(mensagem.contains("ASN.1"));
    assertFalse(mensagem.contains("secret"));
  }

  @Test
  public void errosDeBancoEInicializacaoNaoExibemDetalhesDaException() {
    String mensagemBanco = MensagensErro.cadastro(
        new SQLException("SQLITE_CONSTRAINT: dado interno"));
    String mensagemInicializacao = MensagensErro.inicializacao(
        new Exception("InvalidKeyException: detalhe interno"));

    assertFalse(mensagemBanco.contains("SQLITE_CONSTRAINT"));
    assertFalse(mensagemBanco.contains("dado interno"));
    assertFalse(mensagemInicializacao.contains("InvalidKeyException"));
    assertFalse(mensagemInicializacao.contains("detalhe interno"));
  }
}
