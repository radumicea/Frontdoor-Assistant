from datetime import datetime
from gtts import gTTS
from pygame import mixer
import os
import requests
import sys
import threading

sys.path.append('.')

from utils.config_helpers import read_config


door_lock = threading.Lock()

mixer.init()


def __get_weather_alerts():
    url = read_config('ApiAddress')
    user_name = read_config('UserName')
    password = read_config('Password')

    response = requests.post(f'{url}/Weather/FetchWeatherAlerts',
                             json={'userName': user_name, 'password': password})

    return response.text


def play_weather_alerts():
    if door_lock.locked():
        return
    
    with door_lock:
        tts = gTTS(__get_weather_alerts())
        name = f'tmp_{datetime.now().timestamp()}.mp3'
        tts.save(name)
        mixer.music.load(name)
        mixer.music.play()
        while mixer.music.get_busy() == True:
            continue
        mixer.music.unload()
        os.remove(name)


if __name__ == '__main__':
    play_weather_alerts()
