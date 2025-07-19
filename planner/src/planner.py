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
    """
    Callback when the planner connects to the MQTT broker.

    Args:
        client (Client): MQTT client instance
        userdata (Any): User data
        flags (dict): Connection flags
        rc (int): Connection result code
    """
    print(f"✅ Planner connected to MQTT broker")
    client.subscribe("analysis/results")
    print("Subscribed to analysis/results")

def on_message(client, userdata, msg):
    
    """
    Callback when the planner receives a message from the MQTT broker.

    Args:
        client (Client): MQTT client instance
        userdata (Any): User data
        msg (MqttMessage): Received MQTT message

    Notes:
        The planner is subscribed to the "analysis/results" topic, and this
        callback is invoked when a message is published to that topic. The
        message payload is expected to be a JSON string containing the
        analysis results. The planner will then plan actions based on the
        results.
    """
    if msg.topic == "analysis/results":
        try:
            results = json.loads(msg.payload.decode())
            print("📋 Received analysis results. Planning actions...")
            plan_actions(results)
        except Exception as e:
            print(f"⚠️ Error processing results: {str(e)}")

def plan_actions(analysis_results):
    """
    Plan actions based on the given analysis results.

    Args:
        analysis_results (dict): Analysis results with metric names as keys and
            dictionaries as values. Each dictionary should contain the
            following keys:
                - status (str): Status of the analysis (WARNING, CRITICAL, etc.)
                - current (float): Current value of the metric

    Returns:
        None

    Notes:
        This function will plan actions based on the given analysis results and
        publish them to the corresponding MQTT topics. The actions will be
        logged to the console.
    """
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
    """
    Main entry point of the planner. Configures the MQTT connection and starts
    the loop to wait for analysis results.

    Notes:
        The planner is subscribed to the "analysis/results" topic, and when a
        message is published to that topic, the on_message callback is invoked.
        The planner will then plan actions based on the analysis results and
        publish them to the corresponding MQTT topics.
    """
    mqtt_client.on_connect = on_connect
    mqtt_client.on_message = on_message
    mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)

    print("🚦 Planner started. Waiting for analysis results...")
    mqtt_client.loop_forever()

if __name__ == "__main__":
    main()
