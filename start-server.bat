@echo off
chcp 65001 >nul
title GiftGPT Server
cd /d "%~dp0backend\giftgpt-server"

echo [INFO] Looking for JDK 11...
set "JDK=%USERPROFILE%\jdk-11"
if exist "%JDK%\bin\java.exe" goto :found
set "JDK=C:\Program Files\Eclipse Adoptium\jdk-11.0.31.11-hotspot"
if exist "%JDK%\bin\java.exe" goto :found
set "JDK=%USERPROFILE%\jdk-17\jdk-17.0.19+10"
if exist "%JDK%\bin\java.exe" (
    echo [WARN] JDK 11 not found. Using JDK 17, but it may fail on this Windows version.
    echo If the server crashes, install JDK 11 and set it to %%USERPROFILE%%\jdk-11
    goto :run
)
echo [ERROR] No compatible JDK found.
pause
exit /b 1

:found
echo [INFO] Using JDK: %JDK%
:run

REM Ensure data directory exists for persistent storage
if not exist "data" mkdir data

REM Build the jar if it is missing (avoids "Unable to access jarfile")
if not exist "target\giftgpt-server-1.0.0-SNAPSHOT.jar" (
    echo [INFO] Jar not found, building with Maven JDK 17...
    set "JAVA_HOME=%USERPROFILE%\jdk-17\jdk-17.0.19+10"
    cd /d "%~dp0backend"
    call mvn package -DskipTests -q
    if errorlevel 1 (
        echo [ERROR] Maven build failed. Run build-and-run.bat or mvn package manually.
        pause
        exit /b 1
    )
    cd /d "%~dp0backend\giftgpt-server"
)

echo [INFO] Starting GiftGPT Server on http://localhost:8080
echo [INFO] Swagger:  http://localhost:8080/swagger-ui.html
echo [INFO] Database: .\data\giftgpt.mv.db (persistent)
echo [INFO] Profile:  local (loads API keys from application-local.yml)
echo.
echo Use Ctrl+C to stop the server.
echo.

"%JDK%\bin\java" -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar target\giftgpt-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=local
pause
