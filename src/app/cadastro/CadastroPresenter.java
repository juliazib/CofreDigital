package app.cadastro;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.util.Arrays;

import app.MensagensErro;
import service.CadastroService;
import service.CertificadoChaveService;

public final class CadastroPresenter {

  public interface View {
    void preencherModelo(CadastroModel model);
    boolean confirmarCertificado(X509Certificate certificado)
        throws Exception;
    void exibirSegredoTotp(String segredo);
    void exibirErro(String mensagem);
    void limparCredenciais();
    void habilitarCadastro(boolean habilitado);
    void fechar();
  }

  private View view;
  private final CadastroService cadastroService;
  private final CadastroModel model;

  public CadastroPresenter(
      Connection connection,
      CadastroModel model) {
    cadastroService = new CadastroService(connection);
    this.model = model;
  }

  public void abrir(Window owner, Runnable aoFechar) {
    if (view != null) {
      throw new IllegalStateException(
          "A janela de cadastro já foi aberta.");
    }
    CadastroWindow janela = new CadastroWindow(owner, model, this);
    view = janela;
    janela.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent evento) {
        if (aoFechar != null) {
          aoFechar.run();
        }
      }
    });
    janela.setVisible(true);
  }

  public void cadastrar() {
    char[] senha = null;
    char[] confirmacao = null;
    char[] fraseSecreta = null;
    byte[] chavePrivadaCifrada = null;

    try {
      view.preencherModelo(model);
      senha = model.getSenha();
      confirmacao = model.getConfirmacaoSenha();
      fraseSecreta = model.getFraseSecreta();
      String caminhoCertificado = model.getCaminhoCertificado();
      String caminhoChave = model.getCaminhoChavePrivada();
      if (caminhoCertificado == null || caminhoCertificado.trim().isEmpty()
          || caminhoChave == null || caminhoChave.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Selecione o certificado e a chave privada.");
      }

      String certificadoPem = new String(
          Files.readAllBytes(new File(caminhoCertificado).toPath()),
          StandardCharsets.UTF_8);
      chavePrivadaCifrada =
          Files.readAllBytes(new File(caminhoChave).toPath());
      X509Certificate certificado =
          CertificadoChaveService.validarCertificadoPem(certificadoPem);
      if (!view.confirmarCertificado(certificado)) {
        return;
      }

      view.habilitarCadastro(false);
      String segredoTotp = model.isCadastroInicial()
          ? cadastroService.cadastrarAdministrador(
              senha,
              confirmacao,
              certificadoPem,
              chavePrivadaCifrada,
              fraseSecreta)
          : cadastroService.cadastrarUsuario(
              model.getUidAdministrador(),
              senha,
              confirmacao,
              certificadoPem,
              chavePrivadaCifrada,
              fraseSecreta);
      view.exibirSegredoTotp(segredoTotp);
      view.fechar();
    } catch (Exception erro) {
      view.exibirErro(MensagensErro.cadastro(erro));
    } finally {
      limpar(senha);
      limpar(confirmacao);
      limpar(fraseSecreta);
      model.limparFormulario();
      if (chavePrivadaCifrada != null) {
        Arrays.fill(chavePrivadaCifrada, (byte) 0);
      }
      view.limparCredenciais();
      view.habilitarCadastro(true);
    }
  }

  private static void limpar(char[] credencial) {
    if (credencial != null) {
      Arrays.fill(credencial, '\0');
    }
  }
}
