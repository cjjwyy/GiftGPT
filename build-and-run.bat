@echo off
chcp 65001 >nul
title GiftGPT Build & Run
setlocal enabledelayedexpansion
cd /d "%~dp0backend"

echo ============================================
echo  GiftGPT - Build (JDK 17) + Run (JDK 11)
echo ============================================
echo.

REM ============================================================
REM Step 0: Find JDKs
REM ============================================================
set "JDK17=%USERPROFILE%\jdk-17\jdk-17.0.19+10"
if not exist "%JDK17%\bin\java.exe" (
    echo [ERROR] JDK 17 not found at %JDK17%
    echo Please install JDK 17 to: %USERPROFILE%\jdk-17\jdk-17.0.19+10
    pause
    exit /b 1
)

set "JDK11=%USERPROFILE%\jdk-11"
if not exist "%JDK11%\bin\java.exe" (
    set "JDK11=C:\Program Files\Eclipse Adoptium\jdk-11.0.31.11-hotspot"
)
if not exist "%JDK11%\bin\java.exe" (
    echo [ERROR] JDK 11 not found at %USERPROFILE%\jdk-11 or C:\Program Files\Eclipse Adoptium
    echo Please install JDK 11 and place it at one of the above locations.
    pause
    exit /b 1
)

echo [INFO] Build JDK: %JDK17%
echo [INFO] Run JDK:   %JDK11%
echo.

REM ============================================================
REM Step 1: Ensure pom.xml is configured for SB 2.7
REM ============================================================
echo [1/4] Configuring for Spring Boot 2.7...
set "JAVA_HOME=%JDK17%"
set "PATH=%JDK17%\bin;%PATH%"

powershell -NoProfile -Command ^
  "$pom = (Get-Content pom.xml -Raw);" ^
  "$pom = $pom -replace '<version>3\.4\.\d+</version>', '<version>2.7.18</version>';" ^
  "$pom = $pom -replace '<release>11</release>', '<source>11</source><target>11</target>';" ^
  "$pom = $pom -replace 'sa-token-spring-boot3-starter', 'sa-token-spring-boot-starter';" ^
  "$pom = $pom -replace 'knife4j-openapi3-jakarta-spring-boot-starter', 'knife4j-openapi3-spring-boot-starter';" ^
  "$pom = $pom -replace 'mybatis-plus-spring-boot3-starter', 'mybatis-plus-boot-starter';" ^
  "$pom = $pom -replace '<sa-token\.version>1\.39\.\d+</sa-token\.version>', '<sa-token.version>1.34.0</sa-token.version>';" ^
  "$pom = $pom -replace '<knife4j\.version>4\.5\.\d+</knife4j\.version>', '<knife4j.version>4.3.0</knife4j.version>';" ^
  "Set-Content pom.xml -Value $pom -NoNewline"

REM Add lombok version property if missing
findstr /c:"lombok.version" pom.xml >nul
if %ERRORLEVEL% neq 0 (
    powershell -NoProfile -Command ^
      "$pom = (Get-Content pom.xml -Raw);" ^
      "$pom = $pom -replace '(<sa-token\.version>1\.34\.0</sa-token\.version>)', ('$1' + \"`r`n        <lombok.version>1.18.30</lombok.version>\");" ^
      "Set-Content pom.xml -Value $pom -NoNewline"
)

REM Fix lombok annotation processor path
powershell -NoProfile -Command ^
  "$pom = (Get-Content pom.xml -Raw);" ^
  "$pom = $pom -replace '<path><groupId>org\.projectlombok</groupId><artifactId>lombok</artifactId></path>', '<path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>${lombok.version}</version></path>';" ^
  "Set-Content pom.xml -Value $pom -NoNewline"

REM Fix submodule pom files
for %%m in (giftgpt-common giftgpt-auth giftgpt-user giftgpt-recommendation giftgpt-goods giftgpt-order giftgpt-content giftgpt-enterprise giftgpt-server) do (
    if exist "%%m\pom.xml" (
        powershell -NoProfile -Command ^
          "$p = (Get-Content %%m\pom.xml -Raw);" ^
          "$p = $p -replace 'sa-token-spring-boot3-starter', 'sa-token-spring-boot-starter';" ^
          "$p = $p -replace 'knife4j-openapi3-jakarta-spring-boot-starter', 'knife4j-openapi3-spring-boot-starter';" ^
          "$p = $p -replace 'mybatis-plus-spring-boot3-starter', 'mybatis-plus-boot-starter';" ^
          "Set-Content %%m\pom.xml -Value $p -NoNewline"
    )
)

REM Fix Java files: jakarta -> javax
for /r %%f in (*.java) do (
    powershell -NoProfile -Command ^
      "$c = (Get-Content '%%f' -Raw -ErrorAction SilentlyContinue); if ($c) {" ^
      "  $c2 = $c -replace '^import jakarta\.', 'import javax.';" ^
      "  if ($c -ne $c2) { Set-Content '%%f' -Value $c2 -NoNewline }" ^
      "}"
)

echo [OK] Configuration done.
echo.

REM ============================================================
REM Step 2: Compile and package
REM ============================================================
echo [2/4] Compiling with JDK 17...
call mvn clean compile -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compile failed! Check Maven output above.
    pause
    exit /b 1
)
echo [OK] Compile done.

echo [3/4] Packaging...
call mvn package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Package failed!
    pause
    exit /b 1
)
echo [OK] Package done.
echo.

REM ============================================================
REM Step 3: Assemble runtime classpath
REM ============================================================
echo [4/4] Assembling runtime and starting server...

cd giftgpt-server

REM Collect dependencies
call mvn dependency:copy-dependencies -DoutputDirectory=target\full-deps -q 2>nul

REM Build classpath: all module target/classes + full-deps jars
set "CP=..\giftgpt-common\target\classes"
set "CP=%CP%;..\giftgpt-auth\target\classes"
set "CP=%CP%;..\giftgpt-user\target\classes"
set "CP=%CP%;..\giftgpt-recommendation\target\classes"
set "CP=%CP%;..\giftgpt-goods\target\classes"
set "CP=%CP%;..\giftgpt-order\target\classes"
set "CP=%CP%;..\giftgpt-content\target\classes"
set "CP=%CP%;..\giftgpt-enterprise\target\classes"
set "CP=%CP%;target\classes"

REM Add full-deps jars via wildcard
set "CP=%CP%;target\full-deps\*"

REM Remove any SB3 jars that got into full-deps (classpath wildcard picks up everything)
REM We'll do this by only adding safe jars. But for simplicity, wildcard is fine
REM as long as SB 2.7 jars take precedence in classpath order.

echo.
echo [INFO] Starting GiftGPT Server on http://localhost:8080
echo [INFO] Swagger: http://localhost:8080/swagger-ui.html
echo [INFO] Database: .\data\giftgpt.mv.db (persistent)
echo.
echo Use Ctrl+C to stop the server.
echo.

REM Ensure data directory exists for persistent storage
if not exist "data" mkdir data

"%JDK11%\bin\java" -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -cp "%CP%" com.giftgpt.server.GiftGptApplication
pause
endlocal
