// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package crypto;

import java.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

public class CertificadoUtils {

    public static String extrairNome(X509Certificate certificado)
            throws Exception {
        LdapName subject = new LdapName(
                certificado.getSubjectX500Principal().getName());

        for (Rdn rdn : subject.getRdns()) {
            if ("CN".equalsIgnoreCase(rdn.getType())) {
                return rdn.getValue().toString();
            }
        }

        return null;
    }

    public static String extrairEmail(X509Certificate certificado)
            throws Exception {

        LdapName subject = new LdapName(
                certificado.getSubjectX500Principal().getName());

        for (Rdn rdn : subject.getRdns()) {
            String tipo = rdn.getType();

            if ("EMAILADDRESS".equalsIgnoreCase(tipo)
                    || "E".equalsIgnoreCase(tipo)
                    || "1.2.840.113549.1.9.1".equals(tipo)) {
                return valorTexto(rdn.getValue());
            }
        }

        Collection<List<?>> sans = certificado.getSubjectAlternativeNames();

        if (sans != null) {
            for (List<?> san : sans) {
                if (san.size() >= 2
                        && Integer.valueOf(1).equals(san.get(0))
                        && san.get(1) instanceof String) {
                    return (String) san.get(1);
                }
            }
        }

        return null;
    }

    private static String valorTexto(Object valor) {
        if (!(valor instanceof byte[])) {
            return String.valueOf(valor);
        }

        byte[] codificado = (byte[]) valor;
        int deslocamento = 0;
        int tamanho = codificado.length;
        if (tamanho >= 2 && (codificado[0] == 0x0c
                || codificado[0] == 0x13
                || codificado[0] == 0x16)) {
            deslocamento = 2;
            int tamanhoDeclarado = codificado[1] & 0xff;
            if ((tamanhoDeclarado & 0x80) == 0
                    && tamanhoDeclarado <= tamanho - deslocamento) {
                tamanho = tamanhoDeclarado;
            } else {
                deslocamento = 0;
                tamanho = codificado.length;
            }
        }
        return new String(
                codificado,
                deslocamento,
                tamanho,
                StandardCharsets.UTF_8);
    }
}