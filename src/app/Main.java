// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app;

import javax.swing.SwingUtilities;

public final class Main {

  private Main() {
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new CofreDigital().iniciar());
  }
}
