package app.autenticacao;

import java.util.Arrays;

public final class AutenticacaoModel {

  private String email;
  private char[][] paresSenha;
  private String codigoTotp;

  public void preencherLogin(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  public void preencherParesSenha(char[][] paresSenha) {
    limparSenha();
    if (paresSenha == null) {
      return;
    }
    this.paresSenha = new char[paresSenha.length][];
    for (int i = 0; i < paresSenha.length; i++) {
      this.paresSenha[i] =
          paresSenha[i] == null ? null : paresSenha[i].clone();
    }
  }

  public char[][] getParesSenha() {
    if (paresSenha == null) {
      return null;
    }
    char[][] copia = new char[paresSenha.length][];
    for (int i = 0; i < paresSenha.length; i++) {
      copia[i] = paresSenha[i] == null ? null : paresSenha[i].clone();
    }
    return copia;
  }

  public void preencherCodigoTotp(String codigoTotp) {
    this.codigoTotp = codigoTotp;
  }

  public String getCodigoTotp() {
    return codigoTotp;
  }

  public void limparSenha() {
    if (paresSenha != null) {
      for (char[] par : paresSenha) {
        if (par != null) {
          Arrays.fill(par, '\0');
        }
      }
      paresSenha = null;
    }
  }

  public void limparCodigoTotp() {
    codigoTotp = null;
  }
}
