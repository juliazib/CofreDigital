
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

  private static final String URL = "jdbc:sqlite:cofre.db";

  public static Connection getConnection() throws SQLException {
    Connection connection = DriverManager.getConnection(URL);

    try (java.sql.Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }

    return connection;
  }
}
