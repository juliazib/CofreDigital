package app.cadastro;

import java.util.Arrays;

import model.Usuario;

public final class CadastroModel {

  private final Integer uidAdministrador;
  private final String descricao;
  private final String grupoNovoUsuario;
  private String caminhoCertificado;
  private String caminhoChavePrivada;
  private char[] senha;
  private char[] confirmacaoSenha;
  private char[] fraseSecreta;

  private CadastroModel(
      Integer uidAdministrador,
      String descricao,
      String grupoNovoUsuario) {
    this.uidAdministrador = uidAdministrador;
    this.descricao = descricao;
    this.grupoNovoUsuario = grupoNovoUsuario;
  }

  public static CadastroModel cadastroInicial(int totalUsuarios) {
    return new CadastroModel(
        null,
        "Total de usuários do sistema: " + totalUsuarios,
        "Administrador");
  }

  public static CadastroModel cadastroDeUsuario(Usuario administrador) {
    if (administrador == null || administrador.getGid() != 1) {
      throw new IllegalArgumentException(
          "O cadastro de usuários exige uma sessão de administrador.");
    }
    return new CadastroModel(
        administrador.getUid(),
        "Administrador: " + administrador.getNome(),
        "Usuário");
  }

  public boolean isCadastroInicial() {
    return uidAdministrador == null;
  }

  public int getUidAdministrador() {
    if (uidAdministrador == null) {
      throw new IllegalStateException(
          "O cadastro inicial não tem administrador responsável.");
    }
    return uidAdministrador;
  }

  public String getDescricao() {
    return descricao;
  }

  public String getGrupoNovoUsuario() {
    return grupoNovoUsuario;
  }

  public void preencherFormulario(
      String caminhoCertificado,
      String caminhoChavePrivada,
      char[] senha,
      char[] confirmacaoSenha,
      char[] fraseSecreta) {
    limparCredenciais();
    this.caminhoCertificado = caminhoCertificado;
    this.caminhoChavePrivada = caminhoChavePrivada;
    this.senha = copiar(senha);
    this.confirmacaoSenha = copiar(confirmacaoSenha);
    this.fraseSecreta = copiar(fraseSecreta);
  }

  public String getCaminhoCertificado() {
    return caminhoCertificado;
  }

  public String getCaminhoChavePrivada() {
    return caminhoChavePrivada;
  }

  public char[] getSenha() {
    return copiar(senha);
  }

  public char[] getConfirmacaoSenha() {
    return copiar(confirmacaoSenha);
  }

  public char[] getFraseSecreta() {
    return copiar(fraseSecreta);
  }

  public void limparFormulario() {
    caminhoCertificado = null;
    caminhoChavePrivada = null;
    limparCredenciais();
  }

  private void limparCredenciais() {
    limpar(senha);
    limpar(confirmacaoSenha);
    limpar(fraseSecreta);
    senha = null;
    confirmacaoSenha = null;
    fraseSecreta = null;
  }

  private static char[] copiar(char[] valor) {
    return valor == null ? null : valor.clone();
  }

  private static void limpar(char[] valor) {
    if (valor != null) {
      Arrays.fill(valor, '\0');
    }
  }
}
