
package test.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

import crypto.PasswordUtils;
import crypto.TOTP;
import crypto.TokenUtils;
import database.DatabaseSchema;
import database.UsuarioDAO;
import model.Usuario;
import service.AutenticacaoService;

public class AutenticacaoServiceTest {

  private Connection connection;
  private AutenticacaoService autenticacao;


    private String codigoTOTPInvalido(String segredoBase32)
            throws Exception {
        TOTP totp = new TOTP(segredoBase32, 30);
        long agora = System.currentTimeMillis() / 1000L;

        for (int i = 0; i < 1_000_000; i++) {
            String candidato = String.format(
                java.util.Locale.ROOT, "%06d", i
            );

            if (!totp.validateCode(candidato, agora)) {
                return candidato;
            }
        }

        throw new AssertionError(
            "Não foi possível encontrar um código TOTP inválido."
        );
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


  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");

    try (java.sql.Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
    }

    DatabaseSchema.createTables(connection);

    UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
    autenticacao = new AutenticacaoService(usuarioDAO);
  }

  @After
  public void tearDown() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  public void deveRecusarEmailNaoCadastrado() throws Exception {
    assertFalse(
        autenticacao.iniciarLogin("inexistente@example.com"));
  }

  
    @Test
    public void deveRecusarLoginDeUsuarioBloqueado() throws Exception {
    UsuarioDAO usuarioDAO = new UsuarioDAO(connection);

    int uid = usuarioDAO.inserir(
        "Usuário de Teste",
        "teste@example.com",
        "hash-ficticio",
        new byte[] { 1, 2, 3 },
        2
    );

    long agora = System.currentTimeMillis();

    usuarioDAO.registrarFalhaDeSenha(uid, agora);
    usuarioDAO.registrarFalhaDeSenha(uid, agora);
    usuarioDAO.registrarFalhaDeSenha(uid, agora);

    autenticacao = new AutenticacaoService(usuarioDAO);

    assertFalse(autenticacao.iniciarLogin("teste@example.com"));
    }

