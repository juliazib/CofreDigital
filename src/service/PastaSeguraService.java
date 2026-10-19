// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import crypto.PastaSeguraCrypto;
import model.ArquivoSeguro;
import model.Usuario;
import parser.IndicePastaSeguraParser;

public final class PastaSeguraService {

  public static final class FalhaDescriptografia
      extends GeneralSecurityException {
    public FalhaDescriptografia(String mensagem, Throwable causa) {
      super(mensagem, causa);
    }
  }

  public static final class FalhaIntegridade
      extends GeneralSecurityException {
    public FalhaIntegridade(String mensagem, Throwable causa) {
      super(mensagem, causa);
    }
  }

  public static final class DestinoJaExiste extends IOException {
    public DestinoJaExiste(String mensagem, Throwable causa) {
      super(mensagem, causa);
    }
  }

  private final AuditoriaService auditoria;
  private final PastaSeguraCrypto criptografia;
  private final Connection connection;

  public PastaSeguraService(
      Connection connection,
      ContextoAdministrador contextoAdministrador) {
    if (connection == null || contextoAdministrador == null) {
      throw new IllegalArgumentException(
          "A conexão e o contexto do administrador são obrigatórios.");
    }
    this.connection = connection;
    auditoria = AuditoriaService.getInstancia();
    criptografia = new PastaSeguraCrypto(
        connection,
        contextoAdministrador);
  }

  public List<ArquivoSeguro> listarArquivos(
      Path pasta,
      Usuario usuario,
      char[] fraseSecreta)
      throws IOException, GeneralSecurityException, SQLException {
    validarEntrada(pasta, usuario, fraseSecreta);
    validarPasta(pasta, usuario);
    criptografia.validarChaveUsuario(usuario, fraseSecreta);
    return listarAutorizados(pasta, usuario);
  }

