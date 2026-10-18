package app.pastasegura;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import database.RegistroDAO;
import model.ArquivoSeguro;
import service.ContextoAdministrador;
import service.AuditoriaService;
import service.PastaSeguraService;

public final class PastaSeguraPresenter {

  public interface View {
    void exibirArquivos(List<ArquivoSeguro> arquivos);
    void exibirErro(String mensagem);
    void exibirArquivoDescriptografado(Path arquivo);
    void atualizarTotalConsultas(int total);
  }

  private final PastaSeguraModel model;
  private final PastaSeguraService service;
  private final AuditoriaService auditoria;
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
    auditoria = new AuditoriaService(new RegistroDAO(connection));
    this.view = view;
  }

  public void registrarTelaApresentada() {
    try {
      auditoria.registrar(7001, model.getUsuario(), null);
      atualizarTotalConsultas();
    } catch (SQLException erro) {
      view.exibirErro("Não foi possível registrar a consulta: "
          + erro.getMessage());
    }
  }

  public void registrarVoltar() {
    registrarEvento(7002, null);
  }

  public void registrarCaminhoInvalido() {
    registrarEvento(7004, null);
  }

  public boolean registrarListarPressionado() {
    try {
      auditoria.registrar(7003, model.getUsuario(), null);
      atualizarTotalConsultas();
      return true;
    } catch (SQLException erro) {
      view.exibirErro("Não foi possível registrar a consulta: "
          + erro.getMessage());
      return false;
    }
  }

  public void listarArquivos(Path pasta, char[] fraseSecreta) {
    model.limparConsulta();
    try {
      if (pasta == null) {
        auditoria.registrar(7004, model.getUsuario(), null);
        throw new IllegalArgumentException(
            "Selecione o caminho da pasta segura.");
      }
      List<ArquivoSeguro> arquivos =
          service.listarArquivos(pasta, model.getUsuario(), fraseSecreta);
      model.registrarConsulta(pasta, fraseSecreta, arquivos);
      view.exibirArquivos(arquivos);
    } catch (IOException | GeneralSecurityException
        | SecurityException | IllegalArgumentException | SQLException erro) {
      view.exibirErro(erro.getMessage());
    }
  }

  public void descriptografarArquivo(String codigo) {
    ArquivoSeguro selecionado = model.buscarArquivo(codigo);
    String nomeArquivo = selecionado == null ? codigo : selecionado.getNome();
    char[] fraseSecreta = model.obterCopiaDaFraseSecreta();
    if (fraseSecreta == null || model.getPasta() == null) {
      view.exibirErro(
          "Valide a frase secreta e liste os arquivos antes de descriptografar.");
      return;
    }
    try {
      auditoria.registrar(7015, model.getUsuario(), nomeArquivo);
      Path arquivo = service.descriptografarArquivo(
          model.getPasta(),
          model.getUsuario(),
          fraseSecreta,
          codigo);
      view.exibirArquivoDescriptografado(arquivo);
    } catch (IOException | GeneralSecurityException
        | SecurityException | IllegalArgumentException | SQLException erro) {
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

  private void atualizarTotalConsultas() throws SQLException {
    int total = auditoria.contarEventos(
        model.getUsuario().getUid(),
        7003);
    view.atualizarTotalConsultas(total);
  }

  private void registrarEvento(int codigo, String arquivo) {
    try {
      auditoria.registrar(codigo, model.getUsuario(), arquivo);
    } catch (SQLException erro) {
      view.exibirErro("Não foi possível registrar o evento: "
          + erro.getMessage());
    }
  }
}
