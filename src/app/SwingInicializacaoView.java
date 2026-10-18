package app;

import java.awt.Window;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public final class SwingInicializacaoView
    implements InicializacaoPresenter.View {

  private final Window owner;

  public SwingInicializacaoView(Window owner) {
    this.owner = owner;
  }

  @Override
  public char[] solicitarFraseSecreta() {
    JPasswordField campoSenha = new JPasswordField(24);
    int opcao = JOptionPane.showConfirmDialog(
        owner,
        new Object[] {
            "Informe a frase secreta da chave privada do administrador:",
            campoSenha
        },
        "Inicialização do Cofre Digital",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);

    if (opcao != JOptionPane.OK_OPTION) {
      limparCampo(campoSenha);
      return null;
    }

    char[] fraseSecreta = campoSenha.getPassword();
    limparCampo(campoSenha);
    return fraseSecreta;
  }

  @Override
  public void exibirErro(String mensagem) {
    JOptionPane.showMessageDialog(
        owner,
        mensagem + "\nO sistema será encerrado.",
        "Falha na inicialização",
        JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void exibirChaveValidada() {
    JOptionPane.showMessageDialog(
        owner,
        "A chave privada do administrador foi validada. " +
            "O sistema iniciará a autenticação de usuários.",
        "Inicialização validada",
        JOptionPane.INFORMATION_MESSAGE);
  }

  private static void limparCampo(JPasswordField campo) {
    char[] conteudo = campo.getPassword();
    Arrays.fill(conteudo, '\0');
    campo.setText("");
  }
}
