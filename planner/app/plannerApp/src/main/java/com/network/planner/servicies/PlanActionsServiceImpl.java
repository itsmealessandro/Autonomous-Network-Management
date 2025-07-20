package com.network.planner.servicies;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.network.planner.model.PossibleSymptoms;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.network.planner.model.PossibleSymptoms;

@Service
public class PlanActionsServiceImpl implements PlanActionsService {

  @Override
  public String checkSymptoms(Map<String, String> analysisRecap) {
    StringBuilder result = new StringBuilder();
    final String broker = "tcp://broker:1883";
    final String clientId = "planner-mqtt-client";
    final int qos = 1;

    try {
      MqttClient mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);
      mqttClient.connect(connOpts);

      for (Map.Entry<String, String> entry : analysisRecap.entrySet()) {
        String sensorName = entry.getKey();
        String value = entry.getValue();

        try {
          PossibleSymptoms symptom = PossibleSymptoms.valueOf(value.toUpperCase());
          String actuatorName = sensorName + "Act";
          String topic = "Network/" + sensorName + "/commands";
          String command = null;

          switch (symptom) {
            case HIGH:
              command = "WORSENING";
              result.append(sensorName).append(": HIGH → Invia comando WORSENING a ").append(actuatorName).append("\n");
              break;
            case LOW:
              command = "IMPROVEMENT";
              result.append(sensorName).append(": LOW → Invia comando IMPROVEMENT a ").append(actuatorName)
                  .append("\n");
              break;
            case FALSE:
              result.append(sensorName).append(": FALSE → Falso allarme\n");
              break;
            case FINE:
              result.append(sensorName).append(": FINE → Tutto regolare\n");
              break;
          }

          if (command != null) {
            MqttMessage message = new MqttMessage(command.getBytes());
            message.setQos(qos);
            mqttClient.publish(topic, message);
            System.out.println("Inviato comando [" + command + "] al topic: " + topic);
          }

        } catch (IllegalArgumentException e) {
          result.append(sensorName).append(": valore non riconosciuto: ").append(value).append("\n");
          System.err.println("Errore: valore non riconosciuto per il sensore " + sensorName + ": " + value);
        }
      }

      mqttClient.disconnect();

    } catch (MqttException e) {
      e.printStackTrace();
      return "Errore nella connessione MQTT: " + e.getMessage();
    }

    return result.toString();
  }

}
