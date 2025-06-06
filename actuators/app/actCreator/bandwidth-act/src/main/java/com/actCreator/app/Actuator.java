package com.actCreator.app;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;


public class Actuator implements BundleActivator {

  @Override
  public void start(BundleContext ctx){
    System.out.println("Hi I'm am Actuator and I've just STARED ");

  }

  @Override
  public void stop(BundleContext ctx){
    System.out.println("Hi I'm am Actuator and I've just STOPPPED ");

  }

}
