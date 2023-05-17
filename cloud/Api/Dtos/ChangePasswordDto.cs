namespace Api.Dtos;

public sealed class ChangePasswordDto
{
    public string UserName { get; set; } = null!;
    public string OldPassword { get; set; } = null!;
    public string NewPassword { get; set; } = null!;
}
