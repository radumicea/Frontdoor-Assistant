using Azure.Storage.Blobs;

namespace Api.DataAccessors;

public sealed class StorageAccessor
{
    private readonly BlobServiceClient _blobService;

    public StorageAccessor(IConfiguration configuration)
    {
        _blobService = new BlobServiceClient(configuration.GetConnectionString("StorageAccount"));
    }

    private async Task<BlobContainerClient> CreateContainerIfNotExists(string containerName)
    {
        var container = _blobService.GetBlobContainerClient(containerName);
        await container.CreateIfNotExistsAsync();
        return container;
    }

    public async Task SaveBlob(string containerName, Stream stream, string fileName)
    {
        var container = await CreateContainerIfNotExists(containerName);
        var blob = container.GetBlobClient(fileName);
        if (await blob.ExistsAsync())
            return;

        stream.Position = 0;
        await blob.UploadAsync(stream);
        await stream.DisposeAsync();
    }

    public async Task DeleteBlobs(string containerName, string name)
    {
        var container = await CreateContainerIfNotExists(containerName);
        await foreach (var blob in container.GetBlobsAsync(prefix: name))
        {
            await container.DeleteBlobAsync(blob.Name);
        }
    }

    public async Task<Stream> GetBlob(string containerName, string fileName)
    {
        var containerClient = _blobService.GetBlobContainerClient(containerName);
        var blobClient = containerClient.GetBlobClient(fileName);

        var stream = new MemoryStream();
        await blobClient.DownloadToAsync(stream);

        stream.Position = 0;
        return stream;
    }
}
