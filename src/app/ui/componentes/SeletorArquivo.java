// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.ui.componentes;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

public final class SeletorArquivo extends JPanel {

  private final CampoTexto caminho = new CampoTexto(30, 255);

  public SeletorArquivo(String titulo) {
    super(new BorderLayout(6, 0));
    JButton selecionar = new JButton("Selecionar");
    selecionar.addActionListener(evento -> {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle(titulo);
      if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File arquivo = chooser.getSelectedFile();
        caminho.setText(arquivo.getAbsolutePath());
      }
    });
    add(caminho, BorderLayout.CENTER);
    add(selecionar, BorderLayout.EAST);
  }

  public String getCaminho() {
    return caminho.getText();
  }

  public void limpar() {
    caminho.setText("");
  }
}
