// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.ui.componentes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class EntradaSenhaVirtual extends JPanel {

  private static final SecureRandom ALEATORIO = new SecureRandom();
  private static final String DIGITOS = "0123456789";

  private final char[][] paresSelecionados;
  private final int tamanhoMinimo;
  private final int tamanhoMaximo;
  private final JLabel mascara = new JLabel("", SwingConstants.LEFT);
  private final JButton[] botoesPar = new JButton[5];
  private final JButton confirmar = new JButton("OK");
  private JDialog dialogo;
  private char[][] paresConfirmados;
  private int quantidade;

  public EntradaSenhaVirtual(
      int tamanhoMinimo,
      int tamanhoMaximo) {
    super(new BorderLayout(8, 12));
    if (tamanhoMinimo < 0 || tamanhoMaximo < tamanhoMinimo) {
      throw new IllegalArgumentException(
          "Os limites da senha virtual são inválidos.");
    }
    paresSelecionados = new char[tamanhoMaximo][2];
    this.tamanhoMinimo = tamanhoMinimo;
    this.tamanhoMaximo = tamanhoMaximo;

    mascara.setName("senhaMascarada");
    mascara.setPreferredSize(new Dimension(360, 38));
    atualizarMascara();

    JPanel campo = new JPanel(new BorderLayout(12, 0));
    campo.add(new JLabel("Senha pessoal:"), BorderLayout.WEST);
    campo.add(mascara, BorderLayout.CENTER);

    JPanel teclado = criarTeclado();
    add(campo, BorderLayout.NORTH);
    add(teclado, BorderLayout.CENTER);
    setToolTipText(
        "Selecione o par; os dígitos mudam após cada entrada.");
    embaralharPares();
    atualizarBotoes();
  }

  public static char[][] solicitarPares(
      Window owner,
      int tamanhoMinimo,
      int tamanhoMaximo) {
    EntradaSenhaVirtual entrada =
        new EntradaSenhaVirtual(tamanhoMinimo, tamanhoMaximo);
    entrada.dialogo = new JDialog(
        owner,
        "Cofre Digital - Autenticação",
        JDialog.ModalityType.APPLICATION_MODAL);
    entrada.dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    entrada.dialogo.setResizable(false);
    entrada.dialogo.setContentPane(criarTelaAutenticacao(entrada));
    entrada.dialogo.pack();
    entrada.dialogo.setLocationRelativeTo(owner);
    entrada.dialogo.setVisible(true);
    entrada.limpar();
    return entrada.paresConfirmados;
  }

  public char[][] getParesSelecionados() {
    char[][] copia = new char[quantidade][2];
    for (int i = 0; i < quantidade; i++) {
      copia[i] = paresSelecionados[i].clone();
    }
    return copia;
  }

  public void limpar() {
    for (char[] par : paresSelecionados) {
      Arrays.fill(par, '\0');
    }
    quantidade = 0;
    atualizarMascara();
    embaralharPares();
    atualizarBotoes();
  }

  private JPanel criarTeclado() {
    JPanel teclado = new JPanel(new GridLayout(2, 1, 8, 8));
    JPanel primeiraLinha = new JPanel(new GridLayout(1, 4, 8, 8));
    JPanel segundaLinha = new JPanel(new GridLayout(1, 4, 8, 8));
    for (int i = 0; i < botoesPar.length; i++) {
      JButton botao = new JButton();
      botao.setPreferredSize(new Dimension(132, 50));
      botao.addActionListener(
          evento -> adicionarPar(evento.getActionCommand()));
      botoesPar[i] = botao;
    }

    primeiraLinha.add(botoesPar[0]);
    primeiraLinha.add(botoesPar[1]);
    primeiraLinha.add(botoesPar[2]);
    confirmar.addActionListener(evento -> confirmarSenha());
    confirmar.setPreferredSize(new Dimension(132, 50));
    primeiraLinha.add(confirmar);

    segundaLinha.add(botoesPar[3]);
    segundaLinha.add(botoesPar[4]);
    segundaLinha.add(new JPanel());
    JButton limpar = new JButton("LIMPAR");
    limpar.addActionListener(evento -> limpar());
    limpar.setPreferredSize(new Dimension(132, 50));
    segundaLinha.add(limpar);

    teclado.add(primeiraLinha);
    teclado.add(segundaLinha);
    return teclado;
  }

  private static JPanel criarTelaAutenticacao(
      EntradaSenhaVirtual entrada) {
      JPanel tela = new JPanel(new BorderLayout(12, 16));
      tela.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
      JLabel cabecalho = new JLabel(
          "Autenticação bifator: Etapa 2 - Validação da Senha Pessoal",
          SwingConstants.LEFT);
      tela.add(cabecalho, BorderLayout.NORTH);
      JPanel formulario = new JPanel(new BorderLayout(8, 10));
      formulario.setBorder(BorderFactory.createTitledBorder(
          "Cofre Digital - Autenticação"));
      formulario.add(entrada, BorderLayout.CENTER);
      tela.add(formulario, BorderLayout.CENTER);
      return tela;
  }

  private void adicionarPar(String par) {
    if (quantidade >= tamanhoMaximo) {
      return;
    }
    paresSelecionados[quantidade][0] = par.charAt(0);
    paresSelecionados[quantidade][1] = par.charAt(2);
    quantidade++;
    atualizarMascara();
    embaralharPares();
    atualizarBotoes();
  }

  private void confirmarSenha() {
    if (quantidade < tamanhoMinimo) {
      JOptionPane.showMessageDialog(
          dialogo,
          "Informe pelo menos " + tamanhoMinimo + " dígitos.",
          "Senha incompleta",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    paresConfirmados = getParesSelecionados();
    if (dialogo != null) {
      dialogo.dispose();
    }
  }

  private void atualizarMascara() {
    char[] caracteres = new char[quantidade];
    Arrays.fill(caracteres, '\u2022');
    mascara.setText(new String(caracteres));
    Arrays.fill(caracteres, '\0');
  }

  private void embaralharPares() {
    char[] embaralhados = DIGITOS.toCharArray();
    for (int i = embaralhados.length - 1; i > 0; i--) {
      int indice = ALEATORIO.nextInt(i + 1);
      char temporario = embaralhados[i];
      embaralhados[i] = embaralhados[indice];
      embaralhados[indice] = temporario;
    }
    for (int i = 0; i < botoesPar.length; i++) {
      char primeiro = embaralhados[i * 2];
      char segundo = embaralhados[i * 2 + 1];
      JButton botao = botoesPar[i];
      String par = primeiro + " " + segundo;
      botao.setActionCommand(par);
      botao.setText(par);
      botao.getAccessibleContext().setAccessibleName(
          "Par de dígitos " + primeiro + " e " + segundo
              + "; clique para selecionar ambos.");
    }
    Arrays.fill(embaralhados, '\0');
  }

  private void atualizarBotoes() {
    boolean habilitado = quantidade < tamanhoMaximo;
    for (JButton botao : botoesPar) {
      botao.setEnabled(habilitado);
    }
    confirmar.setEnabled(quantidade >= tamanhoMinimo);
  }

}
