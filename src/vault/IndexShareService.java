package vault;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class IndexShareService {

  private static final Pattern PADRAO_ALFANUMERICO = Pattern.compile("[A-Za-z0-9]+");
  private static final Pattern PADRAO_IDENTIFICADOR_USUARIO =
      Pattern.compile("[A-Za-z0-9@._+-]+");
  private static final Pattern PADRAO_INTEIRO_POSITIVO = Pattern.compile("[0-9]+");

  private IndexShareService() {
  }

  public static List<IndexEntry> parseIndex(byte[] conteudo) {
    String[] lines = decodeLines(conteudo, "index");
    List<IndexEntry> entries = new ArrayList<IndexEntry>();
    Set<String> codes = new HashSet<String>();

    for (int i = 0; i < lines.length; i++) {
      String[] fields = lines[i].split(" ", -1);
      if (fields.length != 4
          || !matches(PADRAO_ALFANUMERICO, fields[0])
          || !matches(PADRAO_ALFANUMERICO, fields[1])
          || !matches(PADRAO_IDENTIFICADOR_USUARIO, fields[2])
          || !matches(PADRAO_ALFANUMERICO, fields[3])) {
        throw invalidLine("index", i + 1);
      }
      if (!codes.add(fields[0])) {
        throw new IllegalArgumentException(
            "Código de arquivo duplicado no index, linha " + (i + 1) + ": " + fields[0]);
      }

      entries.add(new IndexEntry(fields[0], fields[1], fields[2], fields[3]));
    }

    return immutableCopy(entries);
  }

  public static List<ShareEntry> parseShare(byte[] conteudo) {
    String[] lines = decodeLines(conteudo, "share");
    List<ShareEntry> entries = new ArrayList<ShareEntry>();
    Set<String> shares = new HashSet<String>();

    for (int i = 0; i < lines.length; i++) {
      String[] fields = lines[i].split(" ", -1);
      if (fields.length != 3
          || !matches(PADRAO_ALFANUMERICO, fields[0])
          || !matches(PADRAO_INTEIRO_POSITIVO, fields[1])
          || !matches(PADRAO_IDENTIFICADOR_USUARIO, fields[2])) {
        throw invalidLine("share", i + 1);
      }

      long shareId;
      try {
        shareId = Long.parseLong(fields[1]);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "ID de compartilhamento fora do intervalo suportado na linha " + (i + 1) + ".", e);
      }
      if (shareId <= 0) {
        throw invalidLine("share", i + 1);
      }

      String key = fields[0] + "\u0000" + shareId;
      if (!shares.add(key)) {
        throw new IllegalArgumentException(
            "ID de compartilhamento duplicado para o arquivo " + fields[0] + ": " + shareId);
      }

      entries.add(new ShareEntry(fields[0], shareId, fields[2]));
    }

    return immutableCopy(entries);
  }

  public static void validateReferences(
      List<IndexEntry> index,
      List<ShareEntry> share) {
    if (index == null || share == null) {
      throw new IllegalArgumentException("Index e share não podem ser nulos.");
    }

    Set<String> indexCodes = new HashSet<String>();
    for (IndexEntry entry : index) {
      if (entry == null) {
        throw new IllegalArgumentException("O index não pode conter entradas nulas.");
      }
      indexCodes.add(entry.getCodigoArquivo());
    }

    for (ShareEntry entry : share) {
      if (entry == null) {
        throw new IllegalArgumentException("O share não pode conter entradas nulas.");
      }
      if (!indexCodes.contains(entry.getCodigoArquivo())) {
        throw new IllegalArgumentException(
            "O share referencia um arquivo ausente no index: " + entry.getCodigoArquivo());
      }
    }
  }

  private static String[] decodeLines(byte[] contents, String fileName) {
    if (contents == null) {
      throw new IllegalArgumentException("O conteúdo do " + fileName + " não pode ser nulo.");
    }
    if (contents.length == 0) {
      return new String[0];
    }

    final String text;
    try {
      text = StandardCharsets.US_ASCII.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(contents))
          .toString();
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException(
          "O conteúdo do " + fileName + " deve conter somente caracteres ASCII.", e);
    }

    if (!text.endsWith("\n")) {
      throw new IllegalArgumentException(
          "Cada registro do " + fileName + " deve terminar com LF.");
    }

    String[] linesWithTrailingEmpty = text.split("\n", -1);
    String[] lines = new String[linesWithTrailingEmpty.length - 1];
    System.arraycopy(linesWithTrailingEmpty, 0, lines, 0, lines.length);
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].isEmpty()) {
        throw invalidLine(fileName, i + 1);
      }
    }
    return lines;
  }

  private static boolean matches(Pattern pattern, String value) {
    return pattern.matcher(value).matches();
  }

  private static IllegalArgumentException invalidLine(String fileName, int line) {
    return new IllegalArgumentException(
        "Registro inválido no " + fileName + ", linha " + line + ".");
  }

  private static <T> List<T> immutableCopy(List<T> entries) {
    return Collections.unmodifiableList(new ArrayList<T>(entries));
  }
}
