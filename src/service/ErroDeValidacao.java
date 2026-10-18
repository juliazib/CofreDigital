package service;

public final class ErroDeValidacao extends IllegalArgumentException {

  public ErroDeValidacao(String mensagem) {
    super(mensagem);
  }
}
