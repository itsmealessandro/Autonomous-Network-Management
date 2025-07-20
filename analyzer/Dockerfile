FROM python:3.10-slim

WORKDIR /app

# Installa dipendenze di sistema
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Installa dipendenze Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Crea una directory per la configurazione
RUN mkdir -p /app/config

# Copia il file delle soglie nella directory di configurazione
COPY config/thresholds.json /app/config/thresholds.json

# Copia il codice sorgente
COPY src/ ./src/

# Imposta variabili d'ambiente
# Rimosso INFLUX_TOKEN_FILE dal Dockerfile, poiché l'Analyzer non lo usa più.
ENV THRESHOLDS_FILE=/app/config/thresholds.json

# Imposta il comando di avvio per l'applicazione.
# È stato semplificato dato che non si usa più INFLUX_TOKEN_FILE.
CMD ["python", "src/analyzer.py"]