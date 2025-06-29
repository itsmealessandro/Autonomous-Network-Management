FROM python:3.10-slim

WORKDIR /app

# Installa dipendenze di sistema
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Installa dipendenze Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copia il codice sorgente
COPY src/ ./src/

# Imposta variabili d'ambiente
ENV INFLUX_TOKEN_FILE=/run/secrets/influx_token

# Imposta entrypoint
CMD ["sh", "-c", "export INFLUX_TOKEN=$(cat ${INFLUX_TOKEN_FILE}) && python src/analyzer.py"]