package app.pastasegura;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import model.ArquivoSeguro;
import service.ContextoAdministrador;
import service.PastaSeguraService;

public final class PastaSeguraPresenter {

  public interface View {
    void exibirArquivos(List<ArquivoSeguro> arquivos);
    void exibirErro(String mensagem);
    void exibirArquivoDescriptografado(Path arquivo);
  }

  private final PastaSeguraModel model;
  private final PastaSeguraService service;
  private final View view;

  public PastaSeguraPresenter(
      Connection connection,
      PastaSeguraModel model,
      ContextoAdministrador contextoAdministrador,
      View view) {
    if (connection == null || model == null
        || contextoAdministrador == null || view == null) {
      throw new IllegalArgumentException(
          "A conexão, o modelo, o contexto e a interface são obrigatórios.");
    }
    this.model = model;
    service = new PastaSeguraService(
        connection,
        contextoAdministrador);
    this.view = view;
  }

  public void listarArquivos(Path pasta, char[] fraseSecreta) {
    model.limparConsulta();
    try {
      List<ArquivoSeguro> arquivos =
          service.listarArquivos(pasta, model.getUsuario(), fraseSecreta);
      model.registrarConsulta(pasta, fraseSecreta, arquivos);
      view.exibirArquivos(arquivos);
    } catch (IOException | GeneralSecurityException
        | SecurityException | IllegalArgumentException erro) {
      view.exibirErro(erro.getMessage());
    }
  }

  public void descriptografarArquivo(String codigo) {
    char[] fraseSecreta = model.obterCopiaDaFraseSecreta();
    if (fraseSecreta == null || model.getPasta() == null) {
      view.exibirErro(
          "Valide a frase secreta e liste os arquivos antes de descriptografar.");
      return;
    }
    try {
      Path arquivo = service.descriptografarArquivo(
          model.getPasta(),
          model.getUsuario(),
          fraseSecreta,
          codigo);
      view.exibirArquivoDescriptografado(arquivo);
    } catch (IOException | GeneralSecurityException
        | SecurityException | IllegalArgumentException erro) {
      view.exibirErro(erro.getMessage());
    } finally {
      Arrays.fill(fraseSecreta, '\0');
    }
  }

  public void encerrar() {
    model.limparConsulta();
  }

  public void limparConsulta() {
    model.limparConsulta();
  }
}
