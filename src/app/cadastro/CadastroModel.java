// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.cadastro;

import java.util.Arrays;

import model.Usuario;

public final class CadastroModel {

  private final Integer uidAdministrador;
  private final String loginAdministrador;
  private final String nomeAdministrador;
  private final String grupoAdministrador;
  private int totalUsuarios;
  private String grupoNovoUsuario;
  private String caminhoCertificado;
  private String caminhoChavePrivada;
  private char[] senha;
  private char[] confirmacaoSenha;
  private char[] fraseSecreta;

  private CadastroModel(
      Integer uidAdministrador,
      String loginAdministrador,
      String nomeAdministrador,
      String grupoAdministrador,
      int totalUsuarios,
      String grupoNovoUsuario) {
    this.uidAdministrador = uidAdministrador;
    this.loginAdministrador = loginAdministrador;
    this.nomeAdministrador = nomeAdministrador;
    this.grupoAdministrador = grupoAdministrador;
    this.totalUsuarios = totalUsuarios;
    this.grupoNovoUsuario = grupoNovoUsuario;
  }

  public static CadastroModel cadastroInicial(int totalUsuarios) {
    return new CadastroModel(
        null,
        null,
        null,
        null,
        totalUsuarios,
        "Administrador");
  }

  public static CadastroModel cadastroDeUsuario(
      Usuario administrador,
      int totalUsuarios) {
    if (administrador == null || administrador.getGid() != 1) {
      throw new IllegalArgumentException(
          "O cadastro de usuários exige uma sessão de administrador.");
    }
    if (totalUsuarios < 0) {
      throw new IllegalArgumentException(
          "O total de usuários não pode ser negativo.");
    }
    return new CadastroModel(
        administrador.getUid(),
        administrador.getEmail(),
        administrador.getNome(),
        administrador.getGrupo(),
        totalUsuarios,
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
    return "Total de usuários do sistema: " + totalUsuarios;
  }

  public int getTotalUsuarios() {
    return totalUsuarios;
  }

  public void registrarUsuarioCadastrado() {
    totalUsuarios++;
  }

  public String getLoginAdministrador() {
    return loginAdministrador;
  }

  public String getNomeAdministrador() {
    return nomeAdministrador;
  }

  public String getGrupoAdministrador() {
    return grupoAdministrador;
  }

  public String getGrupoNovoUsuario() {
    return grupoNovoUsuario;
  }

  public int getGidNovoUsuario() {
    return "Administrador".equals(grupoNovoUsuario) ? 1 : 2;
  }

  public void setGrupoNovoUsuario(String grupo) {
    if (isCadastroInicial()) {
      if (!"Administrador".equals(grupo)) {
        throw new IllegalArgumentException(
            "O cadastro inicial deve criar um administrador.");
      }
    } else if (!"Administrador".equals(grupo)
        && !"Usuário".equals(grupo)) {
      throw new IllegalArgumentException("O grupo selecionado é inválido.");
    }
    grupoNovoUsuario = grupo;
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
