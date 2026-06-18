@echo off
chcp 65001 >nul
title GiftGPT Launcher

cd /d "%~dp0"
set "JDK=%USERPROFILE%\jdk-17\jdk-17.0.19+10"

echo ========================================
echo   GiftGPT One-Click Launcher
echo ========================================
echo.

if not exist "%JDK%\bin\java.exe" (
    echo [ERROR] JDK 17 not found: %JDK%
    pause
    exit /b 1
)

:: Start backend in its own visible window (closing it stops backend)
echo [1/2] Starting backend...
start "GiftGPT Backend" "%~dp0start-server.bat"

:: Start frontend in its own visible window (closing it stops frontend)
echo [2/2] Starting frontend...
start "GiftGPT Frontend" "%~dp0start-frontend.bat"

:: Wait for services to be ready, then open browser
echo.
echo Waiting for services to be ready...
timeout /t 20 /nobreak >nul

echo Opening http://localhost:3000 ...
start "" http://localhost:3000

echo.
echo ========================================
echo   Backend:  http://localhost:8080/swagger-ui.html
echo   Frontend: http://localhost:3000
echo ========================================
echo.
echo Close each service window to stop it.
pause
