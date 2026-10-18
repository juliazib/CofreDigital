package crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import database.ChaveiroDAO;
import database.UsuarioDAO;
import model.Usuario;
import service.AuditoriaService;
import service.CertificadoChaveService;
import service.ContextoAdministrador;

public final class PastaSeguraCrypto {

  private final ChaveiroDAO chaveiroDAO;
  private final UsuarioDAO usuarioDAO;
  private final ContextoAdministrador contextoAdministrador;
  private final AuditoriaService auditoria;

  public PastaSeguraCrypto(
      java.sql.Connection connection,
      ContextoAdministrador contextoAdministrador,
      AuditoriaService auditoria) {
    chaveiroDAO = new ChaveiroDAO(connection);
    usuarioDAO = new UsuarioDAO(connection);
    this.contextoAdministrador = contextoAdministrador;
    this.auditoria = auditoria;
  }

  public PrivateKey validarChaveUsuario(
      Usuario usuario,
      char[] fraseSecreta) throws GeneralSecurityException {
    try {
      ChaveiroDAO.ChaveiroRegistro registro =
          chaveiroDAO.buscarPorUsuario(usuario.getUid());
      if (registro == null) {
        throw new GeneralSecurityException(
            "O usuário não possui certificado e chave cadastrados.");
      }
      X509Certificate certificado =
          CertificadoChaveService.validarCertificadoPem(
              registro.getCertificadoPem());
      String emailCertificado = CertificadoUtils.extrairEmail(certificado);
      if (emailCertificado == null
          || !usuario.getEmail().equalsIgnoreCase(emailCertificado)) {
        throw new GeneralSecurityException(
            "O certificado digital não pertence ao usuário autenticado.");
      }

      byte[] chavePrivadaCifrada = registro.getChavePrivadaCifrada();
      PrivateKey chavePrivada;
      try {
        chavePrivada =
            CertificadoChaveService.descriptografarChavePrivada(
                chavePrivadaCifrada,
                fraseSecreta);
      } finally {
        Arrays.fill(chavePrivadaCifrada, (byte) 0);
      }
      if (!CertificadoChaveService.validarParChaves(
          chavePrivada,
          certificado)) {
        throw new GeneralSecurityException(
            "A chave privada não corresponde ao certificado do usuário.");
      }
      return chavePrivada;
    } catch (GeneralSecurityException erro) {
      throw erro;
    } catch (Exception erro) {
      throw new GeneralSecurityException(
          "Não foi possível validar a frase secreta do usuário.",
          erro);
    }
  }

  public byte[] lerIndiceAssinado(
      java.nio.file.Path pasta,
      String nome,
      Usuario usuario) throws IOException, GeneralSecurityException,
      SQLException {
    int codigoSucessoDecriptacao = nome.equals("index") ? 7005 : 7010;
    int codigoSucessoVerificacao = nome.equals("index") ? 7006 : 7011;
    int codigoFalhaDecriptacao = nome.equals("index") ? 7007 : 7012;
    int codigoFalhaVerificacao = nome.equals("index") ? 7008 : 7013;
    byte[] conteudo;
    try {
      byte[] criptograma =
          java.nio.file.Files.readAllBytes(pasta.resolve(nome + ".enc"));
      byte[] envelope =
          java.nio.file.Files.readAllBytes(pasta.resolve(nome + ".env"));
      conteudo = descriptografar(
          criptograma,
          envelope,
          contextoAdministrador.getChavePrivada());
    } catch (IOException | GeneralSecurityException erro) {
      auditoria.registrar(codigoFalhaDecriptacao, usuario, null);
      throw erro;
    }
    auditoria.registrar(codigoSucessoDecriptacao, usuario, null);

    byte[] assinatura;
    try {
      assinatura =
          java.nio.file.Files.readAllBytes(pasta.resolve(nome + ".asd"));
    } catch (IOException erro) {
      auditoria.registrar(codigoFalhaVerificacao, usuario, null);
      Arrays.fill(conteudo, (byte) 0);
      throw erro;
    }

    boolean assinaturaValida = false;
    boolean falhaVerificacaoRegistrada = false;
    try {
      Signature verificador = Signature.getInstance("SHA1withRSA");
      verificador.initVerify(contextoAdministrador.getCertificado());
      verificador.update(conteudo);
      assinaturaValida = verificador.verify(assinatura);
      if (!assinaturaValida) {
        auditoria.registrar(codigoFalhaVerificacao, usuario, null);
        falhaVerificacaoRegistrada = true;
        throw new GeneralSecurityException(
            "A assinatura digital do arquivo " + nome + " é inválida.");
      }
      auditoria.registrar(codigoSucessoVerificacao, usuario, null);
      return conteudo;
    } catch (GeneralSecurityException erro) {
      if (!assinaturaValida && !falhaVerificacaoRegistrada) {
        auditoria.registrar(codigoFalhaVerificacao, usuario, null);
      }
      throw erro;
    } finally {
      if (!assinaturaValida) {
        Arrays.fill(conteudo, (byte) 0);
      }
    }
  }

