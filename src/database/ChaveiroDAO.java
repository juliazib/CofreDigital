// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)


package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ChaveiroDAO {

  private final Connection connection;

  public ChaveiroDAO(Connection connection) {
    if (connection == null) {
      throw new IllegalArgumentException("A conexão não pode ser nula.");
    }

    this.connection = connection;
  }

  public int inserir(
      int uid,
      String certificadoPem,
      byte[] chavePrivadaCifrada) throws SQLException {

    if (certificadoPem == null || certificadoPem.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "O certificado PEM não pode ser vazio.");
    }

    if (chavePrivadaCifrada == null || chavePrivadaCifrada.length == 0) {
      throw new IllegalArgumentException(
          "A chave privada cifrada não pode ser vazia.");
    }

    boolean gerenciaTransacao = connection.getAutoCommit();

    if (gerenciaTransacao) {
      connection.setAutoCommit(false);
    }

    try {
      int kid = inserirRegistro(uid, certificadoPem, chavePrivadaCifrada);
      atualizarKidUsuario(uid, kid);

      if (gerenciaTransacao) {
        connection.commit();
      }

      return kid;

    } catch (SQLException | RuntimeException e) {
      if (gerenciaTransacao) {
        try {
          connection.rollback();
        } catch (SQLException erroRollback) {
          e.addSuppressed(erroRollback);
        }
      }

      throw e;

    } finally {
      if (gerenciaTransacao) {
        connection.setAutoCommit(true);
      }
    }
  }

  private int inserirRegistro(
      int uid,
      String certificadoPem,
      byte[] chavePrivadaCifrada) throws SQLException {

    String sql =
        "INSERT INTO Chaveiro " +
        "(UID, certificado_pem, chave_privada_cifrada) " +
        "VALUES (?, ?, ?)";

    try (PreparedStatement statement = connection.prepareStatement(
        sql, Statement.RETURN_GENERATED_KEYS)) {

      statement.setInt(1, uid);
      statement.setString(2, certificadoPem);
      statement.setBytes(3, chavePrivadaCifrada);

      statement.executeUpdate();

      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          return keys.getInt(1);
        }
      }
    }

    // Alternativa para drivers SQLite que não retornem generated keys.
    try (Statement statement = connection.createStatement();
         ResultSet result =
             statement.executeQuery("SELECT last_insert_rowid()")) {

      if (result.next()) {
        return result.getInt(1);
      }
    }

    throw new SQLException("Não foi possível obter o KID gerado.");
  }

  private void atualizarKidUsuario(int uid, int kid)
      throws SQLException {

    String sql = "UPDATE Usuarios SET KID = ? WHERE UID = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, kid);
      statement.setInt(2, uid);

      if (statement.executeUpdate() != 1) {
        throw new SQLException("Usuário não encontrado: " + uid);
      }
    }
  }

  public ChaveiroRegistro buscarPorUsuario(int uid)
      throws SQLException {

    String sql =
        "SELECT KID, UID, certificado_pem, chave_privada_cifrada " +
        "FROM Chaveiro WHERE UID = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, uid);

      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return null;
        }

        return new ChaveiroRegistro(
            result.getInt("KID"),
            result.getInt("UID"),
            result.getString("certificado_pem"),
            result.getBytes("chave_privada_cifrada"));
      }
    }
  }

  public static final class ChaveiroRegistro {

    private final int kid;
    private final int uid;
    private final String certificadoPem;
    private final byte[] chavePrivadaCifrada;

    public ChaveiroRegistro(
        int kid,
        int uid,
        String certificadoPem,
        byte[] chavePrivadaCifrada) {

      this.kid = kid;
      this.uid = uid;
      this.certificadoPem = certificadoPem;
      this.chavePrivadaCifrada = chavePrivadaCifrada.clone();
    }

    public int getKid() {
      return kid;
    }

    public int getUid() {
      return uid;
    }

    public String getCertificadoPem() {
      return certificadoPem;
    }

    public byte[] getChavePrivadaCifrada() {
      return chavePrivadaCifrada.clone();
    }
  }
}
