package app.inicializacao;

import java.util.Arrays;

public final class InicializacaoModel {

  private char[] fraseSecreta;

  public void preencherFraseSecreta(char[] fraseSecreta) {
    limpar();
    this.fraseSecreta =
        fraseSecreta == null ? null : fraseSecreta.clone();
  }

  public char[] consumirFraseSecreta() {
    if (fraseSecreta == null) {
      return null;
    }
    char[] frase = fraseSecreta;
    fraseSecreta = null;
    return frase;
  }

  public void limpar() {
    if (fraseSecreta != null) {
      Arrays.fill(fraseSecreta, '\0');
      fraseSecreta = null;
    }
  }
}
