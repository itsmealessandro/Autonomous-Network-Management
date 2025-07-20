import os
import json
import logging
import uuid
import sys
import time
import socket
import paho.mqtt.client as mqtt
from datetime import datetime

# --- Configurazione del Logging ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# --- Configurazione da variabili d'ambiente ---
MQTT_BROKER = os.getenv('MQTT_BROKER', 'mosquitto-container')
MQTT_PORT = int(os.getenv('MQTT_PORT', 1883))

# Genera un ID client MQTT univoco
MQTT_CLIENT_ID = f"planner-{uuid.uuid4()}"

# --- DICHIARA mqtt_client COME VARIABILE GLOBALE QUI ---
mqtt_client = None # Inizializza a None, verrà assegnato in main()

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

# --- Caricamento delle Azioni da File o default ---
ACTIONS_FILE = os.getenv('ACTIONS_FILE', '/app/config/actions.json')
ACTIONS = {}

def load_actions():
    global ACTIONS
    if os.path.exists(ACTIONS_FILE):
        try:
            with open(ACTIONS_FILE, 'r') as f:
                ACTIONS = json.load(f)
            logger.info(f"Azioni caricate da {ACTIONS_FILE}: {ACTIONS}")
        except Exception as e:
            logger.error(f"Errore nel caricamento delle azioni da {ACTIONS_FILE}: {e}. Verranno usate le azioni di default/interne.", exc_info=True)
            # Azioni di fallback
            ACTIONS = {
                "bandwidth": {
                    "CRITICAL": "REDUCE_BANDWIDTH_USAGE",
                    "WARNING": "OPTIMIZE_BANDWIDTH"
                },
                "latency": {
                    "CRITICAL": "PRIORITIZE_TRAFFIC",
                    "WARNING": "OPTIMIZE_ROUTES"
                },
                "packet_loss": {
                    "CRITICAL": "ACTIVATE_REDUNDANCY",
                    "WARNING": "CHECK_CONNECTIONS"
                },
                "suspicious_activity": {
                    "CRITICAL": "BLOCK_SOURCE",
                    "WARNING": "INCREASE_MONITORING"
                },
                "traffic_flow": {
                    "CRITICAL": "REDUCE_TRAFFIC_SOURCES",
                    "WARNING": "ADJUST_QOS"
                }
            }
    else:
        logger.warning(f"File azioni non trovato: {ACTIONS_FILE}. Verranno usate le azioni di default/interne.")
        ACTIONS = {
            "bandwidth": {"CRITICAL": "REDUCE_BANDWIDTH_USAGE", "WARNING": "OPTIMIZE_BANDWIDTH"},
            "latency": {"CRITICAL": "PRIORITIZE_TRAFFIC", "WARNING": "OPTIMIZE_ROUTES"},
            "packet_loss": {"CRITICAL": "ACTIVATE_REDUNDANCY", "WARNING": "CHECK_CONNECTIONS"},
            "suspicious_activity": {"CRITICAL": "BLOCK_SOURCE", "WARNING": "INCREASE_MONITORING"},
            "traffic_flow": {"CRITICAL": "REDUCE_TRAFFIC_SOURCES", "WARNING": "ADJUST_QOS"}
        }

load_actions() # Carica le azioni all'avvio

def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code == 0:
        logger.info(f"Connesso al broker MQTT con successo. Client ID: {MQTT_CLIENT_ID}")
        client.subscribe("analysis/results")
        logger.info("Sottoscritto al topic 'analysis/results'")
    else:
        logger.error(f"Fallita la connessione al broker MQTT con codice: {reason_code}")

def on_message(client, userdata, msg):
    if msg.topic == "analysis/results":
        try:
            results = json.loads(msg.payload.decode())
            logger.info("Ricevuti risultati di analisi. Pianificazione azioni...")
            plan_actions(results)
        except json.JSONDecodeError as e:
            logger.error(f"Errore di parsing JSON nei risultati di analisi: {str(e)}. Payload: {msg.payload.decode()}", exc_info=True)
        except Exception as e:
            logger.error(f"Errore durante l'elaborazione dei risultati di analisi: {str(e)}", exc_info=True)

def plan_actions(analysis_result):
    """
    Pianifica azioni basate sui risultati di analisi ricevuti.
    """
    global mqtt_client # DICHIARA DI VOLER USARE LA VARIABILE GLOBALE QUI

    metric = analysis_result.get('metric')
    status = analysis_result.get('status')
    current_value = analysis_result.get('value')

    if not all([metric, status, current_value is not None]):
        logger.warning(f"Risultati di analisi incompleti o malformati: {analysis_result}")
        return

    planned_action = None
    action_details = ACTIONS.get(metric, {})

    if status == 'CRITICAL':
        planned_action = action_details.get('CRITICAL')
    elif status == 'WARNING':
        planned_action = action_details.get('WARNING')

    if planned_action:
        action_topic = f"Network/{metric}/commands"
        payload = json.dumps({
            'action': planned_action,
            'metric': metric,
            'value': current_value,
            'status': status,
            'timestamp': datetime.utcnow().isoformat()
        })

        try:
            # Assicurati che mqtt_client sia stato inizializzato
            if mqtt_client:
                mqtt_client.publish(action_topic, payload)
                logger.info(f"Azione pianificata e pubblicata: Metrica='{metric}', Azione='{planned_action}', Topic='{action_topic}'")
            else:
                logger.error("Errore: mqtt_client non è stato inizializzato. Impossibile pubblicare l'azione.")
        except Exception as mqtt_pub_err:
            logger.error(f"Errore durante la pubblicazione dell'azione per '{metric}': {mqtt_pub_err}", exc_info=True)
    else:
        logger.info(f"Nessuna azione necessaria o definita per metrica '{metric}' con stato '{status}'.")

# --- Main logic ---
def main():
    global mqtt_client # DICHIARA DI VOLER ASSEGNARE ALLA VARIABILE GLOBALE QUI

    # 1. Attendi che il broker MQTT sia disponibile
    logger.info(f"Planner: Attendo che il broker MQTT ({MQTT_BROKER}:{MQTT_PORT}) sia pronto...")
    if not wait_for_service(MQTT_BROKER, MQTT_PORT):
        logger.critical("Planner: Il broker MQTT non è disponibile. Esco.")
        sys.exit(1)

    # 2. Configura e connetti il client MQTT
    # ASSEGNAZIONE ALLA VARIABILE GLOBALE
    mqtt_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=MQTT_CLIENT_ID)
    mqtt_client.on_connect = on_connect
    mqtt_client.on_message = on_message

    logger.info(f"Planner: Tentativo di connessione a MQTT Broker: {MQTT_BROKER}:{MQTT_PORT}")
    try:
        mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)
    except Exception as e:
        logger.critical(f"Planner: Impossibile connettersi al broker MQTT dopo aver atteso: {e}")
        sys.exit(1)

    logger.info("Planner avviato. In attesa di risultati di analisi...")
    mqtt_client.loop_forever()

if __name__ == "__main__":
    main()