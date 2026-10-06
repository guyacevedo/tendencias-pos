#!/usr/bin/env bash
# Prepara Java 21 en el shell Linux de Claude (no es necesario en tu Mac).
# Uso: source scripts/claude-env.sh
JDK_DIR="$HOME/jdk"
if ! ls "$JDK_DIR"/jdk-21* >/dev/null 2>&1; then
  mkdir -p "$JDK_DIR"
  arch=$(uname -m); [ "$arch" = "arm64" ] && arch=aarch64; [ "$arch" = "x86_64" ] && arch=x64
  curl -sL "https://api.adoptium.net/v3/binary/latest/21/ga/linux/$arch/jdk/hotspot/normal/eclipse" | tar xz -C "$JDK_DIR"
fi
export JAVA_HOME=$(ls -d "$JDK_DIR"/jdk-21* | head -1)
export PATH="$JAVA_HOME/bin:$PATH"
# Compilar y probar sin ruido: solo errores.
tpos_build() { ./gradlew spotlessApply build -q --console=plain "$@" 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|Sharing is only supported|^\s*$'; return "${PIPESTATUS[0]}"; }
