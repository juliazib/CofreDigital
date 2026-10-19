// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package support;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import database.ChaveiroDAO;
import database.ChaveiroDAO.ChaveiroRegistro;
import database.Database;
import database.RegistroDAO;
import database.UsuarioDAO;
import model.RegistroAuditoria;
import model.Usuario;
import service.CertificadoChaveService;

public final class LogView extends JFrame {

  private static final int TAMANHO_DESAFIO = 2048;
  private static final DateTimeFormatter FORMATO_DATA_HORA =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'")
          .withZone(ZoneOffset.UTC);
  private static final Pattern MARCADOR_MENSAGEM =
      Pattern.compile("<(login_name|arq_name)>");

  public LogView(List<RegistroAuditoria> registros) {
    super("LogView - Auditoria do Cofre Digital");
    if (registros == null) {
      throw new IllegalArgumentException("A lista de registros é obrigatória.");
    }
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setMinimumSize(new java.awt.Dimension(900, 420));
    setSize(1100, 560);
    setContentPane(criarConteudo(registros));
    setLocationByPlatform(true);
  }

  public static void main(String[] args) {
    if (args == null || args.length != 1 || args[0].trim().isEmpty()) {
      System.err.println(
          "Uso: java -cp \"bin:lib/*\" support.LogView <caminho-da-chave-privada>");
      return;
    }

    Console console = System.console();
    if (console == null) {
      System.err.println(
          "O LogView precisa de um terminal para ler a frase secreta sem eco.");
      return;
    }

    List<RegistroAuditoria> registros;
    try {
      registros = executar(args[0], console);
    } catch (IOException | SQLException | GeneralSecurityException erro) {
      System.err.println("Não foi possível consultar o log: "
          + erro.getMessage());
      return;
    } catch (IllegalArgumentException erro) {
      System.err.println("Argumento inválido: " + erro.getMessage());
      return;
    }
    SwingUtilities.invokeLater(() -> new LogView(registros).setVisible(true));
  }

  private JPanel criarConteudo(List<RegistroAuditoria> registros) {
    JPanel conteudo = new JPanel(new java.awt.BorderLayout(10, 10));
    conteudo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
    JTable tabela = new JTable(new RegistroTableModel(registros));
    tabela.setFillsViewportHeight(true);
    conteudo.add(new JScrollPane(tabela), java.awt.BorderLayout.CENTER);
    return conteudo;
  }

  static List<RegistroAuditoria> executar(
      String caminhoChave,
      Console console)
      throws IOException, SQLException, GeneralSecurityException {
    if (console == null) {
      throw new IllegalArgumentException(
          "Um terminal é obrigatório para ler a frase secreta sem eco.");
    }

    char[] fraseSecreta =
        console.readPassword("Frase secreta da chave privada do administrador: ");
    if (fraseSecreta == null) {
      throw new GeneralSecurityException(
          "A leitura da frase secreta foi cancelada.");
    }

    byte[] chavePrivadaCifrada = null;
    try (Connection connection = Database.getConnection()) {
      Usuario administrador = new UsuarioDAO(connection).buscarAdministrador();
      if (administrador == null) {
        throw new SQLException("Não há administrador cadastrado.");
      }
      ChaveiroRegistro chaveiro =
          new ChaveiroDAO(connection).buscarPorUsuario(
              administrador.getUid());
      if (chaveiro == null) {
        throw new SQLException(
            "O administrador não possui certificado cadastrado.");
      }

      X509Certificate certificado;
      try {
        certificado = CertificadoChaveService.validarCertificadoPem(
            chaveiro.getCertificadoPem());
      } catch (Exception erro) {
        throw new GeneralSecurityException(
            "O certificado do administrador é inválido.",
            erro);
      }

      Path arquivoChave = Paths.get(caminhoChave);
      chavePrivadaCifrada = Files.readAllBytes(arquivoChave);
      PrivateKey chavePrivada;
      try {
        chavePrivada =
            CertificadoChaveService.descriptografarChavePrivada(
                chavePrivadaCifrada,
                fraseSecreta);
      } catch (Exception erro) {
        throw new GeneralSecurityException(
            "Não foi possível validar a frase secreta ou abrir a chave "
                + "privada do administrador.",
            erro);
      }

      if (!validarChavePrivada(chavePrivada, certificado)) {
        throw new GeneralSecurityException(
            "A chave privada não corresponde ao certificado do administrador.");
      }

      List<RegistroAuditoria> registros =
          new RegistroDAO(connection).listar();
      return Collections.unmodifiableList(new ArrayList<>(registros));
    } finally {
      Arrays.fill(fraseSecreta, '\0');
      if (chavePrivadaCifrada != null) {
        Arrays.fill(chavePrivadaCifrada, (byte) 0);
      }
    }
  }

