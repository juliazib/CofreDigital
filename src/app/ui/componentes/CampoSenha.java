// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.ui.componentes;

import java.util.Arrays;

import javax.swing.JPasswordField;

public final class CampoSenha extends JPasswordField {

  public CampoSenha(int colunas, int limiteCaracteres) {
    super(colunas);
    LimiteCaracteres.aplicar(this, limiteCaracteres);
  }

  public void limpar() {
    char[] senha = getPassword();
    Arrays.fill(senha, '\0');
    setText("");
  }
}
