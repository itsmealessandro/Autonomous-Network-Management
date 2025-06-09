# Explaining actuators
This container contains The OSGi system that handle all the actuators.
The actuators are listening on a specific topic of MQTT waiting to act.
When a signal is received they modify the JSON file that simulate the environment.
## OSGi actuators
There is a Actuators Handler Jar that constantly checks for: updates, new actuators and dismissed actuators.
Each actuator is in a JAR file and every time a new actuator is created the handler will activate it.
## How the actuators works
Each actuator is listening to a specific topic. When a specific value in this topic is published, the actuator execute an action depending on the value.
The has different behavior based on the received value.

- **off** : turn off and remove the activator
- **numeric value** (e.g.1240.4) : write on the env.JSON file that specific value


