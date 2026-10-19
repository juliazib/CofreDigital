// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.ui.componentes;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

final class LimiteCaracteres {

  private LimiteCaracteres() {
  }

  static void aplicar(JTextField campo, int limite) {
    if (limite < 0) {
      throw new IllegalArgumentException("O limite não pode ser negativo.");
    }
    ((AbstractDocument) campo.getDocument()).setDocumentFilter(
        new DocumentFilter() {
          @Override
          public void insertString(
              FilterBypass bypass,
              int offset,
              String texto,
              AttributeSet atributos) throws BadLocationException {
            if (texto != null
                && bypass.getDocument().getLength() + texto.length()
                    <= limite) {
              bypass.insertString(offset, texto, atributos);
            }
          }

          @Override
          public void replace(
              FilterBypass bypass,
              int offset,
              int comprimento,
              String texto,
              AttributeSet atributos) throws BadLocationException {
            int tamanho = bypass.getDocument().getLength()
                - comprimento
                + (texto == null ? 0 : texto.length());
            if (tamanho <= limite) {
              bypass.replace(offset, comprimento, texto, atributos);
            }
          }
        });
  }
}
