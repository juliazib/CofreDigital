
package test.service;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import database.DatabaseSchema;
import database.UsuarioDAO;
import service.CadastroService;

public class CadastroServiceTest {

    private Connection connection;
    private UsuarioDAO usuarioDAO;
    private CadastroService cadastroService;

    @Before
    public void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        DatabaseSchema.createTables(connection);

        usuarioDAO = new UsuarioDAO(connection);
        cadastroService = new CadastroService(connection);
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void deveDesfazerCadastroQuandoCertificadoForInvalido()
            throws Exception {
        try {
            cadastroService.cadastrar(
                "Teste",
                "teste@example.com",
                "hash-teste",
                new byte[] { 1, 2, 3 },
                2,
                "",
                new byte[] { 4, 5, 6 }
            );

            fail("Era esperada uma IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, usuarioDAO.contarUsuarios());
    }

    @Test
    public void deveCadastrarUsuarioEChaveiroComSucesso() throws Exception {
        int uid = cadastroService.cadastrar(
            "Teste",
            "teste@example.com",
            "hash-teste",
            new byte[] { 1, 2, 3 },
            2,
            "certificado-pem-teste",
            new byte[] { 4, 5, 6 }
        );

        assertTrue(uid > 0);

        assertNotNull(usuarioDAO.buscarPorId(uid));
        assertNotNull(
            new database.ChaveiroDAO(connection).buscarPorUsuario(uid)
        );

        assertEquals(
            Integer.valueOf(
                new database.ChaveiroDAO(connection)
                    .buscarPorUsuario(uid)
                    .getKid()
            ),
            usuarioDAO.buscarPorId(uid).getKid()
        );
    }

}
