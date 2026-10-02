#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

SERVER_JAR=""
for candidate in spigot-1.8.8.jar spigot.jar paper-1.8.8.jar paper.jar craftbukkit-1.8.8.jar server.jar; do
  if [[ -f "$candidate" ]]; then SERVER_JAR="$candidate"; break; fi
done

if [[ -z "$SERVER_JAR" && -n "${SPIGOT_JAR:-}" && -f "$SPIGOT_JAR" ]]; then
  SERVER_JAR="$SPIGOT_JAR"
fi

if [[ -z "$SERVER_JAR" ]]; then
  echo "[ERROR] Put your 1.8.8 Spigot/Paper server jar in this folder, or set SPIGOT_JAR." >&2
  exit 1
fi

rm -rf build
mkdir -p build/classes
javac -source 1.8 -target 1.8 -encoding UTF-8 -cp "$SERVER_JAR" -d build/classes \
  src/main/java/com/notgamingop/transparentaudit/TransparentAudit.java
cp src/main/resources/plugin.yml build/classes/plugin.yml
cp src/main/resources/config.yml build/classes/config.yml
jar cf build/TransparentAudit-1.2.0.jar -C build/classes .
echo "BUILD OK: build/TransparentAudit-1.2.0.jar"
