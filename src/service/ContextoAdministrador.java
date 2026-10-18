package service;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public final class ContextoAdministrador implements AutoCloseable {

  private PrivateKey chavePrivada;
  private X509Certificate certificado;
  private char[] fraseSecreta;

  ContextoAdministrador(
      PrivateKey chavePrivada,
      X509Certificate certificado,
      char[] fraseSecreta) {
    this.chavePrivada = chavePrivada;
    this.certificado = certificado;
    this.fraseSecreta = fraseSecreta.clone();
  }

  public PrivateKey getChavePrivada() {
    garantirAberto();
    return chavePrivada;
  }

  public X509Certificate getCertificado() {
    garantirAberto();
    return certificado;
  }

  @Override
  public void close() {
    if (fraseSecreta != null) {
      Arrays.fill(fraseSecreta, '\0');
      fraseSecreta = null;
    }
    chavePrivada = null;
    certificado = null;
  }

  private void garantirAberto() {
    if (fraseSecreta == null) {
      throw new IllegalStateException(
          "O contexto do administrador já foi encerrado.");
    }
  }
}
