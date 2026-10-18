package model;

import java.time.Instant;

public final class RegistroAuditoria {

  private final int rid;
  private final Instant dataHora;
  private final Integer uid;
  private final int codigoMensagem;
  private final String mensagem;
  private final String loginName;
  private final String arquivo;

  public RegistroAuditoria(
      int rid,
      Instant dataHora,
      Integer uid,
      int codigoMensagem,
      String mensagem,
      String loginName,
      String arquivo) {
    this.rid = rid;
    this.dataHora = dataHora;
    this.uid = uid;
    this.codigoMensagem = codigoMensagem;
    this.mensagem = mensagem;
    this.loginName = loginName;
    this.arquivo = arquivo;
  }

  public int getRid() {
    return rid;
  }

  public Instant getDataHora() {
    return dataHora;
  }

  public Integer getUid() {
    return uid;
  }

  public int getCodigoMensagem() {
    return codigoMensagem;
  }

  public String getMensagem() {
    return mensagem;
  }

  public String getLoginName() {
    return loginName;
  }

  public String getArquivo() {
    return arquivo;
  }
}
