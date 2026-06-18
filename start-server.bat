@echo off
chcp 65001 >nul
title GiftGPT Server
cd /d "%~dp0backend\giftgpt-server"

set "JDK=%USERPROFILE%\jdk-17\jdk-17.0.19+10"
if not exist "%JDK%\bin\java.exe" (
    echo [ERROR] JDK 17 not found at %JDK%
    pause
    exit /b 1
)

echo [INFO] JDK: %JDK%
echo [INFO] Starting GiftGPT Server on http://localhost:8080
echo [INFO] Swagger:  http://localhost:8080/swagger-ui.html
echo.
echo Use Ctrl+C to stop the server.
echo.

"%JDK%\bin\java" -Djava.net.preferIPv4Stack=true -jar target\giftgpt-server-1.0.0-SNAPSHOT.jar
pause
