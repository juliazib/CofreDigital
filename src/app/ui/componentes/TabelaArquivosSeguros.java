package app.ui.componentes;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import model.ArquivoSeguro;

public final class TabelaArquivosSeguros extends JPanel {

  private final DefaultTableModel modelo = new DefaultTableModel(
      new Object[] { "Arquivo", "Dono", "Grupo" },
      0) {
    @Override
    public boolean isCellEditable(int row, int column) {
      return false;
    }
  };
  private final JTable tabela = new JTable(modelo);
  private List<ArquivoSeguro> arquivos = Collections.emptyList();

  public TabelaArquivosSeguros() {
    super(new BorderLayout());
    tabela.setFillsViewportHeight(true);
    add(new JScrollPane(tabela), BorderLayout.CENTER);
  }

  public void exibirArquivos(List<ArquivoSeguro> arquivos) {
    this.arquivos = Collections.unmodifiableList(
        new ArrayList<>(arquivos));
    modelo.setRowCount(0);
    for (ArquivoSeguro arquivo : this.arquivos) {
      modelo.addRow(new Object[] {
          arquivo.getNome(),
          arquivo.getDono(),
          arquivo.getGrupo()
      });
    }
  }

  public void limpar() {
    arquivos = Collections.emptyList();
    modelo.setRowCount(0);
  }

  public String getCodigoSelecionado() {
    int linhaSelecionada = tabela.getSelectedRow();
    if (linhaSelecionada < 0) {
      return null;
    }
    int linhaModelo = tabela.convertRowIndexToModel(linhaSelecionada);
    return arquivos.get(linhaModelo).getCodigo();
  }

  public int getQuantidadeArquivos() {
    return arquivos.size();
  }
}
