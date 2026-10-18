package app.principal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class PrincipalWindow extends JFrame {

  PrincipalWindow(
      Window owner,
      PrincipalModel model,
      PrincipalPresenter presenter) {
    super("Cofre Digital");
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setMinimumSize(new Dimension(520, 340));
    setSize(600, 400);
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

    JPanel cabecalho = new JPanel(new GridLayout(0, 1, 4, 4));
    cabecalho.add(new JLabel("Sessão autenticada"));
    cabecalho.add(new JLabel("Nome: " + model.getNomeUsuario()));
    cabecalho.add(new JLabel("E-mail: " + model.getEmailUsuario()));
    cabecalho.add(new JLabel("Grupo: " + model.getGrupoUsuario()));
    cabecalho.add(new JLabel(
        "Acessos realizados: " + presenter.getTotalAcessos()));
    conteudo.add(cabecalho, BorderLayout.NORTH);

    JPanel funcionalidades = new JPanel(new GridLayout(0, 1, 8, 8));
    funcionalidades.setBorder(
        BorderFactory.createTitledBorder("Funcionalidades"));
    JButton pastaSegura = new JButton("Pasta segura");
    pastaSegura.setEnabled(presenter.podeConsultarPastaSegura());
    if (!presenter.podeConsultarPastaSegura()) {
      pastaSegura.setToolTipText(
          "O contexto seguro do administrador não está disponível.");
    }
    pastaSegura.addActionListener(
        evento -> executarAcao(() -> presenter.consultarPastaSegura()));
    funcionalidades.add(pastaSegura);

    if (model.podeGerenciarUsuarios()) {
      JButton gerenciamentoUsuarios =
          new JButton("Gerenciamento de usuários");
      gerenciamentoUsuarios.addActionListener(
          evento -> executarAcao(
              () -> presenter.abrirCadastroUsuarios()));
      funcionalidades.add(gerenciamentoUsuarios);
    }
    conteudo.add(funcionalidades, BorderLayout.CENTER);

    JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton sair = new JButton("Sair");
    sair.addActionListener(
        evento -> mostrarOpcoesDeSaida(presenter));
    rodape.add(sair);

    JPanel areaInferior = new JPanel(new BorderLayout(8, 8));
    areaInferior.add(new JLabel(
        "Consulte os arquivos autorizados na pasta segura.",
        SwingConstants.CENTER), BorderLayout.CENTER);
    areaInferior.add(rodape, BorderLayout.SOUTH);
    conteudo.add(areaInferior, BorderLayout.SOUTH);

    return conteudo;
  }

  private void mostrarOpcoesDeSaida(PrincipalPresenter presenter) {
    try {
      presenter.iniciarSaida();
    } catch (IllegalStateException erro) {
      exibirErroDeAuditoria(erro);
      return;
    }

    Object[] opcoes = {
        "Encerrar sessão",
        "Encerrar sistema",
        "Voltar ao menu"
    };
    int escolha = JOptionPane.showOptionDialog(
        this,
        "Escolha como deseja sair.",
        "Sair do Cofre Digital",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        opcoes,
        opcoes[0]);

    try {
      if (escolha == 0) {
        presenter.encerrarSessao();
      } else if (escolha == 1) {
        presenter.encerrarSistema();
      } else {
        presenter.voltarDaSaida();
      }
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