  public static boolean validarChavePrivada(
      PrivateKey chavePrivada,
      X509Certificate certificado) throws GeneralSecurityException {
    if (chavePrivada == null || certificado == null) {
      throw new IllegalArgumentException(
          "A chave privada e o certificado são obrigatórios.");
    }

    byte[] desafio = new byte[TAMANHO_DESAFIO];
    byte[] assinaturaDigital = null;
    try {
      new SecureRandom().nextBytes(desafio);
      Signature assinatura = Signature.getInstance("SHA1withRSA");
      assinatura.initSign(chavePrivada);
      assinatura.update(desafio);
      assinaturaDigital = assinatura.sign();

      assinatura.initVerify(certificado.getPublicKey());
      assinatura.update(desafio);
      return assinatura.verify(assinaturaDigital);
    } finally {
      Arrays.fill(desafio, (byte) 0);
      if (assinaturaDigital != null) {
        Arrays.fill(assinaturaDigital, (byte) 0);
      }
    }
  }

  public static final class RegistroTableModel extends AbstractTableModel {

    private static final String[] COLUNAS = {
        "Data/hora (UTC)",
        "UID",
        "Código",
        "Mensagem",
        "Arquivo selecionado"
    };

    private final List<RegistroAuditoria> registros;

    public RegistroTableModel(List<RegistroAuditoria> registros) {
      this.registros = Collections.unmodifiableList(
          new ArrayList<>(registros));
    }

    @Override
    public int getRowCount() {
      return registros.size();
    }

    @Override
    public int getColumnCount() {
      return COLUNAS.length;
    }

    @Override
    public String getColumnName(int coluna) {
      return COLUNAS[coluna];
    }

    @Override
    public Object getValueAt(int linha, int coluna) {
      RegistroAuditoria registro = registros.get(linha);
      switch (coluna) {
        case 0:
          return FORMATO_DATA_HORA.format(registro.getDataHora());
        case 1:
          return registro.getUid() == null
              ? "—"
              : registro.getUid().toString();
        case 2:
          return registro.getCodigoMensagem();
        case 3:
          return formatarMensagem(registro);
        case 4:
          return registro.getArquivo() == null ? "—" : registro.getArquivo();
        default:
          throw new IndexOutOfBoundsException(
              "Coluna inexistente: " + coluna);
      }
    }

    private String formatarMensagem(RegistroAuditoria registro) {
      Matcher matcher = MARCADOR_MENSAGEM.matcher(registro.getMensagem());
      StringBuffer mensagem = new StringBuffer();
      while (matcher.find()) {
        String valor = "login_name".equals(matcher.group(1))
            ? registro.getLoginName()
            : registro.getArquivo();
        matcher.appendReplacement(
            mensagem,
            Matcher.quoteReplacement(valor == null ? "—" : valor));
      }
      matcher.appendTail(mensagem);
      return mensagem.toString();
    }

    @Override
    public Class<?> getColumnClass(int coluna) {
      return coluna == 2 ? Integer.class : String.class;
    }

    @Override
    public boolean isCellEditable(int linha, int coluna) {
      return false;
    }
  }
}
