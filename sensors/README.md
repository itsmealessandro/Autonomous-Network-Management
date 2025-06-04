# Explaining sensors
This container contains The OSGi system that handle all the sensors.
Each sensors is constantly reading the environment JSON file.
Based on the data they read, they publish on their specific topic a value.
Occasionally they will sends bad data that has to be handled correctly.
## OSGi sensors
There is a Sensors Handler Jar that constantly checks for: updates, new actuators and dismissed sensors.
Each sensor is in a JAR file and every time a new one is created the handler will activate it.
