#!/usr/bin/env bash
# Checks the puzzle logic on any JDK (8+): no Android, no Gradle, no dependencies.
# Weigh.java is plain Java on purpose; tools/WeighCheck.java says what is checked.
set -euo pipefail
cd "$(dirname "$0")/.."
out=build/logic-check
mkdir -p "$out"
javac -encoding UTF-8 -d "$out" app/src/main/java/io/github/foadsf/balancescale/Weigh.java tools/WeighCheck.java
java -ea -cp "$out" io.github.foadsf.balancescale.WeighCheck
