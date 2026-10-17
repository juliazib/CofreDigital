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
  | sed 's|src/test/||; s|/|.|g; s|\.java$||' \
  | xargs java -cp "bin:lib/*" org.junit.runner.JUnitCore
```