/* INFO: 
 * This class define a Sensor that already has defined client id and the topic.
 * It has to be: compiled -> zipped in a jar with a specific manifest -> activated from OSGI activator
 */
package com.sensCreator.app;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class Sensor implements BundleActivator, Runnable {
	final String ANSI_RESET = "\u001B[0m";
	final String ANSI_BLACK = "\u001B[30m";
	final String ANSI_RED = "\u001B[31m"; // important operation
	final String ANSI_GREEN = "\u001B[32m"; // good operation result
	final String ANSI_YELLOW = "\u001B[33m";
	final String ANSI_BLUE = "\u001B[34m";
	final String ANSI_PURPLE = "\u001B[35m";
	final String ANSI_CYAN = "\u001B[36m";
	final String ANSI_WHITE = "\u001B[37m";

	private final Thread thread = new Thread(this);

	@Override
	public void start(BundleContext bc) {
		System.out.println(ANSI_BLUE + "Bundle has started, I'm a sensor");
		thread.start();
	}

	@Override
	public void stop(BundleContext context) {
		System.out.println("Bundle is stopping." + this.getClass().getName());
	}

	@Override
	public void run() {

		System.out.println("I'm running, I'm traffic_flow sensor ...");
		final String topicName = "Network/latency/value";
		final String clientId = "3";
		System.out.println("topic:" + topicName);
		System.out.println("clientId:" + clientId);

		String topic = topicName;
		int qos = 1;
		String broker = "tcp://broker:1883";
		MemoryPersistence persistence = new MemoryPersistence();

		boolean active = true;

		System.out.println(ANSI_GREEN + "Dynamic sensor activated" + topicName);

		try {
			// NOTE: enstablishing MQTT Connection
			MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			System.out.println("------------------------------------------------------------");
			System.out.println("Connecting to broker: " + broker);
			sampleClient.connect(connOpts);
			// System.out.println("Connected");
			// System.out.println("------------------------------------------------------------");
			Thread.sleep(2000);

			while (active) {
				System.out.println("---------------- sensor" + topicName +" running --------------");

				// NOTE: JSON

				// Creazione del mapper JSON
				ObjectMapper objectMapper = new ObjectMapper();

				File jsonFile = new File("/simulated_env/env.json");
				if (!jsonFile.exists()) {
					System.out.println("Errore: Il file JSON " + jsonFile.getAbsolutePath()
							+ " non esiste.");
					return;
				}

				String[] splittedTopic = topic.split("/");
				System.out.println("this is the splitted topic") ;
          for (String s: splittedTopic){
            System.out.println(s);
          }

				// Lettura del file JSON
				JsonNode rootNode = objectMapper.readTree(jsonFile);

				// Navigazione nel JSON
				JsonNode room = rootNode.get(splittedTopic[1]);

				int value = room.get(splittedTopic[2]).asInt();

				System.out.println("---------------- env data begin --------------");

				System.out.println("data:" + splittedTopic[1]);
				System.out.println("value:" + value);

				System.out.println("---------------- env data end --------------");

				String content = null;
				Random random = new Random();
				// 5% probability to trigger alarm
				int alertProb = random.nextInt(0, 100);
				if (alertProb <= 5) {

					// System.out.println("------------------------------------------------------------");
					System.out.println(ANSI_RED + " ALERT VALUE sensor:" + topicName);
					// System.out.println("------------------------------------------------------------");
					content = String.valueOf(random.nextInt(6, 10));

				} else {
					// System.out.println("------------------------------------------------------------");
					// System.out.println(" normal VALUE sens1");
					// System.out.println("------------------------------------------------------------");
					content = String.valueOf(value);
				}

				Thread.sleep(500);

				MqttMessage message = new MqttMessage(content.getBytes());

				// System.out.println("Publishing message: " + content);
				message.setQos(qos);
				sampleClient.publish(topic, message);
				// System.out.println("Message published");

			}

			// Disconnecting
			// System.out.println("------------------------------------------------------------");
			sampleClient.disconnect();
			// System.out.println("Disconnected");
			System.exit(0);
		} catch (MqttException me) {
			// System.out.println("reason " + me.getReasonCode());
			// System.out.println("msg " + me.getMessage());
			// System.out.println("loc " + me.getLocalizedMessage());
			// System.out.println("cause " + me.getCause());
			// System.out.println("excep " + me);
			me.printStackTrace();
			System.exit(1);
		} catch (InterruptedException | IOException e) {
			// System.out.println("time exeption");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
