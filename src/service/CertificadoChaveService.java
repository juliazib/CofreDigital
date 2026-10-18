package service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

public class CertificadoChaveService {

  private static final int TAMANHO_DESAFIO_CADASTRO = 10_240;

  /**
   * Valida um certificado X.509 a partir de sua representação em string PEM.
   */
  public static X509Certificate validarCertificadoPem(String certificadoPem) throws Exception {
    if (certificadoPem == null || certificadoPem.trim().isEmpty()) {
      throw new ErroDeValidacao("Selecione o arquivo do certificado.");
    }

    try {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      try (ByteArrayInputStream bais = new ByteArrayInputStream(
          certificadoPem.getBytes(StandardCharsets.UTF_8))) {
        X509Certificate certificado =
            (X509Certificate) factory.generateCertificate(bais);
        certificado.checkValidity(new Date());
        return certificado;
      }
    } catch (Exception erro) {
      throw new ErroDeValidacao(
          "O certificado selecionado é inválido, expirou ou ainda não é válido.");
    }
  }

  public static boolean validarParChaves(
      PrivateKey chavePrivada,
      X509Certificate certificado) throws GeneralSecurityException {
    if (certificado == null) {
      throw new IllegalArgumentException("O certificado não pode ser nulo.");
    }
    return validarParChaves(chavePrivada, certificado.getPublicKey());
  }

  private static boolean validarParChaves(
      PrivateKey chavePrivada,
      PublicKey chavePublica) throws GeneralSecurityException {
    if (chavePrivada == null || chavePublica == null) {
      throw new IllegalArgumentException(
          "As chaves privada e pública não podem ser nulas.");
    }

    byte[] desafio = new byte[TAMANHO_DESAFIO_CADASTRO];
    new SecureRandom().nextBytes(desafio);

    Signature assinatura = Signature.getInstance("SHA1withRSA");
    assinatura.initSign(chavePrivada);
    assinatura.update(desafio);
    byte[] assinaturaDigital = assinatura.sign();

    assinatura.initVerify(chavePublica);
    assinatura.update(desafio);
    return assinatura.verify(assinaturaDigital);
  }

  /**
   * Descriptografa a chave privada (PKCS#8 cifrada ou PEM tradicional cifrado)
   * usando a frase secreta.
   */
  public static PrivateKey descriptografarChavePrivada(byte[] chavePrivadaCifrada, String senha) throws Exception {
    if (senha == null) {
      throw new IllegalArgumentException("A senha para descriptografar a chave privada não pode ser nula.");
    }
    char[] senhaCaracteres = senha.toCharArray();
    try {
      return descriptografarChavePrivada(
          chavePrivadaCifrada,
          senhaCaracteres);
    } finally {
      Arrays.fill(senhaCaracteres, '\0');
    }
  }

