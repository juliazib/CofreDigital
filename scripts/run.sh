
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

CLASSPATH="bin:lib/*"
ATK_WRAPPER="/usr/share/java/java-atk-wrapper.jar"

if [[ -f "$ATK_WRAPPER" ]]; then
    CLASSPATH="$CLASSPATH:$ATK_WRAPPER"
fi

exec java -cp "$CLASSPATH" app.Main
