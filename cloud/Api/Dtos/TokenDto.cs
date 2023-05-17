namespace Api.Dtos;

public sealed class TokenDto
{
    public string Token { get; set; } = default!;
    public string RefreshToken { get; set; } = default!;
    public string? FirebaseToken { get; set; }
}
