// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.cadastro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.cert.X509Certificate;

import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;

import app.ui.componentes.CampoSenha;
import app.ui.componentes.SeletorArquivo;
import crypto.CertificadoUtils;

public final class CadastroWindow extends JDialog
    implements CadastroPresenter.View {

  private final SeletorArquivo caminhoCertificado =
      new SeletorArquivo("Selecionar certificado digital");
  private final SeletorArquivo caminhoChavePrivada =
      new SeletorArquivo("Selecionar chave privada");
  private final CampoSenha fraseSecreta = new CampoSenha(24, 255);
  private final CampoSenha senhaPessoal = new CampoSenha(10, 10);
  private final CampoSenha confirmacaoSenha = new CampoSenha(10, 10);
  private final JComboBox<String> grupoNovoUsuario =
      new JComboBox<>(new String[] { "Administrador", "Usuário" });
  private final JButton botaoCadastrar = new JButton("Cadastrar");
  private final CadastroPresenter presenter;
  private final CadastroModel model;
  private final JLabel totalUsuarios = new JLabel();

  CadastroWindow(
      Window owner,
      CadastroModel model,
      CadastroPresenter presenter) {
    super(
        owner,
        model.isCadastroInicial()
            ? "Cofre Digital - Cadastro do administrador"
            : "Cofre Digital - Cadastro de usuário",
        ModalityType.APPLICATION_MODAL);
    this.model = model;
    this.presenter = presenter;
    setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    setContentPane(criarConteudo());
    pack();
    setSize(calcularTamanhoInicial(
        getSize(),
        obterAreaUtil(owner)));
    setLocationRelativeTo(owner);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evento) {
        presenter.voltar();
      }
    });
  }

  private JPanel criarConteudo() {
    JPanel conteudo = new JPanel(new BorderLayout(12, 12));
    conteudo.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
    conteudo.add(criarCabecalho(), BorderLayout.NORTH);

    JPanel corpos = new JPanel(new BorderLayout(8, 10));
    JPanel corpo1 = new JPanel(new BorderLayout());
    totalUsuarios.setText(model.getDescricao());
    corpo1.add(totalUsuarios, BorderLayout.CENTER);
    corpos.add(corpo1, BorderLayout.NORTH);
    JPanel corpo2 = new JPanel(new BorderLayout(8, 8));
    javax.swing.JScrollPane formulario =
        new javax.swing.JScrollPane(criarFormulario());
    formulario.setBorder(null);
    formulario.setHorizontalScrollBarPolicy(
        javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    formulario.getVerticalScrollBar().setUnitIncrement(16);
    corpo2.add(formulario, BorderLayout.CENTER);

    JPanel botoes = new JPanel();
    botaoCadastrar.addActionListener(evento -> presenter.cadastrar());
    botoes.add(botaoCadastrar);
    JButton botaoFechar = new JButton(
        model.isCadastroInicial() ? "Cancelar" : "Voltar ao menu");
    botaoFechar.addActionListener(evento -> presenter.voltar());
    botoes.add(botaoFechar);
    corpo2.add(botoes, BorderLayout.SOUTH);
    corpos.add(corpo2, BorderLayout.CENTER);
    conteudo.add(corpos, BorderLayout.CENTER);

    return conteudo;
  }

  private JPanel criarCabecalho() {
    JPanel cabecalho = new JPanel(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(2, 6, 2, 16);
    constraints.gridy = 0;
    String login = model.getLoginAdministrador();
    String grupo = model.getGrupoAdministrador();
    String nome = model.getNomeAdministrador();
    cabecalho.add(new JLabel("Login: " + (login == null ? "—" : login)),
        constraints);
    constraints.gridy++;
    cabecalho.add(new JLabel("Grupo: " + (grupo == null ? "—" : grupo)),
        constraints);
    constraints.gridy++;
    cabecalho.add(new JLabel("Nome: " + (nome == null ? "—" : nome)),
        constraints);
    return cabecalho;
  }

  private JPanel criarFormulario() {
    JPanel formulario = new JPanel(new GridBagLayout());
    formulario.setBorder(BorderFactory.createTitledBorder(
        "Formulário de Cadastro"));

    adicionarLinha(
        formulario,
        0,
        "Caminho do arquivo do certificado digital:",
        caminhoCertificado);
    adicionarLinha(
        formulario,
        1,
        "Caminho do arquivo da chave privada:",
        caminhoChavePrivada);
    adicionarLinha(
        formulario,
        2,
        "Frase secreta:",
        fraseSecreta);
    adicionarLinha(
        formulario,
        3,
        "Grupo:",
        criarCampoGrupo());
    adicionarLinha(formulario, 4, "Senha pessoal (8 a 10 dígitos):", senhaPessoal);
    adicionarLinha(
        formulario,
        5,
        "Confirmação da senha pessoal:",
        confirmacaoSenha);

    return formulario;
  }

  private java.awt.Component criarCampoGrupo() {
    if (model.isCadastroInicial()) {
      return new JLabel(model.getGrupoNovoUsuario());
    }
    grupoNovoUsuario.setName("grupoNovoUsuario");
    grupoNovoUsuario.setSelectedItem(model.getGrupoNovoUsuario());
    grupoNovoUsuario.addActionListener(evento -> {
      String grupo = (String) grupoNovoUsuario.getSelectedItem();
      if (grupo != null) {
        model.setGrupoNovoUsuario(grupo);
      }
    });
    return grupoNovoUsuario;
  }

  @Override
  public void preencherModelo(CadastroModel model) {
    char[] senha = senhaPessoal.getPassword();
    char[] confirmacao = confirmacaoSenha.getPassword();
    char[] frase = fraseSecreta.getPassword();
    try {
      model.preencherFormulario(
          caminhoCertificado.getCaminho(),
          caminhoChavePrivada.getCaminho(),
          senha,
          confirmacao,
          frase);
    } finally {
      java.util.Arrays.fill(senha, '\0');
      java.util.Arrays.fill(confirmacao, '\0');
      java.util.Arrays.fill(frase, '\0');
    }
  }

  @Override
  public boolean confirmarCertificado(X509Certificate certificado)
      throws Exception {
    String nome = CertificadoUtils.extrairNome(certificado);
    String email = CertificadoUtils.extrairEmail(certificado);
    String detalhes = "Versão: " + certificado.getVersion()
        + "\nSérie: " + certificado.getSerialNumber()
        + "\nVálido de: " + certificado.getNotBefore()
        + "\nVálido até: " + certificado.getNotAfter()
        + "\nTipo de assinatura: "
        + certificado.getSigAlgName()
        + "\nEmissor: " + certificado.getIssuerX500Principal()
        + "\nSujeito: " + nome
        + "\nE-mail: " + email
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
        model.isCadastroInicial()
            ? "Administrador cadastrado"
            : "Usuário cadastrado",
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
  public void limparFormulario() {
    caminhoCertificado.limpar();
    caminhoChavePrivada.limpar();
    senhaPessoal.limpar();
    confirmacaoSenha.limpar();
    fraseSecreta.limpar();
  }

  @Override
  public void atualizarTotalUsuarios(int total) {
    totalUsuarios.setText("Total de usuários do sistema: " + total);
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

  public static Dimension calcularTamanhoInicial(
      Dimension tamanhoPreferido,
      Dimension areaUtil) {
    if (tamanhoPreferido == null || areaUtil == null
        || tamanhoPreferido.width <= 0 || tamanhoPreferido.height <= 0
        || areaUtil.width <= 0 || areaUtil.height <= 0) {
      throw new IllegalArgumentException(
          "O tamanho preferido e a área útil devem ser positivos.");
    }
    return new Dimension(
        Math.min(tamanhoPreferido.width, areaUtil.width),
        Math.min(tamanhoPreferido.height, areaUtil.height));
  }

  private Dimension obterAreaUtil(Window owner) {
    GraphicsConfiguration configuracao = owner == null
        ? getGraphicsConfiguration()
        : owner.getGraphicsConfiguration();
    Rectangle limites = configuracao.getBounds();
    Insets recuos = Toolkit.getDefaultToolkit().getScreenInsets(configuracao);
    return new Dimension(
        limites.width - recuos.left - recuos.right,
        limites.height - recuos.top - recuos.bottom);
  }

}
