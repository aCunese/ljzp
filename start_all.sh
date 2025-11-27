#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VUE_DIR="$ROOT_DIR/yolo_cropDisease_detection_vue"
SPRING_DIR="$ROOT_DIR/yolo_cropDisease_detection_springboot"
FLASK_DIR="$ROOT_DIR/yolo_cropDisease_detection_flask"
LOG_DIR="$ROOT_DIR/.devlogs"

INSTALL_DEPS=0
START_FRONTEND=1
START_BACKEND=1
START_FLASK=1

usage() {
  cat <<'EOF'
Usage: ./start_all.sh [options]

Options:
  --install-deps     Force dependency installation for all services.
  --skip-frontend    Do not start the Vue dev server.
  --skip-backend     Do not start the Spring Boot service.
  --skip-flask       Do not start the Flask inference service.
  -h, --help         Show this help.

Environment overrides:
  FRONTEND_CMD   Command (relative to Vue dir) used to start the frontend. Default: "npm run dev"
  BACKEND_CMD    Command (relative to Spring dir) used to start the backend. Default: "./mvnw spring-boot:run"
  FLASK_CMD      Command (relative to Flask dir) used to start the Flask service. Default: "<python> main.py"
  PYTHON_BIN     Python binary used to create a virtualenv when needed (defaults to python3/python).
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --install-deps) INSTALL_DEPS=1 ;;
    --skip-frontend) START_FRONTEND=0 ;;
    --skip-backend) START_BACKEND=0 ;;
    --skip-flask) START_FLASK=0 ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
  shift
done

mkdir -p "$LOG_DIR"

pids=()

log() {
  printf '[launcher] %s\n' "$*"
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

ensure_frontend() {
  [[ $START_FRONTEND -eq 1 ]] || return 0
  if ! command_exists npm; then
    echo "npm is required to start the frontend." >&2
    exit 1
  fi
  if [[ ! -d "$VUE_DIR" ]]; then
    echo "Vue directory not found: $VUE_DIR" >&2
    exit 1
  fi
  if [[ $INSTALL_DEPS -eq 1 || ! -d "$VUE_DIR/node_modules" ]]; then
    log "Installing frontend dependencies..."
    (cd "$VUE_DIR" && npm install)
  fi
}

ensure_backend() {
  [[ $START_BACKEND -eq 1 ]] || return 0
  if [[ ! -d "$SPRING_DIR" ]]; then
    echo "Spring Boot directory not found: $SPRING_DIR" >&2
    exit 1
  fi
  chmod +x "$SPRING_DIR/mvnw" >/dev/null 2>&1 || true
  if [[ $INSTALL_DEPS -eq 1 ]]; then
    log "Pre-fetching backend dependencies (mvnw dependency:go-offline)..."
    (cd "$SPRING_DIR" && ./mvnw -q dependency:go-offline -DskipTests)
  fi
}

ensure_flask() {
  [[ $START_FLASK -eq 1 ]] || return 0
  if [[ ! -d "$FLASK_DIR" ]]; then
    echo "Flask directory not found: $FLASK_DIR" >&2
    exit 1
  fi

  local python_bin="${PYTHON_BIN:-}"
  local venv_dir="$FLASK_DIR/.venv"

  if [[ -z "$python_bin" ]]; then
    if command_exists python3; then
      python_bin="$(command -v python3)"
    elif command_exists python; then
      python_bin="$(command -v python)"
    else
      echo "python3/python is required to manage the Flask service." >&2
      exit 1
    fi
  fi

  if [[ $INSTALL_DEPS -eq 1 || ! -x "$venv_dir/bin/python" ]]; then
    log "Preparing Flask virtual environment..."
    "$python_bin" -m venv "$venv_dir"
  fi

  if [[ -x "$venv_dir/bin/pip" && ($INSTALL_DEPS -eq 1 || ! -f "$venv_dir/.deps_installed") ]]; then
    log "Installing Flask dependencies..."
    "$venv_dir/bin/pip" install -r "$FLASK_DIR/requirements.txt"
    touch "$venv_dir/.deps_installed"
  fi
}

launch_service() {
  local name="$1"
  local workdir="$2"
  shift 2
  local log_file="$LOG_DIR/${name}.log"

  log "Starting ${name} (logs: ${log_file})"

  (
    cd "$workdir"
    "$@"
  ) >>"$log_file" 2>&1 &

  local pid=$!
  pids+=("$pid")
  log "${name} started with PID ${pid}"
}

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM
  if [[ ${#pids[@]} -gt 0 ]]; then
    echo
    log "Stopping services..."
  fi
  for pid in "${pids[@]}"; do
    if kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
      wait "$pid" >/dev/null 2>&1 || true
    fi
  done
  exit "$exit_code"
}

trap cleanup EXIT INT TERM

ensure_frontend
ensure_backend
ensure_flask

if [[ $START_FRONTEND -eq 1 ]]; then
  IFS=' ' read -r -a frontend_cmd <<< "${FRONTEND_CMD:-npm run dev}"
  launch_service "frontend" "$VUE_DIR" "${frontend_cmd[@]}"
fi

if [[ $START_BACKEND -eq 1 ]]; then
  IFS=' ' read -r -a backend_cmd <<< "${BACKEND_CMD:-./mvnw spring-boot:run}"
  launch_service "backend" "$SPRING_DIR" "${backend_cmd[@]}"
fi

if [[ $START_FLASK -eq 1 ]]; then
  flask_python="$FLASK_DIR/.venv/bin/python"
  if [[ -n "${FLASK_CMD:-}" ]]; then
    flask_cmd_string="$FLASK_CMD"
  elif [[ -x "$flask_python" ]]; then
    flask_cmd_string="$flask_python main.py"
  else
    flask_cmd_string="python main.py"
  fi
  IFS=' ' read -r -a flask_cmd <<< "$flask_cmd_string"
  launch_service "flask" "$FLASK_DIR" "${flask_cmd[@]}"
fi

if [[ ${#pids[@]} -eq 0 ]]; then
  log "No services were selected to start."
  exit 0
fi

echo
log "All requested services are running. Press Ctrl+C to stop them."
wait "${pids[@]}"