  public byte[] descriptografarArquivo(
      byte[] criptograma,
      byte[] envelope,
      PrivateKey chavePrivada) throws GeneralSecurityException {
    return descriptografar(criptograma, envelope, chavePrivada);
  }

  public void verificarAssinaturaArquivo(
      byte[] conteudo,
      byte[] assinatura,
      String emailDono) throws GeneralSecurityException, SQLException {
    X509Certificate certificadoDono = carregarCertificadoDono(emailDono);
    Signature verificador = Signature.getInstance("SHA1withRSA");
    verificador.initVerify(certificadoDono.getPublicKey());
    verificador.update(conteudo);
    if (!verificador.verify(assinatura)) {
      throw new GeneralSecurityException(
          "A assinatura digital do arquivo é inválida.");
    }
  }

  private byte[] descriptografar(
      byte[] criptograma,
      byte[] envelope,
      PrivateKey chavePrivada) throws GeneralSecurityException {
    byte[] semente = null;
    try {
      Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      rsa.init(Cipher.DECRYPT_MODE, chavePrivada);
      semente = rsa.doFinal(envelope);

      SecureRandom aleatorio = SecureRandom.getInstance("SHA1PRNG");
      aleatorio.setSeed(semente);
      KeyGenerator gerador = KeyGenerator.getInstance("AES");
      gerador.init(256, aleatorio);
      SecretKey chave = gerador.generateKey();

      Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
      aes.init(Cipher.DECRYPT_MODE, chave);
      return aes.doFinal(criptograma);
    } finally {
      if (semente != null) {
        Arrays.fill(semente, (byte) 0);
      }
    }
  }

  private X509Certificate carregarCertificadoDono(String email)
      throws GeneralSecurityException, SQLException {
    try {
      if (email.equalsIgnoreCase(
          CertificadoUtils.extrairEmail(
              contextoAdministrador.getCertificado()))) {
        return contextoAdministrador.getCertificado();
      }
      Usuario dono = usuarioDAO.buscarPorEmail(email);
      if (dono == null) {
        throw new GeneralSecurityException(
            "O proprietário do arquivo não está cadastrado.");
      }
      ChaveiroDAO.ChaveiroRegistro registro =
          chaveiroDAO.buscarPorUsuario(dono.getUid());
      if (registro == null) {
        throw new GeneralSecurityException(
            "O proprietário não possui certificado cadastrado.");
      }
      X509Certificate certificado =
          CertificadoChaveService.validarCertificadoPem(
              registro.getCertificadoPem());
      String emailCertificado = CertificadoUtils.extrairEmail(certificado);
      if (emailCertificado == null
          || !email.equalsIgnoreCase(emailCertificado)) {
        throw new GeneralSecurityException(
            "O certificado do proprietário não corresponde ao índice.");
      }
      return certificado;
    } catch (GeneralSecurityException erro) {
      throw erro;
    } catch (Exception erro) {
      throw new GeneralSecurityException(
          "Não foi possível validar o certificado do proprietário.",
          erro);
    }
  }
}
