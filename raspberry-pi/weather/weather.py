from datetime import datetime
import sys
from gtts import gTTS
import requests

sys.path.append('.')

from utils.config_helpers import read_config


def __get_weather_alerts():
    url = read_config('ApiAddress')
    user_name = read_config('UserName')
    password = read_config('Password')

    response = requests.post(f'{url}/Weather/FetchWeatherAlerts', json={'userName': user_name, 'password': password}, verify=False)

    return response.text


def play_weather_alerts():
    tts = gTTS(__get_weather_alerts())
    name = f'tmp_{datetime.now().timestamp()}.mp3'
    tts.save(name)



if __name__ == '__main__':
    play_weather_alerts()