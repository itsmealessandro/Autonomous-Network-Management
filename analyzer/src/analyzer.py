import os
import json
import time
import logging
import uuid
# from influxdb_client import InfluxDBClient, Point, WritePrecision # Rimosso se il Monitor scrive i dati raw
# from influxdb_client.client.write_api import SYNCHRONOUS # Rimosso
import paho.mqtt.client as mqtt
from datetime import datetime

# --- Configurazione del Logging ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# --- Configurazione da variabili d'ambiente ---
MQTT_BROKER = os.getenv('MQTT_BROKER', 'mosquitto-container')
MQTT_PORT = int(os.getenv('MQTT_PORT', 1883))
# INFLUX_URL = os.getenv('INFLUX_URL', 'http://influxdb:8086') # Rimosso
# INFLUX_TOKEN = os.getenv('INFLUX_TOKEN', 'my-super-secret-token') # Rimosso
# INFLUX_ORG = os.getenv('INFLUX_ORG', 'network-monitoring') # Rimosso
# INFLUX_BUCKET = os.getenv('INFLUX_BUCKET', 'network-metrics') # Rimosso

# Genera un ID client MQTT univoco
MQTT_CLIENT_ID = f"analyzer-{uuid.uuid4()}"

# --- Caricamento delle Soglie da File o default ---
THRESHOLDS_FILE = os.getenv('THRESHOLDS_FILE', '/app/config/thresholds.json')
THRESHOLDS = {}

def load_thresholds():
    global THRESHOLDS
    if os.path.exists(THRESHOLDS_FILE):
        try:
            with open(THRESHOLDS_FILE, 'r') as f:
                THRESHOLDS = json.load(f)
            logger.info(f"Soglie caricate da {THRESHOLDS_FILE}: {THRESHOLDS}")
        except Exception as e:
            logger.error(f"Errore nel caricamento delle soglie da {THRESHOLDS_FILE}: {e}. Verranno usate le soglie di default/interne.", exc_info=True)
            # Soglie di fallback se il file non è disponibile o è malformato
            THRESHOLDS = {
                "bandwidth": {"WARNING": 70, "CRITICAL": 90},
                "latency": {"WARNING": 100, "CRITICAL": 200},
                "packet_loss": {"WARNING": 5, "CRITICAL": 10},
                "suspicious_activity": {"WARNING": 10, "CRITICAL": 20},
                "traffic_flow": {"WARNING": 800, "CRITICAL": 1200} # Esempio aggiuntivo
            }
    else:
        logger.warning(f"File soglie non trovato: {THRESHOLDS_FILE}. Verranno usate le soglie di default/interne.")
        THRESHOLDS = {
            "bandwidth": {"WARNING": 70, "CRITICAL": 90},
            "latency": {"WARNING": 100, "CRITICAL": 200},
            "packet_loss": {"WARNING": 5, "CRITICAL": 10},
            "suspicious_activity": {"WARNING": 10, "CRITICAL": 20},
            "traffic_flow": {"WARNING": 800, "CRITICAL": 1200}
        }

load_thresholds() # Carica le soglie all'avvio

def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code == 0:
        logger.info(f"Connesso al broker MQTT con successo. Client ID: {MQTT_CLIENT_ID}")
        client.subscribe("Network/#") # Sottoscrivi ai dati raw dei sensori
        logger.info("Sottoscritto ai topic 'Network/#' per l'analisi.")
    else:
        logger.error(f"Fallita la connessione al broker MQTT con codice: {reason_code}")

