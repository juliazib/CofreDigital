
package service;

import java.sql.Connection;
import java.sql.SQLException;

import database.ChaveiroDAO;
import database.UsuarioDAO;

public class CadastroService {

    private final Connection connection;
    private final UsuarioDAO usuarioDAO;
    private final ChaveiroDAO chaveiroDAO;

    public CadastroService(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException(
                "A conexão não pode ser nula."
            );
        }

        this.connection = connection;
        this.usuarioDAO = new UsuarioDAO(connection);
        this.chaveiroDAO = new ChaveiroDAO(connection);
    }

    public int cadastrar(
            String nome,
            String email,
            String senhaHash,
            byte[] tokenCifrado,
            int gid,
            String certificadoPem,
            byte[] chavePrivadaCifrada) throws SQLException {

        boolean gerenciaTransacao = connection.getAutoCommit();

        if (gerenciaTransacao) {
            connection.setAutoCommit(false);
        }

        try {
            int uid = usuarioDAO.inserir(
                nome,
                email,
                senhaHash,
                tokenCifrado,
                gid
            );

            chaveiroDAO.inserir(
                uid,
                certificadoPem,
                chavePrivadaCifrada
            );

            if (gerenciaTransacao) {
                connection.commit();
            }

            return uid;

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
}
