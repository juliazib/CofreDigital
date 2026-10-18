#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"

if [[ "$#" -ne 1 || -z "$1" ]]; then
    echo "Uso: $0 <caminho-da-chave-privada-do-administrador>" >&2
    exit 2
fi

cd "$PROJECT_ROOT"
"$SCRIPT_DIR/compile.sh"

CLASSPATH="bin:lib/*"
ATK_WRAPPER="/usr/share/java/java-atk-wrapper.jar"
if [[ -f "$ATK_WRAPPER" ]]; then
    CLASSPATH="$CLASSPATH:$ATK_WRAPPER"
fi

exec java -cp "$CLASSPATH" support.LogView "$1"
