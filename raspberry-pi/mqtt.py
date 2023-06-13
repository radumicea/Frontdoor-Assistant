import paho.mqtt.client as mqtt
import io
import threading
from weather import weather
from facial_recognition import facial_recognition


import time

address = "rabbitmq"
connString = f"mqtt://{address}:1883"


faceFails = 0
maxFails = 5


face_lock = threading.Lock()


def handle_face(payload):
    if face_lock.locked():
        return

    with face_lock:
        global faceFails, maxFails

        with open("image.jpg", "wb") as file:
            file.write(payload)
        facesFound = facial_recognition.recognize_faces('image.jpg')
        if not facesFound:
            faceFails += 1
            print('No face found')
        else:
            faceFails = 0
            print('Face found')

        if faceFails == maxFails:
            client.publish('stop', 'two')


def handle_image(topic, payload):
    print("Image received")

    threading.Thread(target=handle_face(payload)).start()


def handle_door(topic, payload):

    if payload.decode() == "open":
        print('Door opened')
        threading.Thread(target=weather.play_weather_alerts).start()
    else:
        print('Door closed')


def handle_movement(topic, payload):
    global faceFails
    if payload.decode() == "found":
        print('Movement detected')
        client.publish("start", "two")
        faceFails = 0
    else:
        print('Movement stopped')


def on_connect(client, userdata, flags, rc):
    print("connected")
    client.subscribe("door")
    client.subscribe("movement")
    client.subscribe("face")


def on_message(client, userdata, msg):
    if msg.topic == "door":
        threading.Thread(target=handle_door, args=(
            msg.topic, msg.payload)).start()
    elif msg.topic == "face":
        threading.Thread(target=handle_image, args=(
            msg.topic, msg.payload)).start()
    elif msg.topic == "movement":
        threading.Thread(target=handle_movement, args=(
            msg.topic, msg.payload)).start()


client = mqtt.Client(client_id="server")
client.username_pw_set("mqtt-test", "mqtt-test")
client.reconnect_delay_set(1, 5)
client.on_connect = on_connect
client.on_message = on_message


client.connect(address)
client.loop_forever()
