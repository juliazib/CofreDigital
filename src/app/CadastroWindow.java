package app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.security.cert.X509Certificate;
import java.sql.Connection;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public final class CadastroWindow extends JFrame
    implements CadastroPresenter.View {

  private final JTextField nome = campoTexto(100);
  private final JTextField email = campoTexto(255);
  private final JTextField caminhoCertificado = campoTexto(255);
  private final JTextField caminhoChavePrivada = campoTexto(255);
  private final JPasswordField fraseSecreta = campoSenha(255);
  private final JPasswordField senhaPessoal = campoSenha(10);
  private final JPasswordField confirmacaoSenha = campoSenha(10);
  private final JButton botaoCadastrar = new JButton("Cadastrar");
  private final CadastroPresenter presenter;

  public CadastroWindow(Connection connection, int totalUsuarios) {
    super("Cofre Digital - Cadastro");
    this.presenter = new CadastroPresenter(connection, this);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(680, 500));
    setSize(800, 620);
    setLocationRelativeTo(null);
    setContentPane(criarConteudo(totalUsuarios));
  }

  private JPanel criarConteudo(int totalUsuarios) {
    JPanel conteudo = new JPanel(new BorderLayout(12, 12));
    conteudo.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
    conteudo.add(criarCabecalho(), BorderLayout.NORTH);

    JPanel corpo = new JPanel(new BorderLayout(8, 16));
    corpo.add(
        new JLabel("Total de usuários do sistema: " + totalUsuarios),
        BorderLayout.NORTH);
    corpo.add(criarFormulario(), BorderLayout.CENTER);
    conteudo.add(corpo, BorderLayout.CENTER);

    JPanel botoes = new JPanel();
    botaoCadastrar.addActionListener(evento -> presenter.cadastrar());
    botoes.add(botaoCadastrar);
    JButton botaoFechar = new JButton("Fechar");
    botaoFechar.addActionListener(evento -> dispose());
    botoes.add(botaoFechar);
    conteudo.add(botoes, BorderLayout.SOUTH);

    return conteudo;
  }

  private JPanel criarCabecalho() {
    JPanel cabecalho = new JPanel(new GridBagLayout());
    cabecalho.setBorder(BorderFactory.createTitledBorder("Cabeçalho"));
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(2, 6, 2, 16);
    constraints.gridy = 0;
    cabecalho.add(new JLabel("Login: (sessão não autenticada)"), constraints);
    constraints.gridy++;
    cabecalho.add(new JLabel("Grupo: —"), constraints);
    constraints.gridy++;
    cabecalho.add(new JLabel("Nome: —"), constraints);
    return cabecalho;
  }

  private JPanel criarFormulario() {
    JPanel formulario = new JPanel(new GridBagLayout());
    formulario.setBorder(BorderFactory.createTitledBorder(
        "Formulário de Cadastro"));

    adicionarLinha(formulario, 0, "Nome:", nome);
    adicionarLinha(formulario, 1, "E-mail:", email);
    adicionarLinha(
        formulario,
        2,
        "Caminho do arquivo do certificado digital:",
        seletorArquivo(caminhoCertificado));
    adicionarLinha(
        formulario,
        3,
        "Caminho do arquivo da chave privada:",
        seletorArquivo(caminhoChavePrivada));
    adicionarLinha(
        formulario,
        4,
        "Frase secreta:",
        fraseSecreta);
    adicionarLinha(formulario, 5, "Grupo:", new JLabel("Administrador"));
    adicionarLinha(formulario, 6, "Senha pessoal (8 a 10 dígitos):", senhaPessoal);
    adicionarLinha(
        formulario,
        7,
        "Confirmação da senha pessoal:",
        confirmacaoSenha);

    return formulario;
  }

  private JPanel seletorArquivo(JTextField campo) {
    JPanel seletor = new JPanel(new BorderLayout(6, 0));
    JButton botaoSelecionar = new JButton("Selecionar");
    botaoSelecionar.addActionListener(evento -> {
      JFileChooser seletorArquivo = new JFileChooser();
      if (seletorArquivo.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File arquivo = seletorArquivo.getSelectedFile();
        campo.setText(arquivo.getAbsolutePath());
      }
    });
    seletor.add(campo, BorderLayout.CENTER);
    seletor.add(botaoSelecionar, BorderLayout.EAST);
    return seletor;
  }

  @Override
  public String getNome() {
    return nome.getText();
  }

  @Override
  public String getEmail() {
    return email.getText();
  }

  @Override
  public String getCaminhoCertificado() {
    return caminhoCertificado.getText();
  }

  @Override
  public String getCaminhoChavePrivada() {
    return caminhoChavePrivada.getText();
  }

  @Override
  public char[] getSenha() {
    return senhaPessoal.getPassword();
  }

  @Override
  public char[] getConfirmacaoSenha() {
    return confirmacaoSenha.getPassword();
  }

  @Override
  public char[] getFraseSecreta() {
    return fraseSecreta.getPassword();
  }

  @Override
  public boolean confirmarCertificado(X509Certificate certificado) {
    String detalhes = "Titular: " + certificado.getSubjectX500Principal()
        + "\nEmissor: " + certificado.getIssuerX500Principal()
        + "\nSérie: " + certificado.getSerialNumber()
        + "\nVálido de: " + certificado.getNotBefore()
        + "\nVálido até: " + certificado.getNotAfter()
        + "\nAlgoritmo de assinatura: "
        + certificado.getSigAlgName()
        + "\n\nDeseja prosseguir com este certificado?";
    return JOptionPane.showConfirmDialog(
        this,
        detalhes,
        "Confirmar certificado",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.INFORMATION_MESSAGE) == JOptionPane.YES_OPTION;
  }

  @Override
  public void exibirSegredoTotp(String segredo) {
    JOptionPane.showMessageDialog(
        this,
        "Cadastro concluído. Configure este segredo no aplicativo " +
            "autenticador agora; ele não será exibido novamente:\n\n" +
            segredo,
        "Administrador cadastrado",
        JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  public void exibirErro(String mensagem) {
    JOptionPane.showMessageDialog(
        this,
        mensagem,
        "Erro no cadastro",
        JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void limparCredenciais() {
    senhaPessoal.setText("");
    confirmacaoSenha.setText("");
    fraseSecreta.setText("");
  }

  @Override
  public void habilitarCadastro(boolean habilitado) {
    botaoCadastrar.setEnabled(habilitado);
  }

  @Override
  public void fechar() {
    dispose();
  }

  private static void adicionarLinha(
      JPanel painel,
      int linha,
      String rotulo,
      java.awt.Component campo) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = linha;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(7, 8, 7, 12);
    painel.add(new JLabel(rotulo), constraints);

    constraints.gridx = 1;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(7, 0, 7, 8);
    painel.add(campo, constraints);
  }

  private static JTextField campoTexto(int limite) {
    JTextField campo = new JTextField();
    limitarCaracteres(campo, limite);
    return campo;
  }

  private static JPasswordField campoSenha(int limite) {
    JPasswordField campo = new JPasswordField();
    limitarCaracteres(campo, limite);
    return campo;
  }

  private static void limitarCaracteres(
      JTextField campo,
      int limite) {
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