    @Test
    public void deveAceitarSenhaCorreta() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        String hash = PasswordUtils.gerarHash("12345678");

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            new byte[] { 1, 2, 3 },
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertTrue(
            autenticacao.validarSenha("teste@example.com", "12345678")
        );
    }

    @Test
    public void deveValidarPinQuandoSenhaFoiInseridaComoPares() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            gerarHashRapidoParaTeste("12345678"),
            new byte[] { 1, 2, 3 },
            2
        );
        autenticacao = new AutenticacaoService(usuarioDAO);

        char[][] paresSelecionados = {
            { '1', '9' },
            { '3', '2' },
            { '3', '9' },
            { '4', '3' },
            { '0', '5' },
            { '4', '6' },
            { '7', '6' },
            { '6', '8' }
        };

        assertEquals(
            "12345678",
            autenticacao.validarSenhaPorPares(
                "teste@example.com",
                paresSelecionados));
        assertEquals(
            0,
            usuarioDAO.buscarPorEmail("teste@example.com").getFalhasSenha());
    }

    @Test
    public void deveEncontrarPinMesmoQuandoTodosOsDigitosEstaoNoSegundoLado()
            throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            PasswordUtils.gerarHash("12345678"),
            new byte[] { 1, 2, 3 },
            2
        );
        autenticacao = new AutenticacaoService(usuarioDAO);
        char[][] paresSelecionados = {
            { '9', '1' },
            { '8', '2' },
            { '7', '3' },
            { '6', '4' },
            { '5', '5' },
            { '4', '6' },
            { '3', '7' },
            { '2', '8' }
        };
        paresSelecionados[4][1] = '0';

        assertEquals(
            "12345678",
            autenticacao.validarSenhaPorPares(
                "teste@example.com",
                paresSelecionados));
    }

    @Test
    public void deveValidarPinDeDezDigitosComUltimoDigitoNoSegundoLado()
            throws Exception {
        String pin = "1234567890";
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            gerarHashRapidoParaTeste(pin),
            new byte[] { 1, 2, 3 },
            2
        );
        autenticacao = new AutenticacaoService(usuarioDAO);

        char[][] paresSelecionados = new char[pin.length()][2];
        for (int i = 0; i < pin.length(); i++) {
            char digitoAlternativo =
                (char) ('0' + (pin.charAt(i) - '0' + 1) % 10);
            paresSelecionados[i][0] = pin.charAt(i);
            paresSelecionados[i][1] = digitoAlternativo;
        }
        paresSelecionados[pin.length() - 1][0] = '1';
        paresSelecionados[pin.length() - 1][1] = '0';

        assertEquals(
            pin,
            autenticacao.validarSenhaPorPares(
                "teste@example.com",
                paresSelecionados));
    }

    @Test
    public void deveContabilizarUmaFalhaQuandoNenhumaCombinacaoCorresponde()
            throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            gerarHashRapidoParaTeste("12345678"),
            new byte[] { 1, 2, 3 },
            2
        );
        autenticacao = new AutenticacaoService(usuarioDAO);
        char[][] paresSelecionados = {
            { '0', '9' },
            { '3', '2' },
            { '3', '9' },
            { '4', '3' },
            { '0', '5' },
            { '4', '6' },
            { '7', '6' },
            { '6', '8' }
        };

        assertNull(
            autenticacao.validarSenhaPorPares(
                "teste@example.com",
                paresSelecionados));
        assertEquals(
            1,
            usuarioDAO.buscarPorEmail("teste@example.com").getFalhasSenha());
    }

    @Test
    public void deveRecusarSenhaIncorreta() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        String hash = PasswordUtils.gerarHash("12345678");

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            new byte[] { 1, 2, 3 },
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertFalse(
            autenticacao.validarSenha("teste@example.com", "senhaerrada")
        );

        Usuario usuario = usuarioDAO.buscarPorEmail("teste@example.com");
        org.junit.Assert.assertEquals(1, usuario.getFalhasSenha());
    }

    @Test
    public void deveBloquearAposTresSenhasIncorretas() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
        String hash = PasswordUtils.gerarHash("12345678");

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            new byte[] { 1, 2, 3 },
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertFalse(autenticacao.validarSenha("teste@example.com", "errada1"));
        assertFalse(autenticacao.validarSenha("teste@example.com", "errada2"));
        assertFalse(autenticacao.validarSenha("teste@example.com", "errada3"));

        assertFalse(
            "O login deve ser recusado enquanto o usuário estiver bloqueado.",
            autenticacao.validarSenha("teste@example.com", "12345678")
        );
    }

    @Test
    public void deveAceitarTokenTOTPValido() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);

        String senha = "12345678";
        String segredoBase32 = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        byte[] tokenCifrado = TokenUtils.cifrar(segredoBase32, senha);
        String hash = PasswordUtils.gerarHash(senha);

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            tokenCifrado,
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        // Este teste será concluído quando implementarmos validarToken.
        long agora = System.currentTimeMillis() / 1000L;
        TOTP totp = new TOTP(segredoBase32, 30);
        String codigo = totp.generateCode(agora);

        assertTrue(
            autenticacao.validarToken(
                "teste@example.com",
                senha,
                codigo
            )
        );
    }

    @Test
    public void deveRecusarTokenTOTPInvalido() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);

        String senha = "12345678";
        String segredoBase32 = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        byte[] tokenCifrado = TokenUtils.cifrar(segredoBase32, senha);
        String hash = PasswordUtils.gerarHash(senha);

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            tokenCifrado,
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

    String codigoInvalido = codigoTOTPInvalido(segredoBase32);

    assertFalse(
        autenticacao.validarToken(
            "teste@example.com",
            senha,
            codigoInvalido
        )
    );
    }

    @Test
    public void deveBloquearAposTresTokensIncorretos() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);

        String senha = "12345678";
        String segredoBase32 = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        byte[] tokenCifrado = TokenUtils.cifrar(segredoBase32, senha);
        String hash = PasswordUtils.gerarHash(senha);

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            tokenCifrado,
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertFalse(
            autenticacao.validarToken(
                "teste@example.com", senha, codigoTOTPInvalido(segredoBase32)
            )
        );
        assertFalse(
            autenticacao.validarToken(
                "teste@example.com", senha, codigoTOTPInvalido(segredoBase32)
            )
        );
        assertFalse(
            autenticacao.validarToken(
                "teste@example.com", senha, codigoTOTPInvalido(segredoBase32)
            )
        );

        assertFalse(
            "O login deve ser recusado após três falhas de TOTP.",
            autenticacao.validarToken(
                "teste@example.com", senha, "444444"
            )
        );
    }


    @Test
    public void devePermitirLoginAposExpiracaoDoBloqueio() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);

        String senha = "12345678";
        String hash = PasswordUtils.gerarHash(senha);

        int uid = usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            new byte[] { 1, 2, 3 },
            2
        );

        long agora = System.currentTimeMillis();

        // Simula um bloqueio que já expirou.
        usuarioDAO.registrarFalhaDeSenha(uid, agora - 180_000L);
        usuarioDAO.registrarFalhaDeSenha(uid, agora - 180_000L);
        usuarioDAO.registrarFalhaDeSenha(uid, agora - 180_000L);

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertTrue(
            "O login deve ser permitido após a expiração do bloqueio.",
            autenticacao.iniciarLogin("teste@example.com")
        );

        Usuario usuario = usuarioDAO.buscarPorEmail("teste@example.com");

        assertFalse(usuario.estaBloqueado(System.currentTimeMillis()));
        org.junit.Assert.assertNull(usuario.getBloqueadoAte());
    }

    
    @Test
    public void deveZerarFalhasDeSenhaQuandoSenhaCorretaNoValidarToken()
            throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);

        String senha = "12345678";
        String segredoBase32 = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        String hash = PasswordUtils.gerarHash(senha);
        byte[] tokenCifrado = TokenUtils.cifrar(segredoBase32, senha);

        usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            tokenCifrado,
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        assertFalse(
            autenticacao.validarSenha("teste@example.com", "senhaerrada")
        );

        assertFalse(
            autenticacao.validarToken("teste@example.com", senha, "111111")
        );

        Usuario usuario = usuarioDAO.buscarPorEmail("teste@example.com");

        org.junit.Assert.assertEquals(0, usuario.getFalhasSenha());
    }
    
    @Test
    public void deveZerarFalhasDeTokenAposTokenValido() throws Exception {
        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);

        String senha = "12345678";
        String segredoBase32 = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        String hash = PasswordUtils.gerarHash(senha);
        byte[] tokenCifrado = TokenUtils.cifrar(segredoBase32, senha);

        int uid = usuarioDAO.inserir(
            "Usuário de Teste",
            "teste@example.com",
            hash,
            tokenCifrado,
            2
        );

        autenticacao = new AutenticacaoService(usuarioDAO);

        // Registra uma falha de TOTP.
        String codigoInvalido = codigoTOTPInvalido(segredoBase32);

        assertFalse(
            autenticacao.validarToken(
                "teste@example.com",
                senha,
                codigoInvalido
            )
        );
        // Gera um código válido para o horário atual.
        long agora = System.currentTimeMillis() / 1000L;
        TOTP totp = new TOTP(segredoBase32, 30);
        String codigoValido = totp.generateCode(agora);

        assertTrue(
            autenticacao.validarToken(
                "teste@example.com", senha, codigoValido
            )
        );

        Usuario usuario = usuarioDAO.buscarPorId(uid);

        org.junit.Assert.assertEquals(0, usuario.getFalhasToken());
    }


}
