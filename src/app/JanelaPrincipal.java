package app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import app.cadastro.CadastroModel;
import app.cadastro.CadastroPresenter;
import model.Usuario;

public final class JanelaPrincipal extends JFrame {

  public JanelaPrincipal(
      Usuario usuario,
      Connection connection,
      Runnable aoEncerrarSessao) {
    super("Cofre Digital");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(520, 340));
    setSize(600, 400);
    setLocationRelativeTo(null);
    setContentPane(criarConteudo(usuario, connection));
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent evento) {
        aoEncerrarSessao.run();
      }
    });
  }

  private JPanel criarConteudo(Usuario usuario, Connection connection) {
    JPanel conteudo = new JPanel(new BorderLayout(12, 12));
    conteudo.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

    JPanel cabecalho = new JPanel(new GridLayout(0, 1, 4, 4));
    cabecalho.add(new JLabel("Sessão autenticada"));
    cabecalho.add(new JLabel("Nome: " + usuario.getNome()));
    cabecalho.add(new JLabel("E-mail: " + usuario.getEmail()));
    cabecalho.add(new JLabel("Grupo: " + usuario.getGrupo()));
    conteudo.add(cabecalho, BorderLayout.NORTH);

    JPanel funcionalidades = new JPanel(new GridLayout(0, 1, 8, 8));
    funcionalidades.setBorder(BorderFactory.createTitledBorder("Funcionalidades"));
    JButton pastaSegura = new JButton("Pasta segura");
    pastaSegura.setEnabled(false);
    pastaSegura.setToolTipText(
        "A pasta segura será disponibilizada em uma próxima etapa.");
    funcionalidades.add(pastaSegura);

    if (usuario.getGid() == 1) {
      JButton gerenciamentoUsuarios = new JButton("Gerenciamento de usuários");
      gerenciamentoUsuarios.addActionListener(evento -> {
        CadastroPresenter cadastro = new CadastroPresenter(
            connection,
            CadastroModel.cadastroDeUsuario(usuario));
        cadastro.abrir(this, null);
      });
      funcionalidades.add(gerenciamentoUsuarios);
    }
    conteudo.add(funcionalidades, BorderLayout.CENTER);

    JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton encerrarSessao = new JButton("Encerrar sessão");
    encerrarSessao.addActionListener(evento -> dispose());
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
