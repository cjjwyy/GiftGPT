@echo off
chcp 65001 >nul
title GiftGPT Frontend

cd /d "%~dp0frontend\giftgpt-web"

echo [INFO] Starting GiftGPT Frontend on http://localhost:3000
echo.

call npm run dev
pause
