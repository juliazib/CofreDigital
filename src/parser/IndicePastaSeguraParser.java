// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import model.ArquivoSeguro;

public final class IndicePastaSeguraParser {

  private IndicePastaSeguraParser() {
  }

  public static List<ArquivoSeguro> interpretarIndice(byte[] bytes)
      throws GeneralSecurityException {
    String conteudo = decodificarAscii(bytes, "index");
    String[] linhas = conteudo.split("\\r?\\n", -1);
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

  public static Map<String, Integer> interpretarCompartilhamentos(
      byte[] bytes,
      List<ArquivoSeguro> arquivos,
      String emailUsuario) throws GeneralSecurityException {
    String conteudo = decodificarAscii(bytes, "share");
    Set<String> codigosIndice = new HashSet<>();
    for (ArquivoSeguro arquivo : arquivos) {
      codigosIndice.add(arquivo.getCodigo());
    }

    Map<String, Integer> compartilhados = new LinkedHashMap<>();
    String[] linhas = conteudo.split("\\r?\\n", -1);
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

  public static String normalizar(String valor) {
    String semAcentos = Normalizer.normalize(
        valor,
        Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    return semAcentos.toLowerCase(Locale.ROOT);
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
}
