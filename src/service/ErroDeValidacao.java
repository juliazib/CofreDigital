package service;

public final class ErroDeValidacao extends IllegalArgumentException {

  private final int codigoAuditoria;

  public ErroDeValidacao(String mensagem) {
    this(mensagem, 0);
  }

  public ErroDeValidacao(String mensagem, int codigoAuditoria) {
    super(mensagem);
    this.codigoAuditoria = codigoAuditoria;
  }

  public int getCodigoAuditoria() {
    return codigoAuditoria;
  }
}
