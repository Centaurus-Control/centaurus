#!/usr/bin/env bash
set -euo pipefail

echo "Centaurus dev echo script"
echo "args=$*"
echo "target=${CENTAURUS_PARAM_TARGET:-unset}"
echo "dryRun=${CENTAURUS_PARAM_DRYRUN:-unset}"
echo "message=${CENTAURUS_PARAM_MESSAGE:-unset}"
