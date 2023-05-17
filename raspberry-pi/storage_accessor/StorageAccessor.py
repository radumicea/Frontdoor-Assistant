from azure.storage.blob import BlobServiceClient
from pathlib import Path
import sys

sys.path.append('.')

from shared.config_helpers import *


class StorageAccessor:
    __blob_service_client = BlobServiceClient.from_connection_string(read_config('StorageConnectionString'))
    __container_name = read_config('UserName').lower()

    @staticmethod
    def save_then_delete_blobs(folder_name: str) -> None:
        container = StorageAccessor.__blob_service_client.get_container_client(StorageAccessor.__container_name)

        Path(folder_name).mkdir(exist_ok=True)

        for blob_prop in container.list_blobs(folder_name):
            blob_client = container.get_blob_client(blob_prop.name)

            with open(blob_prop.name, 'wb') as f:
                download_stream = blob_client.download_blob()
                f.write(download_stream.readall())

            blob_client.delete_blob('include')