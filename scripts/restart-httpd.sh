#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

HTTPD_CONF="$SCRIPT_DIR/frontend/httpd.conf"
HTTPD_BIN="$(command -v httpd)"

PIDFILE="$SCRIPT_DIR/httpd.pid"
LOGFILE="$SCRIPT_DIR/httpd.log"

if [[ -z "$HTTPD_BIN" ]]; then
  echo "ERROR: httpd not found in PATH" >&2
  exit 1
fi

if [[ ! -f "$HTTPD_CONF" ]]; then
  echo "ERROR: httpd.conf not found at $HTTPD_CONF" >&2
  exit 1
fi

find_httpd_pids() {
  pgrep -f "httpd.*-f $HTTPD_CONF" || true
}

is_running() {
  local pids
  pids="$(find_httpd_pids)"
  [[ -n "${pids// }" ]]
}

stop_httpd() {
  local pids
  pids="$(find_httpd_pids)"

  if [[ -z "${pids// }" ]]; then
    echo "httpd is not running."
    rm -f "$PIDFILE" >/dev/null 2>&1 || true
    return 0
  fi

  echo "httpd is running (pid(s): $(echo "$pids" | tr '\n' ' ')). Stopping..."

  "$HTTPD_BIN" -f "$HTTPD_CONF" -k stop || true

  for _ in {1..10}; do
    if ! is_running; then
      echo "httpd stopped gracefully."
      rm -f "$PIDFILE" >/dev/null 2>&1 || true
      return 0
    fi
    sleep 1
  done

  echo "Graceful stop timed out. Sending SIGTERM..."
  while read -r pid; do
    [[ -n "$pid" ]] && kill "$pid" >/dev/null 2>&1 || true
  done <<< "$pids"

  sleep 3

  if is_running; then
    echo "Still running. Sending SIGKILL..."
    while read -r pid; do
      [[ -n "$pid" ]] && kill -9 "$pid" >/dev/null 2>&1 || true
    done <<< "$pids"
  fi

  rm -f "$PIDFILE" >/dev/null 2>&1 || true
  echo "httpd stopped."
}

start_httpd() {
  echo "Starting httpd..."
  rm -f "$PIDFILE" >/dev/null 2>&1 || true

  "$HTTPD_BIN" -f "$HTTPD_CONF" -k start >>"$LOGFILE" 2>&1

  for _ in {1..10}; do
    local pid
    pid="$(find_httpd_pids | head -n 1 || true)"
    if [[ -n "${pid:-}" ]]; then
      echo "$pid" > "$PIDFILE"
      echo "httpd started (pid=$pid). Logs: $LOGFILE"
      return 0
    fi
    sleep 1
  done

  echo "ERROR: httpd did not start. Check $LOGFILE" >&2
  exit 1
}

main() {
  if is_running; then
    stop_httpd
  fi
  start_httpd
}

main "$@"
