package test.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import app.InicializacaoPresenter;
import database.ChaveiroDAO;
import database.DatabaseSchema;
import database.UsuarioDAO;
import service.ContextoAdministrador;

public class InicializacaoPresenterTest {

  private Connection connection;
  private char[] frase;
  private FakeView view;
  private InicializacaoPresenter presenter;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }
    DatabaseSchema.createTables(connection);

    byte[] chave = Files.readAllBytes(Paths.get(
        "test/resources/certificados/admin-pkcs8-aes.key"));
    String certificado = new String(
        Files.readAllBytes(Paths.get(
            "test/resources/certificados/admin-x509.crt")),
        StandardCharsets.UTF_8);
    int uid = new UsuarioDAO(connection).inserir(
        "Administrador",
        "admin@inf1416.puc-rio.br",
        "hash",
        new byte[] { 1 },
        1);
    new ChaveiroDAO(connection).inserir(uid, certificado, chave);

    frase = "admin".toCharArray();
    view = new FakeView(frase);
    presenter = new InicializacaoPresenter(connection, view);
  }

  @After
  public void tearDown() throws Exception {
    Arrays.fill(frase, '\0');
    connection.close();
  }

  @Test
  public void deveValidarFraseEExibirConfirmacao() {
    ContextoAdministrador contexto = presenter.validarAdministrador();

    assertNotNull(contexto);
    assertTrue(view.chaveValidada);
    assertNull(view.erro);
    assertTrue(fraseZerada());
    contexto.close();
  }

  @Test
  public void deveNotificarFraseIncorretaENaoProsseguir() {
    Arrays.fill(frase, '\0');
    frase = "incorreta".toCharArray();
    view.frase = frase;

    ContextoAdministrador contexto = presenter.validarAdministrador();

    assertNull(contexto);
    assertFalse(view.chaveValidada);
    assertNotNull(view.erro);
    assertTrue(fraseZerada());
  }

  @Test
  public void devePararQuandoUsuarioCancela() {
    view.frase = null;

    assertNull(presenter.validarAdministrador());
    assertFalse(view.chaveValidada);
    assertNull(view.erro);
  }

  private boolean fraseZerada() {
    for (char caractere : frase) {
      if (caractere != '\0') {
        return false;
      }
    }
    return true;
  }

  private static final class FakeView
      implements InicializacaoPresenter.View {

    private char[] frase;
    private String erro;
    private boolean chaveValidada;

    private FakeView(char[] frase) {
      this.frase = frase;
    }

    @Override
    public char[] solicitarFraseSecreta() {
      return frase;
    }

    @Override
    public void exibirErro(String mensagem) {
      erro = mensagem;
    }

    @Override
    public void exibirChaveValidada() {
      chaveValidada = true;
    }
  }
}
