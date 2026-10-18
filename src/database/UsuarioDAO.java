
package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import model.Usuario;

public class UsuarioDAO {

  private final Connection connection;

  public UsuarioDAO(Connection connection) {
    this.connection = connection;
  }

  private static final String SELECT_USUARIO = "SELECT u.UID, u.nome, u.email, u.senha_hash, " +
      "u.token_cifrado, u.KID, u.GID, g.nome AS grupo, " +
      "u.falhas_senha, u.falhas_token, u.bloqueado_ate " +
      "FROM Usuarios u " +
      "JOIN Grupos g ON g.GID = u.GID ";

  public Usuario buscarPorEmail(String email) throws SQLException {
    String sql = SELECT_USUARIO + "WHERE u.email = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, email);

      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? mapearUsuario(result) : null;
      }
    }
  }

  public Usuario buscarPorId(int uid) throws SQLException {
    String sql = SELECT_USUARIO + "WHERE u.UID = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, uid);

      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? mapearUsuario(result) : null;
      }
    }
  }

  public Usuario buscarAdministrador() throws SQLException {
    String sql = SELECT_USUARIO + "WHERE u.GID = 1 LIMIT 1";

    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sql)) {
      return result.next() ? mapearUsuario(result) : null;
    }
  }

  public boolean existeEmail(String email) throws SQLException {
    String sql = "SELECT 1 FROM Usuarios WHERE email = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, email);

      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    }
  }

  public int inserir(
      String nome,
      String email,
      String senhaHash,
      byte[] tokenCifrado,
      int gid) throws SQLException {

    String sql = "INSERT INTO Usuarios " +
        "(nome, email, senha_hash, token_cifrado, KID, GID) " +
        "VALUES (?, ?, ?, ?, NULL, ?)";

    try (PreparedStatement statement = connection.prepareStatement(
        sql, Statement.RETURN_GENERATED_KEYS)) {

      statement.setString(1, nome);
      statement.setString(2, email);
      statement.setString(3, senhaHash);
      statement.setBytes(4, tokenCifrado);
      statement.setInt(5, gid);

      statement.executeUpdate();

      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          return keys.getInt(1);
        }
      }
    }

    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
      if (result.next()) {
        return result.getInt(1);
      }
    }

    throw new SQLException("Não foi possível obter o UID gerado.");
  }

  public void atualizarKid(int uid, int kid) throws SQLException {
    String sql = "UPDATE Usuarios SET KID = ? WHERE UID = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, kid);
      statement.setInt(2, uid);

      if (statement.executeUpdate() != 1) {
        throw new SQLException("Usuário não encontrado: " + uid);
      }
    }
  }

  public int contarUsuarios() throws SQLException {
    String sql = "SELECT COUNT(*) FROM Usuarios";

    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sql)) {
      result.next();
      return result.getInt(1);
    }
  }

  public int contarAcessos(int uid) throws SQLException {
    String sql = "SELECT COUNT(*) " +
        "FROM Registros r " +
        "JOIN Mensagens m ON m.MID = r.MID " +
        "WHERE r.UID = ? AND m.codigo = 1003";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, uid);

      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getInt(1);
      }
    }
  }

  public void limparFalhasDeSenha(int uid) throws SQLException {
    atualizarContador(uid, "falhas_senha", 0);
  }

  public void limparFalhasDeToken(int uid) throws SQLException {
    atualizarContador(uid, "falhas_token", 0);
  }

  public void registrarFalhaDeSenha(int uid, long agoraMillis) throws SQLException {
    registrarFalha(uid, "falhas_senha", agoraMillis);
  }

  public void registrarFalhaDeToken(int uid, long agoraMillis) throws SQLException {
    registrarFalha(uid, "falhas_token", agoraMillis);
  }

  private void registrarFalha(int uid, String coluna, long agoraMillis) throws SQLException {
    if (!coluna.equals("falhas_senha")
        && !coluna.equals("falhas_token")) {
      throw new IllegalArgumentException("Contador inválido.");
    }

    String sql = "UPDATE Usuarios SET " +
        coluna + " = CASE WHEN " + coluna +
        " + 1 >= 3 THEN 0 ELSE " + coluna + " + 1 END, " +
        "bloqueado_ate = CASE WHEN " + coluna +
        " + 1 >= 3 THEN ? ELSE bloqueado_ate END " +
        "WHERE UID = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, agoraMillis + 120_000L);
      statement.setInt(2, uid);

      if (statement.executeUpdate() != 1) {
        throw new SQLException("Usuário não encontrado: " + uid);
      }
    }
  }

  private void atualizarContador(int uid, String coluna, int valor) throws SQLException {
    if (!coluna.equals("falhas_senha")
        && !coluna.equals("falhas_token")) {
      throw new IllegalArgumentException("Contador inválido.");
    }

    String sql = "UPDATE Usuarios SET " + coluna + " = ? WHERE UID = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, valor);
      statement.setInt(2, uid);

      if (statement.executeUpdate() != 1) {
        throw new SQLException("Usuário não encontrado: " + uid);
      }
    }
  }

  public void limparBloqueioExpirado(int uid, long agoraMillis) throws SQLException {
    String sql = "UPDATE Usuarios SET bloqueado_ate = NULL " +
        "WHERE UID = ? AND bloqueado_ate <= ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, uid);
      statement.setLong(2, agoraMillis);
      statement.executeUpdate();
    }
  }

  private Usuario mapearUsuario(ResultSet result) throws SQLException {
    int kidValor = result.getInt("KID");
    Integer kid = result.wasNull() ? null : kidValor;

    long bloqueioValor = result.getLong("bloqueado_ate");
    Long bloqueadoAte = result.wasNull() ? null : bloqueioValor;

    return new Usuario(
        result.getInt("UID"),
        result.getString("nome"),
        result.getString("email"),
        result.getString("senha_hash"),
        result.getBytes("token_cifrado"),
        kid,
        result.getInt("GID"),
        result.getString("grupo"),
        result.getInt("falhas_senha"),
        result.getInt("falhas_token"),
        bloqueadoAte);
  }
  
  public boolean existeAdministrador() throws SQLException {
      String sql =
          "SELECT 1 FROM Usuarios WHERE GID = 1 LIMIT 1";

      try (Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery(sql)) {
          return result.next();
      }
  }

}
