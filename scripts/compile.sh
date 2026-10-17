
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"
mkdir -p bin

find src -name "*.java" -print0 |
    xargs -0 javac -d bin -cp "lib/*"

echo "Compilação concluída."