  public static PrivateKey descriptografarChavePrivada(
      byte[] chavePrivadaCifrada,
      char[] fraseSecreta) throws Exception {
    if (chavePrivadaCifrada == null || chavePrivadaCifrada.length == 0) {
      throw new IllegalArgumentException("A chave privada cifrada não pode ser vazia.");
    }
    if (fraseSecreta == null) {
      throw new IllegalArgumentException("A senha para descriptografar a chave privada não pode ser nula.");
    }

    if (!comecaComPem(chavePrivadaCifrada)) {
      return descriptografarChaveBinaria(
          chavePrivadaCifrada,
          fraseSecreta);
    }

    registrarProviderBouncyCastle();

    try (PEMParser pemParser = new PEMParser(new InputStreamReader(
        new ByteArrayInputStream(chavePrivadaCifrada), StandardCharsets.UTF_8))) {

      Object obj = pemParser.readObject();
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

      if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {
        // Formato PKCS#8 cifrado com AES (comum em admin-pkcs8-aes.key /
        // user01-pkcs8-aes.key)
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) obj;
        char[] senhaTemporaria = fraseSecreta.clone();
        PrivateKeyInfo privateKeyInfo;
        try {
          org.bouncycastle.operator.InputDecryptorProvider decProv =
              new JceOpenSSLPKCS8DecryptorProviderBuilder()
                  .setProvider("BC")
                  .build(senhaTemporaria);
          privateKeyInfo =
              encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decProv);
        } finally {
          Arrays.fill(senhaTemporaria, '\0');
        }
        return converter.getPrivateKey(privateKeyInfo);

      } else if (obj instanceof PEMEncryptedKeyPair) {
        // Formato tradicional OpenSSL cifrado
        PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) obj;
        char[] senhaTemporaria = fraseSecreta.clone();
        PEMKeyPair pemKeyPair;
        try {
          PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
              .setProvider("BC")
              .build(senhaTemporaria);
          pemKeyPair = encryptedKeyPair.decryptKeyPair(decProv);
        } finally {
          Arrays.fill(senhaTemporaria, '\0');
        }
        return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());

      } else if (obj instanceof PrivateKeyInfo) {
        // Chave PKCS#8 não cifrada
        return converter.getPrivateKey((PrivateKeyInfo) obj);

      } else if (obj instanceof PEMKeyPair) {
        // Chave PEM não cifrada
        return converter.getKeyPair((PEMKeyPair) obj).getPrivate();

      } else {
        throw new Exception("Formato de chave privada desconhecido ou não suportado: " +
            (obj != null ? obj.getClass().getName() : "nulo"));
      }
    }
  }

  private static PrivateKey descriptografarChaveBinaria(
      byte[] chaveCifrada,
      char[] fraseSecreta) throws GeneralSecurityException {
    try {
      SecretKey chaveAes = gerarChaveAes(fraseSecreta);

      Cipher cifra = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cifra.init(Cipher.DECRYPT_MODE, chaveAes);
      byte[] chavePrivadaCodificada = cifra.doFinal(chaveCifrada);
      try {
        if (comecaComPem(chavePrivadaCodificada)) {
          registrarProviderBouncyCastle();
          try (PEMParser pemParser = new PEMParser(new InputStreamReader(
              new ByteArrayInputStream(chavePrivadaCodificada),
              StandardCharsets.US_ASCII))) {
            Object chavePem = pemParser.readObject();
            JcaPEMKeyConverter converter =
                new JcaPEMKeyConverter().setProvider("BC");
            if (chavePem instanceof PrivateKeyInfo) {
              return converter.getPrivateKey((PrivateKeyInfo) chavePem);
            }
            if (chavePem instanceof PEMKeyPair) {
              return converter.getKeyPair((PEMKeyPair) chavePem).getPrivate();
            }
            throw new GeneralSecurityException(
                "O arquivo não contém uma chave privada PKCS#8 válida.");
          } catch (IOException erro) {
            throw new GeneralSecurityException(
                "Não foi possível ler a chave privada PKCS#8 descriptografada.",
                erro);
          }
        }

        byte[] chavePrivadaBase64 =
            removerEspacosAscii(chavePrivadaCodificada);
        byte[] chavePrivadaDer = null;
        try {
          chavePrivadaDer = Base64.getDecoder().decode(
              chavePrivadaBase64);
          return KeyFactory.getInstance("RSA").generatePrivate(
              new PKCS8EncodedKeySpec(chavePrivadaDer));
        } finally {
          Arrays.fill(chavePrivadaBase64, (byte) 0);
          if (chavePrivadaDer != null) {
            Arrays.fill(chavePrivadaDer, (byte) 0);
          }
        }
      } finally {
        Arrays.fill(chavePrivadaCodificada, (byte) 0);
      }
    } catch (GeneralSecurityException erro) {
      throw new GeneralSecurityException(
          "Não foi possível descriptografar a chave privada. " +
              "Verifique a frase secreta e o formato do arquivo.",
          erro);
    } catch (IllegalArgumentException erro) {
      throw new GeneralSecurityException(
          "A chave privada descriptografada não contém PKCS#8 válido.",
          erro);
    }
  }

  private static SecretKey gerarChaveAes(char[] fraseSecreta)
      throws GeneralSecurityException {
    ByteBuffer codificado =
        StandardCharsets.UTF_8.encode(CharBuffer.wrap(fraseSecreta));
    byte[] semente = new byte[codificado.remaining()];
    codificado.get(semente);
    try {
      SecureRandom geradorAleatorio =
          SecureRandom.getInstance("SHA1PRNG");
      geradorAleatorio.setSeed(semente);

      KeyGenerator geradorChave = KeyGenerator.getInstance("AES");
      geradorChave.init(256, geradorAleatorio);
      return geradorChave.generateKey();
    } finally {
      Arrays.fill(semente, (byte) 0);
      if (codificado.hasArray()) {
        Arrays.fill(codificado.array(), (byte) 0);
      }
    }
  }

  private static byte[] removerEspacosAscii(byte[] dados) {
    int tamanho = 0;
    for (byte dado : dados) {
      if (dado == ' ' || dado == '\t' || dado == '\r' || dado == '\n') {
        continue;
      }
      tamanho++;
    }

    byte[] resultado = new byte[tamanho];
    int indice = 0;
    for (byte dado : dados) {
      if (dado != ' ' && dado != '\t' && dado != '\r' && dado != '\n') {
        resultado[indice++] = dado;
      }
    }
    return resultado;
  }

  private static void registrarProviderBouncyCastle() {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  private static boolean comecaComPem(byte[] conteudo) {
    byte[] cabecalhoPem = "-----BEGIN ".getBytes(StandardCharsets.US_ASCII);
    int inicio = 0;
    while (inicio < conteudo.length
        && (conteudo[inicio] == ' ' || conteudo[inicio] == '\t'
            || conteudo[inicio] == '\r' || conteudo[inicio] == '\n')) {
      inicio++;
    }
    if (conteudo.length - inicio < cabecalhoPem.length) {
      return false;
    }
    for (int i = 0; i < cabecalhoPem.length; i++) {
      if (conteudo[inicio + i] != cabecalhoPem[i]) {
        return false;
      }
    }
    return true;
  }
}