// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package test.app.ui.componentes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.JTable;

import org.junit.Test;

import app.ui.componentes.TabelaArquivosSeguros;
import model.ArquivoSeguro;

public class TabelaArquivosSegurosTest {

  @Test
  public void deveExibirArquivosSemPermitirEdicao() {
    TabelaArquivosSeguros tabela = new TabelaArquivosSeguros();
    tabela.exibirArquivos(Arrays.asList(
        new ArquivoSeguro("COD123", "arquivo.txt", "dono", "grupo")));

    JTable componenteTabela = encontrarTabela(tabela);

    assertEquals(1, tabela.getQuantidadeArquivos());
    assertEquals("arquivo.txt", componenteTabela.getValueAt(0, 0));
    assertEquals("dono", componenteTabela.getValueAt(0, 1));
    assertEquals("grupo", componenteTabela.getValueAt(0, 2));
    assertFalse(componenteTabela.isCellEditable(0, 0));
  }

  @Test
  public void deveRetornarCodigoDaLinhaSelecionada() {
    TabelaArquivosSeguros tabela = new TabelaArquivosSeguros();
    tabela.exibirArquivos(Arrays.asList(
        new ArquivoSeguro("COD123", "primeiro.txt", "dono", "grupo"),
        new ArquivoSeguro("COD456", "segundo.txt", "dono", "grupo")));
    JTable componenteTabela = encontrarTabela(tabela);
    componenteTabela.setRowSelectionInterval(1, 1);

    assertEquals("COD456", tabela.getCodigoSelecionado());
  }

  @Test
  public void limparDeveRemoverLinhasESelecao() {
    TabelaArquivosSeguros tabela = new TabelaArquivosSeguros();
    tabela.exibirArquivos(Collections.singletonList(
        new ArquivoSeguro("COD123", "arquivo.txt", "dono", "grupo")));
    encontrarTabela(tabela).setRowSelectionInterval(0, 0);

    tabela.limpar();

    assertEquals(0, tabela.getQuantidadeArquivos());
    assertNull(tabela.getCodigoSelecionado());
  }

  private static JTable encontrarTabela(Container container) {
    for (Component componente : container.getComponents()) {
      if (componente instanceof JTable) {
        return (JTable) componente;
      }
      if (componente instanceof Container) {
        JTable encontrada = encontrarTabela((Container) componente);
        if (encontrada != null) {
          return encontrada;
        }
      }
    }
    return null;
  }
}
