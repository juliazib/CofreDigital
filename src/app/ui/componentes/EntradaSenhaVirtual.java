package app.ui.componentes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

  private final char[] senha;
  private final int tamanhoMinimo;
  private final int tamanhoMaximo;
  private final JLabel mascara = new JLabel("", SwingConstants.LEFT);
  private final JPanel[] pares = new JPanel[5];
  private final JButton[][] botoesDigito = new JButton[5][2];
  private final JButton confirmar = new JButton("OK");
  private JDialog dialogo;
  private char[] senhaConfirmada;
  private int quantidade;

  public EntradaSenhaVirtual(
      int tamanhoMinimo,
      int tamanhoMaximo) {
    super(new BorderLayout(8, 12));
    if (tamanhoMinimo < 0 || tamanhoMaximo < tamanhoMinimo) {
      throw new IllegalArgumentException(
          "Os limites da senha virtual são inválidos.");
    }
    senha = new char[tamanhoMaximo];
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
        "Selecione um dos dígitos do par; os pares mudam após cada entrada.");
    embaralharPares();
    atualizarBotoes();
  }

  public static char[] solicitarSenha(
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
    return entrada.senhaConfirmada;
  }

  public char[] getPassword() {
    return Arrays.copyOf(senha, quantidade);
  }

  public void limpar() {
    Arrays.fill(senha, '\0');
    quantidade = 0;
    atualizarMascara();
    embaralharPares();
    atualizarBotoes();
  }

  private JPanel criarTeclado() {
    JPanel teclado = new JPanel(new GridLayout(2, 1, 8, 8));
    JPanel primeiraLinha = new JPanel(new GridLayout(1, 4, 8, 8));
    JPanel segundaLinha = new JPanel(new GridLayout(1, 4, 8, 8));
    for (int i = 0; i < pares.length; i++) {
      pares[i] = new JPanel(new GridLayout(1, 2, 6, 0));
      pares[i].setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createEtchedBorder(),
          BorderFactory.createEmptyBorder(4, 4, 4, 4)));
      pares[i].setPreferredSize(new Dimension(132, 50));
      for (int j = 0; j < botoesDigito[i].length; j++) {
        JButton botao = new JButton();
        botao.addActionListener(evento -> adicionarDigito(
            botao.getActionCommand().charAt(0)));
        botoesDigito[i][j] = botao;
        pares[i].add(botao);
      }
    }

    primeiraLinha.add(pares[0]);
    primeiraLinha.add(pares[1]);
    primeiraLinha.add(pares[2]);
    confirmar.addActionListener(evento -> confirmarSenha());
    confirmar.setPreferredSize(new Dimension(132, 50));
    primeiraLinha.add(confirmar);

    segundaLinha.add(pares[3]);
    segundaLinha.add(pares[4]);
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

  private void adicionarDigito(char digito) {
    if (quantidade >= tamanhoMaximo) {
      return;
    }
    senha[quantidade++] = digito;
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
    senhaConfirmada = getPassword();
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
    for (int i = 0; i < pares.length; i++) {
      for (int j = 0; j < botoesDigito[i].length; j++) {
        String digito = Character.toString(embaralhados[i * 2 + j]);
        JButton botao = botoesDigito[i][j];
        botao.setText(digito);
        botao.setActionCommand(digito);
        botao.getAccessibleContext().setAccessibleName(
            "Dígito " + digito);
      }
    }
    Arrays.fill(embaralhados, '\0');
  }

  private void atualizarBotoes() {
    boolean habilitado = quantidade < tamanhoMaximo;
    for (JButton[] par : botoesDigito) {
      for (JButton botao : par) {
        botao.setEnabled(habilitado);
      }
    }
    confirmar.setEnabled(quantidade >= tamanhoMinimo);
  }
}
