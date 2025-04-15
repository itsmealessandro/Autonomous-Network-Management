# Autonomous System with MAPE-K Loop

## Overview

This project implements an autonomous software system based on the MAPE-K loop (Monitor, Analyze, Plan, Execute, Knowledge).  
The system is designed to monitor and adapt the behavior of a **computer network** by simulating network activity and reacting in real-time to changing traffic, bandwidth usage, and possible anomalies.

All components are containerized via Docker, and the system is fully modular thanks to the use of **OSGi** for dynamic management of sensors and actuators.

## Application Domain

The goal of the system is to manage and optimize the behavior of a **simulated network infrastructure**. It achieves this by continuously analyzing key performance indicators such as:

- Traffic flow
- Bandwidth usage
- Network latency
- Packet loss
- Number of connected devices
- Suspicious activity detection

The system ensures that the network remains healthy, responsive, and protected through self-adaptive mechanisms implemented in the MAPE-K cycle.

## Tools and Frameworks

- **Docker**: orchestration of containers
- **Mosquitto**: MQTT broker for sensor/actuator communication
- **OSGi (Equinox)**: modular simulation of sensors and actuators as bundles
- **Server-1**: Analyzer (analyzes incoming metrics)
- **Server-2**: Planner (generates action plans)
- **InfluxDB**: Knowledge component, used as a time series database
- **NodeRed**: Used to send data from sensors to InfluxDB

## Simulated Network Sensors

Each sensor is implemented as a separate OSGi bundle that generates simulated data and publishes it to a dedicated MQTT topic. Each topic corresponds to a type of sensor:

| Topic                     | Description                          | Sample Payload |
|--------------------------|--------------------------------------|----------------|
| `traffic_flow`   | Current traffic (kB/s)               | `2300.45`      |
| `bandwidth_usage`| Bandwidth usage (%)                  | `72.8`         |
| `latency`        | Average latency (ms)                 | `43`           |
| `packet_loss`    | Packet loss rate (%)                 | `0.5`          |
| `connected_devices` | Number of active devices         | `12`           |
| `suspicious_activity` | Risk level or alert             | `low`, `true`  |

Each bundle can be started/stopped independently, enabling or disabling a specific metric stream dynamically.

---

# Autonomous Behavior

## Self-Configuration

The system is able to self-configure by dynamically discovering and integrating new sensor bundles via OSGi. When a new sensor JAR is deployed, it automatically starts publishing data to its respective topic without requiring changes to the rest of the system.

**Example**: Deploying a new `latency` sensor bundle causes the system to start monitoring network delay and adapting accordingly, even if the metric was previously unavailable.

---

## Self-Optimization

Based on the metrics collected, the system identifies optimization opportunities such as:

- High bandwidth usage → suggest rerouting or throttling.
- Increasing latency → increase priority for real-time traffic.
- Unbalanced traffic flow → redistribute loads or devices.

The planner uses heuristic or rule-based logic to decide how to optimize resource usage and maximize network performance.

---

## Self-Healing

The system can detect abnormal or degraded performance patterns and attempt corrective actions automatically.

**Example**:

- Spike in packet loss or latency → restart affected components or reduce non-critical traffic.
- Sudden drop in traffic flow or connected devices → assume failure and initiate reconnection or alert.

---

## Self-Protection

Basic anomaly detection is implemented to simulate **intrusion detection** and mitigate risks.

**Example**:

- Suspicious activity detected (`true` or `high` on `network/suspicious_activity`) → system may simulate firewall updates, temporary connection blocks, or send notifications to the administrator group.

---

## Environment simulation
All the decision taken by the system are simulated by the actuators on a JSON file that simulates the environment.
