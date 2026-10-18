#!/usr/bin/env bash
# Checks the level logic on any JDK (8+): no Android, no Gradle, no dependencies.
# Level.java is plain Java on purpose; tools/LevelCheck.java says what is checked.
set -euo pipefail
cd "$(dirname "$0")/.."
out=build/logic-check
mkdir -p "$out"
javac -encoding UTF-8 -d "$out" app/src/main/java/io/github/foadsf/robotpath/Level.java tools/LevelCheck.java
java -ea -cp "$out" io.github.foadsf.robotpath.LevelCheck
