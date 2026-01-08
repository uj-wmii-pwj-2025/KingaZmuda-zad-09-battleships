@echo off
REM Start Battleships Server
REM Default port: 12345
REM Usage: start-server.bat [port]

setlocal enabledelayedexpansion

cd /d "%~dp0\.."

if exist target\battleships-1.0.0.jar (
    set PORT=12345
    if not "%1"=="" set PORT=%1
    
    echo Starting Battleships Server on port !PORT!...
    java -cp target\battleships-1.0.0.jar kingazm.net.Server -port !PORT!
) else (
    echo Error: battleships-1.0.0.jar not found.
    echo Please run: mvn clean package
    pause
)
