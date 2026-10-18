package app.inicializacao;

import java.awt.Window;

import javax.swing.JOptionPane;

import app.ui.componentes.CampoSenha;

public final class InicializacaoWindow
    implements InicializacaoPresenter.View {

  private final Window owner;

  InicializacaoWindow(Window owner) {
    this.owner = owner;
  }

  @Override
  public boolean solicitarFraseSecreta(InicializacaoModel model) {
    CampoSenha campoSenha = new CampoSenha(24, 255);
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
      campoSenha.limpar();
      return false;
    }

    char[] fraseSecreta = campoSenha.getPassword();
    campoSenha.limpar();
    model.preencherFraseSecreta(fraseSecreta);
    java.util.Arrays.fill(fraseSecreta, '\0');
    return true;
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

}
