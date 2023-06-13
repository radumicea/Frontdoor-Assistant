sed -i "s/{user}/$USER/" mqtt.service
sed -i "s/{user}/$USER/" iothub_client.service

sudo cp mqtt.service /etc/systemd/system/mqtt.service
sudo cp iothub_client.service /etc/systemd/system/iothub_client.service

sudo systemctl start mqtt.service
sudo systemctl enable mqtt.service

sudo systemctl start iothub_client.service
sudo systemctl enable iothub_client.service