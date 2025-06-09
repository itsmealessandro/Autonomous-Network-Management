package com.actCreator.app;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;

public class Actuator implements BundleActivator, Runnable {
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

	private final Thread thread = new Thread(this);

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
    System.out.println(debugInfo + "thread activated, I'm an Actuator, I'm running");

System.out.println(debugInfo + "I'm running, I'm bandwith actuator ...");
		final String topicName = "Network/bandwidth_usage/value";
		final String clientId = "1";
		System.out.println("topic:" + topicName);
		System.out.println("clientId:" + clientId);

		String topic = topicName;
		int qos = 1;
		String broker = "tcp://broker:1883";
		MemoryPersistence persistence = new MemoryPersistence();

		boolean active = true;

		// System.out.println(ANSI_GREEN + "Dynamic sensor activated" + topicName);

    // NOTE: enstablishing MQTT Connection
    try {
      
          MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
          MqttConnectOptions connOpts = new MqttConnectOptions();
          connOpts.setCleanSession(true);
          System.out.println("------------------------------------------------------------");
          System.out.println(debugInfo + "Connecting to broker: " + broker);

          sampleClient.connect(connOpts);
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

    } catch (MqttException e) {
      e.printStackTrace();
      System.err.println("oops");
    }


  }

}
