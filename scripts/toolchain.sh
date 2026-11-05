#!/usr/bin/env bash
# Shared local tool discovery. Source this file from a project script.
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  export PATH="${JAVA_HOME}/bin:${PATH}"
elif command -v java >/dev/null 2>&1; then
  JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
  export JAVA_HOME
else
  for ALGOVIZ_JDK in "${HOME}"/.jdks/*; do
    if [[ -x "${ALGOVIZ_JDK}/bin/java" ]]; then
      export JAVA_HOME="${ALGOVIZ_JDK}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      break
    fi
  done
  unset ALGOVIZ_JDK
fi
if ! command -v java >/dev/null 2>&1; then
  echo "Java 17+ is required. Set JAVA_HOME to a local JDK." >&2
  return 1
fi
if ! command -v node >/dev/null 2>&1 && [[ -s "${HOME}/.nvm/nvm.sh" ]]; then
  source "${HOME}/.nvm/nvm.sh"
fi
