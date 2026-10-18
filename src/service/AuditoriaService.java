package service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import database.RegistroDAO;
import model.Usuario;

public final class AuditoriaService {

  private static final AuditoriaService INSTANCIA =
      new AuditoriaService();
  private static final Map<Integer, String> MENSAGENS = criarMensagens();

  private AuditoriaService() {
  }

  public static AuditoriaService getInstancia() {
    return INSTANCIA;
  }

  public void registrar(
      java.sql.Connection connection,
      int codigo,
      Usuario usuario,
      String arquivo)
      throws SQLException {
    registrar(
        connection,
        codigo,
        usuario == null ? null : usuario.getUid(),
        arquivo);
  }

  public void registrar(
      java.sql.Connection connection,
      int codigo,
      Integer uid,
      String arquivo)
      throws SQLException {
    if (connection == null) {
      throw new IllegalArgumentException("A conexão é obrigatória.");
    }
    String mensagem = MENSAGENS.get(codigo);
    if (mensagem == null) {
      throw new IllegalArgumentException(
          "Código de auditoria não definido: " + codigo);
    }
    new RegistroDAO(connection).registrar(codigo, mensagem, uid, arquivo);
  }

  public int contarEventos(
      java.sql.Connection connection,
      int uid,
      int codigo) throws SQLException {
    if (connection == null) {
      throw new IllegalArgumentException("A conexão é obrigatória.");
    }
    return new RegistroDAO(connection).contarEventos(uid, codigo);
  }

