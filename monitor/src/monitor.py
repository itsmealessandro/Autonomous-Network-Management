import os
import json
import time
import logging
import uuid
import sys
import socket
from collections import deque # Importa deque per una coda thread-safe
import threading # Importa threading per la scrittura in background

from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import ASYNCHRONOUS # Cambia a ASYNCHRONOUS
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

# Coda per i punti dati InfluxDB
influx_write_queue = deque()
# Evento per segnalare la disponibilità di dati nella coda
queue_event = threading.Event()
# Flag per indicare l'arresto del thread di scrittura
stop_writing_thread = False

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

# Inizializza client InfluxDB (globale per il thread di scrittura)
influx_client = None
write_api = None

def init_influxdb():
    global influx_client, write_api
    logger.info(f"Monitor: Attendo che InfluxDB ({INFLUX_URL}) sia pronto...")
    influx_host = INFLUX_URL.split('//')[1].split(':')[0]
    influx_port = int(INFLUX_URL.split(':')[-1])
    if not wait_for_service(influx_host, influx_port):
        logger.critical("Monitor: InfluxDB non è disponibile. Esco.")
        sys.exit(1)

    try:
        influx_client = InfluxDBClient(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG)
        # Usa ASYNCHRONOUS per la scrittura
        write_api = influx_client.write_api(write_options=ASYNCHRONOUS)
        logger.info("Connesso a InfluxDB con successo (modalità ASINCRONA).")
    except Exception as e:
        logger.critical(f"Errore nella connessione a InfluxDB: {e}")
        sys.exit(1)

def influx_writer_thread():
    """Thread che legge dalla coda e scrive su InfluxDB."""
    while not stop_writing_thread:
        # Attendi che ci siano dati nella coda o che il thread debba fermarsi
        queue_event.wait(timeout=1) # Attendi al massimo 1 secondo
        if stop_writing_thread:
            break

        while influx_write_queue:
            try:
                point = influx_write_queue.popleft() # Prendi il punto più vecchio
                if write_api:
                    write_api.write(bucket=INFLUX_BUCKET, record=point)
                    # logger.debug(f"Punto scritto in InfluxDB in background: {point}") # Debug per non intasare i log
                else:
                    logger.warning("write_api InfluxDB non inizializzato nel thread di scrittura.")
            except IndexError: # Coda vuota
                break
            except Exception as e:
                logger.error(f"Errore nel thread di scrittura InfluxDB: {e}", exc_info=True)
        queue_event.clear() # Resetta l'evento dopo aver svuotato la coda

    # Scrivi eventuali dati rimanenti prima di terminare
    while influx_write_queue:
        try:
            point = influx_write_queue.popleft()
            if write_api:
                write_api.write(bucket=INFLUX_BUCKET, record=point)
            logger.info(f"Scritto punto rimanente durante l'arresto: {point}")
        except Exception as e:
            logger.error(f"Errore nella scrittura del punto rimanente: {e}", exc_info=True)
    logger.info("Thread di scrittura InfluxDB terminato.")


def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code == 0:
        logger.info(f"Connesso al broker MQTT con successo. Client ID: {MQTT_CLIENT_ID}")
        client.subscribe("Network/#")
        logger.info("Sottoscritto al topic 'Network/#'")
    else:
        logger.error(f"Fallita la connessione al broker MQTT con codice: {reason_code}")
        # MQTT loop_forever() tenterà la riconnessione automatica.

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
        
        # Aggiungi il punto alla coda invece di scriverlo direttamente
        influx_write_queue.append(point)
        queue_event.set() # Segnala che ci sono nuovi dati nella coda
        logger.info(f"Dato aggiunto alla coda InfluxDB: Topic='{topic}', Metrica='{metric_name}', Valore={value}")
        
    except Exception as e:
        logger.error(f"Errore durante l'elaborazione del messaggio MQTT dal topic '{msg.topic}': {e}", exc_info=True)

# --- Main logic ---
def main():
    global stop_writing_thread

    # 1. Inizializza InfluxDB (attende che il servizio sia pronto)
    init_influxdb()

    # 2. Avvia il thread di scrittura InfluxDB
    writer_thread = threading.Thread(target=influx_writer_thread, daemon=True)
    writer_thread.start()
    logger.info("Thread di scrittura InfluxDB avviato.")

    # 3. Attendi che il broker MQTT sia disponibile
    logger.info(f"Monitor: Attendo che il broker MQTT ({MQTT_BROKER}:{MQTT_PORT}) sia pronto...")
    if not wait_for_service(MQTT_BROKER, MQTT_PORT):
        logger.critical("Monitor: Il broker MQTT non è disponibile. Esco.")
        stop_writing_thread = True # Segnala al thread di scrittura di fermarsi
        queue_event.set() # Sblocca il thread se è in attesa
        writer_thread.join(timeout=5) # Attendi che il thread termini
        sys.exit(1)

    # 4. Configura e connetti il client MQTT
    mqtt_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=MQTT_CLIENT_ID)
    mqtt_client.on_connect = on_connect
    mqtt_client.on_message = on_message

    logger.info(f"Monitor: Tentativo di connessione a MQTT Broker: {MQTT_BROKER}:{MQTT_PORT}")
    try:
        mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)
    except Exception as e:
        logger.critical(f"Monitor: Impossibile connettersi al broker MQTT dopo aver atteso: {e}")
        stop_writing_thread = True # Segnala al thread di scrittura di fermarsi
        queue_event.set() # Sblocca il thread
        writer_thread.join(timeout=5)
        sys.exit(1)

    try:
        mqtt_client.loop_forever()
    except KeyboardInterrupt:
        logger.info("Monitor interrotto dall'utente (Ctrl+C).")
    finally:
        logger.info("Arresto del Monitor. Segnalo al thread di scrittura di terminare...")
        stop_writing_thread = True # Segnala al thread di scrittura di fermarsi
        queue_event.set() # Sblocca il thread se è in attesa
        writer_thread.join(timeout=10) # Attendi che il thread termini (con timeout)
        logger.info("Monitor terminato.")

if __name__ == "__main__":
    main()