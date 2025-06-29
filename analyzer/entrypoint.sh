#!/bin/sh

# Verifica presenza token
if [ -f "/run/secrets/influx_token" ]; then
    export INFLUX_TOKEN=$(cat /run/secrets/influx_token)
    echo " Token letto correttamente (lunghezza: ${#INFLUX_TOKEN})"
    exec python src/analyzer.py
else
    echo " ERRORE: File /run/secrets/influx_token non trovato!"
    echo "Contenuto di /run/secrets:"
    ls -la /run/secrets
    exit 1
fi