def analyze_metric(metric, value):
    """
    Analizza un valore per un certo metric e restituisce un dizionario con informazioni sull'analisi.
    """
    analysis = {"metric": metric, "value": value, "timestamp": datetime.utcnow().isoformat()}
    
    try:
        threshold = THRESHOLDS.get(metric)
        if not threshold:
            analysis["status"] = "UNKNOWN"
            analysis["recommendation"] = f"No thresholds defined for {metric}"
            logger.warning(f"Nessuna soglia definita per la metrica: {metric}")
            return analysis
        
        # Validazione dei valori di soglia
        if not all(k in threshold and isinstance(threshold[k], (int, float)) for k in ["WARNING", "CRITICAL"]):
            analysis["status"] = "ERROR"
            analysis["recommendation"] = f"Invalid threshold definition for {metric}"
            logger.error(f"Definizione soglia non valida per {metric}: {threshold}")
            return analysis

        if value >= threshold["CRITICAL"]:
            analysis["status"] = "CRITICAL"
            analysis["recommendation"] = f"Azione immediata richiesta per '{metric}'. Valore critico rilevato: {value} (Soglia CRITICAL: {threshold['CRITICAL']})"
        elif value >= threshold["WARNING"]:
            analysis["status"] = "WARNING"
            analysis["recommendation"] = f"Monitorare attentamente '{metric}'. Valore di avviso rilevato: {value} (Soglia WARNING: {threshold['WARNING']})"
        else:
            analysis["status"] = "NORMAL"
            analysis["recommendation"] = f"La metrica '{metric}' è nel range normale."
            
        # Logica aggiuntiva basata su correlazioni (esempio, espandibile)
        if metric == "latency" and value > 150:
            analysis["related_metrics"] = ["bandwidth", "packet_loss"]
            analysis["recommendation"] += " Considerare l'impatto su banda e packet loss."
        elif metric == "packet_loss" and value > 7:
            analysis["related_metrics"] = ["latency"]
            analysis["recommendation"] += " Probabile impatto sulla latenza."
            
    except Exception as e:
        logger.error(f"Errore durante l'analisi della metrica '{metric}' con valore '{value}': {str(e)}", exc_info=True)
        analysis["status"] = "ERROR"
        analysis["recommendation"] = f"Errore interno di analisi: {str(e)}"
    
    return analysis

def on_message(client, userdata, msg):
    try:
        topic = msg.topic
        payload = msg.payload.decode('utf-8')
        
        # Estrai il nome della metrica dal topic (es. Network/bandwidth -> bandwidth)
        topic_parts = topic.split('/')
        if len(topic_parts) < 2:
            logger.warning(f"Topic non valido: '{topic}'. Non è possibile estrarre la metrica.")
            return

        source = topic_parts[0] # Es. "Network"
        metric = topic_parts[1] # Es. "bandwidth" o "latency"
        
        value = None
        try:
            data = json.loads(payload)
            if isinstance(data, dict) and 'value' in data:
                value = data['value']
            elif isinstance(data, (int, float)): # Se il JSON è solo un numero
                value = data
        except json.JSONDecodeError:
            pass # Non è JSON, prosegui
        
        if value is None:
            try:
                value = float(payload)
            except ValueError:
                pass

        if not isinstance(value, (int, float)):
            logger.warning(f"Valore non valido o non numerico per la metrica '{metric}' dal payload: '{payload}'")
            return
        
        # Esegui analisi
        analysis_result = analyze_metric(metric, value)
        
        # Pubblica risultati analisi su un topic specifico
        # Es: 'analysis/results' o 'analysis/network/bandwidth'
        analysis_topic = f"analysis/results" # O f"analysis/{source}/{metric}" se vuoi più granularità
        
        client.publish(analysis_topic, json.dumps(analysis_result))
        logger.info(f"Analisi pubblicata per '{metric}': Valore={value}, Stato='{analysis_result['status']}'")
        
    except Exception as e:
        logger.error(f"Errore durante l'elaborazione del messaggio MQTT in Analyzer: {str(e)}", exc_info=True)

# Configura client MQTT
mqtt_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=MQTT_CLIENT_ID)
mqtt_client.on_connect = on_connect
mqtt_client.on_message = on_message

logger.info(f"Tentativo di connessione a MQTT Broker: {MQTT_BROKER}:{MQTT_PORT}")
try:
    mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)
except Exception as e:
    logger.critical(f"Impossibile connettersi al broker MQTT: {e}")
    exit(1)

mqtt_client.loop_forever()