  public Path descriptografarArquivo(
      Path pasta,
      Usuario usuario,
      char[] fraseSecreta,
      String codigo)
      throws IOException, GeneralSecurityException, SQLException {
    validarEntrada(pasta, usuario, fraseSecreta);
    if (codigo == null || !codigo.matches("[A-Za-z0-9]+")) {
      throw new IllegalArgumentException(
          "A pasta, o usuário, a frase secreta e o código são obrigatórios.");
    }
    validarPasta(pasta, usuario);

    PrivateKey chaveUsuario =
        criptografia.validarChaveUsuario(usuario, fraseSecreta);
    byte[] indice = null;
    byte[] compartilhamentos = null;
    byte[] conteudo = null;
    try {
      indice = criptografia.lerIndiceAssinado(pasta, "index", usuario);
      compartilhamentos =
          criptografia.lerIndiceAssinado(pasta, "share", usuario);
      List<ArquivoSeguro> arquivos =
          IndicePastaSeguraParser.interpretarIndice(indice);
      Map<String, Integer> shares =
          IndicePastaSeguraParser.interpretarCompartilhamentos(
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
      byte[] envelope;
      try {
        envelope = Files.readAllBytes(pasta.resolve(nomeEnvelope));
      } catch (IOException erro) {
        auditoria.registrar(connection, 7020, usuario, arquivo.getNome());
        throw new FalhaDescriptografia(
            "Não foi possível ler o envelope criptográfico.", erro);
      }

      auditoria.registrar(connection, 7016, usuario, arquivo.getNome());
      try {
        byte[] criptograma =
            Files.readAllBytes(pasta.resolve(codigo + ".enc"));
        conteudo = criptografia.descriptografarArquivo(
            criptograma,
            envelope,
            chaveUsuario);
      } catch (IOException | GeneralSecurityException erro) {
        auditoria.registrar(connection, 7020, usuario, arquivo.getNome());
        throw new FalhaDescriptografia(
            "Não foi possível descriptografar o arquivo.", erro);
      }
      auditoria.registrar(connection, 7018, usuario, arquivo.getNome());

      byte[] assinatura;
      try {
        assinatura = Files.readAllBytes(pasta.resolve(codigo + ".asd"));
      } catch (IOException erro) {
        auditoria.registrar(connection, 7021, usuario, arquivo.getNome());
        throw new FalhaIntegridade(
            "A assinatura digital do arquivo não pôde ser lida.", erro);
      }
      try {
        criptografia.verificarAssinaturaArquivo(
            conteudo,
            assinatura,
            arquivo.getDono());
      } catch (GeneralSecurityException erro) {
        auditoria.registrar(connection, 7021, usuario, arquivo.getNome());
        throw new FalhaIntegridade(
            "A assinatura digital do arquivo é inválida.", erro);
      }
      auditoria.registrar(connection, 7019, usuario, arquivo.getNome());
      return gravarArquivoDescriptografado(pasta, arquivo, conteudo);
    } catch (GeneralSecurityException erro) {
      throw erro;
    } catch (SecurityException erro) {
      auditoria.registrar(connection, 7017, usuario, codigo);
      throw erro;
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
      Usuario usuario) throws IOException, GeneralSecurityException,
      SQLException {
    byte[] indice = null;
    byte[] compartilhamentos = null;
    try {
      indice = criptografia.lerIndiceAssinado(pasta, "index", usuario);
      compartilhamentos =
          criptografia.lerIndiceAssinado(pasta, "share", usuario);
      List<ArquivoSeguro> arquivos =
          IndicePastaSeguraParser.interpretarIndice(indice);
      Map<String, Integer> shares =
          IndicePastaSeguraParser.interpretarCompartilhamentos(
              compartilhamentos,
              arquivos,
              usuario.getEmail());

      List<ArquivoSeguro> autorizados = new ArrayList<>();
      String grupoUsuario =
          IndicePastaSeguraParser.normalizar(usuario.getGrupo());
      for (ArquivoSeguro arquivo : arquivos) {
        Integer idShare = shares.get(arquivo.getCodigo());
        if (usuario.getEmail().equalsIgnoreCase(arquivo.getDono())
            || grupoUsuario.equals(
                IndicePastaSeguraParser.normalizar(arquivo.getGrupo()))
            || idShare != null) {
          autorizados.add(new ArquivoSeguro(
              arquivo.getCodigo(),
              arquivo.getNome(),
              arquivo.getDono(),
              arquivo.getGrupo(),
              idShare));
        }
      }

      auditoria.registrar(connection, 7009, usuario, null);
      auditoria.registrar(connection, 7014, usuario, null);
      return Collections.unmodifiableList(autorizados);
    } finally {
      limpar(indice);
      limpar(compartilhamentos);
    }
  }

  private ArquivoSeguro encontrarArquivoAutorizado(
      List<ArquivoSeguro> arquivos,
      Map<String, Integer> shares,
      Usuario usuario,
      String codigo) {
    String grupoUsuario =
        IndicePastaSeguraParser.normalizar(usuario.getGrupo());
    for (ArquivoSeguro arquivo : arquivos) {
      if (codigo.equals(arquivo.getCodigo())
          && (usuario.getEmail().equalsIgnoreCase(arquivo.getDono())
              || grupoUsuario.equals(
                  IndicePastaSeguraParser.normalizar(arquivo.getGrupo()))
              || shares.containsKey(codigo))) {
        return arquivo;
      }
    }
    throw new SecurityException(
        "O usuário não tem permissão para acessar este arquivo.");
  }

  private Path gravarArquivoDescriptografado(
      Path pasta,
      ArquivoSeguro arquivo,
      byte[] conteudo) throws IOException, GeneralSecurityException {
    Path pastaAbsoluta = pasta.toAbsolutePath().normalize();
    Path destino = pastaAbsoluta.resolve(arquivo.getNome()).normalize();
    if (!pastaAbsoluta.equals(destino.getParent())) {
      throw new GeneralSecurityException(
          "O nome secreto do arquivo não é válido.");
    }
    try {
      return Files.write(
          destino,
          conteudo,
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE);
    } catch (FileAlreadyExistsException erro) {
      throw new DestinoJaExiste(
          "Já existe um arquivo com o nome secreto selecionado.", erro);
    }
  }

  private static void validarEntrada(
      Path pasta,
      Usuario usuario,
      char[] fraseSecreta) {
    if (pasta == null || usuario == null
        || fraseSecreta == null || fraseSecreta.length == 0) {
      throw new IllegalArgumentException(
          "A pasta, o usuário e a frase secreta são obrigatórios.");
    }
  }

  private void validarPasta(Path pasta, Usuario usuario) throws IOException,
      SQLException {
    if (!Files.isDirectory(pasta)) {
      auditoria.registrar(connection, 7004, usuario, null);
      throw new IOException("O caminho informado não é uma pasta.");
    }
  }

  private static void limpar(byte[] bytes) {
    if (bytes != null) {
      Arrays.fill(bytes, (byte) 0);
    }
  }
}
