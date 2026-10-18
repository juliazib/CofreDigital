package app.principal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class PrincipalWindow extends JFrame {

  PrincipalWindow(
      Window owner,
      PrincipalModel model,
      PrincipalPresenter presenter) {
    super("Cofre Digital");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(520, 340));
    setSize(600, 400);
    setLocationRelativeTo(owner);
    setContentPane(criarConteudo(model, presenter));
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
    conteudo.add(cabecalho, BorderLayout.NORTH);

    JPanel funcionalidades = new JPanel(new GridLayout(0, 1, 8, 8));
    funcionalidades.setBorder(
        BorderFactory.createTitledBorder("Funcionalidades"));
    JButton pastaSegura = new JButton("Pasta segura");
    pastaSegura.setEnabled(false);
    pastaSegura.setToolTipText(
        "A pasta segura será disponibilizada em uma próxima etapa.");
    funcionalidades.add(pastaSegura);

    if (model.podeGerenciarUsuarios()) {
      JButton gerenciamentoUsuarios =
          new JButton("Gerenciamento de usuários");
      gerenciamentoUsuarios.addActionListener(
          evento -> presenter.abrirCadastroUsuarios());
      funcionalidades.add(gerenciamentoUsuarios);
    }
    conteudo.add(funcionalidades, BorderLayout.CENTER);

    JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton encerrarSessao = new JButton("Encerrar sessão");
    encerrarSessao.addActionListener(evento -> presenter.encerrarSessao());
    rodape.add(encerrarSessao);

    JPanel areaInferior = new JPanel(new BorderLayout(8, 8));
    areaInferior.add(new JLabel(
        "As funcionalidades serão habilitadas conforme forem implementadas.",
        SwingConstants.CENTER), BorderLayout.CENTER);
    areaInferior.add(rodape, BorderLayout.SOUTH);
    conteudo.add(areaInferior, BorderLayout.SOUTH);

    return conteudo;
  }
}
