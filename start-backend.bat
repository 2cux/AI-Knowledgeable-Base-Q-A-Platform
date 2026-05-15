@echo off
cd /d "%~dp0backend"
echo [AIKB] Starting backend with Maven...
echo [AIKB] Make sure Docker services are running: docker compose up -d
echo.
call mvn spring-boot:run
echo.
echo [AIKB] Backend stopped.
pause
