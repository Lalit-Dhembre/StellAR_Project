@echo off
title StellAR Remote Access
cd /d "%~dp0"

echo ==========================================
echo      StellAR Server Remote Access
echo ==========================================

:: 1. Check for Ngrok
if not exist ngrok.exe (
    echo [ERROR] ngrok.exe is missing!
    echo.
    echo 1. Download ngrok from: https://dashboard.ngrok.com/get-started/setup
    echo 2. Extract 'ngrok.exe' into this folder:
    echo    %CD%
    echo.
    echo Once valid, re-run this script.
    pause
    exit /b
)

:: 2. Menu
echo.
echo [1] Configure Ngrok Auth Token (Select this for first time setup)
echo [2] Start Server and Tunnel (If already configured)
echo.
set /p CHOICE="Select an option (1 or 2): "

if "%CHOICE%"=="1" (
    echo.
    echo You provided this token in chat:
    echo 397jsLeXBQiZD2bKVK74wZiSxRb_4Mi78P5u4HpEigKZP17DY
    echo.
    set /p TOKEN="Press Enter to use above token, or paste a new one: "
    if "%TOKEN%"=="" set TOKEN=397jsLeXBQiZD2bKVK74wZiSxRb_4Mi78P5u4HpEigKZP17DY
    
    ngrok config add-authtoken %TOKEN%
    echo.
    echo Token successfully configured!
    echo.
)

:START_TUNNEL
:: 3. Get Static Domain
set DOMAIN=chun-nonimpulsive-nondeficiently.ngrok-free.dev
echo.
echo Using Fixed Domain: %DOMAIN%
echo.

:: 4. Start Server
echo.
echo [INFO] Starting Python Server (port 5000)...
start "StellAR Backend" cmd /k "call venv\Scripts\activate.bat && python app.py"

:: 5. Start Tunnel
echo [INFO] Starting Ngrok Tunnel...
echo.
echo The Fixed URL will be: https://%DOMAIN%
echo.
ngrok http --domain=%DOMAIN% 5000

pause
