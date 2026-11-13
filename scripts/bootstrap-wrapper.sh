#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DEST="$ROOT/gradle/wrapper/gradle-wrapper.jar"
EXPECTED="1b33c55baabb587c669f562ae36f953de2481846"
URL="https://raw.githubusercontent.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/e93d7f09d4eec12c76d1d7be9bf4eaad0246e9e0/gradle/wrapper/gradle-wrapper.jar"
if ! command -v git >/dev/null 2>&1; then
    echo "Git is required to validate the Gradle Wrapper JAR." >&2
    exit 1
fi
if [ ! -f "$DEST" ]; then
    echo "Downloading pinned official Gradle Wrapper JAR..."
    curl -fsSL --retry 3 "$URL" -o "$DEST"
fi
ACTUAL=$(git hash-object "$DEST")
if [ "$ACTUAL" != "$EXPECTED" ]; then
    rm -f "$DEST"
    echo "Gradle Wrapper JAR checksum mismatch." >&2
    exit 1
fi
echo "Gradle Wrapper JAR validated."
