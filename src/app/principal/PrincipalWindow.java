// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.principal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public final class PrincipalWindow extends JFrame {

  private final PrincipalModel model;

  PrincipalWindow(
      Window owner,
      PrincipalModel model,
      PrincipalPresenter presenter) {
    super("Cofre Digital");
    this.model = model;
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setMinimumSize(new Dimension(560, 420));
    setSize(640, 460);
    setLocationRelativeTo(owner);
    setContentPane(criarConteudo(model, presenter));
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evento) {
        mostrarOpcoesDeSaida(presenter);
      }
    });
  }

  private JPanel criarConteudo(
      PrincipalModel model,
      PrincipalPresenter presenter) {
    JPanel conteudo = new JPanel(new BorderLayout(12, 12));
    conteudo.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

    conteudo.add(criarCabecalho(model), BorderLayout.NORTH);
    JPanel corpo1 = new JPanel(new BorderLayout());
    corpo1.add(new JLabel(
        "Total de acessos do usuário: " + presenter.getTotalAcessos()),
        BorderLayout.WEST);

    JPanel menu = new JPanel(new GridBagLayout());
    int linhaMenu = 0;
    if (model.podeGerenciarUsuarios()) {
      JButton cadastro = new JButton("Cadastrar um novo usuário");
      cadastro.addActionListener(
          evento -> executarAcao(() -> presenter.abrirCadastroUsuarios()));
      adicionarBotaoMenu(menu, cadastro, linhaMenu++);
    }
    adicionarBotaoMenu(menu, criarBotaoConsulta(presenter), linhaMenu++);

    JButton sair = new JButton("Sair do Sistema");
    sair.addActionListener(
        evento -> mostrarOpcoesDeSaida(presenter));
    adicionarBotaoMenu(menu, sair, linhaMenu);
    JPanel corpos = new JPanel(new BorderLayout(8, 8));
    corpos.add(corpo1, BorderLayout.NORTH);
    corpos.add(menu, BorderLayout.CENTER);
    conteudo.add(corpos, BorderLayout.CENTER);

    return conteudo;
  }

  public static void adicionarBotaoMenu(
      JPanel menu,
      JButton botao,
      int linha) {
    Dimension tamanho = new Dimension(420, 42);
    botao.setPreferredSize(tamanho);
    botao.setMinimumSize(tamanho);
    botao.setMaximumSize(tamanho);

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = linha;
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.insets = new Insets(6, 0, 6, 0);
    menu.add(botao, constraints);
  }

  private JButton criarBotaoConsulta(PrincipalPresenter presenter) {
    JButton consulta = new JButton(
        "Consultar pasta de arquivos secretos do usuário");
    consulta.setEnabled(presenter.podeConsultarPastaSegura());
    if (!presenter.podeConsultarPastaSegura()) {
      consulta.setToolTipText(
          "O contexto seguro do administrador não está disponível.");
    }
    consulta.addActionListener(
        evento -> executarAcao(() -> presenter.consultarPastaSegura()));
    return consulta;
  }

  private static JPanel criarCabecalho(PrincipalModel model) {
    JPanel cabecalho = new JPanel(new GridLayout(0, 1, 4, 4));
    cabecalho.add(new JLabel("Login: " + model.getEmailUsuario()));
    cabecalho.add(new JLabel("Grupo: " + model.getGrupoUsuario()));
    cabecalho.add(new JLabel("Nome: " + model.getNomeUsuario()));
    return cabecalho;
  }

  private void mostrarOpcoesDeSaida(PrincipalPresenter presenter) {
    try {
      presenter.iniciarSaida();
    } catch (IllegalStateException erro) {
      exibirErroDeAuditoria(erro);
      return;
    }

    mostrarTelaDeSaida(presenter, model);
  }

  private void mostrarTelaDeSaida(
      PrincipalPresenter presenter,
      PrincipalModel model) {
    javax.swing.JDialog dialogo = new javax.swing.JDialog(
        this,
        "Saída do Cofre Digital",
        javax.swing.JDialog.ModalityType.APPLICATION_MODAL);
    dialogo.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
    JPanel conteudo = new JPanel(new BorderLayout(12, 12));
    conteudo.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
    conteudo.add(criarCabecalho(model), BorderLayout.NORTH);

    JPanel corpo1 = new JPanel(new BorderLayout());
    corpo1.add(new JLabel(
        "Total de acessos do usuário: " + presenter.getTotalAcessos()),
        BorderLayout.WEST);

    JPanel corpo2 = new JPanel(new BorderLayout(8, 12));
    corpo2.add(new JLabel(
        "Pressione o botão Encerrar Sessão ou o botão Encerrar Sistema "
            + "para confirmar."), BorderLayout.NORTH);
    JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    JButton encerrarSessao = new JButton("Encerrar Sessão");
    encerrarSessao.addActionListener(evento -> executarSaida(
        presenter::encerrarSessao,
        dialogo));
    JButton encerrarSistema = new JButton("Encerrar Sistema");
    encerrarSistema.addActionListener(evento -> executarSaida(
        presenter::encerrarSistema,
        dialogo));
    JButton voltar = new JButton("Voltar");
    voltar.addActionListener(evento -> executarSaida(
        presenter::voltarDaSaida,
        dialogo));
    botoes.add(encerrarSessao);
    botoes.add(encerrarSistema);
    botoes.add(voltar);
    corpo2.add(botoes, BorderLayout.SOUTH);
    JPanel corpos = new JPanel(new BorderLayout(8, 8));
    corpos.add(corpo1, BorderLayout.NORTH);
    corpos.add(corpo2, BorderLayout.CENTER);
    conteudo.add(corpos, BorderLayout.CENTER);
    dialogo.setContentPane(conteudo);
    dialogo.setMinimumSize(new Dimension(600, 380));
    dialogo.pack();
    dialogo.setLocationRelativeTo(this);
    dialogo.setVisible(true);
  }

  private void executarSaida(Runnable acao, javax.swing.JDialog dialogo) {
    try {
      acao.run();
      dialogo.dispose();
    } catch (IllegalStateException erro) {
      exibirErroDeAuditoria(erro);
    }
  }

  private void exibirErroDeAuditoria(IllegalStateException erro) {
    JOptionPane.showMessageDialog(
        this,
        erro.getMessage(),
        "Falha de auditoria",
        JOptionPane.ERROR_MESSAGE);
  }

  private void executarAcao(Runnable acao) {
    try {
      acao.run();
    } catch (IllegalStateException erro) {
      exibirErroDeAuditoria(erro);
    }
  }
}
