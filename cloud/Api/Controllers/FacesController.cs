﻿using Api.DataAccessors;
using Api.Dtos;
using Api.Models;
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
    [Route("AddToBlacklist")]
    public async Task<IActionResult> AddToBlacklist([FromForm] IFormFileCollection files, [FromQuery] string folderName)
    {
        if (folderName.StartsWith("_history_"))
            return BadRequest();

        var user = await _userManager.GetUserAsync(User);

        // Save faces to blob storage
        var tasks = files.Select(async f =>
        {
            await _storage.SaveBlob(user.NormalizedUserName.ToLower(), f.OpenReadStream(), $"{folderName}/{f.FileName}");
        });
        await Task.WhenAll(tasks);

        // Save person's folderName in the database
        var blackList = JsonSerializer.Deserialize<List<string>>(user.BlackListed)!;
        if (!blackList.Contains(folderName))
        {
            blackList.Add(folderName);
            user.BlackListed = JsonSerializer.Serialize(blackList);
            await _dbContext.SaveChangesAsync();
        }

        // Send message to raspberry pi
        var msg = new Message(Encoding.ASCII.GetBytes($"{{\"method\":\"encode_new_faces\",\"args\":\"{folderName}\"}}"));
        await _serviceClient.SendAsync(user.UserName, msg);

        return Ok();
    }

    [Authorize]
    [HttpGet]
    [Route("RemoveFromBlacklist")]
    public async Task<IActionResult> RemoveFromBlacklist([FromQuery] string folderName)
    {
        if (folderName.StartsWith("_history_"))
            return BadRequest();

        var user = await _userManager.GetUserAsync(User);

        // Delete from blob
        await _storage.DeleteBlobs(user.NormalizedUserName.ToLower(), folderName);

        // Delete from database
        var blackList = JsonSerializer.Deserialize<List<string>>(user.BlackListed)!;
        int idx;
        if ((idx = blackList.IndexOf(folderName)) >= 0)
        {
            blackList.RemoveAt(idx);
            user.BlackListed = JsonSerializer.Serialize(blackList);
            await _dbContext.SaveChangesAsync();
        }

        // Send message to raspberry pi
        var msg = new Message(Encoding.ASCII.GetBytes($"{{\"method\":\"remove_encoded_faces\",\"args\":\"{folderName}\"}}"));
        await _serviceClient.SendAsync(user.UserName, msg);

        return Ok();
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
            var data = b.stream.ToArray();
            await b.stream.DisposeAsync();
            return new { Data = data, FileName = b.fileName };
        }).ToList();

        await _storage.DeleteBlobs(containerName, folderName);

        return Ok(res);
    }

    [Authorize]
    [HttpGet]
    [Route("FetchHistory")]
    public async Task<IActionResult> FetchHistory()
    {
        var containerName = User.FindFirst(ClaimTypes.Name)!.Value.ToLower();
        var folderName = "_history_";

        var blobs = await _storage.GetBlobs(containerName, folderName);
        var res = blobs.Select(async b =>
        {
            var data = b.stream.ToArray();
            await b.stream.DisposeAsync();
            return new { Data = data, FileName = b.fileName.Replace($"{folderName}/", "") };
        }).ToList();

        return Ok(res);
    }
}