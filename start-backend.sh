#!/usr/bin/env bash
# macOS / Linux startup script for backend
set -euo pipefail

cd "$(dirname "$0")/backend"

echo "[AIKB] Starting backend with Maven..."
echo "[AIKB] Make sure Docker services are running: docker compose up -d"
echo ""

mvn spring-boot:run

echo ""
echo "[AIKB] Backend stopped."
