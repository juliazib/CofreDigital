package vault;

public final class ShareEntry {

  private final String codigoArquivo;
  private final long idCompartilhamento;
  private final String usuarioAutorizado;

  ShareEntry(
      String codigoArquivo,
      long idCompartilhamento,
      String usuarioAutorizado) {
    this.codigoArquivo = codigoArquivo;
    this.idCompartilhamento = idCompartilhamento;
    this.usuarioAutorizado = usuarioAutorizado;
  }

  public String getCodigoArquivo() {
    return codigoArquivo;
  }

  public long getIdCompartilhamento() {
    return idCompartilhamento;
  }

  public String getUsuarioAutorizado() {
    return usuarioAutorizado;
  }
}
