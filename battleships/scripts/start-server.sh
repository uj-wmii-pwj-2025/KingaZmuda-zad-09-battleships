#!/bin/bash
# Start Battleships Server
# Default port: 12345
# Usage: ./start-server.sh [port]

PORT=${1:-12345}

cd "$(dirname "$0")/.."

if [ -f "target/battleships-1.0.0.jar" ]; then
    echo "Starting Battleships Server on port $PORT..."
    java -cp target/battleships-1.0.0.jar kingazm.net.Server -port $PORT
else
    echo "Error: battleships-1.0.0.jar not found."
    echo "Please run: mvn clean package"
    exit 1
fi
