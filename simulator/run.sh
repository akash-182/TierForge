#!/usr/bin/env bash
# Checks prerequisites and starts the TierForge Enrichment API Simulator.
#
# Usage:
#   ./run.sh              # starts on port 8000
#   PORT=8123 ./run.sh    # starts on a different port

set -euo pipefail

cd "$(dirname "$0")"

REQUIRED_JAVA_MAJOR=21
PORT="${PORT:-8000}"

fail_with_java_install_hint() {
    echo "Error: $1" >&2
    echo >&2
    echo "This project requires Java ${REQUIRED_JAVA_MAJOR}+. Install it with one of:" >&2
    echo "  macOS (Homebrew):   brew install openjdk@${REQUIRED_JAVA_MAJOR}" >&2
    echo "  Ubuntu/Debian:      sudo apt-get install openjdk-${REQUIRED_JAVA_MAJOR}-jdk" >&2
    echo "  Windows/other:      https://adoptium.net/temurin/releases/?version=${REQUIRED_JAVA_MAJOR}" >&2
    echo >&2
    echo "After installing, make sure 'java -version' reports ${REQUIRED_JAVA_MAJOR} or higher, then re-run ./run.sh" >&2
    exit 1
}

if ! command -v java >/dev/null 2>&1; then
    fail_with_java_install_hint "'java' was not found on your PATH."
fi

java_version_output="$(java -version 2>&1 | head -n 1)"
# Extracts the quoted version string, e.g. "21.0.11" or legacy "1.8.0_312".
java_version_string="$(echo "$java_version_output" | sed -E 's/^[^"]*"([^"]+)".*/\1/')"
java_major="$(echo "$java_version_string" | cut -d. -f1)"
if [ "$java_major" = "1" ]; then
    # Legacy versioning: "1.8.0_312" means Java 8.
    java_major="$(echo "$java_version_string" | cut -d. -f2)"
fi

if ! [[ "$java_major" =~ ^[0-9]+$ ]] || [ "$java_major" -lt "$REQUIRED_JAVA_MAJOR" ]; then
    fail_with_java_install_hint "found Java version '${java_version_output}', but Java ${REQUIRED_JAVA_MAJOR}+ is required."
fi

echo "Java ${java_major} detected — OK."
echo "Starting TierForge Enrichment API Simulator on port ${PORT} (via Gradle wrapper, no separate Gradle install needed)..."
echo

exec ./gradlew bootRun --args="--server.port=${PORT}"
