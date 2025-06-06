package com.actCreator.app;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;

public class Actuator implements BundleActivator, Runnable {

	private final Thread thread = new Thread(this);

  @Override
  public void start(BundleContext ctx){
    System.out.println("Hi I'm am Actuator and I've just STARED ");

		thread.start(); // this will launch run method

  }

  @Override
  public void stop(BundleContext ctx){
    System.out.println("Hi I'm am Actuator and I've just STOPPPED ");

  }

  @Override 
  public void run(){
    System.out.println("thread activated, I'm an Actuator, I'm running");
    for (int i = 0; i < 10; i++) {
      try {
        
      java.lang.Thread.sleep(500);

      System.out.println("hello:" + i);
      } catch (InterruptedException e) {
        System.err.println("interr ex");
      }
      
    }

  }

}
