package service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import database.RegistroDAO;
import model.Usuario;

public final class AuditoriaService {

  private static final int CODIGO_SESSAO_INICIADA = 1003;
  private static final Map<Integer, String> MENSAGENS = criarMensagens();

  private final RegistroDAO registroDAO;

  public AuditoriaService(RegistroDAO registroDAO) {
    if (registroDAO == null) {
      throw new IllegalArgumentException(
          "O DAO de registros é obrigatório.");
    }
    this.registroDAO = registroDAO;
  }

  public void registrarAcesso(int uid) throws SQLException {
    registrar(CODIGO_SESSAO_INICIADA, uid, null);
  }

  public void registrar(int codigo, Usuario usuario, String arquivo)
      throws SQLException {
    registrar(codigo, usuario == null ? null : usuario.getUid(), arquivo);
  }

  public void registrar(int codigo, Integer uid, String arquivo)
      throws SQLException {
    String mensagem = MENSAGENS.get(codigo);
    if (mensagem == null) {
      throw new IllegalArgumentException(
          "Código de auditoria não definido: " + codigo);
    }
    registroDAO.registrar(codigo, mensagem, uid, arquivo);
  }

  public int contarEventos(int uid, int codigo) throws SQLException {
    return registroDAO.contarEventos(uid, codigo);
  }

  private static Map<Integer, String> criarMensagens() {
    Map<Integer, String> mensagens = new HashMap<>();
    mensagens.put(1003, "Sessão iniciada para <login_name>.");
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
