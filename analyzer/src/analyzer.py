import os
import json
import time
import paho.mqtt.client as mqtt
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS
import numpy as np

# Configurazione
MQTT_BROKER = os.getenv('MQTT_BROKER', 'mosquitto-container')
MQTT_PORT = int(os.getenv('MQTT_PORT', 1883))
INFLUX_URL = os.getenv('INFLUX_URL', 'http://influxdb:8086')
INFLUX_TOKEN = os.getenv('INFLUX_TOKEN', 'my-super-secret-token')
INFLUX_ORG = os.getenv('INFLUX_ORG', 'network-monitoring')
INFLUX_BUCKET = os.getenv('INFLUX_BUCKET', 'network-metrics')

# Soglie per l'analisi (personalizzabili)
THRESHOLDS = {
    'bandwidth_usage': {'warning': 70, 'critical': 90},
    'latency': {'warning': 100, 'critical': 200},
    'packet_loss': {'warning': 2, 'critical': 5},
    'suspicious_activity': {'warning': 5, 'critical': 10}
}

# Inizializza client InfluxDB
influx_client = InfluxDBClient(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG)
query_api = influx_client.query_api()

# Client MQTT
mqtt_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)

def on_connect(client, userdata, flags, rc):
    print(f"✅ Analyzer connected to MQTT broker")
    client.subscribe("analysis/commands")
    print("Subscribed to analysis/commands")

def on_message(client, userdata, msg):
    print(f"Received command: {msg.payload.decode()}")
    if msg.topic == "analysis/commands":
        handle_command(msg.payload.decode())

def handle_command(command):
    if command == "ANALYZE_NOW":
        print("🚀 Triggering immediate analysis")
        analyze_metrics()

def analyze_metrics():
    print("🔍 Starting analysis of network metrics...")
    results = {}

    for metric in THRESHOLDS.keys():
        # Query per ottenere gli ultimi 5 minuti di dati
        query = f'''from(bucket: "{INFLUX_BUCKET}")
            |> range(start: -5m)
            |> filter(fn: (r) => r._measurement == "{metric}")
            |> keep(columns: ["_value", "_time"])
            |> sort(columns: ["_time"], desc: false)'''

        tables = query_api.query(query, org=INFLUX_ORG)
        values = [row.get_value() for table in tables for row in table.records]

        if not values:
            continue

        current_value = values[-1]
        avg_value = np.mean(values) if len(values) > 0 else current_value
        max_value = np.max(values) if len(values) > 0 else current_value

        # Valutazione dello stato
        status = "NORMAL"
        if current_value > THRESHOLDS[metric]['critical']:
            status = "CRITICAL"
        elif current_value > THRESHOLDS[metric]['warning']:
            status = "WARNING"

        # Rilevamento anomalie
        anomaly = False
        if len(values) > 10:
            std_dev = np.std(values)
            if abs(current_value - avg_value) > 3 * std_dev:
                anomaly = True

        results[metric] = {
            'current': round(current_value, 2),
            'average': round(avg_value, 2),
            'max': round(max_value, 2),
            'status': status,
            'anomaly': anomaly,
            'timestamp': int(time.time())
        }

    # Pubblica risultati
    if results:
        payload = json.dumps(results)
        mqtt_client.publish("analysis/results", payload)
        print(f"📊 Published analysis results: {payload}")
    else:
        print("⚠️ No data available for analysis")

def main():
    # Configura MQTT
    mqtt_client.on_connect = on_connect
    mqtt_client.on_message = on_message
    mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)

    # Loop principale
    mqtt_client.loop_start()
    print("🚦 Analyzer started. Waiting for commands...")

    # Analisi periodica (ogni 60 secondi)
    while True:
        analyze_metrics()
        time.sleep(60)

if __name__ == "__main__":
    main()
