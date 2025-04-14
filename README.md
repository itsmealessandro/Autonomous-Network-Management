# Autonomous Network Management System with MAPE-K Loop

## Overview

This project implements an autonomous software system based on the MAPE-K loop (Monitor, Analyze, Plan, Execute, Knowledge).  
The system autonomously manages a local network by continuously observing network conditions and adapting its configuration to ensure efficiency, reliability, and security.  
It utilizes containerized components and OSGi-based modules to simulate a smart and reactive network environment.

![system image view](file:///home/alessandro/Desktop/Università/magistrale/1_anno/as/autonomous-system/mape-k.png?msec=1744634057812)

## Topic

The main goal of the system is to implement an **autonomous network management system** capable of:

- Optimizing traffic flow and bandwidth usage.
- Reacting to performance issues (e.g., high latency, congestion, packet loss).
- Reconfiguring network parameters and QoS settings dynamically.
- Detecting and reacting to suspicious or unauthorized devices.
- Supporting high-level goals like "prioritize real-time video conferencing" or "minimize energy usage".

The system is useful in smart environments such as home networks, small offices, or campus LANs.

## Tools and Frameworks

To enhance modularity and flexibility, the system is built using Docker containers, each representing a component of the MAPE-K loop:

- **Monitor**
  - **mosquitto**: MQTT broker used to coordinate communication between components.
  - **sensors (OSGi)**: Simulated OSGi bundles collecting data about network conditions (e.g., traffic load, latency, connected devices).
  
- **Analyze**
  - **analyzer (PHP)**: Receives and processes sensor data to detect anomalies and identify optimization opportunities.
  
- **Plan**
  - **planner (Node.js)**: Decides on concrete adaptation actions based on the analysis and current goals.
  
- **Execute**
  - **actuators (OSGi)**: OSGi bundles that receive execution commands and simulate actions like bandwidth throttling, QoS reconfiguration, or device isolation.

- **Knowledge**
  - **influxdb**: Time-series database used to store historical network data and knowledge for supporting decision-making.

## Autonomous System Definition

This is a self-managing autonomic system that supports the following properties:

- **Self-configuration**: Automatically configures network parameters and adapts to newly connected devices.
- **Self-optimization**: Monitors and improves network performance by adjusting QoS, balancing bandwidth, or rerouting traffic.
- **Self-healing**: Detects and mitigates faults like lost connectivity, degraded performance, or service interruption.
- **Self-protection**: Recognizes abnormal network activity and isolates suspicious devices or traffic patterns.

## Self-Management Implementations

### Self-Configuration

- Automatically discovers and configures new sensors and actuators.
- Assigns default roles or priorities to new devices based on type or usage.

### Self-Optimization

- Detects bandwidth congestion and reroutes or throttles traffic.
- Identifies peak hours and adapts QoS to prioritize critical services (e.g., video calls over downloads).

### Self-Healing

- Monitors for anomalies such as excessive packet loss or device unreachability.
- Reconfigures network routes or resets affected components when a fault is detected.

### Self-Protection

- Detects unexpected devices connecting to the network.
- Executes mitigation strategies like isolating MAC addresses, notifying administrators, or limiting access.

## Architecture Overview

- Modular, containerized architecture.
- Each MAPE-K component runs in its own container.
- Sensors and actuators are dynamically managed OSGi bundles.
- MQTT is used as the communication backbone for decoupled and extensible interactions.

## Distribution

You can run this project on your personal machine using Docker.

### Prerequisites

- Git
- Docker + Docker Compose
- The following ports must be available:
  - `1883` (MQTT Broker)
  - `8080` (Analyzer server)
  - `3000` (Planner server)
  - `8086` (InfluxDB)

### Installation

```bash
git clone https://github.com/itsmealessandro/autonomous-system
docker compose up
