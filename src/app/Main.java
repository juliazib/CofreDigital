package app;

import java.sql.Connection;
import java.sql.SQLException;

import database.Database;
import database.DatabaseSchema;

public class Main {
  public static void main(String[] args) {
    try (Connection connection = Database.getConnection()) {
      DatabaseSchema.createTables(connection);

      System.out.println("Cofre Digital");
      System.out.println("Banco de dados inicializado com sucesso.");

    } catch (SQLException e) {
      System.err.println(
          "Erro ao inicializar o banco de dados: " + e.getMessage());
    }
  }
}
