import sys
from azure.iot.device import IoTHubDeviceClient
import json
import time

sys.path.append('.')

from shared.config_helpers import *
from facial_recognition import facial_recognition


CONNECTION_STRING = read_config('IoTConnectionString')


def handle_change_password(new_password: str) -> None:
    write_config({'Password': new_password})


def handle_encode_new_faces(name: str) -> None:
    facial_recognition.encode_new_faces(name)


def handle_remove_encoded_faces(name: str) -> None:
    facial_recognition.remove_encoded_faces(name)


def message_handler(message):
    print(f'Message received: {message.data}')

    data = json.loads(message.data.decode('utf8'))

    match data['method']:
        case 'change_password':
            handle_change_password(data['args'])
        case 'encode_new_faces':
            handle_encode_new_faces(data['args'])
        case 'remove_encoded_faces':
            handle_remove_encoded_faces(data['args'])


def main():
    print ('Starting the Python IoT Hub C2D Messaging device...')

    # Instantiate the client
    client = IoTHubDeviceClient.create_from_connection_string(CONNECTION_STRING)

    print ('Waiting for C2D messages, press Ctrl-C to exit')
    try:
        # Attach the handler to the client
        client.on_message_received = message_handler

        while True:
            time.sleep(1000)
    except KeyboardInterrupt:
        print('IoT Hub C2D Messaging device sample stopped')
    finally:
        # Graceful exit
        print('Shutting down IoT Hub Client')
        client.shutdown()


if __name__ == '__main__':
    main()