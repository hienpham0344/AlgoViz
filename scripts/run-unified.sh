#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

source "${SCRIPT_DIR}/toolchain.sh"
cd "${ROOT_DIR}"

JAR_FILE="${ROOT_DIR}/backend/target/algoViz-0.0.1-SNAPSHOT.jar"

if [[ ! -f "${JAR_FILE}" ]]; then
  echo "Unified JAR not found at ${JAR_FILE}. Triggering build first..."
  bash "${SCRIPT_DIR}/build-unified.sh"
fi

ARGS=()
USE_H2=false

for arg in "$@"; do
  case "${arg}" in
    --offline|--h2)
      USE_H2=true
      ;;
    *)
      ARGS+=("${arg}")
      ;;
  esac
done

if [[ "${USE_H2}" == "true" || "${OFFLINE:-false}" == "true" ]]; then
  echo ">>> [Offline Mode] Launching with in-memory H2 (PostgreSQL compatible mode)..."
  ARGS+=(
    "--spring.datasource.url=jdbc:h2:mem:algovizdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    "--spring.datasource.driver-class-name=org.h2.Driver"
    "--spring.datasource.username=sa"
    "--spring.datasource.password="
  )
fi

echo "=========================================================="
echo " Starting AlgoViz Unified Application on http://localhost:8080"
echo "=========================================================="
exec "${JAVA_HOME}/bin/java" -jar "${JAR_FILE}" "${ARGS[@]}"
