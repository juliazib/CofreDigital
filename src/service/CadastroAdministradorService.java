package service;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import crypto.Base32;
import crypto.PasswordUtils;
import crypto.TokenUtils;
import database.UsuarioDAO;

public final class CadastroAdministradorService {

  private static final int GID_ADMINISTRADOR = 1;
  private static final int TAMANHO_SEGREDO_TOTP = 20;

  private final CadastroService cadastroService;
  private final UsuarioDAO usuarioDAO;

  public CadastroAdministradorService(Connection connection) {
    if (connection == null) {
      throw new IllegalArgumentException("A conexão não pode ser nula.");
    }

    this.cadastroService = new CadastroService(connection);
    this.usuarioDAO = new UsuarioDAO(connection);
  }

  public String cadastrar(
      String nome,
      String email,
      char[] senha,
      char[] confirmacaoSenha,
      String certificadoPem,
      byte[] chavePrivadaCifrada,
      char[] fraseSecreta) throws Exception {
    validarDados(nome, email, senha, confirmacaoSenha, fraseSecreta);
    if (usuarioDAO.existeAdministrador()) {
      throw new IllegalStateException(
          "Já existe um administrador cadastrado.");
    }

    String senhaTexto = new String(senha);
    String fraseSecretaTexto = new String(fraseSecreta);
    X509Certificate certificado =
        CertificadoChaveService.validarCertificadoPem(certificadoPem);
    PrivateKey chavePrivada = CertificadoChaveService
        .descriptografarChavePrivada(
            chavePrivadaCifrada,
            fraseSecretaTexto);

    if (!CertificadoChaveService.validarParChaves(
        chavePrivada,
        certificado)) {
      throw new IllegalArgumentException(
          "A chave privada não corresponde ao certificado.");
    }

    byte[] segredo = new byte[TAMANHO_SEGREDO_TOTP];
    new SecureRandom().nextBytes(segredo);
    try {
      String segredoBase32 = new Base32(
          Base32.Alphabet.BASE32,
          false,
          false).toString(segredo);

      byte[] tokenCifrado = TokenUtils.cifrar(segredoBase32, senhaTexto);
      cadastroService.cadastrar(
          nome.trim(),
          email.trim(),
          PasswordUtils.gerarHash(senhaTexto),
          tokenCifrado,
          GID_ADMINISTRADOR,
          certificadoPem,
          chavePrivadaCifrada);
      return segredoBase32;
    } finally {
      Arrays.fill(segredo, (byte) 0);
    }
  }

  private void validarDados(
      String nome,
      String email,
      char[] senha,
      char[] confirmacaoSenha,
      char[] fraseSecreta) throws SQLException {
    if (nome == null || nome.trim().isEmpty()) {
      throw new IllegalArgumentException("Informe o nome do administrador.");
    }
    if (email == null || !email.trim().matches(
        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
      throw new IllegalArgumentException("Informe um e-mail válido.");
    }
    if (senha == null || senha.length < 8 || senha.length > 10) {
      throw new IllegalArgumentException(
          "A senha deve conter de 8 a 10 dígitos.");
    }
    for (char caractere : senha) {
      if (caractere < '0' || caractere > '9') {
        throw new IllegalArgumentException(
            "A senha pessoal deve conter somente dígitos.");
      }
    }
    if (senhaRepetida(senha)) {
      throw new IllegalArgumentException(
          "A senha não pode conter uma sequência numérica repetida.");
    }
    if (confirmacaoSenha == null || !Arrays.equals(senha, confirmacaoSenha)) {
      throw new IllegalArgumentException(
          "A confirmação da senha não corresponde.");
    }
    if (fraseSecreta == null || fraseSecreta.length == 0) {
      throw new IllegalArgumentException(
          "Informe a frase secreta da chave privada.");
    }
    if (usuarioDAO.existeEmail(email.trim())) {
      throw new IllegalArgumentException(
          "Já existe um usuário cadastrado com esse e-mail.");
    }
  }

  private boolean senhaRepetida(char[] senha) {
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
