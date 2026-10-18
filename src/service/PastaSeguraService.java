package service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import crypto.CertificadoUtils;
import database.ChaveiroDAO;
import database.UsuarioDAO;
import model.ArquivoSeguro;
import model.Usuario;

public final class PastaSeguraService {

  private final ChaveiroDAO chaveiroDAO;
  private final UsuarioDAO usuarioDAO;
  private final ContextoAdministrador contextoAdministrador;

  public PastaSeguraService(
      java.sql.Connection connection,
      ContextoAdministrador contextoAdministrador) {
    if (connection == null || contextoAdministrador == null) {
      throw new IllegalArgumentException(
          "A conexão e o contexto do administrador são obrigatórios.");
    }
    chaveiroDAO = new ChaveiroDAO(connection);
    usuarioDAO = new UsuarioDAO(connection);
    this.contextoAdministrador = contextoAdministrador;
  }

  public List<ArquivoSeguro> listarArquivos(
      Path pasta,
      Usuario usuario,
      char[] fraseSecreta) throws IOException, GeneralSecurityException {
    if (pasta == null || usuario == null
        || fraseSecreta == null || fraseSecreta.length == 0) {
      throw new IllegalArgumentException(
          "A pasta, o usuário e a frase secreta são obrigatórios.");
    }
    if (!Files.isDirectory(pasta)) {
      throw new IOException("O caminho informado não é uma pasta.");
    }

    validarChaveUsuario(usuario, fraseSecreta);
    return listarAutorizados(pasta, usuario);
  }

  public Path descriptografarArquivo(
      Path pasta,
      Usuario usuario,
      char[] fraseSecreta,
      String codigo) throws IOException, GeneralSecurityException {
    if (pasta == null || usuario == null
        || fraseSecreta == null || fraseSecreta.length == 0
        || codigo == null || !codigo.matches("[A-Za-z0-9]+")) {
      throw new IllegalArgumentException(
          "A pasta, o usuário, a frase secreta e o código são obrigatórios.");
    }
    if (!Files.isDirectory(pasta)) {
      throw new IOException("O caminho informado não é uma pasta.");
    }

    PrivateKey chaveUsuario = validarChaveUsuario(usuario, fraseSecreta);
    byte[] indice = null;
    byte[] compartilhamentos = null;
    byte[] conteudo = null;
    try {
      indice = lerIndiceAssinado(pasta, "index");
      compartilhamentos = lerIndiceAssinado(pasta, "share");
      List<ArquivoSeguro> arquivos = interpretarIndice(indice);
      Map<String, Integer> shares = interpretarCompartilhamentos(
          compartilhamentos,
          arquivos,
          usuario.getEmail());
      ArquivoSeguro arquivo = encontrarArquivoAutorizado(
          arquivos,
          shares,
          usuario,
          codigo);
      boolean dono = usuario.getEmail().equalsIgnoreCase(arquivo.getDono());
      Integer idShare = shares.get(codigo);
      if (!dono && idShare == null) {
        throw new SecurityException(
            "O arquivo está visível pelo grupo, mas não há envelope "
                + "criptográfico autorizado para este usuário.");
      }

      String nomeEnvelope = dono
          ? codigo + ".env"
          : codigo + "." + idShare + ".shr";
      byte[] envelope = Files.readAllBytes(pasta.resolve(nomeEnvelope));
      byte[] criptograma =
          Files.readAllBytes(pasta.resolve(codigo + ".enc"));
      byte[] assinatura =
          Files.readAllBytes(pasta.resolve(codigo + ".asd"));
      conteudo = descriptografar(criptograma, envelope, chaveUsuario);

      X509Certificate certificadoDono =
          carregarCertificadoDono(arquivo.getDono());
      Signature verificador = Signature.getInstance("SHA1withRSA");
      verificador.initVerify(certificadoDono.getPublicKey());
      verificador.update(conteudo);
      if (!verificador.verify(assinatura)) {
        throw new GeneralSecurityException(
            "A assinatura digital do arquivo " + codigo + " é inválida.");
      }

      Path pastaAbsoluta = pasta.toAbsolutePath().normalize();
      Path destino = pastaAbsoluta.resolve(arquivo.getNome()).normalize();
      if (!pastaAbsoluta.equals(destino.getParent())) {
        throw new GeneralSecurityException(
            "O nome secreto do arquivo não é válido.");
      }
      Files.write(
          destino,
          conteudo,
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE);
      return destino;
    } finally {
      if (indice != null) {
        Arrays.fill(indice, (byte) 0);
      }
      if (compartilhamentos != null) {
        Arrays.fill(compartilhamentos, (byte) 0);
      }
      if (conteudo != null) {
        Arrays.fill(conteudo, (byte) 0);
      }
    }
  }

  private List<ArquivoSeguro> listarAutorizados(
      Path pasta,
      Usuario usuario) throws IOException, GeneralSecurityException {
    byte[] indice = null;
    byte[] compartilhamentos = null;
    try {
      indice = lerIndiceAssinado(pasta, "index");
      compartilhamentos = lerIndiceAssinado(pasta, "share");
      List<ArquivoSeguro> arquivos = interpretarIndice(indice);
      Map<String, Integer> shares = interpretarCompartilhamentos(
          compartilhamentos,
          arquivos,
          usuario.getEmail());

      List<ArquivoSeguro> autorizados = new ArrayList<>();
      String grupoUsuario = normalizar(usuario.getGrupo());
      for (ArquivoSeguro arquivo : arquivos) {
        Integer idShare = shares.get(arquivo.getCodigo());
        if (usuario.getEmail().equalsIgnoreCase(arquivo.getDono())
            || grupoUsuario.equals(normalizar(arquivo.getGrupo()))
            || idShare != null) {
          autorizados.add(new ArquivoSeguro(
              arquivo.getCodigo(),
              arquivo.getNome(),
              arquivo.getDono(),
              arquivo.getGrupo(),
              idShare));
        }
      }
      return Collections.unmodifiableList(autorizados);
    } finally {
      if (indice != null) {
        Arrays.fill(indice, (byte) 0);
      }
      if (compartilhamentos != null) {
        Arrays.fill(compartilhamentos, (byte) 0);
      }
    }
  }

