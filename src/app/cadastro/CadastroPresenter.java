package app.cadastro;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import app.MensagensErro;
import service.AuditoriaService;
import service.CadastroService;
import service.CertificadoChaveService;
import service.ErroDeValidacao;

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
  private final Connection connection;
  private final AuditoriaService auditoria;

  public CadastroPresenter(
      Connection connection,
      CadastroModel model) {
    this.connection = connection;
    cadastroService = new CadastroService(connection);
    this.model = model;
    auditoria = AuditoriaService.getInstancia();
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
    registrarEvento(6001);
    janela.setVisible(true);
  }

  public void cadastrar() {
    if (!registrarEvento(6002)) {
      return;
    }
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
      if (caminhoCertificado == null || caminhoCertificado.trim().isEmpty()) {
        if (!registrarEvento(6004)) {
          return;
        }
        throw new IllegalArgumentException(
            "Selecione o certificado digital.");
      }
      if (caminhoChave == null || caminhoChave.trim().isEmpty()) {
        if (!registrarEvento(6005)) {
          return;
        }
        throw new IllegalArgumentException(
            "Selecione a chave privada.");
      }

      String certificadoPem;
      try {
        certificadoPem = new String(
            Files.readAllBytes(new File(caminhoCertificado).toPath()),
            StandardCharsets.UTF_8);
      } catch (IOException erro) {
        if (!registrarEvento(6004)) {
          return;
        }
        throw erro;
      }

      try {
        chavePrivadaCifrada =
            Files.readAllBytes(new File(caminhoChave).toPath());
      } catch (IOException erro) {
        if (!registrarEvento(6005)) {
          return;
        }
        throw erro;
      }

      X509Certificate certificado;
      try {
        certificado =
            CertificadoChaveService.validarCertificadoPem(certificadoPem);
      } catch (Exception erro) {
        if (!registrarEvento(6004)) {
          return;
        }
        throw erro;
      }

      boolean certificadoAceito = view.confirmarCertificado(certificado);
      if (!registrarEvento(certificadoAceito ? 6008 : 6009)) {
        return;
      }
      if (!certificadoAceito) {
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
      if (erro instanceof ErroDeValidacao) {
        int codigoAuditoria =
            ((ErroDeValidacao) erro).getCodigoAuditoria();
        if (codigoAuditoria != 0
            && !registrarEvento(codigoAuditoria)) {
          return;
        }
      }
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

  public void voltar() {
    if (model.isCadastroInicial() || registrarEvento(6010)) {
      view.fechar();
    }
  }

  private boolean registrarEvento(int codigo) {
    Integer uid = model.isCadastroInicial()
        ? null
        : model.getUidAdministrador();
    try {
      auditoria.registrar(connection, codigo, uid, null);
      return true;
    } catch (SQLException erro) {
      view.exibirErro(
          "Não foi possível registrar o evento de auditoria "
              + codigo
              + ": "
              + erro.getMessage());
      return false;
    }
  }

  private static void limpar(char[] credencial) {
    if (credencial != null) {
      Arrays.fill(credencial, '\0');
    }
  }
}
