import os
import json
import paho.mqtt.client as mqtt

# Configurazione
MQTT_BROKER = os.getenv('MQTT_BROKER', 'mosquitto-container')
MQTT_PORT = int(os.getenv('MQTT_PORT', 1883))

# Mappatura azioni
ACTIONS = {
    'bandwidth_usage': {
        'CRITICAL': 'REDUCE_BANDWIDTH_USAGE',
        'WARNING': 'OPTIMIZE_BANDWIDTH'
    },
    'latency': {
        'CRITICAL': 'PRIORITIZE_TRAFFIC',
        'WARNING': 'OPTIMIZE_ROUTES'
    },
    'packet_loss': {
        'CRITICAL': 'ACTIVATE_REDUNDANCY',
        'WARNING': 'CHECK_CONNECTIONS'
    },
    'suspicious_activity': {
        'CRITICAL': 'BLOCK_SOURCE',
        'WARNING': 'INCREASE_MONITORING'
    }
}

# Client MQTT
mqtt_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)

def on_connect(client, userdata, flags, rc):
    print(f"✅ Planner connected to MQTT broker")
    client.subscribe("analysis/results")
    print("Subscribed to analysis/results")

def on_message(client, userdata, msg):
    if msg.topic == "analysis/results":
        try:
            results = json.loads(msg.payload.decode())
            print("📋 Received analysis results. Planning actions...")
            plan_actions(results)
        except Exception as e:
            print(f"⚠️ Error processing results: {str(e)}")

def plan_actions(analysis_results):
    planned_actions = []

    for metric, data in analysis_results.items():
        status = data['status']

        if status in ['WARNING', 'CRITICAL']:
            action = ACTIONS.get(metric, {}).get(status)

            if action:
                action_topic = f"Network/{metric}/commands"
                payload = json.dumps({
                    'action': action,
                    'metric': metric,
                    'value': data['current'],
                    'reason': status
                })

                mqtt_client.publish(action_topic, payload)
                planned_actions.append({
                    'metric': metric,
                    'action': action,
                    'topic': action_topic
                })

    if planned_actions:
        print(f"📝 Planned {len(planned_actions)} actions:")
        for action in planned_actions:
            print(f"  - {action['metric']}: {action['action']} (via {action['topic']})")
    else:
        print("✅ No actions needed. System is stable.")

def main():
    # Configura MQTT
    mqtt_client.on_connect = on_connect
    mqtt_client.on_message = on_message
    mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)

    print("🚦 Planner started. Waiting for analysis results...")
    mqtt_client.loop_forever()

if __name__ == "__main__":
    main()
