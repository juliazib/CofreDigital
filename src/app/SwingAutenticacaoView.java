package app;

import java.awt.Window;

import javax.swing.JOptionPane;

import app.ui.componentes.CampoSenha;
import app.ui.componentes.CampoTexto;

public final class SwingAutenticacaoView
    implements AutenticacaoPresenter.View {

  private final Window owner;

  public SwingAutenticacaoView(Window owner) {
    this.owner = owner;
  }

  @Override
  public String solicitarLogin() {
    CampoTexto campoLogin = new CampoTexto(24, 254);
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
  public char[] solicitarSenha(String email) {
    CampoSenha campoSenha = new CampoSenha(24, 255);
    int opcao = JOptionPane.showConfirmDialog(
        owner,
        new Object[] { "Senha pessoal para " + email + ":", campoSenha },
        "Autenticação",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (opcao != JOptionPane.OK_OPTION) {
      campoSenha.limpar();
      return null;
    }

    char[] senha = campoSenha.getPassword();
    campoSenha.limpar();
    return senha;
  }

  @Override
  public String solicitarCodigoTotp() {
    CampoTexto campoCodigo = new CampoTexto(8, 6);
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

}
