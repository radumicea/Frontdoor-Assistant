using Api.DataAccessors;
using Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.Devices;
using System.Text;
using System.Text.Json;

namespace Api.Controllers;

[Route("Api/[controller]")]
[ApiController]
public sealed class BlacklistController : ControllerBase
{
    private readonly AppDbContext _dbContext;
    private readonly StorageAccessor _storage;
    private readonly ServiceClient _serviceClient;
    private readonly UserManager<User> _userManager;

    public BlacklistController(AppDbContext dbContext, StorageAccessor storage, UserManager<User> userManager, IConfiguration configuration)
    {
        _dbContext = dbContext;
        _storage = storage;
        _serviceClient = ServiceClient.CreateFromConnectionString(configuration.GetConnectionString("IotHub"));
        _userManager = userManager;
    }

    [Authorize]
    [HttpPost]
    [Route("AddToBlacklist")]
    public async Task<IActionResult> AddToBlacklist([FromForm] IFormFileCollection files, [FromQuery] string name)
    {
        var user = await _userManager.GetUserAsync(User);

        // Save faces to blob storage
        var tasks = files.Select(async f =>
        {
            await _storage.SaveBlob(user.NormalizedUserName.ToLowerInvariant(), f.OpenReadStream(), $"{name}/{f.FileName}");
        });
        await Task.WhenAll(tasks);

        // Save person's name in the database
        var blackList = JsonSerializer.Deserialize<List<string>>(user.BlackListed)!;
        if (!blackList.Contains(name))
        {
            blackList.Add(name);
            user.BlackListed = JsonSerializer.Serialize(blackList);
            await _dbContext.SaveChangesAsync();
        }

        // Send message to raspberry pi
        var msg = new Message(Encoding.ASCII.GetBytes($"{{\"method\":\"encode_new_faces\",\"args\":\"{name}\"}}"));
        await _serviceClient.SendAsync(user.UserName, msg);

        return Ok();
    }

    [Authorize]
    [HttpGet]
    [Route("RemoveFromBlacklist")]
    public async Task<IActionResult> RemoveFromBlacklist([FromQuery] string name)
    {
        var user = await _userManager.GetUserAsync(User);

        // Delete from blob
        await _storage.DeleteBlobs(user.NormalizedUserName.ToLowerInvariant(), name);

        // Delete from database
        var blackList = JsonSerializer.Deserialize<List<string>>(user.BlackListed)!;
        int idx;
        if ((idx = blackList.IndexOf(name)) >= 0)
        {
            blackList.RemoveAt(idx);
            user.BlackListed = JsonSerializer.Serialize(blackList);
            await _dbContext.SaveChangesAsync();
        }

        // Send message to raspberry pi
        var msg = new Message(Encoding.ASCII.GetBytes($"{{\"method\":\"remove_encoded_faces\",\"args\":\"{name}\"}}"));
        await _serviceClient.SendAsync(user.UserName, msg);

        return Ok();
    }
}
