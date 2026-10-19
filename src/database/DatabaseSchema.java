// Julia Gomes Zibordi (2320934)
// Marcos Paulo Marinho Vieira (2320466)

package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSchema {

  public static void createTables(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "CREATE TABLE IF NOT EXISTS Grupos (" +
              "GID INTEGER PRIMARY KEY, " +
              "nome TEXT NOT NULL UNIQUE" +
              ")");

      statement.execute(
          "CREATE TABLE IF NOT EXISTS Usuarios (" +
              "UID INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "nome TEXT NOT NULL, " +
              "email TEXT NOT NULL UNIQUE, " +
              "senha_hash TEXT NOT NULL, " +
              "token_cifrado BLOB NOT NULL, " +
              "KID INTEGER UNIQUE, " +
              "GID INTEGER NOT NULL, " +
              "falhas_senha INTEGER NOT NULL DEFAULT 0, " +
              "falhas_token INTEGER NOT NULL DEFAULT 0, " +
              "bloqueado_ate INTEGER, " +
              "FOREIGN KEY (GID) REFERENCES Grupos(GID), " +
              "FOREIGN KEY (KID) REFERENCES Chaveiro(KID)" +
              ")");

      statement.execute(
          "CREATE TABLE IF NOT EXISTS Chaveiro (" +
              "KID INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "UID INTEGER NOT NULL UNIQUE, " +
              "certificado_pem TEXT NOT NULL, " +
              "chave_privada_cifrada BLOB NOT NULL, " +
              "FOREIGN KEY (UID) REFERENCES Usuarios(UID)" +
              ")");

      statement.execute(
          "CREATE TABLE IF NOT EXISTS Mensagens (" +
              "MID INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "codigo INTEGER NOT NULL UNIQUE, " +
              "mensagem TEXT NOT NULL" +
              ")");

      statement.execute(
          "CREATE TABLE IF NOT EXISTS Registros (" +
              "RID INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "data_hora TEXT NOT NULL, " +
              "UID INTEGER, " +
              "MID INTEGER NOT NULL, " +
              "arquivo TEXT, " +
              "FOREIGN KEY (UID) REFERENCES Usuarios(UID), " +
              "FOREIGN KEY (MID) REFERENCES Mensagens(MID)" +
              ")");
    }

    insertInitialGroups(connection);
  }

  private static void insertInitialGroups(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT OR IGNORE INTO Grupos (GID, nome) " +
              "VALUES (1, 'Administrador')");

      statement.executeUpdate(
          "INSERT OR IGNORE INTO Grupos (GID, nome) " +
              "VALUES (2, 'Usuário')");
    }
  }
}
