package service;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import crypto.Base32;
import crypto.CertificadoUtils;
import crypto.PasswordUtils;
import crypto.TokenUtils;
import database.ChaveiroDAO;
import database.UsuarioDAO;

public final class CadastroService {

  private static final int TAMANHO_SEGREDO_TOTP = 20;

  private final Connection connection;
  private final UsuarioDAO usuarioDAO;
  private final ChaveiroDAO chaveiroDAO;

  public CadastroService(Connection connection) {
    if (connection == null) {
      throw new ErroDeValidacao(
          "Não foi possível iniciar o cadastro. Tente novamente.");
    }
    this.connection = connection;
    usuarioDAO = new UsuarioDAO(connection);
    chaveiroDAO = new ChaveiroDAO(connection);
  }

  public String cadastrarAdministrador(
      char[] senha,
      char[] confirmacaoSenha,
      String certificadoPem,
      byte[] chavePrivadaCifrada,
      char[] fraseSecreta) throws Exception {
    if (usuarioDAO.existeAdministrador()) {
      throw new IllegalStateException(
          "Já existe um administrador cadastrado.");
    }
    return cadastrarComCertificado(
        senha,
        confirmacaoSenha,
        certificadoPem,
        chavePrivadaCifrada,
        fraseSecreta,
        1);
  }

  public String cadastrarUsuario(
      int uidAdministrador,
      char[] senha,
      char[] confirmacaoSenha,
      String certificadoPem,
      byte[] chavePrivadaCifrada,
      char[] fraseSecreta) throws Exception {
    if (uidAdministrador <= 0) {
      throw new SecurityException(
          "Somente um administrador pode cadastrar usuários.");
    }
    model.Usuario administrador = usuarioDAO.buscarPorId(uidAdministrador);
    if (administrador == null || administrador.getGid() != 1) {
      throw new SecurityException(
          "Somente um administrador pode cadastrar usuários.");
    }
    return cadastrarComCertificado(
        senha,
        confirmacaoSenha,
        certificadoPem,
        chavePrivadaCifrada,
        fraseSecreta,
        2);
  }

  private String cadastrarComCertificado(
      char[] senha,
      char[] confirmacaoSenha,
      String certificadoPem,
      byte[] chavePrivadaCifrada,
      char[] fraseSecreta,
      int gid) throws Exception {
    validarCredenciais(senha, confirmacaoSenha, fraseSecreta);
    if (gid != 1 && gid != 2) {
      throw new ErroDeValidacao("O grupo selecionado é inválido.");
    }

    X509Certificate certificado =
        CertificadoChaveService.validarCertificadoPem(certificadoPem);
    String nome;
    String email;
    try {
      nome = CertificadoUtils.extrairNome(certificado);
      email = CertificadoUtils.extrairEmail(certificado);
    } catch (Exception erro) {
      throw new ErroDeValidacao(
          "Não foi possível ler os dados de identificação do certificado.");
    }
    validarIdentidade(nome, email, gid == 1 ? "administrador" : "usuário");

    PrivateKey chavePrivada;
    try {
      chavePrivada = CertificadoChaveService.descriptografarChavePrivada(
          chavePrivadaCifrada,
          fraseSecreta);
    } catch (Exception erro) {
      throw new ErroDeValidacao(
          "Não foi possível abrir a chave privada. Confira a frase secreta "
              + "e o arquivo selecionado.",
          6006);
    }
    if (!CertificadoChaveService.validarParChaves(
        chavePrivada,
        certificado)) {
      throw new ErroDeValidacao(
          "A chave privada não corresponde ao certificado.",
          6007);
    }

    byte[] segredo = new byte[TAMANHO_SEGREDO_TOTP];
    new SecureRandom().nextBytes(segredo);
    try {
      String senhaTexto = new String(senha);
      String segredoBase32 = new Base32(
          Base32.Alphabet.BASE32,
          false,
          false).toString(segredo);
      byte[] tokenCifrado = TokenUtils.cifrar(segredoBase32, senhaTexto);
      cadastrar(
          nome.trim(),
          email.trim(),
          PasswordUtils.gerarHash(senhaTexto),
          tokenCifrado,
          gid,
          certificadoPem,
          chavePrivadaCifrada);
      return segredoBase32;
    } finally {
      Arrays.fill(segredo, (byte) 0);
    }
  }

  public int cadastrar(
      String nome,
      String email,
      String senhaHash,
      byte[] tokenCifrado,
      int gid,
      String certificadoPem,
      byte[] chavePrivadaCifrada) throws SQLException {
    boolean gerenciaTransacao = connection.getAutoCommit();
    if (gerenciaTransacao) {
      connection.setAutoCommit(false);
    }

    try {
      int uid = usuarioDAO.inserir(
          nome,
          email,
          senhaHash,
          tokenCifrado,
          gid);
      chaveiroDAO.inserir(
          uid,
          certificadoPem,
          chavePrivadaCifrada);

      if (gerenciaTransacao) {
        connection.commit();
      }
      return uid;
    } catch (SQLException | RuntimeException erro) {
      if (gerenciaTransacao) {
        try {
          connection.rollback();
        } catch (SQLException erroRollback) {
          erro.addSuppressed(erroRollback);
        }
      }
      throw erro;
    } finally {
      if (gerenciaTransacao) {
        connection.setAutoCommit(true);
      }
    }
  }

  private void validarCredenciais(
      char[] senha,
      char[] confirmacaoSenha,
      char[] fraseSecreta) {
    if (senha == null || senha.length < 8 || senha.length > 10) {
      throw new ErroDeValidacao(
          "A senha deve conter de 8 a 10 dígitos.",
          6003);
    }
    for (char caractere : senha) {
      if (caractere < '0' || caractere > '9') {
        throw new ErroDeValidacao(
            "A senha pessoal deve conter somente dígitos.",
            6003);
      }
    }
    if (senhaRepetida(senha)) {
      throw new ErroDeValidacao(
          "A senha não pode conter uma sequência numérica repetida.",
          6003);
    }
    if (confirmacaoSenha == null || !Arrays.equals(senha, confirmacaoSenha)) {
      throw new ErroDeValidacao(
          "A confirmação da senha não corresponde.",
          6003);
    }
    if (fraseSecreta == null || fraseSecreta.length == 0) {
      throw new ErroDeValidacao(
          "Informe a frase secreta da chave privada.",
          6006);
    }
  }

  private void validarIdentidade(
      String nome,
      String email,
      String tipoUsuario) throws SQLException {
    if (nome == null || nome.trim().isEmpty()) {
      throw new ErroDeValidacao(
          "O certificado não contém o nome do " + tipoUsuario + ".");
    }
    if (email == null || !email.trim().matches(
        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
      throw new ErroDeValidacao(
          "O certificado não contém um e-mail válido.");
    }
    if (usuarioDAO.existeEmail(email.trim())) {
      throw new ErroDeValidacao(
          "Já existe um usuário cadastrado com esse e-mail.");
    }
  }

  private static boolean senhaRepetida(char[] senha) {
    for (int tamanhoPadrao = 1;
        tamanhoPadrao <= senha.length / 2;
        tamanhoPadrao++) {
      if (senha.length % tamanhoPadrao != 0) {
        continue;
      }
      boolean repetida = true;
      for (int i = tamanhoPadrao; i < senha.length; i++) {
        if (senha[i] != senha[i % tamanhoPadrao]) {
          repetida = false;
          break;
        }
      }
      if (repetida) {
        return true;
      }
    }
    return false;
  }
}
