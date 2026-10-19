// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package app.principal;

import model.Usuario;

public final class PrincipalModel {

  private final Usuario usuario;

  public PrincipalModel(Usuario usuario) {
    if (usuario == null) {
      throw new IllegalArgumentException(
          "A janela principal exige um usuário autenticado.");
    }
    this.usuario = usuario;
  }

  public String getNomeUsuario() {
    return usuario.getNome();
  }

  public String getEmailUsuario() {
    return usuario.getEmail();
  }

  public String getGrupoUsuario() {
    return usuario.getGrupo();
  }

  public boolean podeGerenciarUsuarios() {
    return usuario.getGid() == 1;
  }

  Usuario getUsuarioAutenticado() {
    return usuario;
  }
}
