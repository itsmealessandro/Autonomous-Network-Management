import os
import json
import time
import logging
import uuid
import sys # Importa sys per uscire
import socket # Importa socket per verificare la connettività

from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS
import paho.mqtt.client as mqtt

# --- Configurazione del Logging ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# --- Configurazione da variabili d'ambiente ---
MQTT_BROKER = os.getenv('MQTT_BROKER', 'mosquitto-container')
MQTT_PORT = int(os.getenv('MQTT_PORT', 1883))
INFLUX_URL = os.getenv('INFLUX_URL', 'http://influxdb:8086')
INFLUX_TOKEN = os.getenv('INFLUX_TOKEN', 'my-super-secret-token')
INFLUX_ORG = os.getenv('INFLUX_ORG', 'network-monitoring')
INFLUX_BUCKET = os.getenv('INFLUX_BUCKET', 'network-metrics')

# Genera un ID client MQTT univoco
MQTT_CLIENT_ID = f"monitor-{uuid.uuid4()}"

# --- Funzione per verificare la connettività di rete ---
def wait_for_service(host, port, timeout=30):
    """Attende che un servizio di rete sia disponibile."""
    start_time = time.time()
    while True:
        try:
            with socket.create_connection((host, port), timeout=1):
                logger.info(f"Servizio {host}:{port} è disponibile.")
                return True
        except (socket.timeout, ConnectionRefusedError, OSError) as e:
            logger.warning(f"In attesa del servizio {host}:{port}... ({e}). Riprovo tra 3 secondi.")
            time.sleep(3)
        if time.time() - start_time > timeout:
            logger.critical(f"Timeout raggiunto. Il servizio {host}:{port} non è disponibile dopo {timeout} secondi.")
            return False

# Inizializza client InfluxDB
try:
    influx_client = InfluxDBClient(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG)
    write_api = influx_client.write_api(write_options=SYNCHRONOUS)
    logger.info("Connesso a InfluxDB con successo.")
except Exception as e:
    logger.critical(f"Errore nella connessione a InfluxDB: {e}")
    sys.exit(1) # Esci se InfluxDB non è accessibile

def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code == 0:
        logger.info(f"Connesso al broker MQTT con successo. Client ID: {MQTT_CLIENT_ID}")
        client.subscribe("Network/#")
        logger.info("Sottoscritto al topic 'Network/#'")
    else:
        logger.error(f"Fallita la connessione al broker MQTT con codice: {reason_code}")
        # MQTT non riuscirà a connettersi se il broker non è disponibile,
        # ma loop_forever() tenterà la riconnessione automatica.

def on_message(client, userdata, msg):
    try:
        topic = msg.topic
        payload = msg.payload.decode('utf-8')
        
        value = None
        
        # --- Parsing del payload più esplicito ---
        try:
            data = json.loads(payload)
            if isinstance(data, dict) and 'value' in data:
                value = data['value']
            elif isinstance(data, (int, float)):
                value = data
        except json.JSONDecodeError:
            pass
        
        if value is None:
            try:
                value = float(payload)
            except ValueError:
                pass
        
        if value is None:
            logger.warning(f"Formato payload non valido per il topic '{topic}': '{payload}'")
            return
            
        # Estrai nome metrica dall'argomento MQTT
        parts = topic.split('/')
        if len(parts) < 2:
            logger.warning(f"Formato topic non valido: '{topic}'. Richiede almeno 2 parti (es. Network/bandwidth)")
            return
            
        metric_name = parts[1]
        
        # Crea punto dati per InfluxDB
        point = Point(metric_name) \
            .tag("source", "sensor") \
            .tag("topic", topic) \
            .field("value", float(value)) \
            .time(time.time_ns(), WritePrecision.NS)
        
        # Scrivi su InfluxDB con gestione errori
        try:
            write_api.write(bucket=INFLUX_BUCKET, record=point)
            logger.info(f"Dato salvato in InfluxDB: Topic='{topic}', Metrica='{metric_name}', Valore={value}")
        except Exception as influx_err:
            logger.error(f"Errore nella scrittura su InfluxDB per il topic '{topic}': {influx_err}", exc_info=True)
        
    except Exception as e:
        logger.error(f"Errore durante l'elaborazione del messaggio MQTT dal topic '{msg.topic}': {e}", exc_info=True)

# --- Main logic ---
# 1. Attendi che il broker MQTT sia disponibile
logger.info(f"Monitor: Attendo che il broker MQTT ({MQTT_BROKER}:{MQTT_PORT}) sia pronto...")
if not wait_for_service(MQTT_BROKER, MQTT_PORT):
    logger.critical("Monitor: Il broker MQTT non è disponibile. Esco.")
    sys.exit(1)

# 2. Configura e connetti il client MQTT
mqtt_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=MQTT_CLIENT_ID)
mqtt_client.on_connect = on_connect
mqtt_client.on_message = on_message

logger.info(f"Monitor: Tentativo di connessione a MQTT Broker: {MQTT_BROKER}:{MQTT_PORT}")
try:
    mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)
except Exception as e:
    logger.critical(f"Monitor: Impossibile connettersi al broker MQTT dopo aver atteso: {e}")
    sys.exit(1) # Esci se non riesce a connettersi

mqtt_client.loop_forever()