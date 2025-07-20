FROM python:3.10-slim

WORKDIR /app

# Copia i requisiti e installa le dipendenze Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Crea una directory per la configurazione all'interno del container
RUN mkdir -p /app/config

# Copia il file delle azioni nella directory di configurazione creata
COPY config/actions.json /app/config/actions.json

# Copia il codice sorgente dell'applicazione
COPY src/ /app/

# Imposta le variabili d'ambiente necessarie per il Planner
# Queste possono essere sovrascritte da docker-compose.yaml
ENV MQTT_BROKER=mosquitto-container \
    MQTT_PORT=1883 \
    ACTIONS_FILE=/app/config/actions.json

# Definisce il comando di default per eseguire l'applicazione
CMD ["python", "planner.py"]