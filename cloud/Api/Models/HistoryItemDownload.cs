namespace Api.Models;

public sealed class HistoryItemDownload
{
    public string Name { get; set; } = null!;
    public long TimeStamp { get; set; }
    public byte[] Data { get; set; } = null!;
}