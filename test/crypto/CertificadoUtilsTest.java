
package test.crypto;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.junit.Test;

import crypto.CertificadoUtils;

public class CertificadoUtilsTest {

    private static final String CERTIFICADO_ADMIN = "test/resources/certificados/admin-x509.crt";

    @Test
    public void deveLerCertificadoX509DoAdministrador() throws Exception {
        X509Certificate certificado = carregarCertificado(
            CERTIFICADO_ADMIN
        );

        assertNotNull(certificado);
        assertNotNull(certificado.getSubjectX500Principal());
        assertNotNull(certificado.getIssuerX500Principal());
        assertNotNull(certificado.getSerialNumber());
        assertNotNull(certificado.getNotBefore());
        assertNotNull(certificado.getNotAfter());
        assertNotNull(certificado.getPublicKey());
    }

    @Test
    public void certificadoDeveEstarDentroDoPeriodoDeValidade()
            throws Exception {
        X509Certificate certificado = carregarCertificado(
            CERTIFICADO_ADMIN
        );

        certificado.checkValidity();
    }

    private X509Certificate carregarCertificado(String caminho)
            throws Exception {
        CertificateFactory factory =
            CertificateFactory.getInstance("X.509");

        try (InputStream input = new FileInputStream(caminho)) {
            return (X509Certificate) factory.generateCertificate(input);
        }
    }

    @Test
    public void deveExtrairNomeDoCertificado() throws Exception {
        X509Certificate certificado = carregarCertificado(CERTIFICADO_ADMIN);

        String nome = CertificadoUtils.extrairNome(certificado);

        assertNotNull(nome);
        assertFalse(nome.trim().isEmpty());
    }

    @Test
    public void deveExtrairEmailDoCertificado() throws Exception {
        X509Certificate certificado = carregarCertificado(CERTIFICADO_ADMIN);

        String email = CertificadoUtils.extrairEmail(certificado);

        assertNotNull(email);
        assertFalse(email.trim().isEmpty());
    }


    
}
