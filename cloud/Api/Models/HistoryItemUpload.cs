namespace Api.Models;

public sealed class HistoryItemUpload
{
    public string UserName { get; set; } = null!;
    public string Password { get; set; } = null!;
    public string Name { get; set; } = null!;
    public long TimeStamp { get; set; }
    public IFormFile File { get; set; } = null!;
}