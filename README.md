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

## Auditoria

Os eventos são registrados em `Registros`, associados ao código e à descrição
em `Mensagens`; o texto da mensagem não é duplicado em `Registros`. O
`RegistroDAO` grava data e hora UTC, UID opcional e arquivo opcional, e consulta
os eventos em ordem cronológica. A abertura de uma sessão autenticada registra
o evento 1003, usado pelo contador de acessos. Os eventos 7001–7021 registram
abertura e ações da consulta segura, validação dos índices e tentativas de
descriptografia; o contador da pasta segura considera as consultas iniciadas
(evento 7003). A tela principal exibe o total de sessões iniciadas. Se o
registro do início da sessão falhar, a sessão não é iniciada.
