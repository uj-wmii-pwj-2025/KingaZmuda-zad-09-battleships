@echo off
REM Start Battleships Client
REM Default host: localhost, port: 12345
REM Usage: start-client.bat [host] [port]

setlocal enabledelayedexpansion

cd /d "%~dp0\.."

if exist target\battleships-1.0.0.jar (
    set HOST=localhost
    set PORT=12345
    if not "%1"=="" set HOST=%1
    if not "%2"=="" set PORT=%2
    
    echo Starting Battleships Client connecting to !HOST!:!PORT!...
    java -cp target\battleships-1.0.0.jar kingazm.net.Client -host !HOST! -port !PORT!
) else (
    echo Error: battleships-1.0.0.jar not found.
    echo Please run: mvn clean package
    pause
)
