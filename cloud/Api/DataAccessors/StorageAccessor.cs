using Azure.Storage.Blobs;
using Azure.Storage.Blobs.Models;

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

    public async Task SaveBlob(string containerName, Stream stream, string blobName)
    {
        var container = await CreateContainerIfNotExists(containerName);
        var blob = container.GetBlobClient(blobName);
        if (await blob.ExistsAsync())
            return;

        stream.Position = 0;
        await blob.UploadAsync(stream);
        await stream.DisposeAsync();
    }

    public async Task DeleteBlobs(string containerName, string folderName)
    {
        var container = await CreateContainerIfNotExists(containerName);
        await foreach (var blob in container.GetBlobsAsync(prefix: folderName))
        {
            await container.DeleteBlobIfExistsAsync(blob.Name, DeleteSnapshotsOption.IncludeSnapshots);
        }
    }

    public async Task<IEnumerable<(string fileName, MemoryStream stream)>> GetBlobs(string containerName, string folderName)
    {
        var container = await CreateContainerIfNotExists(containerName);
        var res = new List<(string fileName, MemoryStream stream)>();

        await foreach (var blob in container.GetBlobsAsync(prefix: folderName))
        {
            var blobClient = container.GetBlobClient(blob.Name);
            var stream = new MemoryStream();
            await blobClient.DownloadToAsync(stream);
            res.Add((blob.Name, stream ));
        }

        return res;
    }
}
