package app.autenticacao;

import java.util.Arrays;

public final class AutenticacaoModel {

  private String email;
  private char[] senha;
  private String codigoTotp;

  public void preencherLogin(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  public void preencherSenha(char[] senha) {
    limparSenha();
    this.senha = senha == null ? null : senha.clone();
  }

  public char[] getSenha() {
    return senha == null ? null : senha.clone();
  }

  public void preencherCodigoTotp(String codigoTotp) {
    this.codigoTotp = codigoTotp;
  }

  public String getCodigoTotp() {
    return codigoTotp;
  }

  public void limparSenha() {
    if (senha != null) {
      Arrays.fill(senha, '\0');
      senha = null;
    }
  }

  public void limparCodigoTotp() {
    codigoTotp = null;
  }
}
