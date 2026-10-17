
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"
mkdir -p bin

echo "Compilando código principal..."
find src -name "*.java" -print0 |
    xargs -0 javac -d bin -cp "lib/*"

echo "Compilando testes..."
find test -name "*.java" -print0 |
    xargs -0 javac -d bin -cp "bin:lib/*"

echo "Executando testes..."
find test -name "*Test.java" |
    sed 's|/|.|g; s|\.java$||' |
    xargs -r java -cp "bin:lib/*" org.junit.runner.JUnitCore
