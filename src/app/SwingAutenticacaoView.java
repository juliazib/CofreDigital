package app;

import java.awt.Window;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public final class SwingAutenticacaoView
    implements AutenticacaoPresenter.View {

  private final Window owner;

  public SwingAutenticacaoView(Window owner) {
    this.owner = owner;
  }

  @Override
  public String solicitarLogin() {
    JTextField campoLogin = new JTextField(24);
    int opcao = JOptionPane.showConfirmDialog(
        owner,
        new Object[] { "Login (e-mail):", campoLogin },
        "Autenticação",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    return opcao == JOptionPane.OK_OPTION
        ? campoLogin.getText().trim()
        : null;
  }

  @Override
  public char[] solicitarSenha() {
    JPasswordField campoSenha = new JPasswordField(24);
    int opcao = JOptionPane.showConfirmDialog(
        owner,
        new Object[] { "Senha pessoal:", campoSenha },
        "Autenticação",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (opcao != JOptionPane.OK_OPTION) {
      limparCampo(campoSenha);
      return null;
    }

    char[] senha = campoSenha.getPassword();
    limparCampo(campoSenha);
    return senha;
  }

  @Override
  public String solicitarCodigoTotp() {
    JTextField campoCodigo = new JTextField(8);
    int opcao = JOptionPane.showConfirmDialog(
        owner,
        new Object[] { "Código do autenticador (6 dígitos):", campoCodigo },
        "Autenticação em dois fatores",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    return opcao == JOptionPane.OK_OPTION
        ? campoCodigo.getText().trim()
        : null;
  }

  @Override
  public void exibirAviso(String mensagem) {
    JOptionPane.showMessageDialog(
        owner,
        mensagem,
        "Autenticação",
        JOptionPane.WARNING_MESSAGE);
  }

  public void exibirAutenticacaoConcluida(String nome) {
    JOptionPane.showMessageDialog(
        owner,
        "Autenticação concluída para " + nome + ". " +
            "O menu principal será implementado em uma próxima etapa.",
        "Acesso autorizado",
        JOptionPane.INFORMATION_MESSAGE);
  }

  private static void limparCampo(JPasswordField campo) {
    char[] conteudo = campo.getPassword();
    Arrays.fill(conteudo, '\0');
    campo.setText("");
  }
}
