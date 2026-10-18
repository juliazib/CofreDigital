package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

import model.RegistroAuditoria;

public final class RegistroDAO {

  private static final DateTimeFormatter FORMATO_DATA_HORA =
      new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

  private final Connection connection;
  private final Clock clock;

  public RegistroDAO(Connection connection) {
    this(connection, Clock.systemUTC());
  }

  public RegistroDAO(Connection connection, Clock clock) {
    if (connection == null || clock == null) {
      throw new IllegalArgumentException(
          "A conexão e o relógio são obrigatórios.");
    }
    this.connection = connection;
    this.clock = clock;
  }

  public void registrar(
      int codigoMensagem,
      String mensagem,
      Integer uid,
      String arquivo) throws SQLException {
    validar(codigoMensagem, mensagem, uid, arquivo);

    int mid = obterOuCriarMensagem(codigoMensagem, mensagem);
    Instant agora = clock.instant().truncatedTo(ChronoUnit.MILLIS);
    String sql = "INSERT INTO Registros (data_hora, UID, MID, arquivo) " +
        "VALUES (?, ?, ?, ?)";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, FORMATO_DATA_HORA.format(agora));
      if (uid == null) {
        statement.setNull(2, java.sql.Types.INTEGER);
      } else {
        statement.setInt(2, uid);
      }
      statement.setInt(3, mid);
      statement.setString(4, arquivo);
      statement.executeUpdate();
    }
  }

  public List<RegistroAuditoria> listar() throws SQLException {
    String sql = "SELECT r.RID, r.data_hora, r.UID, m.codigo, " +
        "m.mensagem, r.arquivo " +
        "FROM Registros r " +
        "JOIN Mensagens m ON m.MID = r.MID " +
        "ORDER BY r.data_hora ASC, r.RID ASC";
    List<RegistroAuditoria> registros = new ArrayList<>();

    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sql)) {
      while (result.next()) {
        int uidValor = result.getInt("UID");
        Integer uid = result.wasNull() ? null : uidValor;
        registros.add(new RegistroAuditoria(
            result.getInt("RID"),
            Instant.parse(result.getString("data_hora")),
            uid,
            result.getInt("codigo"),
            result.getString("mensagem"),
            result.getString("arquivo")));
      }
    }

    return registros;
  }

  public int contarEventos(int uid, int codigoMensagem) throws SQLException {
    String sql = "SELECT COUNT(*) " +
        "FROM Registros r " +
        "JOIN Mensagens m ON m.MID = r.MID " +
        "WHERE r.UID = ? AND m.codigo = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, uid);
      statement.setInt(2, codigoMensagem);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getInt(1);
      }
    }
  }

  private int obterOuCriarMensagem(int codigo, String mensagem)
      throws SQLException {
    String inserir = "INSERT OR IGNORE INTO Mensagens (codigo, mensagem) " +
        "VALUES (?, ?)";
    try (PreparedStatement statement = connection.prepareStatement(inserir)) {
      statement.setInt(1, codigo);
      statement.setString(2, mensagem);
      statement.executeUpdate();
    }

    String consultar = "SELECT MID, mensagem FROM Mensagens WHERE codigo = ?";
    try (PreparedStatement statement = connection.prepareStatement(consultar)) {
      statement.setInt(1, codigo);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new SQLException(
              "Não foi possível obter a mensagem de auditoria " + codigo + ".");
        }
        if (!mensagem.equals(result.getString("mensagem"))) {
          throw new SQLException(
              "O código de mensagem " + codigo
                  + " já está associado a outra descrição.");
        }
        return result.getInt("MID");
      }
    }
  }

  private static void validar(
      int codigoMensagem,
      String mensagem,
      Integer uid,
      String arquivo) {
    if (codigoMensagem <= 0) {
      throw new IllegalArgumentException(
          "O código da mensagem deve ser positivo.");
    }
    if (mensagem == null || mensagem.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "A descrição da mensagem é obrigatória.");
    }
    if (uid != null && uid <= 0) {
      throw new IllegalArgumentException("O UID deve ser positivo.");
    }
    if (arquivo != null && arquivo.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "O arquivo deve ser informado ou nulo.");
    }
  }
}
