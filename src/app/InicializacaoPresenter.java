package app;

import java.sql.Connection;
import java.util.Arrays;

import service.ContextoAdministrador;
import service.InicializacaoService;

public final class InicializacaoPresenter {

  public interface View {
    char[] solicitarFraseSecreta();
    void exibirErro(String mensagem);
    void exibirChaveValidada();
  }

  private final InicializacaoService service;
  private final View view;

  public InicializacaoPresenter(Connection connection, View view) {
    this.service = new InicializacaoService(connection);
    this.view = view;
  }

  public ContextoAdministrador validarAdministrador() {
    char[] fraseSecreta = null;
    ContextoAdministrador contexto = null;
    try {
      fraseSecreta = view.solicitarFraseSecreta();
      if (fraseSecreta == null) {
        return null;
      }

      contexto = service.validarAdministrador(fraseSecreta);
      view.exibirChaveValidada();
      ContextoAdministrador contextoValidado = contexto;
      contexto = null;
      return contextoValidado;
    } catch (Exception erro) {
      if (contexto != null) {
        contexto.close();
      }
      view.exibirErro(mensagem(erro));
      return null;
    } finally {
      if (fraseSecreta != null) {
        Arrays.fill(fraseSecreta, '\0');
      }
    }
  }

  private static String mensagem(Exception erro) {
    String mensagem = erro.getMessage();
    return mensagem == null || mensagem.trim().isEmpty()
        ? "Não foi possível validar a chave privada do administrador."
        : mensagem;
  }
}
