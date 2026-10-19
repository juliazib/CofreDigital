// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.ui.componentes;

import javax.swing.JTextField;

public final class CampoTexto extends JTextField {

  public CampoTexto(int colunas, int limiteCaracteres) {
    super(colunas);
    LimiteCaracteres.aplicar(this, limiteCaracteres);
  }
}
