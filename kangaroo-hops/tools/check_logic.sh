#!/usr/bin/env bash
# Checks the puzzle logic on any JDK (8+): no Android, no Gradle, no dependencies.
# Hop.java and Numbers.java are plain Java on purpose; tools/HopCheck.java says what is checked.
set -euo pipefail
cd "$(dirname "$0")/.."
out=build/logic-check
mkdir -p "$out"
src=app/src/main/java/io/github/foadsf/kangaroohops
javac -encoding UTF-8 -d "$out" "$src/Hop.java" "$src/Numbers.java" tools/HopCheck.java
java -ea -cp "$out" io.github.foadsf.kangaroohops.HopCheck
