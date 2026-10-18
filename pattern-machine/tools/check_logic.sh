#!/usr/bin/env bash
# Checks the puzzle logic on any JDK (8+): no Android, no Gradle, no dependencies.
# Tokens.java and Puzzle.java are plain Java on purpose; tools/PuzzleCheck.java says what is checked.
set -euo pipefail
cd "$(dirname "$0")/.."
out=build/logic-check
mkdir -p "$out"
src=app/src/main/java/io/github/foadsf/patternmachine
javac -encoding UTF-8 -d "$out" "$src/Tokens.java" "$src/Puzzle.java" tools/PuzzleCheck.java
java -ea -cp "$out" io.github.foadsf.patternmachine.PuzzleCheck
