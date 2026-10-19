// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.inicializacao;

import java.awt.Window;
import java.sql.Connection;
import java.util.Arrays;

import app.MensagensErro;
import service.ContextoAdministrador;
import service.InicializacaoService;

public final class InicializacaoPresenter {

  public interface View {
    boolean solicitarFraseSecreta(InicializacaoModel model);
    void exibirErro(String mensagem);
    void exibirChaveValidada();
  }

  private final InicializacaoService service;
  private final InicializacaoModel model;
  private View view;

  public InicializacaoPresenter(
      Connection connection,
      InicializacaoModel model) {
    this(connection, model, null);
  }

  public InicializacaoPresenter(
      Connection connection,
      InicializacaoModel model,
      View view) {
    this.service = new InicializacaoService(connection);
    this.model = model;
    this.view = view;
  }

  public ContextoAdministrador validarAdministrador(Window owner) {
    if (view != null) {
      throw new IllegalStateException(
          "A janela de inicialização já foi criada.");
    }
    view = new InicializacaoWindow(owner);
    return validarAdministrador();
  }

  public ContextoAdministrador validarAdministrador() {
    if (view == null) {
      throw new IllegalStateException(
          "A janela de inicialização ainda não foi criada.");
    }
    ContextoAdministrador contexto = null;
    try {
      if (!view.solicitarFraseSecreta(model)) {
        return null;
      }

      char[] fraseSecreta = model.consumirFraseSecreta();
      if (fraseSecreta == null) {
        return null;
      }
      try {
        contexto = service.validarAdministrador(fraseSecreta);
      } finally {
        Arrays.fill(fraseSecreta, '\0');
      }
      view.exibirChaveValidada();
      ContextoAdministrador contextoValidado = contexto;
      contexto = null;
      return contextoValidado;
    } catch (Exception erro) {
      if (contexto != null) {
        contexto.close();
      }
      view.exibirErro(MensagensErro.inicializacao(erro));
      return null;
    } finally {
      model.limpar();
    }
  }

}
