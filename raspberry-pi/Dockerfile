FROM python:3.11

EXPOSE 1883
EXPOSE 80

WORKDIR /app

COPY requirements.txt requirements.txt
RUN echo "y" | apt-get update
RUN echo "y" | apt-get install build-essential
RUN pip3 install cmake
RUN pip3 install -r requirements.txt

COPY . .

Run python mqtt.py