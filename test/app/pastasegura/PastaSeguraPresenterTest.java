package test.app.pastasegura;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import org.junit.Test;

import app.pastasegura.PastaSeguraPresenter;
import service.PastaSeguraService;

public class PastaSeguraPresenterTest {

  @Test
  public void deveExibirMensagemClaraParaFalhaDeDescriptografia() {
    String mensagem = PastaSeguraPresenter.mensagemErroDescriptografia(
        new PastaSeguraService.FalhaDescriptografia(
            "erro técnico", new GeneralSecurityException()));

    assertEquals(
        "Falha de sigilo: não foi possível descriptografar o arquivo. "
            + "Verifique se o arquivo criptografado e o envelope estão "
            + "disponíveis, íntegros e correspondem a este usuário.",
        mensagem);
  }

  @Test
  public void deveExibirMensagemClaraParaFalhaDeAssinatura() {
    String mensagem = PastaSeguraPresenter.mensagemErroDescriptografia(
        new PastaSeguraService.FalhaIntegridade(
            "erro técnico", new GeneralSecurityException()));

    assertEquals(
        "Falha de integridade/autenticidade: não foi possível validar "
            + "a assinatura digital ou a identidade do proprietário. O "
            + "arquivo não foi salvo.",
        mensagem);
  }

  @Test
  public void deveInformarAcessoNegadoSemExporDetalhesTecnicos() {
    String mensagem = PastaSeguraPresenter.mensagemErroDescriptografia(
        new SecurityException("caminho interno"));

    assertEquals(
        "Você não tem permissão para acessar este arquivo.",
        mensagem);
  }

  @Test
  public void deveInformarDestinoExistenteSemExporDetalhesTecnicos() {
    String mensagem = PastaSeguraPresenter.mensagemErroDescriptografia(
        new PastaSeguraService.DestinoJaExiste(
            "caminho interno", new IOException()));

    assertEquals(
        "O arquivo não foi salvo porque já existe um arquivo com esse nome. "
            + "Nenhum arquivo foi sobrescrito.",
        mensagem);
  }

  @Test
  public void deveUsarMensagemGenericaParaErroDeAcessoAoArquivo() {
    String mensagem = PastaSeguraPresenter.mensagemErroDescriptografia(
        new IOException("detalhe do sistema de arquivos"));

    assertEquals(
        "Ocorreu uma falha ao acessar ou salvar os arquivos da pasta "
            + "segura. Verifique a pasta e tente novamente.",
        mensagem);
  }

  @Test
  public void deveUsarMensagemGenericaParaErroDeAuditoria() {
    String mensagem = PastaSeguraPresenter.mensagemErroDescriptografia(
        new SQLException("detalhe interno"));

    assertEquals(
        "Não foi possível concluir a operação devido a uma falha no "
            + "banco de dados ou no registro de auditoria.",
        mensagem);
  }
}
