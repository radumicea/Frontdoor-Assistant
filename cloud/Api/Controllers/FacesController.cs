using Api.DataAccessors;
using Api.Dtos;
using Api.Models;
using FirebaseAdmin.Messaging;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.Devices;
using System.Security.Claims;
using System.Text;
using System.Text.Json;

namespace Api.Controllers;

[Route("Api/[controller]")]
[ApiController]
public sealed class FacesController : ControllerBase
{
    private readonly AppDbContext _dbContext;
    private readonly StorageAccessor _storage;
    private readonly ServiceClient _serviceClient;
    private readonly UserManager<User> _userManager;

    public FacesController(AppDbContext dbContext, StorageAccessor storage, UserManager<User> userManager, IConfiguration configuration)
    {
        _dbContext = dbContext;
        _storage = storage;
        _serviceClient = ServiceClient.CreateFromConnectionString(configuration.GetConnectionString("IotHub"));
        _userManager = userManager;
    }

    [Authorize]
    [HttpPost]
    [Route("AddToBlacklist/{folderName}")]
    public async Task<IActionResult> AddToBlacklist([FromForm] IFormFileCollection photos, [FromRoute] string folderName)
    {
        if (folderName.StartsWith("_history_") || folderName.StartsWith("Unknown", StringComparison.InvariantCultureIgnoreCase))
            return BadRequest();

        var user = await _userManager.GetUserAsync(User);

        // Save faces to blob storage
        var tasks = photos.Select(async f =>
        {
            await _storage.SaveBlob(user.NormalizedUserName.ToLower(), f.OpenReadStream(), $"{folderName}/{f.FileName}");
        });
        await Task.WhenAll(tasks);

        // Save person's folderName in the database
        var blackList = JsonSerializer.Deserialize<SortedSet<string>>(user.BlackListed)!;
        blackList.Add(folderName);
        user.BlackListed = JsonSerializer.Serialize(blackList);
        await _dbContext.SaveChangesAsync();

        // Send message to raspberry pi
        var msg = new Microsoft.Azure.Devices.Message(Encoding.ASCII.GetBytes($"{{\"method\":\"encode_new_faces\",\"args\":\"{folderName}\"}}"));
        await _serviceClient.SendAsync(user.UserName, msg);

        return Ok();
    }

    [Authorize]
    [HttpDelete]
    [Route("RemoveFromBlacklist")]
    public async Task<IActionResult> RemoveFromBlacklist([FromQuery] string[] folderNames)
    {
        folderNames = folderNames.Where(static x => !x.StartsWith("_history_")).ToArray();
        if (folderNames.Length == 0)
            return BadRequest();

        var user = await _userManager.GetUserAsync(User);

        // Delete from blob
        foreach (var folderName in folderNames)
            await _storage.DeleteBlobs(user.NormalizedUserName.ToLower(), folderName);

        // Delete from database
        var blackList = JsonSerializer.Deserialize<SortedSet<string>>(user.BlackListed)!;
        blackList.ExceptWith(folderNames);
        user.BlackListed = JsonSerializer.Serialize(blackList);
        await _dbContext.SaveChangesAsync();

        // Send message to raspberry pi
        var msg = new Microsoft.Azure.Devices.Message(Encoding.ASCII.GetBytes($"{{\"method\":\"remove_encoded_faces\",\"args\":{JsonSerializer.Serialize(folderNames)}}}"));
        await _serviceClient.SendAsync(user.UserName, msg);

        return Ok();
    }

    [Authorize]
    [HttpGet]
    [Route("GetBlacklistNames")]
    public async Task<IActionResult> GetBlacklistNames()
    {
        var user = await _userManager.GetUserAsync(User);
        var blacklist = JsonSerializer.Deserialize<string[]>(user.BlackListed)!;
        return Ok(blacklist);
    }

    [HttpPost]
    [Route("FetchThenClearBlacklist")]
    public async Task<IActionResult> FetchThenClearBlacklist([FromBody] UserDto dto, [FromQuery] string folderName)
    {
        var user = await _userManager.FindByNameAsync(dto.UserName);
        if (user is null)
            return BadRequest();

        if (!await _userManager.CheckPasswordAsync(user, dto.Password))
            return Unauthorized();

        var containerName = user.NormalizedUserName.ToLower();

        var blobs = await _storage.GetBlobs(containerName, folderName);
        var res = blobs.Select(static async b =>
        {
            var data = b.Stream.ToArray();
            await b.Stream.DisposeAsync();
            return new { Data = data, FileName = b.FileName };
        }).ToList();

        await _storage.DeleteBlobs(containerName, folderName);

        return Ok(res);
    }

    [HttpPost]
    [Route("OnPersonSpotted")]
    public async Task<IActionResult> OnPersonSpotted([FromForm] HistoryItemUpload item)
    {
        var user = await _userManager.FindByNameAsync(item.UserName);
        if (user is null)
            return BadRequest();

        if (!await _userManager.CheckPasswordAsync(user, item.Password))
            return Unauthorized();

        var containerName = user.NormalizedUserName.ToLower();

        var extension = Path.GetExtension(item.File.FileName);
        var blobName = $"_history_/{item.Name}/{item.TimeStamp}{extension}";

        await _storage.SaveBlob(containerName, item.File.OpenReadStream(), blobName);

        var blacklist = JsonSerializer.Deserialize<HashSet<string>>(user.BlackListed)!;
        if (blacklist.Contains(item.Name))
        {
            await FirebaseMessaging.DefaultInstance.SendAsync(new FirebaseAdmin.Messaging.Message
            {
                Token = user.FirebaseToken,
                Data = new Dictionary<string, string>
                {
                    { "name", item.Name },
                    { "timeStamp", item.TimeStamp.ToString() }
                }
            });
        }

        return Ok();
    }

    [Authorize]
    [HttpGet]
    [Route("GetHistory")]
    public async Task<IActionResult> GetHistory()
    {
        var containerName = User.FindFirst(ClaimTypes.Name)!.Value.ToLower();

        var blobs = await _storage.GetBlobs(containerName, "_history_");

        var res = blobs.Select(b =>
        {
            var filePath = b.FileName.Replace("_history_/", "");
            var sepIdx = filePath.IndexOf('/');

            var folderName = filePath[..sepIdx];
            var fileName = filePath[(sepIdx + 1)..];
            var timeStamp = long.Parse(Path.GetFileNameWithoutExtension(fileName));

            var item = new HistoryItemDownload
            {
                Name = folderName,
                TimeStamp = timeStamp,
                Data = b.Stream.ToArray()
            };

            b.Stream.Dispose();

            return item;
        }).OrderByDescending(static x => x.TimeStamp).ToList();

        return Ok(res);
    }
}
