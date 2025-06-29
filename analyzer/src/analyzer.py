import os
import json
import time
import logging
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS
import paho.mqtt.client as mqtt

# Configurazione logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler()]
)
logger = logging.getLogger(__name__)

# ==================================================
# Lettura del token da file
TOKEN_FILE_PATH = os.getenv("INFLUX_TOKEN_FILE", "/run/secrets/influx_token")

try:
    with open(TOKEN_FILE_PATH, 'r') as f:
        INFLUX_TOKEN = f.read().strip()
        logger.info(f"Token InfluxDB letto da {TOKEN_FILE_PATH}")
except Exception as e:
    logger.error(f"Errore nella lettura del token da file: {e}")
    exit(1)

# Configurazione da variabili d'ambiente (con default)
required_env_vars = {
    'INFLUX_URL': os.getenv('INFLUX_URL', 'http://influxdb:8086'),
    'INFLUX_ORG': os.getenv('INFLUX_ORG', 'network-monitoring'),
    'INFLUX_BUCKET': os.getenv('INFLUX_BUCKET', 'network-metrics'),
    'MQTT_BROKER': os.getenv('MQTT_BROKER', 'mosquitto-container'),
    'MQTT_PORT': int(os.getenv('MQTT_PORT', 1883))
}

# Logging variabili chiave
logger.info("=" * 50)
logger.info("INFLUX_URL: %s", required_env_vars['INFLUX_URL'])
logger.info("INFLUX_ORG: %s", required_env_vars['INFLUX_ORG'])
logger.info("INFLUX_BUCKET: %s", required_env_vars['INFLUX_BUCKET'])
logger.info("MQTT_BROKER: %s", required_env_vars['MQTT_BROKER'])
logger.info("MQTT_PORT: %d", required_env_vars['MQTT_PORT'])
logger.info("Lunghezza token: %d", len(INFLUX_TOKEN))
logger.info("=" * 50)

# Connessione a InfluxDB
try:
    influx_client = InfluxDBClient(
        url=required_env_vars['INFLUX_URL'],
        token=INFLUX_TOKEN,
        org=required_env_vars['INFLUX_ORG'],
        timeout=30_000,
        enable_gzip=True
    )
    write_api = influx_client.write_api(write_options=SYNCHRONOUS)
    logger.info("Connesso a InfluxDB")
except Exception as e:
    logger.error(f"Errore connessione InfluxDB: {str(e)}")
    exit(1)

# Funzione di connessione MQTT
def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code == 0:
        logger.info("Connesso al broker MQTT")
        client.subscribe("sensors/#")
        client.subscribe("actuators/#")
    else:
        logger.error(f"Connessione fallita: {reason_code}")

# Parsing payload JSON o numerico
def parse_payload(payload):
    try:
        data = json.loads(payload)
        if isinstance(data, dict) and 'value' in data:
            return float(data['value'])
        if isinstance(data, dict):
            for v in data.values():
                if isinstance(v, (int, float)):
                    return float(v)
        return float(payload)
    except (json.JSONDecodeError, ValueError) as e:
        logger.warning(f"Payload non valido: {payload} - {str(e)}")
        return None

# Callback per ricezione messaggi MQTT
def on_message(client, userdata, msg):
    try:
        value = parse_payload(msg.payload.decode('utf-8'))
        if value is None:
            return

        metric = msg.topic.split('/')[-1]
        source = msg.topic.split('/')[0]

        point = Point("network_metrics") \
            .tag("metric_type", metric) \
            .tag("source", source) \
            .field("value", value) \
            .time(time.time_ns(), WritePrecision.NS)

        write_api.write(
            bucket=required_env_vars['INFLUX_BUCKET'],
            record=point
        )
        logger.info(f"Metrica salvata: {metric} = {value}")

    except Exception as e:
        logger.error(f"Errore processamento messaggio: {str(e)}", exc_info=True)

# Setup client MQTT
def setup_mqtt():
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_message = on_message
    client.reconnect_delay_set(min_delay=1, max_delay=120)

    try:
        client.connect(
            required_env_vars['MQTT_BROKER'],
            required_env_vars['MQTT_PORT'],
            keepalive=60
        )
        client.loop_forever(retry_first_connection=True)
    except KeyboardInterrupt:
        logger.info("Interruzione manuale")
    except Exception as e:
        logger.error(f"Errore MQTT: {str(e)}")
    finally:
        client.disconnect()
        influx_client.close()

# Main
if __name__ == "__main__":
    logger.info("Avvio analyzer...")
    setup_mqtt()
