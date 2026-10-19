// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package service;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.SQLException;

import database.ChaveiroDAO;
import database.ChaveiroDAO.ChaveiroRegistro;
import database.UsuarioDAO;
import model.Usuario;

public final class InicializacaoService {

  private static final int GRUPO_ADMINISTRADOR = 1;

  private final UsuarioDAO usuarioDAO;
  private final ChaveiroDAO chaveiroDAO;

  public InicializacaoService(Connection connection) {
    if (connection == null) {
      throw new ErroDeValidacao(
          "Não foi possível iniciar a validação do administrador.");
    }
    usuarioDAO = new UsuarioDAO(connection);
    chaveiroDAO = new ChaveiroDAO(connection);
  }

  public boolean deveCadastrarAdministrador() throws SQLException {
    return usuarioDAO.buscarAdministrador() == null;
  }

  public ContextoAdministrador validarAdministrador(char[] fraseSecreta)
      throws Exception {
    if (fraseSecreta == null || fraseSecreta.length == 0) {
      throw new ErroDeValidacao(
          "A frase secreta do administrador não pode ser vazia.");
    }

    Usuario administrador = usuarioDAO.buscarAdministrador();
    if (administrador == null
        || administrador.getGid() != GRUPO_ADMINISTRADOR) {
      throw new SQLException("Não há administrador cadastrado no sistema.");
    }

    ChaveiroRegistro chaveiro =
        chaveiroDAO.buscarPorUsuario(administrador.getUid());
    if (chaveiro == null) {
      throw new SQLException(
          "O administrador não possui certificado e chave cadastrados.");
    }

    X509Certificate certificado =
        CertificadoChaveService.validarCertificadoPem(
            chaveiro.getCertificadoPem());
    PrivateKey chavePrivada;
    try {
      chavePrivada = CertificadoChaveService.descriptografarChavePrivada(
          chaveiro.getChavePrivadaCifrada(),
          fraseSecreta);
    } catch (Exception erro) {
      throw new ErroDeValidacao(
          "Não foi possível abrir a chave privada do administrador. "
              + "Confira a frase secreta cadastrada.");
    }

    if (!CertificadoChaveService.validarParChaves(
        chavePrivada,
        certificado)) {
      throw new GeneralSecurityException(
          "A chave privada não corresponde ao certificado do administrador.");
    }

    return new ContextoAdministrador(
        chavePrivada,
        certificado,
        fraseSecreta);
  }
}
