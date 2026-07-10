#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
OUTPUT_DIR="$ROOT_DIR/dist"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

git -C "$ROOT_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "Not inside a Git working tree."

if [[ -n "$(git -C "$ROOT_DIR" status --porcelain)" ]]; then
  fail "Working tree is not clean. Commit or stash changes before creating a deployment package."
fi

VERSION="$(git -C "$ROOT_DIR" describe --tags --exact-match HEAD 2>/dev/null)" || fail "HEAD is not tagged. Refusing to create an unversioned deployment package."

ARCHIVE_NAME="centaurus-deploy-$VERSION.tar.gz"
ARCHIVE_PATH="$OUTPUT_DIR/$ARCHIVE_NAME"

mkdir -p "$OUTPUT_DIR"

tar \
  --create \
  --gzip \
  --file "$ARCHIVE_PATH" \
  --directory "$ROOT_DIR" \
  --exclude='deploy/.env' \
  --exclude='deploy/compose/.env' \
  --exclude='deploy/build-output' \
  --exclude='deploy/build-output/*' \
  deploy

echo "$ARCHIVE_PATH"
