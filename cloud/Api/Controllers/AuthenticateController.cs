using Api.DataAccessors;
using Api.Dtos;
using Api.Helpers;
using Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.Devices;
using Microsoft.IdentityModel.Tokens;
using System.Data;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;

namespace Api.Controllers;

[Route("Api/[controller]")]
[ApiController]
public sealed class AuthenticateController : ControllerBase
{
    private readonly UserManager<User> _userManager;
    private readonly RoleManager<IdentityRole> _roleManager;
    private readonly IConfiguration _configuration;
    private readonly AppDbContext _dbContext;
    private readonly ServiceClient _serviceClient;

    public AuthenticateController(
        UserManager<User> userManager,
        RoleManager<IdentityRole> roleManager,
        IConfiguration configuration,
        AppDbContext dbContext)
    {
        _userManager = userManager;
        _roleManager = roleManager;
        _configuration = configuration;
        _dbContext = dbContext;
        _serviceClient = ServiceClient.CreateFromConnectionString(configuration.GetConnectionString("IotHub"));
    }

    [HttpPost]
    [Route("Login")]
    public async Task<IActionResult> Login([FromBody] UserDto dto)
    {
        var user = await _userManager.FindByNameAsync(dto.UserName);
        if (user is null || !await _userManager.CheckPasswordAsync(user, dto.Password))
            return Unauthorized();

        var userRoles = await _userManager.GetRolesAsync(user);

        var authClaims = new List<Claim>
        {
            new(ClaimTypes.Name, user.UserName),
            new(ClaimTypes.NameIdentifier, user.Id),
            new(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
        };
        authClaims.AddRange(userRoles.Select(static userRole => new Claim(ClaimTypes.Role, userRole)));

        var now = DateTime.UtcNow;

        var token = CreateToken(authClaims, now);
        var refreshToken = GenerateRefreshToken();

        var refreshTokenValidityInDays = int.Parse(_configuration["JWT:RefreshTokenValidityInDays"]!);

        user.RefreshToken = refreshToken;
        user.RefreshTokenExpiryTime = now.AddDays(refreshTokenValidityInDays);
        user.FirebaseToken = dto.FirebaseToken;

        await _dbContext.SaveChangesAsync();

        return Ok(new
        {
            Token = new JwtSecurityTokenHandler().WriteToken(token),
            RefreshToken = refreshToken,
        });
    }

    [Authorize(Roles = UserRoles.Admin)]
    [HttpPost]
    [Route("Register")]
    public async Task<IActionResult> Register([FromBody] UserDto dto)
    {
        var userExists = await _userManager.FindByNameAsync(dto.UserName);
        if (userExists is not null)
            return StatusCode(StatusCodes.Status409Conflict, "User already exists!");

        User user = new()
        {
            SecurityStamp = Guid.NewGuid().ToString(),
            UserName = dto.UserName,
        };

        var result = await _userManager.CreateAsync(user, dto.Password);
        if (!result.Succeeded)
            return StatusCode(StatusCodes.Status500InternalServerError);

        if (!await _roleManager.RoleExistsAsync(UserRoles.User))
            await _roleManager.CreateAsync(new IdentityRole(UserRoles.User));

        await _userManager.AddToRoleAsync(user, UserRoles.User);

        return Ok();
    }

    [HttpPost]
    [Route("ChangePassword")]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordDto dto)
    {
        var user = await _userManager.FindByNameAsync(dto.UserName);
        if (user is null)
            return BadRequest("No such user!");

        // Change password in database
        var res = await _userManager.ChangePasswordAsync(user, dto.OldPassword, dto.NewPassword);

        if (!res.Succeeded)
            return Unauthorized("Incorrect old password!");

        // Send message to raspberry pi
        var msg = new Message(Encoding.ASCII.GetBytes($"{{\"method\":\"change_password\",\"args\":\"{dto.NewPassword}\"}}"));
        await _serviceClient.SendAsync(user.UserName, msg);

        return Ok();
    }

    [HttpPost]
    [Route("RefreshToken")]
    public async Task<IActionResult> RefreshToken([FromBody] TokenDto dto)
    {
        var token = dto.Token;
        var refreshToken = dto.RefreshToken;

        var principal = GetPrincipalFromExpiredToken(token);
        if (principal is null) return BadRequest("Invalid access token or refresh token!");

        var userName = principal.Identity!.Name!;

        var user = await _userManager.FindByNameAsync(userName);

        var now = DateTime.UtcNow;

        if (user is null || user.RefreshToken != refreshToken || user.RefreshTokenExpiryTime <= now)
            return Unauthorized("Invalid access token or refresh token!");

        var newAccessToken = CreateToken(principal.Claims.ToList(), now);
        var newRefreshToken = GenerateRefreshToken();

        user.RefreshToken = newRefreshToken;
        user.FirebaseToken = dto.FirebaseToken;
        await _dbContext.SaveChangesAsync();

        return Ok(new
        {
            Token = new JwtSecurityTokenHandler().WriteToken(newAccessToken),
            RefreshToken = newRefreshToken
        });
    }

    [Authorize]
    [HttpGet]
    [Route("LogOut")]
    public async Task<IActionResult> LogOut()
    {
        var user = await _userManager.GetUserAsync(User);

        user.RefreshToken = null;
        await _userManager.UpdateAsync(user);

        return Ok();
    }

    private JwtSecurityToken CreateToken(IEnumerable<Claim> authClaims, DateTime now)
    {
        var authSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_configuration["JWT:Secret"]!));
        var tokenValidityInMinutes = int.Parse(_configuration["JWT:TokenValidityInMinutes"]!);

        var token = new JwtSecurityToken(
            _configuration["JWT:ValidIssuer"],
            _configuration["JWT:ValidAudience"],
            expires: now.AddMinutes(tokenValidityInMinutes),
            claims: authClaims,
            signingCredentials: new SigningCredentials(authSigningKey, SecurityAlgorithms.HmacSha256)
        );

        return token;
    }

    private static string GenerateRefreshToken()
    {
        var randomNumber = new byte[64];
        using var rng = RandomNumberGenerator.Create();
        rng.GetBytes(randomNumber);
        return Convert.ToBase64String(randomNumber);
    }

    private ClaimsPrincipal? GetPrincipalFromExpiredToken(string? token)
    {
        var tokenValidationParameters = new TokenValidationParameters
        {
            ValidateAudience = false,
            ValidateIssuer = false,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_configuration["JWT:Secret"]!)),
            ValidateLifetime = false
        };

        var tokenHandler = new JwtSecurityTokenHandler();
        var principal =
            tokenHandler.ValidateToken(token, tokenValidationParameters, out var securityToken);
        if (securityToken is not JwtSecurityToken jwtSecurityToken ||
            !jwtSecurityToken.Header.Alg.Equals(SecurityAlgorithms.HmacSha256,
                StringComparison.InvariantCultureIgnoreCase))
            throw new SecurityTokenException("Invalid token!");

        return principal;
    }
}
