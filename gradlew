#!/bin/sh
# Gradle wrapper script for Unix-like systems (sh compatible)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_WRAPPER_JAR="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
  echo "Gradle wrapper jar not found at $GRADLE_WRAPPER_JAR"
  exit 1
fi

exec java -jar "$GRADLE_WRAPPER_JAR" "$@"
