
package model;

public class Usuario {

  private final int uid;
  private final String nome;
  private final String email;
  private final String senhaHash;
  private final byte[] tokenCifrado;
  private final Integer kid;
  private final int gid;
  private final String grupo;
  private final int falhasSenha;
  private final int falhasToken;
  private final Long bloqueadoAte;

  public Usuario(
      int uid,
      String nome,
      String email,
      String senhaHash,
      byte[] tokenCifrado,
      Integer kid,
      int gid,
      String grupo,
      int falhasSenha,
      int falhasToken,
      Long bloqueadoAte) {
    this.uid = uid;
    this.nome = nome;
    this.email = email;
    this.senhaHash = senhaHash;
    this.tokenCifrado = tokenCifrado;
    this.kid = kid;
    this.gid = gid;
    this.grupo = grupo;
    this.falhasSenha = falhasSenha;
    this.falhasToken = falhasToken;
    this.bloqueadoAte = bloqueadoAte;
  }

  public int getUid() {
    return uid;
  }

  public String getNome() {
    return nome;
  }

  public String getEmail() {
    return email;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public byte[] getTokenCifrado() {
    return tokenCifrado == null ? null : tokenCifrado.clone();
  }

  public Integer getKid() {
    return kid;
  }

  public int getGid() {
    return gid;
  }

  public String getGrupo() {
    return grupo;
  }

  public int getFalhasSenha() {
    return falhasSenha;
  }

  public int getFalhasToken() {
    return falhasToken;
  }

  public Long getBloqueadoAte() {
    return bloqueadoAte;
  }

  public boolean estaBloqueado(long agoraMillis) {
    return bloqueadoAte != null && agoraMillis < bloqueadoAte;
  }
}
