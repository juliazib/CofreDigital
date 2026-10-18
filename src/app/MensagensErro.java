package app;

import java.io.IOException;
import java.sql.SQLException;

import service.ErroDeValidacao;

public final class MensagensErro {

  private MensagensErro() {
  }

  public static String cadastro(Throwable erro) {
    if (erro instanceof ErroDeValidacao) {
      return mensagemValidacao(erro,
          "Revise os dados informados e os arquivos selecionados.");
    }
    if (erro instanceof IllegalStateException) {
      return "Não foi possível concluir o cadastro. Verifique se já existe "
          + "um usuário com esses dados.";
    }
    if (erro instanceof SecurityException) {
      return "Você não tem permissão para realizar este cadastro.";
    }
    if (erro instanceof IOException) {
      return "Não foi possível ler um dos arquivos selecionados. "
          + "Confira o caminho e tente novamente.";
    }
    if (erro instanceof SQLException) {
      return "Não foi possível salvar o cadastro. Tente novamente.";
    }
    return "Não foi possível concluir o cadastro. Confira os dados e os "
        + "arquivos selecionados e tente novamente.";
  }

  public static String inicializacao(Throwable erro) {
    if (erro instanceof SQLException) {
      return "Não foi possível acessar os dados do administrador. "
          + "Verifique a instalação e tente novamente.";
    }
    return "Não foi possível validar a chave privada do administrador. "
        + "Confira a frase secreta, o certificado e a chave selecionada.";
  }

  public static String erroAplicacao() {
    return "O Cofre Digital não conseguiu acessar os dados necessários. "
        + "Tente novamente ou contate o suporte.";
  }

  private static String mensagemValidacao(
      Throwable erro,
      String mensagemPadrao) {
    if (!(erro instanceof ErroDeValidacao)) {
      return mensagemPadrao;
    }
    String mensagem = erro.getMessage();
    if (mensagem == null || mensagem.trim().isEmpty()) {
      return mensagemPadrao;
    }
    return mensagem;
  }
}
