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

  // NOTE: usefull 4 debug
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

  HashSet<String> commands = new HashSet<>(); // Use diamond operator for generics

  private enum COMMANDS {
    OPTIMIZE,
    DEGRADE
  }

  // MQTT
  final String CLIENT_ID = "act_bandwith";
  final String GENERAL_TOPIC = "Network/bandwidth_usage";
  final String COMMANDS_TOPIC = GENERAL_TOPIC + "/commands";

  // JSON env file
  final String ENV_FILE_PATH = "/simulated_env/env.json";
  final String ENV_NODE = "bandwidth_usage";
  final String NODE_VALUE = "value";
  final String BROKER = "tcp://broker:1883";
  final double MAX_VAL = 100.0;
  final double MIN_VAL = 0.0;

  private final Thread thread = new Thread(this);

  @Override
  public String toString() {
    // Definisci la larghezza massima per i nomi dei campi per l'allineamento
    int fieldNameWidth = 15; // Adatta questo valore se i tuoi nomi di campo sono più lunghi

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

  private void setupCommands() {
    // Add the new enum commands to the HashSet
    for (COMMANDS cmd : COMMANDS.values()) {
      this.commands.add(cmd.name()); // Use .name() to get the String representation of the enum
    }
  }

  @Override
  public void start(BundleContext ctx) {
    System.out.println(debugInfo + "Hi I'm am Actuator and I've just STARED " + ANSI_RESET);
    System.out.println(debugInfo + "Setup commands" + ANSI_RESET);
    setupCommands();
    System.out.println(debugInfo + "command list: " + this.commands + ANSI_RESET);

    thread.start(); // this will launch run method

  }

  @Override
  public void stop(BundleContext ctx) {
    System.out.println(debugInfo + "Hi I'm am Actuator and I've just STOPPPED ");

  }

  @Override
  public void run() {
    System.out.println(debugInfo + "I'm running, I'm bandwith actuator");
    System.out.println("------------------------------------------------------------");
    System.out.println(ANSI_WHITE + this + ANSI_RESET);
    System.out.println("------------------------------------------------------------");

    // NOTE: enstablishing MQTT Connection
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

      // Callback per ricevere i messaggi
      client.setCallback(new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
          System.out.println(ANSI_RED + "Connection LOST" + ANSI_RESET + cause.getMessage());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
          String msgText = new String(message.getPayload());
          System.out.println(ANSI_WHITE + "Message Received:" + msgText + ANSI_RESET);

          if (!commands.contains(msgText)) { // Changed to use ! for clarity
            System.out.println(ANSI_RED + "command: [" + msgText + "] not recognized" + ANSI_RESET);
            return;
          }

          System.out.println(ANSI_GREEN + "command: [" + msgText + "] recognized" + ANSI_RESET);

          // Use the enum directly for comparison
          if (msgText.equals(COMMANDS.OPTIMIZE.name())) {
            optimizeCommand();
          } else if (msgText.equals(COMMANDS.DEGRADE.name())) {
            degradeCommand();
          } else {
            System.out.println(ANSI_RED + msgText + " NOT in command list (unexpected)" + ANSI_RESET);
          }

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }

      });

      client.subscribe(COMMANDS_TOPIC);
      System.out.println(debugInfo + "subscribed to topic: " + COMMANDS_TOPIC + ANSI_RESET);

    } catch (MqttException e) {
      e.printStackTrace();
      System.err.println(ANSI_RED + "MQTT Problems" + ANSI_RESET);
    }

  }

  /*
   * return 0: OK
   * return 1: can't optimize (e.g., already at min value)
   * return 2: NOT OK (e.g., value not a number)
   * bandwidth will increase so the bandwidth_usage will decrease
   */
  private int optimizeCommand() {
    System.out.println(debugInfo + "OPTIMIZING ...");

    // NOTE: JSON stuff
    try {
      // create object mapper instance
      ObjectMapper mapper = new ObjectMapper();
      File file = Paths.get(ENV_FILE_PATH).toFile();

      // Parsing JSON
      Map<String, Map<String, Object>> map = mapper.readValue(file, new TypeReference<>() {
      });

      // Update value
      if (map.containsKey(ENV_NODE)) { // Use ENV_NODE constant
        Map<String, Object> innerMap = map.get(ENV_NODE);

        Object valueObj = innerMap.get(NODE_VALUE); // Use NODE_VALUE constant
        double newValue = 0;
        if (valueObj instanceof Number) {
          double currentValue = ((Number) valueObj).doubleValue();

          if ((currentValue - 10) < MIN_VAL) {
            System.err.println(ANSI_RED + "OPTIMIZE: MIN_VAL Exceeded. Cannot decrease further." + ANSI_RESET);
            return 1;
          }

          newValue = currentValue - 10.0;
          innerMap.put(NODE_VALUE, newValue); // Use NODE_VALUE constant
        } else {
          System.err.println(ANSI_RED + "OPTIMIZE: value is not a Number in JSON." + ANSI_RESET);
          return 2;
        }
      }

      // Write on file the updates
      mapper.writerWithDefaultPrettyPrinter().writeValue(file, map);
      System.out.println(debugInfo + "Value updated (Optimized)");

    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ANSI_RED + "JSON Error during OPTIMIZE operation." + ANSI_RESET);
      return 2; // Indicate an error
    }

    return 0; // OK
  }

  /*
   * return 0: OK
   * return 1: can't degrade (e.g., already at max value)
   * return 2: NOT OK (e.g., value not a number)
   */
  private int degradeCommand() {
    System.out.println(debugInfo + "DEGRADING ...");

    // NOTE: JSON stuff
    try {
      // create object mapper instance
      ObjectMapper mapper = new ObjectMapper();
      File file = Paths.get(ENV_FILE_PATH).toFile();

      // Parsing JSON
      Map<String, Map<String, Object>> map = mapper.readValue(file, new TypeReference<>() {
      });

      // Update value
      if (map.containsKey(ENV_NODE)) { // Use ENV_NODE constant
        Map<String, Object> innerMap = map.get(ENV_NODE);

        Object valueObj = innerMap.get(NODE_VALUE); // Use NODE_VALUE constant
        double newValue = 0;
        if (valueObj instanceof Number) {
          double currentValue = ((Number) valueObj).doubleValue();

          if ((currentValue + 10) > MAX_VAL) {
            System.err.println(ANSI_RED + "DEGRADE: MAX_VAL Exceeded. Cannot increase further." + ANSI_RESET);
            return 1;
          }

          newValue = currentValue + 10.0;
          innerMap.put(NODE_VALUE, newValue); // Use NODE_VALUE constant
        } else {
          System.err.println(ANSI_RED + "DEGRADE: value is not a Number in JSON." + ANSI_RESET);
          return 2;
        }
      }

      // Write on file the updates
      mapper.writerWithDefaultPrettyPrinter().writeValue(file, map);
      System.out.println(debugInfo + "Value updated (Degraded)");

    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ANSI_RED + "JSON Error during DEGRADE operation." + ANSI_RESET);
      return 2; // Indicate an error
    }

    return 0; // OK
  }
}
