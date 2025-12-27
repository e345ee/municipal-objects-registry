#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WILDFLY_HOME="$SCRIPT_DIR/wildfly-preview-26.1.3.Final"

STANDALONE_SH="$WILDFLY_HOME/bin/standalone.sh"
CLI_SH="$WILDFLY_HOME/bin/jboss-cli.sh"

PIDFILE="$SCRIPT_DIR/wildfly.pid"
LOGFILE="$SCRIPT_DIR/wildfly.log"

export JAVA_OPTS='-server -Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Xss256k -Xlog:gc* --add-opens=java.base/java.util=ALL-UNNAMED'

find_wildfly_java_pids() {
  pgrep -f "$WILDFLY_HOME" | while read -r pid; do
    if ps -p "$pid" -o args= 2>/dev/null | grep -q '\-D\[Standalone\]'; then
      echo "$pid"
    fi
  done
}

is_running() {
  local pids
  pids="$(find_wildfly_java_pids || true)"
  [[ -n "${pids// }" ]]
}

stop_wildfly() {
  local pids
  pids="$(find_wildfly_java_pids || true)"
  if [[ -z "${pids// }" ]]; then
    echo "WildFly is not running."
    rm -f "$PIDFILE" >/dev/null 2>&1 || true
    return 0
  fi

  echo "WildFly is running (java pid(s): $(echo "$pids" | tr '\n' ' ')). Stopping..."

  if [[ -x "$CLI_SH" ]]; then
    "$CLI_SH" --connect --controller=localhost:10020 command=:shutdown >/dev/null 2>&1 || true
  fi

  for _ in {1..25}; do
    if ! is_running; then
      echo "WildFly stopped gracefully."
      rm -f "$PIDFILE" >/dev/null 2>&1 || true
      return 0
    fi
    sleep 1
  done

  echo "Graceful stop timed out. Sending SIGTERM..."
  while read -r pid; do
    [[ -n "$pid" ]] && kill "$pid" >/dev/null 2>&1 || true
  done <<< "$pids"

  for _ in {1..15}; do
    if ! is_running; then
      echo "WildFly stopped after SIGTERM."
      rm -f "$PIDFILE" >/dev/null 2>&1 || true
      return 0
    fi
    sleep 1
  done

  echo "Still running. Sending SIGKILL..."
  pids="$(find_wildfly_java_pids || true)"
  while read -r pid; do
    [[ -n "$pid" ]] && kill -9 "$pid" >/dev/null 2>&1 || true
  done <<< "$pids"

  rm -f "$PIDFILE" >/dev/null 2>&1 || true
  echo "WildFly killed."
}

start_wildfly() {
  if [[ ! -x "$STANDALONE_SH" ]]; then
    echo "ERROR: standalone.sh not found or not executable: $STANDALONE_SH" >&2
    exit 1
  fi

  rm -f "$PIDFILE" >/dev/null 2>&1 || true

  echo "Starting WildFly..."
  nohup "$STANDALONE_SH" >"$LOGFILE" 2>&1 &

  for _ in {1..30}; do
    local new_pid
    new_pid="$(find_wildfly_java_pids | tail -n 1 || true)"
    if [[ -n "${new_pid:-}" ]] && kill -0 "$new_pid" >/dev/null 2>&1; then
      echo "$new_pid" > "$PIDFILE"
      echo "WildFly started (java pid=$new_pid). Logs: $LOGFILE"
      return 0
    fi
    sleep 1
  done

  echo "ERROR: WildFly did not start. Check $LOGFILE" >&2
  exit 1
}

main() {
  if is_running; then
    stop_wildfly
  else
    echo "WildFly is not running."
  fi
  start_wildfly
}

main "$@"
