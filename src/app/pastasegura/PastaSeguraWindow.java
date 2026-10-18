package app.pastasegura;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import app.ui.componentes.CampoSenha;
import app.ui.componentes.CampoTexto;
import app.ui.componentes.TabelaArquivosSeguros;
import model.ArquivoSeguro;
import model.Usuario;
import service.ContextoAdministrador;

public final class PastaSeguraWindow
    extends JDialog
    implements PastaSeguraPresenter.View {

  private final PastaSeguraModel model;
  private final PastaSeguraPresenter presenter;
  private final CampoTexto campoPasta = new CampoTexto(32, 255);
  private final CampoSenha campoFrase = new CampoSenha(24, 255);
  private final JLabel totalConsulta =
      new JLabel("Total de consultas do usuário: 0");
  private final JLabel resumo = new JLabel("Arquivos acessíveis: —");
  private final TabelaArquivosSeguros tabela = new TabelaArquivosSeguros();
  private final JButton descriptografar =
      new JButton("Descriptografar selecionado");

  public PastaSeguraWindow(
      Window owner,
      Connection connection,
      Usuario usuario,
      ContextoAdministrador contextoAdministrador) {
    super(owner, "Consultar pasta segura", ModalityType.APPLICATION_MODAL);
    model = new PastaSeguraModel(usuario);
    presenter = new PastaSeguraPresenter(
        connection,
        model,
        contextoAdministrador,
        this);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(720, 480));
    setContentPane(criarConteudo());
    pack();
    setLocationRelativeTo(owner);
    presenter.registrarTelaApresentada();
  }

  private JPanel criarConteudo() {
    JPanel conteudo = new JPanel(new BorderLayout(10, 10));
    conteudo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

    JPanel cabecalho = new JPanel(new GridLayout(0, 1, 3, 3));
    cabecalho.add(new JLabel("Login: " + model.getUsuario().getEmail()));
    cabecalho.add(new JLabel("Grupo: " + model.getUsuario().getGrupo()));
    cabecalho.add(new JLabel("Nome: " + model.getUsuario().getNome()));
    conteudo.add(cabecalho, BorderLayout.NORTH);

    JPanel corpos = new JPanel(new BorderLayout(8, 8));
    JPanel corpo1 = new JPanel(new BorderLayout());
    corpo1.add(totalConsulta, BorderLayout.WEST);
    corpos.add(corpo1, BorderLayout.NORTH);

    JPanel formulario = new JPanel(new GridLayout(0, 1, 6, 6));
    formulario.setBorder(BorderFactory.createTitledBorder("Pasta segura"));
    JPanel seletor = new JPanel(new BorderLayout(6, 0));
    seletor.add(campoPasta, BorderLayout.CENTER);
    JButton escolherPasta = new JButton("Selecionar pasta");
    escolherPasta.addActionListener(evento -> selecionarPasta());
    seletor.add(escolherPasta, BorderLayout.EAST);
    formulario.add(new JLabel("Caminho da pasta:"));
    formulario.add(seletor);
    formulario.add(new JLabel("Frase secreta da chave privada:"));
    formulario.add(campoFrase);

    JPanel corpo2 = new JPanel(new BorderLayout(8, 8));
    corpo2.add(formulario, BorderLayout.NORTH);
    JPanel tabelaComResumo = new JPanel(new BorderLayout(4, 4));
    tabelaComResumo.add(resumo, BorderLayout.NORTH);
    tabelaComResumo.add(tabela, BorderLayout.CENTER);
    corpo2.add(tabelaComResumo, BorderLayout.CENTER);

    JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton listar = new JButton("Listar");
    descriptografar.setEnabled(false);
    listar.addActionListener(evento -> listarArquivos());
    descriptografar.addActionListener(
        evento -> descriptografarSelecionado());
    JButton voltar = new JButton("Voltar");
    voltar.addActionListener(evento -> {
      presenter.registrarVoltar();
      dispose();
    });
    botoes.add(listar);
    botoes.add(descriptografar);
    botoes.add(voltar);
    corpo2.add(botoes, BorderLayout.SOUTH);
    corpos.add(corpo2, BorderLayout.CENTER);
    conteudo.add(corpos, BorderLayout.CENTER);
    campoPasta.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent evento) {
        invalidarConsulta();
      }

      @Override
      public void removeUpdate(DocumentEvent evento) {
        invalidarConsulta();
      }

      @Override
      public void changedUpdate(DocumentEvent evento) {
        invalidarConsulta();
      }
    });
    return conteudo;
  }

  private void selecionarPasta() {
    JFileChooser seletor = new JFileChooser();
    seletor.setDialogTitle("Selecione a pasta segura");
    seletor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      campoPasta.setText(seletor.getSelectedFile().getAbsolutePath());
    }
  }

  private void listarArquivos() {
    presenter.limparConsulta();
    limparTabela();
    String caminho = campoPasta.getText().trim();
    char[] frase = campoFrase.getPassword();
    try {
      if (!presenter.registrarListarPressionado()) {
        return;
      }
      presenter.listarArquivos(
          caminho.isEmpty() ? null : Paths.get(caminho),
          frase);
    } catch (IllegalArgumentException erro) {
      presenter.registrarCaminhoInvalido();
      exibirErro(erro.getMessage());
    } finally {
      java.util.Arrays.fill(frase, '\0');
      campoFrase.limpar();
    }
  }

  private void descriptografarSelecionado() {
    String codigo = tabela.getCodigoSelecionado();
    if (codigo == null) {
      JOptionPane.showMessageDialog(
          this,
          "Selecione um arquivo da lista.",
          "Pasta segura",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    presenter.descriptografarArquivo(codigo);
  }

  private void limparTabela() {
    tabela.limpar();
    descriptografar.setEnabled(false);
    atualizarResumo("—");
  }

  private void invalidarConsulta() {
    presenter.limparConsulta();
    limparTabela();
  }

  @Override
  public void exibirArquivos(List<ArquivoSeguro> arquivos) {
    tabela.exibirArquivos(arquivos);
    descriptografar.setEnabled(!arquivos.isEmpty());
    atualizarResumo(Integer.toString(arquivos.size()));
    if (arquivos.isEmpty()) {
      JOptionPane.showMessageDialog(
          this,
          "Não há arquivos autorizados para este usuário.",
          "Pasta segura",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  @Override
  public void exibirErro(String mensagem) {
    limparTabela();
    JOptionPane.showMessageDialog(
        this,
        mensagem,
        "Pasta segura",
        JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void exibirArquivoDescriptografado(Path arquivo) {
    JOptionPane.showMessageDialog(
        this,
        "Arquivo descriptografado em:\n" + arquivo,
        "Pasta segura",
        JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  public void atualizarTotalConsultas(int total) {
    totalConsulta.setText("Total de consultas do usuário: " + total);
    atualizarResumo("—");
  }

  private void atualizarResumo(String quantidadeArquivos) {
    resumo.setText("Arquivos acessíveis: " + quantidadeArquivos);
  }

  @Override
  public void dispose() {
    presenter.encerrar();
    super.dispose();
  }
}
