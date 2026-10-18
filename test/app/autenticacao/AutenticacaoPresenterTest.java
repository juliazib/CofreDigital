package test.app.autenticacao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import app.autenticacao.AutenticacaoModel;
import app.autenticacao.AutenticacaoPresenter;
import crypto.TOTP;
import crypto.TokenUtils;
import database.DatabaseSchema;
import database.RegistroDAO;
import database.UsuarioDAO;
import model.Usuario;

public class AutenticacaoPresenterTest {

  private static final String EMAIL = "admin@example.com";
  private static final String EMAIL_SECUNDARIO = "usuario@example.com";
  private static final String SENHA = "12345678";
  private static final String SEGREDO_TOTP =
      "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

  private Connection connection;
  private UsuarioDAO usuarioDAO;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }
    DatabaseSchema.createTables(connection);
    usuarioDAO = new UsuarioDAO(connection);
    usuarioDAO.inserir(
        "Administrador",
        EMAIL,
        gerarHashRapidoParaTeste(SENHA),
        TokenUtils.cifrar(SEGREDO_TOTP, SENHA),
        1);
  }

  @After
  public void tearDown() throws Exception {
    connection.close();
  }

  @Test
  public void deveAutenticarComSenhaETotpValidos() throws Exception {
    char[] senha = SENHA.toCharArray();
    FakeView view = new FakeView(
        new String[] { EMAIL },
        new char[][] { senha },
        new String[] { gerarCodigoValido() });

    Usuario autenticado =
        new AutenticacaoPresenter(
            connection,
            new AutenticacaoModel(),
            view).autenticar();

    assertEquals(EMAIL, autenticado.getEmail());
    assertTrue(view.avisos.isEmpty());
    assertTrue(caracteresZerados(senha));
    int uid = usuarioDAO.buscarPorEmail(EMAIL).getUid();
    assertEventosAnonimos(2001, 1);
    assertEventos(uid, 2002, 1);
    assertEventos(uid, 2003, 1);
    assertEventos(uid, 3001, 1);
    assertEventos(uid, 3002, 1);
    assertEventos(uid, 3003, 1);
    assertEventos(uid, 4001, 1);
    assertEventos(uid, 4002, 1);
    assertEventos(uid, 4003, 1);
  }

  @Test
  public void deveRepetirCodigoAposFalhaETerminarComSucesso()
      throws Exception {
    FakeView view = new FakeView(
        new String[] { EMAIL },
        new char[][] { SENHA.toCharArray() },
        new String[] { "not-a-code", gerarCodigoValido() });

    Usuario autenticado =
        new AutenticacaoPresenter(
            connection,
            new AutenticacaoModel(),
            view).autenticar();

    assertEquals(EMAIL, autenticado.getEmail());
    assertEquals(1, view.avisos.size());
    assertEquals("Código TOTP incorreto.", view.avisos.get(0));
    assertEquals(
        0,
        usuarioDAO.buscarPorEmail(EMAIL).getFalhasToken());
    int uid = usuarioDAO.buscarPorEmail(EMAIL).getUid();
    assertEventos(uid, 4004, 1);
    assertEventos(uid, 4003, 1);
  }

  @Test
  public void deveRepetirLoginAposSenhaIncorreta() throws Exception {
    char[] senhaIncorreta = "senha-errada".toCharArray();
    char[] senhaCorreta = SENHA.toCharArray();
    FakeView view = new FakeView(
        new String[] { EMAIL },
        new char[][] { senhaIncorreta, senhaCorreta },
        new String[] { gerarCodigoValido() });

    Usuario autenticado =
        new AutenticacaoPresenter(
            connection,
            new AutenticacaoModel(),
            view).autenticar();

    assertEquals(EMAIL, autenticado.getEmail());
    assertEquals(1, view.avisos.size());
    assertEquals(
        "Senha pessoal incorreta. Tente novamente.",
        view.avisos.get(0));
    assertTrue(caracteresZerados(senhaIncorreta));
    assertTrue(caracteresZerados(senhaCorreta));
    assertEquals(2, view.emailsSolicitadosParaSenha.size());
    assertEquals(EMAIL, view.emailsSolicitadosParaSenha.get(0));
    assertEquals(EMAIL, view.emailsSolicitadosParaSenha.get(1));
    int uid = usuarioDAO.buscarPorEmail(EMAIL).getUid();
    assertEventos(uid, 3004, 1);
    assertEventos(uid, 3003, 1);
  }

  @Test
  public void deveInformarLoginInexistenteEContinuar() throws Exception {
    FakeView view = new FakeView(
        new String[] { "inexistente@example.com", EMAIL },
        new char[][] { SENHA.toCharArray(), SENHA.toCharArray() },
        new String[] { gerarCodigoValido() });

    Usuario autenticado =
        new AutenticacaoPresenter(
            connection,
            new AutenticacaoModel(),
            view).autenticar();

    assertEquals(EMAIL, autenticado.getEmail());
    assertEquals(1, view.avisos.size());
    assertEquals(
        "Usuário não identificado.",
        view.avisos.get(0));
    assertEquals(
        1,
        new RegistroDAO(connection).listar().stream()
            .filter(registro -> registro.getUid() == null
                && registro.getCodigoMensagem() == 2005)
            .count());
    assertEventos(usuarioDAO.buscarPorEmail(EMAIL).getUid(), 2003, 1);
  }

  @Test
  public void deveInterromperAposTresFalhasDeTotp() throws Exception {
    FakeView view = new FakeView(
        new String[] { EMAIL },
        new char[][] { SENHA.toCharArray() },
        new String[] {
            "not-a-code",
            "not-a-code",
            "not-a-code"
        });

    Usuario autenticado =
        new AutenticacaoPresenter(
            connection,
            new AutenticacaoModel(),
            view).autenticar();

    assertNull(autenticado);
    assertTrue(view.avisos.contains(
        "Acesso bloqueado por dois minutos após três tentativas incorretas."));
    assertTrue(
        usuarioDAO.buscarPorEmail(EMAIL).estaBloqueado(
            System.currentTimeMillis()));
    int uid = usuarioDAO.buscarPorEmail(EMAIL).getUid();
    assertEventos(uid, 4006, 1);
    assertEventos(uid, 4007, 1);
  }

  @Test
  public void devePermitirCancelarNoPedidoDeLogin() throws Exception {
    FakeView view = new FakeView(
        new String[] { null },
        new char[0][],
        new String[0]);

    assertNull(new AutenticacaoPresenter(
        connection,
        new AutenticacaoModel(),
        view).autenticar());
    assertTrue(view.avisos.isEmpty());
  }

  @Test
  public void devePermitirCancelarDurantePedidoDeSenha() throws Exception {
    FakeView view = new FakeView(
        new String[] { EMAIL },
        new char[0][],
        new String[0]);

    assertNull(new AutenticacaoPresenter(
        connection,
        new AutenticacaoModel(),
        view).autenticar());
    assertTrue(view.avisos.isEmpty());
  }

  @Test
  public void deveAvisarBloqueioAposTresSenhasIncorretas()
      throws Exception {
    FakeView view = new FakeView(
        new String[] { EMAIL, EMAIL_SECUNDARIO },
        new char[][] {
            "senha-errada".toCharArray(),
            "senha-errada".toCharArray(),
            "senha-errada".toCharArray(),
            SENHA.toCharArray()
        },
        new String[] { gerarCodigoValido() });
    adicionarUsuarioSecundario();

    Usuario autenticado =
        new AutenticacaoPresenter(
            connection,
            new AutenticacaoModel(),
            view).autenticar();

    assertEquals(EMAIL_SECUNDARIO, autenticado.getEmail());
    assertEquals(2, view.loginsSolicitados);
    assertEquals(3, view.avisos.size());
    assertEquals(
        "Acesso bloqueado por dois minutos após três tentativas incorretas.",
        view.avisos.get(2));
    assertTrue(
        usuarioDAO.buscarPorEmail(EMAIL).estaBloqueado(
            System.currentTimeMillis()));
    int uid = usuarioDAO.buscarPorEmail(EMAIL).getUid();
    assertEventos(uid, 3004, 1);
    assertEventos(uid, 3005, 1);
    assertEventos(uid, 3006, 1);
    assertEventos(uid, 3007, 1);
  }

  private void adicionarUsuarioSecundario() throws Exception {
    usuarioDAO.inserir(
        "Usuário",
        EMAIL_SECUNDARIO,
        gerarHashRapidoParaTeste(SENHA),
        TokenUtils.cifrar(SEGREDO_TOTP, SENHA),
        2);
  }

  private String gerarCodigoValido() throws Exception {
    return new TOTP(SEGREDO_TOTP, 30).generateCode();
  }

  private void assertEventos(int uid, int codigo, int esperado)
      throws Exception {
    assertEquals(
        esperado,
        new RegistroDAO(connection).contarEventos(uid, codigo));
  }

  private void assertEventosAnonimos(int codigo, int esperado)
      throws Exception {
    assertEquals(
        esperado,
        new RegistroDAO(connection).listar().stream()
            .filter(registro -> registro.getUid() == null
                && registro.getCodigoMensagem() == codigo)
            .count());
  }

  private static String gerarHashRapidoParaTeste(String senha) {
    char[] caracteres = senha.toCharArray();
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    try {
      return OpenBSDBCrypt.generate("2y", caracteres, salt, 4);
    } finally {
      Arrays.fill(caracteres, '\0');
      Arrays.fill(salt, (byte) 0);
    }
  }

  private static boolean caracteresZerados(char[] caracteres) {
    for (char caractere : caracteres) {
      if (caractere != '\0') {
        return false;
      }
    }
    return true;
  }

  private static final class FakeView
      implements AutenticacaoPresenter.View {

    private final String[] logins;
    private final char[][] senhas;
    private final String[] codigos;
    private final List<String> avisos = new ArrayList<>();
    private final List<String> emailsSolicitadosParaSenha =
        new ArrayList<>();
    private int loginAtual;
    private int loginsSolicitados;
    private int senhaAtual;
    private int codigoAtual;

    private FakeView(String[] logins, char[][] senhas, String[] codigos) {
      this.logins = logins;
      this.senhas = senhas;
      this.codigos = codigos;
    }

    @Override
    public boolean solicitarLogin(AutenticacaoModel model) {
      loginsSolicitados++;
      if (loginAtual >= logins.length || logins[loginAtual] == null) {
        loginAtual++;
        return false;
      }
      model.preencherLogin(logins[loginAtual++]);
      return true;
    }

    @Override
    public boolean solicitarSenha(AutenticacaoModel model) {
      emailsSolicitadosParaSenha.add(model.getEmail());
      if (senhaAtual >= senhas.length) {
        return false;
      }
      char[] senha = senhas[senhaAtual++];
      model.preencherParesSenha(paresPara(senha));
      java.util.Arrays.fill(senha, '\0');
      return true;
    }

    private static char[][] paresPara(char[] senha) {
      char[][] pares = new char[senha.length][2];
      for (int i = 0; i < senha.length; i++) {
        pares[i][0] = senha[i];
        pares[i][1] = senha[i] >= '0' && senha[i] <= '9'
            ? (char) ('0' + (senha[i] - '0' + 1) % 10)
            : senha[i];
      }
      return pares;
    }

    @Override
    public boolean solicitarCodigoTotp(AutenticacaoModel model) {
      if (codigoAtual >= codigos.length || codigos[codigoAtual] == null) {
        codigoAtual++;
        return false;
      }
      model.preencherCodigoTotp(codigos[codigoAtual++]);
      return true;
    }

    @Override
    public void exibirAviso(String mensagem) {
      avisos.add(mensagem);
    }
  }
}
