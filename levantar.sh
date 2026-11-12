#!/usr/bin/env bash
#
# Levanta el backend (Spring Boot, puerto 8080) y el frontend (Vite, puerto 5173) con un solo comando.
#
# Uso:   ./levantar.sh
# Parar: Ctrl+C (detiene los dos).
#
# El frontend se busca en la carpeta hermana ../ProyectoFinalED-frontend. Si está en otro lugar:
#   FRONTEND_DIR=/ruta/al/frontend ./levantar.sh
#
# El backend escribe su salida en target/backend.log; el frontend escribe en esta terminal.

set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="${FRONTEND_DIR:-$BACKEND_DIR/../ProyectoFinalED-frontend}"
LOG_BACKEND="$BACKEND_DIR/target/backend.log"
PUERTO_BACKEND=8080
PUERTO_FRONTEND=5173
ESPERA_MAXIMA_S=90

# --- Comprobaciones antes de arrancar ------------------------------------------------------------

if [[ ! -f "$FRONTEND_DIR/package.json" ]]; then
  echo "No encuentro el frontend en: $FRONTEND_DIR"
  echo "Clónalo junto a este repositorio o indica su ruta con FRONTEND_DIR=/ruta ./levantar.sh"
  exit 1
fi

for comando in java npm curl; do
  if ! command -v "$comando" > /dev/null; then
    echo "Falta '$comando' en el sistema."
    exit 1
  fi
done

# Un puerto ocupado es la causa más común de que el backend termine con "exit code: 1".
for puerto in $PUERTO_BACKEND $PUERTO_FRONTEND; do
  if (exec 3<> "/dev/tcp/127.0.0.1/$puerto") 2> /dev/null; then
    echo "El puerto $puerto ya está ocupado: quizá hay otro backend o frontend corriendo."
    echo "Ciérralo y vuelve a intentar (para ver quién lo usa: ss -ltnp | grep :$puerto)."
    exit 1
  fi
done

if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
  echo "Instalando las dependencias del frontend (solo la primera vez)..."
  (cd "$FRONTEND_DIR" && npm install)
fi

# --- Backend ---------------------------------------------------------------------------------------

mkdir -p "$BACKEND_DIR/target"
echo "Levantando el backend (salida en target/backend.log)..."
# setsid lo pone en su propio grupo de procesos: así, al parar, se detienen Maven y la JVM que lanza.
setsid "$BACKEND_DIR/mvnw" -q -f "$BACKEND_DIR/pom.xml" spring-boot:run > "$LOG_BACKEND" 2>&1 &
PID_BACKEND=$!

# Al salir (Ctrl+C, error o fin del frontend) se detiene el backend.
detener() {
  echo
  echo "Deteniendo el backend..."
  kill -- "-$PID_BACKEND" 2> /dev/null || true
  wait "$PID_BACKEND" 2> /dev/null || true
}
trap detener EXIT
trap 'exit 130' INT TERM

# Espera a que la API responda antes de abrir el frontend.
for ((i = 0; i < ESPERA_MAXIMA_S; i++)); do
  if curl -s -o /dev/null "http://localhost:$PUERTO_BACKEND/api/simulacion"; then
    break
  fi
  if ! kill -0 "$PID_BACKEND" 2> /dev/null; then
    echo "El backend terminó con error. Últimas líneas de target/backend.log:"
    tail -n 20 "$LOG_BACKEND"
    exit 1
  fi
  sleep 1
done

if ! curl -s -o /dev/null "http://localhost:$PUERTO_BACKEND/api/simulacion"; then
  echo "El backend no respondió en $ESPERA_MAXIMA_S segundos. Revisa target/backend.log"
  exit 1
fi
echo "Backend listo en http://localhost:$PUERTO_BACKEND"

# --- Frontend --------------------------------------------------------------------------------------

echo "Levantando el frontend en http://localhost:$PUERTO_FRONTEND (Ctrl+C para detener los dos)"
cd "$FRONTEND_DIR"
npm run dev -- --port "$PUERTO_FRONTEND" --strictPort
