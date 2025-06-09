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



  // MQTT 
  final String CLIENT_ID = "act_bandwith";
  final String GENERAL_TOPIC = "Network/bandwidth_usage";
  final String COMMANDS_TOPIC = GENERAL_TOPIC + "/commands";

  // JSON env file
  final String ENV_FILE_PATH = "/simulated_env/env.json";
  final String ENV_NODE = "bandwidth_usage";
  final String NODE_VALUE = "value";
	final String BROKER = "tcp://broker:1883";

	private final Thread thread = new Thread(this);


@Override
public String toString() {
    // Definisci la larghezza massima per i nomi dei campi per l'allineamento
    int fieldNameWidth = 15; // Adatta questo valore se i tuoi nomi di campo sono più lunghi

    return "ActuatorData {\n" +
           String.format("  %-"+ fieldNameWidth +"s: '%s'\n", "CLIENT_ID", CLIENT_ID) +
           String.format("  %-"+ fieldNameWidth +"s: '%s'\n", "BROKER", BROKER) +
           String.format("  %-"+ fieldNameWidth +"s: '%s'\n", "GENERAL_TOPIC", GENERAL_TOPIC) +
           String.format("  %-"+ fieldNameWidth +"s: '%s'\n", "COMMANDS_TOPIC", COMMANDS_TOPIC) +
           String.format("  %-"+ fieldNameWidth +"s: '%s'\n", "ENV_FILE_PATH", ENV_FILE_PATH) +
           String.format("  %-"+ fieldNameWidth +"s: '%s'\n", "ENV_NODE", ENV_NODE) +
           String.format("  %-"+ fieldNameWidth +"s: '%s'\n", "NODE_VALUE", NODE_VALUE) +
           "}";
}

  @Override
  public void start(BundleContext ctx){
    System.out.println(debugInfo +"Hi I'm am Actuator and I've just STARED ");

		thread.start(); // this will launch run method

  }

  @Override
  public void stop(BundleContext ctx){
    System.out.println(debugInfo +"Hi I'm am Actuator and I've just STOPPPED ");

  }

  @Override 
  public void run(){
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

          //TODO: 
          // 1- listen to the specific actuator topic
          // 2- when a new message arrives, check it and print it.
          // 3- define possible commands that can arrive via that message
          // 4- When a specific commoands arrives the actuator has different behaviors based on that command
          // 5- if command:"INCREASE" then get the specific value of the JSON env file 
          // of this specific actuator and increase it by an arbitrary value, write it on the file.
          // print the change done on console.


          // Callback per ricevere i messaggi
          client.setCallback(new MqttCallback() {
              @Override
              public void connectionLost(Throwable cause) {
                  System.out.println(ANSI_RED + "Connection LOST"+ ANSI_RESET + cause.getMessage());
              }

              @Override
              public void messageArrived(String topic, MqttMessage message) {
                  System.out.println(ANSI_WHITE +"Message Received:" + new String(message.getPayload()) + ANSI_RESET);
              }

              @Override
              public void deliveryComplete(IMqttDeliveryToken token) {}

          });


      client.subscribe(COMMANDS_TOPIC);
      System.out.println(debugInfo+ "subscribed to topic: " + COMMANDS_TOPIC + ANSI_RESET);


    } catch (MqttException e) {
      e.printStackTrace();
      System.err.println(ANSI_RED+ "MQTT Problems" + ANSI_RESET);
    }


  }

}