  private static Map<Integer, String> criarMensagens() {
    Map<Integer, String> mensagens = new HashMap<>();
    mensagens.put(1001, "Sistema iniciado.");
    mensagens.put(1002, "Sistema encerrado.");
    mensagens.put(1003, "Sessão iniciada para <login_name>.");
    mensagens.put(1004, "Sessão encerrada para <login_name>.");
    mensagens.put(
        1005,
        "Partida do sistema iniciada para cadastro do administrador.");
    mensagens.put(
        1006,
        "Partida do sistema iniciada para operação normal pelos usuários.");
    mensagens.put(2001, "Autenticação etapa 1 iniciada.");
    mensagens.put(2002, "Autenticação etapa 1 encerrada.");
    mensagens.put(2003,
        "Login name <login_name> identificado com acesso liberado.");
    mensagens.put(2004,
        "Login name <login_name> identificado com acesso bloqueado.");
    mensagens.put(2005, "Login name <login_name> não identificado.");
    mensagens.put(3001,
        "Autenticação etapa 2 iniciada para <login_name>.");
    mensagens.put(3002,
        "Autenticação etapa 2 encerrada para <login_name>.");
    mensagens.put(3003,
        "Senha pessoal verificada positivamente para <login_name>.");
    mensagens.put(3004,
        "Primeiro erro da senha pessoal contabilizado para <login_name>.");
    mensagens.put(3005,
        "Segundo erro da senha pessoal contabilizado para <login_name>.");
    mensagens.put(3006,
        "Terceiro erro da senha pessoal contabilizado para <login_name>.");
    mensagens.put(3007,
        "Acesso do usuario <login_name> bloqueado pela autenticação etapa 2.");
    mensagens.put(4001,
        "Autenticação etapa 3 iniciada para <login_name>.");
    mensagens.put(4002,
        "Autenticação etapa 3 encerrada para <login_name>.");
    mensagens.put(4003,
        "Token verificado positivamente para <login_name>.");
    mensagens.put(4004,
        "Primeiro erro de token contabilizado para <login_name>.");
    mensagens.put(4005,
        "Segundo erro de token contabilizado para <login_name>.");
    mensagens.put(4006,
        "Terceiro erro de token contabilizado para <login_name>.");
    mensagens.put(4007,
        "Acesso do usuario <login_name> bloqueado pela autenticação etapa 3.");
    mensagens.put(5001, "Tela principal apresentada para <login_name>.");
    mensagens.put(5002,
        "Opção 1 do menu principal selecionada por <login_name>.");
    mensagens.put(5003,
        "Opção 2 do menu principal selecionada por <login_name>.");
    mensagens.put(5004,
        "Opção 3 do menu principal selecionada por <login_name>.");
    mensagens.put(6001, "Tela de cadastro apresentada para <login_name>.");
    mensagens.put(6002, "Botão cadastrar pressionado por <login_name>.");
    mensagens.put(6003,
        "Senha pessoal inválida fornecida por <login_name>.");
    mensagens.put(6004,
        "Caminho do certificado digital inválido fornecido por <login_name>.");
    mensagens.put(6005,
        "Chave privada verificada negativamente para <login_name> (caminho inválido).");
    mensagens.put(6006,
        "Chave privada verificada negativamente para <login_name> (frase secreta inválida).");
    mensagens.put(6007,
        "Chave privada verificada negativamente para <login_name> (assinatura digital inválida).");
    mensagens.put(6008,
        "Confirmação de dados aceita por <login_name>.");
    mensagens.put(6009,
        "Confirmação de dados rejeitada por <login_name>.");
    mensagens.put(
        6010,
        "Botão voltar de cadastro para o menu principal pressionado por <login_name>.");
    mensagens.put(8001, "Tela de saída apresentada para <login_name>.");
    mensagens.put(
        8002,
        "Botão encerrar sessão pressionado por <login_name>.");
    mensagens.put(
        8003,
        "Botão encerrar sistema pressionado por <login_name>.");
    mensagens.put(
        8004,
        "Botão voltar de sair para o menu principal pressionado por <login_name>.");
    mensagens.put(7001,
        "Tela de consulta de arquivos secretos apresentada para <login_name>.");
    mensagens.put(7002,
        "Botão voltar de consulta para o menu principal pressionado por <login_name>.");
    mensagens.put(7003,
        "Botão Listar de consulta pressionado por <login_name>.");
    mensagens.put(7004,
        "Caminho de pasta inválido fornecido por <login_name>.");
    mensagens.put(7005,
        "Arquivo de índice decriptado com sucesso para <login_name>.");
    mensagens.put(7006,
        "Arquivo de índice verificado (integridade e autenticidade) com sucesso para <login_name>.");
    mensagens.put(7007,
        "Falha na decriptação do arquivo de índice para <login_name>.");
    mensagens.put(7008,
        "Falha na verificação (integridade e autenticidade) do arquivo de índice para <login_name>.");
    mensagens.put(7009,
        "Lista de arquivos presentes no índice apresentada para <login_name>.");
    mensagens.put(7010,
        "Arquivo de share decriptado com sucesso para <login_name>.");
    mensagens.put(7011,
        "Arquivo de share verificado (integridade e autenticidade) com sucesso para <login_name>.");
    mensagens.put(7012,
        "Falha na decriptação do arquivo de share para <login_name>.");
    mensagens.put(7013,
        "Falha na verificação (integridade e autenticidade) do arquivo de share para <login_name>.");
    mensagens.put(7014,
        "Lista de arquivos presentes no share apresentada para <login_name>.");
    mensagens.put(7015,
        "Arquivo <arq_name> selecionado por <login_name> para decriptação.");
    mensagens.put(7016,
        "Acesso permitido ao arquivo <arq_name> para <login_name>.");
    mensagens.put(7017,
        "Acesso negado ao arquivo <arq_name> para <login_name>.");
    mensagens.put(7018,
        "Arquivo <arq_name> decriptado com sucesso para <login_name>.");
    mensagens.put(7019,
        "Arquivo <arq_name> verificado (integridade e autenticidade) com sucesso para <login_name>.");
    mensagens.put(7020,
        "Falha na decriptação do arquivo <arq_name> para <login_name>.");
    mensagens.put(7021,
        "Falha na verificação (integridade e autenticidade) do arquivo <arq_name> para <login_name>.");
    return Collections.unmodifiableMap(mensagens);
  }
}
