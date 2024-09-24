# Front Door Assistant

## Project Overview

The Front Door Assistant is a home security and convenience system that leverages facial recognition and weather alerts to enhance safety and comfort. It integrates IoT devices like cameras, sensors, and microcontrollers with a Raspberry Pi for local processing, and a cloud backend for storage and communication. This system is ideal for monitoring front-door activity and receiving real-time weather alerts.

### Features
- **Facial Recognition for Security**: Detects and identifies individuals at the front door. If someone from the blacklist is identified, a notification is sent to the mobile app. Unknown individuals are still logged without a notification.
- **Weather Alerts**: Reads out loud weather alerts when the door is opened, giving users quick weather updates before they leave the house.
- **Local Processing (Edge Computing)**: Image processing and facial recognition are handled on the Raspberry Pi, reducing reliance on cloud processing and saving bandwidth.
- **GUI Dashboard for Sensors**: A web-based interface allows users to configure and manage the sensors locally.

### Use Cases
1. **Home Security**: Monitor front-door activity with facial recognition, track known or unknown visitors, and receive alerts when blacklisted individuals are detected.
2. **Weather Information**: Automatically receive real-time weather alerts when leaving the house, based on door sensor triggers.
3. **Local Control and Configuration**: Easily configure door and motion sensors via a local web interface without needing complex setups.

## System Architecture

The system architecture is divided into four key components:

1. **Mobile Application**: The mobile app provides users with access to data and notifications, and communicates with the cloud via HTTPS. It allows the user to interact with features like the blacklist, weather alerts, and the history of recognized individuals.

2. **Cloud**: The cloud acts as a central data hub for user data, images, and communication between the mobile app and Raspberry Pi. It consists of:
    - **Web API**: Manages user requests and mediates between the mobile app, Raspberry Pi, and other cloud components.
    - **Blob Storage**: Stores images, metadata, and temporary files.
    - **AMQP Broker**: Facilitates communication between the cloud and the Raspberry Pi.
    - **Database**: Stores user information, including blacklists.

   ![Cloud Architecture Diagram](https://github.com/user-attachments/assets/b09a4554-c361-428b-a109-ee418e16047f)

3. **Raspberry Pi**: The core processing unit of the system. The Raspberry Pi handles:
    - **Facial Recognition**: Processes images locally, comparing faces with stored encodings to determine if someone is recognized or blacklisted.
    - **Edge Computing**: Reduces cloud load by processing images locally and only sending important results to the cloud.
    - **MQTT Client**: Communicates with the microcontrollers to receive sensor data, such as door sensor triggers or motion detection.
    - **AMQP Client**: Listens for commands from the cloud, such as adding/removing faces from the blacklist or fetching weather alerts.

   ![System Architecture Diagram](https://github.com/user-attachments/assets/fc684d4e-c0bd-437c-a6cd-c11cb01c8bed)

4. **Microcontrollers**: Two ESP-32 microcontrollers are used to handle the sensors:
    - **PIR Sensor & Camera**: Detect motion and capture images when someone is near the door.
    - **Door Sensor**: Detects when the door is opened or closed, triggering weather alerts.
    - **Wi-Fi Dashboard**: Microcontrollers offer a local Wi-Fi-based GUI to configure network settings and connect to the Raspberry Pi via MQTT.

   ![Microcontroller Flow Diagram](https://github.com/user-attachments/assets/ba31d73c-4001-4531-b6fc-0f4deb24f1e3)

## Technologies Used

- **Mobile App**: Android SDK (Kotlin)
- **Cloud Backend**: ASP.NET (C#), SQLite, Azure Blob Storage, Azure IoT Hub, RabbitMQ, Firebase Cloud Messaging
- **Raspberry Pi**: Python (`face-recognition`, `gtts`, `pygame`, `azure-iot-device`, `paho-mqtt`)
- **Microcontrollers**: ESP-32 (C++, PlatformIO), OV2640 Camera, MQTT for communication
- **Weather Alerts**: Open Meteo API for real-time weather information
