package com.actCreator.app;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;

import java.util.HashSet;
import java.util.Map;
import java.nio.file.Paths;
import java.io.File;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class Actuator implements BundleActivator, Runnable {

  // NOTE: useful for debug
  final String ANSI_RESET = "\u001B[0m";
  final String ANSI_BLACK = "\u001B[30m";
  final String ANSI_RED = "\u001B[31m"; // important operation
  final String ANSI_GREEN = "\u001B[32m"; // good operation result
  final String ANSI_YELLOW = "\u001B[33m";
  final String ANSI_BLUE = "\u001B[34m";
  final String ANSI_PURPLE = "\u001B[35m";
  final String ANSI_CYAN = "\u001B[36m";
  final String ANSI_WHITE = "\u001B[37m";

  final String debugInfo = ANSI_WHITE + "[INFO]: " + ANSI_RESET;

  // Use a HashSet to store valid command strings for quick lookup
  HashSet<String> validCommands = new HashSet<>();

  // Define commands using an enum
  private enum COMMANDS {
    BLOCK_IP // This command sets the suspicious_activity value to 0
  }

  // MQTT
  final String CLIENT_ID = "act_suspicious_activity";
  final String GENERAL_TOPIC = "Network/suspicious_activity";
  final String COMMANDS_TOPIC = GENERAL_TOPIC + "/commands";

  // JSON env file
  final String ENV_FILE_PATH = "/simulated_env/env.json";
  final String ENV_NODE = "suspicious_activity";
  final String NODE_VALUE = "value";
  final String BROKER = "tcp://broker:1883";

  // MAX_VAL and MIN_VAL are less relevant now, as the command directly sets to 0.
  // However, they can remain if they define the expected bounds of the value in
  // general.
  final double MAX_VAL = 5.0; // Changed to double for consistency
  final double MIN_VAL = 0.0; // Changed to double for consistency

  private final Thread thread = new Thread(this);

  @Override
  public String toString() {
    // Define max width for field names for alignment
    int fieldNameWidth = 15;

    return "ActuatorData {\n" +
        String.format("  %-" + fieldNameWidth + "s: '%s'\n", "CLIENT_ID", CLIENT_ID) +
        String.format("  %-" + fieldNameWidth + "s: '%s'\n", "BROKER", BROKER) +
        String.format("  %-" + fieldNameWidth + "s: '%s'\n", "GENERAL_TOPIC", GENERAL_TOPIC) +
        String.format("  %-" + fieldNameWidth + "s: '%s'\n", "COMMANDS_TOPIC", COMMANDS_TOPIC) +
        String.format("  %-" + fieldNameWidth + "s: '%s'\n", "ENV_FILE_PATH", ENV_FILE_PATH) +
        String.format("  %-" + fieldNameWidth + "s: '%s'\n", "ENV_NODE", ENV_NODE) +
        String.format("  %-" + fieldNameWidth + "s: '%s'\n", "NODE_VALUE", NODE_VALUE) +
        "}";
  }

  // Method to populate the validCommands HashSet from the enum
  private void setupCommands() {
    for (COMMANDS cmd : COMMANDS.values()) {
      this.validCommands.add(cmd.name()); // Add the string name of each enum constant
    }
  }

  @Override
  public void start(BundleContext ctx) {
    System.out.println(debugInfo + "Hi, I'm an Actuator for Suspicious Activity and I've just STARTED " + ANSI_RESET);
    System.out.println(debugInfo + "Setting up commands..." + ANSI_RESET);
    setupCommands(); // Call the setup method
    System.out.println(debugInfo + "Command list: " + this.validCommands + ANSI_RESET);

    thread.start(); // This will launch the run method
  }

  @Override
  public void stop(BundleContext ctx) {
    System.out.println(debugInfo + "Hi, I'm an Actuator for Suspicious Activity and I've just STOPPED ");
  }

  @Override
  public void run() {
    System.out.println(debugInfo + "I'm running, I'm the suspicious_activity actuator");
    System.out.println("------------------------------------------------------------");
    System.out.println(ANSI_WHITE + this + ANSI_RESET);
    System.out.println("------------------------------------------------------------");

    // Establish MQTT Connection
    try {
      int qos = 1;
      MemoryPersistence persistence = new MemoryPersistence();
      MqttClient client = new MqttClient(BROKER, CLIENT_ID, persistence);
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);
      System.out.println("------------------------------------------------------------");
      System.out.println(debugInfo + "Connecting to BROKER: " + BROKER);

      client.connect(connOpts);
      System.out.println(ANSI_GREEN + "Connected" + ANSI_RESET);
      System.out.println("------------------------------------------------------------");

      // Callback to receive messages
      client.setCallback(new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
          System.out.println(ANSI_RED + "Connection LOST: " + ANSI_RESET + cause.getMessage());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
          String msgText = new String(message.getPayload());
          System.out.println(ANSI_WHITE + "Message Received: " + msgText + ANSI_RESET);

          // Check if the received message is the BLOCK_IP command
          if (msgText.equals(COMMANDS.BLOCK_IP.name())) {
            System.out.println(ANSI_GREEN + "Command: [" + msgText + "] recognized" + ANSI_RESET);
            blockIpCommand(); // Call the specific command method
          } else {
            System.out.println(ANSI_RED + "Command: [" + msgText + "] not recognized. Expected: "
                + COMMANDS.BLOCK_IP.name() + ANSI_RESET);
          }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
          // Not implemented for this example
        }
      });

      client.subscribe(COMMANDS_TOPIC);
      System.out.println(debugInfo + "Subscribed to topic: " + COMMANDS_TOPIC + ANSI_RESET);

    } catch (MqttException e) {
      e.printStackTrace();
      System.err.println(ANSI_RED + "MQTT Problems occurred: " + e.getMessage() + ANSI_RESET);
    }
  }

  /**
   * Responds to the BLOCK_IP command by setting the 'suspicious_activity' value
   * to 0.0 in the JSON file.
   * This implies that blocking the IP resolves or nullifies the suspicious
   * activity.
   *
   * @return 0 if successful, 2 if JSON operation failed.
   */
  private int blockIpCommand() {
    System.out.println(debugInfo + "Executing BLOCK_IP command: Setting suspicious_activity to 0...");

    try {
      ObjectMapper mapper = new ObjectMapper();
      File file = Paths.get(ENV_FILE_PATH).toFile();

      Map<String, Map<String, Object>> map = mapper.readValue(file, new TypeReference<>() {
      });

      if (map.containsKey(ENV_NODE)) {
        Map<String, Object> innerMap = map.get(ENV_NODE);
        // Directly set the value to 0.0 as per the requirement
        double newValue = 0.0;
        innerMap.put(NODE_VALUE, newValue);
        System.out.println(ANSI_GREEN + "Suspicious activity value set to 0.0 (IP Blocked)." + ANSI_RESET);
      } else {
        System.err.println(ANSI_RED + "BLOCK_IP: ENV_NODE '" + ENV_NODE + "' not found in JSON." + ANSI_RESET);
        return 2;
      }

      mapper.writerWithDefaultPrettyPrinter().writeValue(file, map);
      System.out.println(debugInfo + "JSON file updated.");

    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(
          ANSI_RED + "JSON Error during BLOCK_IP operation for " + ENV_NODE + ": " + ex.getMessage() + ANSI_RESET);
      return 2;
    }
    return 0; // OK
  }
}
