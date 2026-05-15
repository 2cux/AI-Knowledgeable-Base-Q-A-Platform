#!/usr/bin/env bash
# macOS / Linux startup script for frontend
set -euo pipefail

cd "$(dirname "$0")/frontend"

if [ -f "pnpm-lock.yaml" ]; then
    echo "[AIKB] Detected pnpm lockfile — installing dependencies with pnpm..."
    pnpm install
    echo ""
    echo "[AIKB] Starting frontend dev server..."
    pnpm dev
elif [ -f "yarn.lock" ]; then
    echo "[AIKB] Detected yarn lockfile — installing dependencies with yarn..."
    yarn install
    echo ""
    echo "[AIKB] Starting frontend dev server..."
    yarn dev
else
    echo "[AIKB] Detected npm lockfile — installing dependencies with npm..."
    npm install
    echo ""
    echo "[AIKB] Starting frontend dev server..."
    npm run dev
fi

echo ""
echo "[AIKB] Frontend stopped."
