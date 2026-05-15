@echo off
cd /d "%~dp0frontend"

if exist pnpm-lock.yaml (
    echo [AIKB] Detected pnpm lockfile — installing dependencies with pnpm...
    call pnpm install
    echo.
    echo [AIKB] Starting frontend dev server...
    pnpm dev
) else if exist yarn.lock (
    echo [AIKB] Detected yarn lockfile — installing dependencies with yarn...
    call yarn install
    echo.
    echo [AIKB] Starting frontend dev server...
    yarn dev
) else (
    echo [AIKB] Detected npm lockfile — installing dependencies with npm...
    call npm install
    echo.
    echo [AIKB] Starting frontend dev server...
    npm run dev
)

echo.
echo [AIKB] Frontend stopped.
pause
