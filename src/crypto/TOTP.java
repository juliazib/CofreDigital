// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

public class TOTP {

  private byte[] key = null;
  private long timeStepInSeconds = 30;

  private static final int QUANTIDADE_DIGITOS = 6;
  private static final int MODULO_CODIGO = 1_000_000;

  // Construtor: recebe o segredo em Base32 e o intervalo de tempo.
  public TOTP(String base32EncodedSecret, long timeStepInSeconds)
      throws Exception {

    if (base32EncodedSecret == null || base32EncodedSecret.isEmpty()) {
      throw new Exception("O segredo Base32 não pode ser vazio.");
    }

    if (timeStepInSeconds <= 0) {
      throw new Exception("O intervalo de tempo deve ser positivo.");
    }

    Base32 base32 = new Base32(
        Base32.Alphabet.BASE32,
        true,
        false
    );

    this.key = base32.fromString(base32EncodedSecret);

    if (this.key == null || this.key.length != 20) {
      throw new Exception("O segredo deve conter exatamente 20 bytes.");
    }

    this.timeStepInSeconds = timeStepInSeconds;
  }

  // Recebe o hash HMAC-SHA1 e gera um código decimal de 6 dígitos.
  private String getTOTPCodeFromHash(byte[] hash) {
    int deslocamento = hash[hash.length - 1] & 0x0F;

    int codigoBinario =
          ((hash[deslocamento] & 0x7F) << 24)
        | ((hash[deslocamento + 1] & 0xFF) << 16)
        | ((hash[deslocamento + 2] & 0xFF) << 8)
        | (hash[deslocamento + 3] & 0xFF);

    int codigo = codigoBinario % MODULO_CODIGO;

    return String.format(
        java.util.Locale.ROOT,
        "%0" + QUANTIDADE_DIGITOS + "d",
        codigo
    );
  }

  // Calcula o HMAC-SHA1 usando o contador e o segredo.
  private byte[] HMAC_SHA1(byte[] counter, byte[] keyByteArray) {
    try {
      Mac hmac = Mac.getInstance("HmacSHA1");
      SecretKeySpec chave =
          new SecretKeySpec(keyByteArray, "HmacSHA1");

      hmac.init(chave);

      return hmac.doFinal(counter);

    } catch (Exception e) {
      throw new IllegalStateException("Erro ao calcular HMAC-SHA1.", e);
    }
  }

  // Gera o código para um contador de intervalos.
  private String TOTPCode(long timeInterval) {
    byte[] contador = new byte[8];

    for (int i = 7; i >= 0; i--) {
      contador[i] = (byte) (timeInterval & 0xFF);
      timeInterval >>>= 8;
    }

    byte[] hash = HMAC_SHA1(contador, key);

    return getTOTPCodeFromHash(hash);
  }

  // Gera o código TOTP para o instante atual.
  public String generateCode() {
    long timestampSegundos = new Date().getTime() / 1000L;
    return generateCode(timestampSegundos);
  }

  // Sobrecarga para permitir testes com um instante específico.
  public String generateCode(long timestampSegundos) {
    long contador = timestampSegundos / timeStepInSeconds;
    return TOTPCode(contador);
  }

  // Valida o código considerando o intervalo anterior, o atual e o seguinte.
  public boolean validateCode(String inputTOTP) {
    long timestampSegundos = new Date().getTime() / 1000L;
    return validateCode(inputTOTP, timestampSegundos);
  }

  // Sobrecarga para permitir testes com um instante específico.
  public boolean validateCode(
      String inputTOTP,
      long timestampSegundos) {

    if (inputTOTP == null || !inputTOTP.matches("[0-9]{6}")) {
      return false;
    }

    long contadorAtual = timestampSegundos / timeStepInSeconds;

    for (long diferenca = -1; diferenca <= 1; diferenca++) {
      long contador = contadorAtual + diferenca;

      if (contador < 0) {
        continue;
      }

      String codigoEsperado = TOTPCode(contador);

      if (compararEmTempoConstante(inputTOTP, codigoEsperado)) {
        return true;
      }
    }

    return false;
  }

  private boolean compararEmTempoConstante(
      String primeiro,
      String segundo) {

    int diferenca = primeiro.length() ^ segundo.length();

    for (int i = 0; i < primeiro.length(); i++) {
      diferenca |= primeiro.charAt(i) ^ segundo.charAt(i);
    }

    return diferenca == 0;
  }
}