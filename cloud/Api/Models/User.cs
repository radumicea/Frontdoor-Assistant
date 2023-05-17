using Microsoft.AspNetCore.Identity;

namespace Api.Models;

public sealed class User : IdentityUser
{
    public string? RefreshToken { get; set; }
    public DateTime RefreshTokenExpiryTime { get; set; }
    public float? Latitude { get; set; }
    public float? Longitude { get; set; }
    public string BlackListed { get; set; } = "[]";
    public string? FirebaseToken { get; set; }
}
