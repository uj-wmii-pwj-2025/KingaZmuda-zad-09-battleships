#!/bin/bash
# Start Battleships Client
# Default host: localhost, port: 12345
# Usage: ./start-client.sh [host] [port]

HOST=${1:-localhost}
PORT=${2:-12345}

cd "$(dirname "$0")/.."

if [ -f "target/battleships-1.0.0.jar" ]; then
    echo "Starting Battleships Client connecting to $HOST:$PORT..."
    java -cp target/battleships-1.0.0.jar kingazm.net.Client -host $HOST -port $PORT
else
    echo "Error: battleships-1.0.0.jar not found."
    echo "Please run: mvn clean package"
    exit 1
fi
