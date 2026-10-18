## Compilar e executar

Execute os comandos a partir da pasta raiz do projeto.

### Linux

```bash
mkdir -p bin
javac -d bin -cp "lib/*" $(find src -name "*.java")
java -cp "bin:lib/*" app.Main
```

### Compilar e executar os testes
```bash
javac -d bin -cp "bin:lib/*" $(find test -name "*.java")
find test -name "*Test.java" \
  | sed 's|test/||; s|/|.|g; s|\.java$||' \
  | xargs java -cp "bin:lib/*" org.junit.runner.JUnitCore
```

## Consultar a pasta segura

Após autenticar, selecione **Pasta segura**, escolha a pasta que contém os
arquivos `index.enc`, `index.env`, `index.asd`, `share.enc`, `share.env` e
`share.asd`, e informe a frase secreta da chave privada do usuário autenticado.
O sistema valida essa chave, verifica as assinaturas dos índices e lista os
arquivos permitidos por proprietário, grupo ou compartilhamento. Selecione um
arquivo e use **Descriptografar selecionado** para restaurá-lo na pasta
escolhida. Arquivos existentes não são sobrescritos. Um arquivo listado apenas
por pertencer ao grupo não pode ser descriptografado sem um envelope pessoal
(`.env`) ou um envelope de compartilhamento (`.shr`) destinado ao usuário.
Falhas de descriptografia, validação da assinatura/autenticidade, permissão e
gravação são apresentadas com mensagens distintas, sem expor detalhes técnicos
do sistema de arquivos ou da criptografia.

## Auditoria

Os eventos são registrados em `Registros`, associados ao código e à descrição
em `Mensagens`; o texto da mensagem não é duplicado em `Registros`. O
`RegistroDAO` grava data e hora UTC, UID opcional e arquivo opcional, e consulta
os eventos em ordem cronológica. A abertura de uma sessão autenticada registra
o evento 1003, usado pelo contador de acessos. O singleton sem estado
`AuditoriaService.getInstancia()` centraliza a gravação pelo método
`registrar(conexao, codigo, usuario, arquivo)` e recebe a conexão da operação,
sem reter conexões entre sessões. Os eventos 2001–2005, 3001–3007 e 4001–4007
registram as etapas de autenticação; os eventos 7001–7021 registram abertura e
ações da consulta segura, validação dos índices e tentativas de
descriptografia. O contador da pasta segura considera as consultas iniciadas
(evento 7003). A tela principal exibe o total de sessões iniciadas. Se o
registro do início da sessão falhar, a sessão não é iniciada.
O início e o encerramento do sistema são registrados pelos eventos 1001 e
1002; a partida distingue o cadastro inicial (1005) da operação normal (1006).
O menu registra a apresentação e as opções selecionadas (5001–5004), o
cadastro registra exibição, confirmação e falhas de validação (6001–6010), e a
tela de saída registra a escolha entre encerrar sessão, encerrar sistema ou
voltar ao menu (8001–8004). Encerrar a sessão registra 1004 e retorna à
autenticação; encerrar o sistema também registra 1004, fecha a conexão e
registra 1002.
No menu principal, a opção 1 (cadastro de usuários) registra 5002 e a opção 2
(pasta segura) registra 5003. O cadastro pelo menu cria usuários no grupo
“Usuário”; erros preservam os campos preenchidos para correção e, após um
cadastro bem-sucedido, o formulário permanece aberto e vazio para o próximo
cadastro.
No primeiro uso, depois que o cadastro inicial do administrador é concluído,
a chave é validada e mantida em memória e a autenticação começa imediatamente.
Ao encerrar uma sessão, a frase secreta e o contexto do administrador são
mantidos durante a execução; uma nova autenticação começa sem solicitá-los de
novo. Eles são descartados quando o sistema é encerrado.
O `LogView` é um programa de apoio separado do Cofre Digital. Execute o script
abaixo passando o caminho do arquivo da chave privada do administrador:

```bash
./scripts/logview.sh /caminho/para/chave-privada
```

O script compila o projeto e inicia o LogView a partir da raiz, onde está
`cofre.db`. O programa solicita a frase secreta pelo terminal sem exibi-la,
valida a chave privada contra o certificado do administrador cadastrado e só
então abre a tela gráfica com os registros em ordem cronológica, exibindo data
e hora, UID do usuário, código e mensagem, além do arquivo selecionado quando
aplicável. Marcadores como `<login_name>` e `<arq_name>` são substituídos pelo
e-mail usado no login e pelo nome do arquivo registrado. O caminho da chave é
fornecido como argumento; a frase secreta nunca é argumento nem aparece na
tela. Execute o script em um terminal interativo.
