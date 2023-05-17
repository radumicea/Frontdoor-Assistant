namespace Api.Dtos;

public sealed class UserDto
{
    public string UserName { get; set; } = null!;
    public string Password { get; set; } = null!;
    public string? FirebaseToken { get; set; }
}
