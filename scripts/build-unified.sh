#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${SCRIPT_DIR}/toolchain.sh"
cd "${ROOT_DIR}"
npm --prefix frontend run build
cd "${ROOT_DIR}/backend"
./mvnw "$@" clean package -DskipTests
printf '\nBuilt backend/target/algoViz-0.0.1-SNAPSHOT.jar\nRun: npm run start:offline\n'
