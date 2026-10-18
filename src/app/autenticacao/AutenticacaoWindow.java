package app.autenticacao;

import java.awt.Window;
import java.util.Arrays;

import javax.swing.JOptionPane;

import app.ui.componentes.CampoTexto;
import app.ui.componentes.EntradaSenhaVirtual;

public final class AutenticacaoWindow
    implements AutenticacaoPresenter.View {

  private final Window owner;

  AutenticacaoWindow(Window owner) {
    this.owner = owner;
  }

  @Override
  public boolean solicitarLogin(AutenticacaoModel model) {
    CampoTexto campoLogin = new CampoTexto(24, 254);
    int opcao = JOptionPane.showConfirmDialog(
        owner,
        new Object[] { "Login (e-mail):", campoLogin },
        "Autenticação",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (opcao != JOptionPane.OK_OPTION) {
      return false;
    }
    model.preencherLogin(campoLogin.getText().trim());
    return true;
  }

  @Override
  public boolean solicitarSenha(AutenticacaoModel model) {
    char[][] pares = EntradaSenhaVirtual.solicitarPares(owner, 8, 10);
    if (pares == null) {
      return false;
    }
    try {
      model.preencherParesSenha(pares);
    } finally {
      for (char[] par : pares) {
        Arrays.fill(par, '\0');
      }
    }
    return true;
  }

  @Override
  public boolean solicitarCodigoTotp(AutenticacaoModel model) {
    CampoTexto campoCodigo = new CampoTexto(8, 6);
    int opcao = JOptionPane.showConfirmDialog(
        owner,
        new Object[] { "Código do autenticador (6 dígitos):", campoCodigo },
        "Autenticação em dois fatores",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (opcao != JOptionPane.OK_OPTION) {
      return false;
    }
    model.preencherCodigoTotp(campoCodigo.getText().trim());
    return true;
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
