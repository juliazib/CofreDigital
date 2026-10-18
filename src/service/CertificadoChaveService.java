package service;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

public class CertificadoChaveService {

  /**
   * Valida um certificado X.509 a partir de sua representação em string PEM.
   */
  public static X509Certificate validarCertificadoPem(String certificadoPem) throws Exception {
    if (certificadoPem == null || certificadoPem.trim().isEmpty()) {
      throw new IllegalArgumentException("O certificado PEM não pode ser vazio.");
    }

    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    try (ByteArrayInputStream bais = new ByteArrayInputStream(certificadoPem.getBytes(StandardCharsets.UTF_8))) {
      X509Certificate certificado = (X509Certificate) factory.generateCertificate(bais);

      // Valida o período de validade
      certificado.checkValidity(new Date());

      return certificado;
    } catch (Exception e) {
      throw new Exception("Certificado X.509 inválido ou expirado: " + e.getMessage(), e);
    }
  }

  /**
   * Descriptografa a chave privada (PKCS#8 cifrada ou PEM tradicional cifrado)
   * usando a frase secreta.
   */
  public static PrivateKey descriptografarChavePrivada(byte[] chavePrivadaCifrada, String senha) throws Exception {
    if (chavePrivadaCifrada == null || chavePrivadaCifrada.length == 0) {
      throw new IllegalArgumentException("A chave privada cifrada não pode ser vazia.");
    }
    if (senha == null) {
      throw new IllegalArgumentException("A senha para descriptografar a chave privada não pode ser nula.");
    }

    try (PEMParser pemParser = new PEMParser(new InputStreamReader(
        new ByteArrayInputStream(chavePrivadaCifrada), StandardCharsets.UTF_8))) {

      Object obj = pemParser.readObject();
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

      if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {
        // Formato PKCS#8 cifrado com AES (comum em admin-pkcs8-aes.key /
        // user01-pkcs8-aes.key)
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) obj;
        org.bouncycastle.operator.InputDecryptorProvider decProv = new JceOpenSSLPKCS8DecryptorProviderBuilder()
            .setProvider("BC")
            .build(senha.toCharArray());
        PrivateKeyInfo privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decProv);
        return converter.getPrivateKey(privateKeyInfo);

      } else if (obj instanceof PEMEncryptedKeyPair) {
        // Formato tradicional OpenSSL cifrado
        PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) obj;
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
            .setProvider("BC")
            .build(senha.toCharArray());
        PEMKeyPair pemKeyPair = encryptedKeyPair.decryptKeyPair(decProv);
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
}