// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package model;

public final class ArquivoSeguro {

  private final String codigo;
  private final String nome;
  private final String dono;
  private final String grupo;
  private final Integer idCompartilhamento;

  public ArquivoSeguro(
      String codigo,
      String nome,
      String dono,
      String grupo) {
    this(codigo, nome, dono, grupo, null);
  }

  public ArquivoSeguro(
      String codigo,
      String nome,
      String dono,
      String grupo,
      Integer idCompartilhamento) {
    this.codigo = codigo;
    this.nome = nome;
    this.dono = dono;
    this.grupo = grupo;
    this.idCompartilhamento = idCompartilhamento;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getNome() {
    return nome;
  }

  public String getDono() {
    return dono;
  }

  public String getGrupo() {
    return grupo;
  }

  public Integer getIdCompartilhamento() {
    return idCompartilhamento;
  }
}
