#!/bin/sh

SCRIPT_DIR=$(CDPATH= cd -P "$(dirname "$0")" && pwd) || exit 1
exec "$SCRIPT_DIR/gradlew" --write-verification-metadata sha256 resolveDependencySources
