package app;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.util.Arrays;

import service.CadastroAdministradorService;
import service.CertificadoChaveService;

public final class CadastroPresenter {

  public interface View {
    String getCaminhoCertificado();
    String getCaminhoChavePrivada();
    char[] getSenha();
    char[] getConfirmacaoSenha();
    char[] getFraseSecreta();
    boolean confirmarCertificado(X509Certificate certificado)
        throws Exception;
    void exibirSegredoTotp(String segredo);
    void exibirErro(String mensagem);
    void limparCredenciais();
    void habilitarCadastro(boolean habilitado);
    void fechar();
  }

  private final View view;
  private final CadastroAdministradorService cadastroService;

  public CadastroPresenter(Connection connection, View view) {
    this.cadastroService = new CadastroAdministradorService(connection);
    this.view = view;
  }

  public void cadastrar() {
    char[] senha = view.getSenha();
    char[] confirmacao = view.getConfirmacaoSenha();
    char[] fraseSecreta = view.getFraseSecreta();

    try {
      if (view.getCaminhoCertificado().trim().isEmpty()
          || view.getCaminhoChavePrivada().trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Selecione o certificado e a chave privada.");
      }

      String certificadoPem = new String(
          Files.readAllBytes(new File(view.getCaminhoCertificado()).toPath()),
          StandardCharsets.UTF_8);
      byte[] chavePrivada = Files.readAllBytes(
          new File(view.getCaminhoChavePrivada()).toPath());
      X509Certificate certificado =
          CertificadoChaveService.validarCertificadoPem(certificadoPem);

      if (!view.confirmarCertificado(certificado)) {
        return;
      }

      view.habilitarCadastro(false);
      String segredoTotp = cadastroService.cadastrar(
          senha,
          confirmacao,
          certificadoPem,
          chavePrivada,
          fraseSecreta);
      view.exibirSegredoTotp(segredoTotp);
      view.fechar();
    } catch (Exception erro) {
      view.exibirErro(erro.getMessage() == null
          ? "Não foi possível concluir o cadastro."
          : erro.getMessage());
    } finally {
      Arrays.fill(senha, '\0');
      Arrays.fill(confirmacao, '\0');
      Arrays.fill(fraseSecreta, '\0');
      view.limparCredenciais();
      view.habilitarCadastro(true);
    }
  }
}
