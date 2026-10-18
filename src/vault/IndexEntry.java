package vault;

public final class IndexEntry {

  private final String codigoArquivo;
  private final String nomeSecreto;
  private final String dono;
  private final String grupo;

  IndexEntry(String codigoArquivo, String nomeSecreto, String dono, String grupo) {
    this.codigoArquivo = codigoArquivo;
    this.nomeSecreto = nomeSecreto;
    this.dono = dono;
    this.grupo = grupo;
  }

  public String getCodigoArquivo() {
    return codigoArquivo;
  }

  public String getNomeSecreto() {
    return nomeSecreto;
  }

  public String getDono() {
    return dono;
  }

  public String getGrupo() {
    return grupo;
  }
}
