package crypto;

import java.security.cert.X509Certificate;
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
                return rdn.getValue().toString();
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
}