  private PrivateKey validarChaveUsuario(
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

  private byte[] lerIndiceAssinado(Path pasta, String nome)
      throws IOException, GeneralSecurityException {
    byte[] criptograma = Files.readAllBytes(pasta.resolve(nome + ".enc"));
    byte[] envelope = Files.readAllBytes(pasta.resolve(nome + ".env"));
    byte[] assinatura = Files.readAllBytes(pasta.resolve(nome + ".asd"));
    byte[] conteudo = descriptografar(criptograma, envelope);

    boolean assinaturaValida = false;
    try {
      Signature verificador = Signature.getInstance("SHA1withRSA");
      verificador.initVerify(contextoAdministrador.getCertificado());
      verificador.update(conteudo);
      assinaturaValida = verificador.verify(assinatura);
      if (!assinaturaValida) {
        throw new GeneralSecurityException(
            "A assinatura digital do arquivo " + nome + " é inválida.");
      }
      return conteudo;
    } finally {
      if (!assinaturaValida) {
        Arrays.fill(conteudo, (byte) 0);
      }
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

  private byte[] descriptografar(
      byte[] criptograma,
      byte[] envelope) throws GeneralSecurityException {
    return descriptografar(
        criptograma,
        envelope,
        contextoAdministrador.getChavePrivada());
  }

  private ArquivoSeguro encontrarArquivoAutorizado(
      List<ArquivoSeguro> arquivos,
      Map<String, Integer> shares,
      Usuario usuario,
      String codigo) throws GeneralSecurityException {
    String grupoUsuario = normalizar(usuario.getGrupo());
    for (ArquivoSeguro arquivo : arquivos) {
      if (codigo.equals(arquivo.getCodigo())
          && (usuario.getEmail().equalsIgnoreCase(arquivo.getDono())
              || grupoUsuario.equals(normalizar(arquivo.getGrupo()))
              || shares.containsKey(codigo))) {
        return arquivo;
      }
    }
    throw new SecurityException(
        "O usuário não tem permissão para acessar este arquivo.");
  }

  private X509Certificate carregarCertificadoDono(String email)
      throws GeneralSecurityException {
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

  private static List<ArquivoSeguro> interpretarIndice(byte[] bytes)
      throws GeneralSecurityException {
    String conteudo = decodificarAscii(bytes, "index");
    String[] linhas = conteudo.split("\n", -1);
    List<ArquivoSeguro> arquivos = new ArrayList<>();
    Set<String> codigos = new HashSet<>();
    for (int i = 0; i < linhas.length; i++) {
      if (i == linhas.length - 1 && linhas[i].isEmpty()) {
        continue;
      }
      String[] campos = linhas[i].split(" ", -1);
      if (campos.length != 4
          || !campos[0].matches("[A-Za-z0-9]+")
          || !campos[1].matches("[A-Za-z0-9._-]+")
          || campos[1].equals(".")
          || campos[1].equals("..")
          || campos[2].isEmpty()
          || campos[3].isEmpty()
          || !codigos.add(campos[0])) {
        throw new GeneralSecurityException(
            "O arquivo index contém uma linha inválida.");
      }
      arquivos.add(new ArquivoSeguro(
          campos[0],
          campos[1],
          campos[2],
          campos[3]));
    }
    return arquivos;
  }

  private static Map<String, Integer> interpretarCompartilhamentos(
      byte[] bytes,
      List<ArquivoSeguro> arquivos,
      String emailUsuario) throws GeneralSecurityException {
    String conteudo = decodificarAscii(bytes, "share");
    Set<String> codigosIndice = new HashSet<>();
    for (ArquivoSeguro arquivo : arquivos) {
      codigosIndice.add(arquivo.getCodigo());
    }

    Map<String, Integer> compartilhados = new LinkedHashMap<>();
    String[] linhas = conteudo.split("\n", -1);
    for (int i = 0; i < linhas.length; i++) {
      if (i == linhas.length - 1 && linhas[i].isEmpty()) {
        continue;
      }
      String[] campos = linhas[i].split(" ", -1);
      if (campos.length != 3
          || !campos[0].matches("[A-Za-z0-9]+")
          || !campos[1].matches("[1-9][0-9]*")
          || campos[2].isEmpty()
          || !codigosIndice.contains(campos[0])) {
        throw new GeneralSecurityException(
            "O arquivo share contém uma linha inválida.");
      }
      if (emailUsuario.equalsIgnoreCase(campos[2])) {
        try {
          compartilhados.putIfAbsent(
              campos[0],
              Integer.parseInt(campos[1]));
        } catch (NumberFormatException erro) {
          throw new GeneralSecurityException(
              "O identificador de compartilhamento é inválido.",
              erro);
        }
      }
    }
    return compartilhados;
  }

  private static String decodificarAscii(byte[] bytes, String nome)
      throws GeneralSecurityException {
    try {
      return StandardCharsets.US_ASCII.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException erro) {
      throw new GeneralSecurityException(
          "O arquivo " + nome + " não está em formato ASCII.",
          erro);
    }
  }

  private static String normalizar(String valor) {
    String semAcentos = Normalizer.normalize(
        valor,
        Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    return semAcentos.toLowerCase(Locale.ROOT);
  }
}
