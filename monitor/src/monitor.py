import os
import json
import time
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS
import paho.mqtt.client as mqtt

# Configurazione da variabili d'ambiente
MQTT_BROKER = os.getenv('MQTT_BROKER', 'mosquitto-container')
MQTT_PORT = int(os.getenv('MQTT_PORT', 1883))
INFLUX_URL = os.getenv('INFLUX_URL', 'http://influxdb:8086')
INFLUX_TOKEN = os.getenv('INFLUX_TOKEN', 'my-super-secret-token')
INFLUX_ORG = os.getenv('INFLUX_ORG', 'network-monitoring')
INFLUX_BUCKET = os.getenv('INFLUX_BUCKET', 'network-metrics')

# Inizializza client InfluxDB
influx_client = InfluxDBClient(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG)
write_api = influx_client.write_api(write_options=SYNCHRONOUS)

def on_connect(client, userdata, flags, reason_code, properties):
    print(f"Connected to MQTT broker with code {reason_code}")
    client.subscribe("Network/#")

def on_message(client, userdata, msg):
    try:
        topic = msg.topic
        payload = msg.payload.decode('utf-8')
        
        # Gestione payload - supporta 3 formati:
        # 1. JSON con campo 'value': {"value": 42.5}
        # 2. Valore numerico diretto: "42.5"
        # 3. JSON con altri campi (cerca 'value' nei campi principali)
        value = None
        
        try:
            # Prova a interpretare come JSON
            data = json.loads(payload)
            
            if isinstance(data, dict):
                # Cerca un campo 'value' nel JSON
                if 'value' in data:
                    value = data['value']
                else:
                    # Cerca qualsiasi campo numerico
                    for k, v in data.items():
                        if isinstance(v, (int, float)):
                            value = v
                            break
            elif isinstance(data, (int, float)):
                value = data
        except json.JSONDecodeError:
            # Non è JSON, prova a convertire direttamente a float
            try:
                value = float(payload)
            except ValueError:
                pass
        
        if value is None:
            print(f"Invalid payload format: {payload}")
            return
            
        # Estrai nome metrica dall'argomento MQTT
        parts = topic.split('/')
        if len(parts) < 2:
            print(f"Invalid topic format: {topic}")
            return
            
        metric_name = parts[1]
        
        # Crea punto dati per InfluxDB
        point = Point(metric_name) \
            .tag("source", "sensor") \
            .field("value", float(value)) \
            .time(time.time_ns(), WritePrecision.NS)
        
        # Scrivi su InfluxDB
        write_api.write(bucket=INFLUX_BUCKET, record=point)
        print(f"Saved metric: {metric_name}={value}")
        
    except Exception as e:
        print(f"Error processing message: {str(e)}")

# Configura client MQTT
mqtt_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
mqtt_client.on_connect = on_connect
mqtt_client.on_message = on_message

print(f"Connecting to MQTT: {MQTT_BROKER}:{MQTT_PORT}")
mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)
mqtt_client.loop_forever()