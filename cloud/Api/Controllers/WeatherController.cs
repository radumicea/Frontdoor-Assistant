using Api.DataAccessors;
using Api.Dtos;
using Api.Models;
using GeoTimeZone;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using TimeZoneConverter;

namespace Api.Controllers;

[Route("Api/[controller]")]
[ApiController]
public sealed class WeatherController : ControllerBase
{
    private readonly UserManager<User> _userManager;
    private readonly HttpClient _httpClient;
    private readonly AppDbContext _dbContext;

    public WeatherController(UserManager<User> userManager, HttpClient httpClient, AppDbContext dbContext)
    {
        _userManager = userManager;
        _httpClient = httpClient;
        _dbContext = dbContext;
    }

    [Authorize]
    [HttpPost]
    [Route("SetLocation")]
    public async Task<IActionResult> SetLocation([FromBody] LocationDto dto)
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null)
            return BadRequest();

        user.Latitude = dto.Latitude;
        user.Longitude = dto.Longitude;

        await _dbContext.SaveChangesAsync();

        return Ok();
    }

    [HttpPost]
    [Route("FetchWeatherAlerts")]
    public async Task<IActionResult> FetchWeatherAlerts([FromBody] UserDto dto)
    {
        var user = await _userManager.FindByNameAsync(dto.UserName);
        if (user is null)
            return BadRequest();

        if (!await _userManager.CheckPasswordAsync(user, dto.Password))
            return Unauthorized();

        if (user.Latitude is null || user.Longitude is null)
            return Ok("Please set your location before using the service!");

        var tzStr = TimeZoneLookup.GetTimeZone(user.Latitude!.Value, user.Longitude!.Value).Result;
        var tzInfo = TZConvert.GetTimeZoneInfo(tzStr);

        var url = $"https://api.open-meteo.com/v1/forecast?forecast_days=2&past_days=1&timezone={tzStr}&latitude={user.Latitude}&longitude={user.Longitude}&hourly=precipitation_probability,temperature_2m,apparent_temperature,windspeed_10m,windgusts_10m";
        var response = (await _httpClient.GetFromJsonAsync<WeatherDto>(url))!;
        var weather = response.GetWeatherObjects();

        var now = TimeZoneInfo.ConvertTime(DateTime.UtcNow, tzInfo);
        now = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0);
        var yesterday = now.AddDays(-1);
        var tomorrow = now.AddDays(1);

        var weatherYesterday = weather.Where(w => w.Time < now && w.Time >= yesterday).ToArray();
        var weatherNow = weather.First(w => w.Time == now);
        var weatherToday = weather.Where(w => w.Time > now && w.Time < tomorrow).ToList();

        var alerts = new List<string>();
        var alertedCurrentTemp = false;

        var maxYesterday = weatherYesterday.MaxBy(static w => w.Temperature)!;
        var maxFeltYesterday = weatherYesterday.MaxBy(static w => w.ApparentTemperature)!;

        if (weatherNow.Temperature >= maxYesterday.Temperature + 5 || weatherNow.ApparentTemperature >= maxFeltYesterday.ApparentTemperature + 5)
        {
            alerts.Add($"High temperature warning! The temperature right now is {weatherNow.Temperature} degrees and it feels like {weatherNow.ApparentTemperature} degrees!");
            alertedCurrentTemp = true;
        }
        else
        {
            var hotToday = weatherToday.FirstOrDefault(w => w.Temperature >= maxYesterday.Temperature + 5);
            var hotFeltToday = weatherToday.FirstOrDefault(w => w.ApparentTemperature >= maxFeltYesterday.ApparentTemperature + 5);

            if (hotToday is not null)
                alerts.Add($"High temperature warning! Today will be {hotToday.Temperature} degrees starting at {hotToday.Time.Hour} o\'clock!");
            else if (hotFeltToday is not null)
                alerts.Add($"High temperature warning! Today will feel like {hotFeltToday.ApparentTemperature} degrees starting at {hotFeltToday.Time.Hour} o\'clock!");
        }

        var minYesterday = weatherYesterday.MinBy(static w => w.Temperature)!;
        var minFeltYesterday = weatherYesterday.MinBy(static w => w.ApparentTemperature)!;

        if (weatherNow.Temperature <= minYesterday.Temperature - 5 || weatherNow.ApparentTemperature <= minFeltYesterday.ApparentTemperature - 5)
        {
            alerts.Add($"Low temperature warning! The temperature right now is {weatherNow.Temperature} degrees and it feels like {weatherNow.ApparentTemperature} degrees!");
            alertedCurrentTemp = true;
        }
        else
        {
            var coldToday = weatherToday.FirstOrDefault(w => w.Temperature <= minYesterday.Temperature - 5);
            var coldFeltToday = weatherToday.FirstOrDefault(w => w.ApparentTemperature <= minFeltYesterday.ApparentTemperature - 5);

            if (coldToday is not null)
                alerts.Add($"Low temperature warning! Today will be {coldToday.Temperature} degrees starting at {coldToday.Time.Hour} o\'clock!");
            else if (coldFeltToday is not null)
                alerts.Add($"Low temperature warning! Today will feel like {coldFeltToday.ApparentTemperature} degrees starting at {coldFeltToday.Time.Hour} o\'clock!");
        }

        if (!alertedCurrentTemp)
            alerts.Insert(0, $"The temperature right now is {weatherNow.Temperature} degrees and it feels like {weatherNow.ApparentTemperature} degrees.");

        weatherToday.Insert(0, weatherNow);

        var wind = weatherToday.FirstOrDefault(static w => w.WindSpeed >= 42);
        var windGust = weatherToday.FirstOrDefault(static w => w.WindGust >= 60);

        if (wind is not null)
            alerts.Add($"High wind speeds warning! Today the wind will blow at {wind.WindSpeed} km/h starting at {wind.Time.Hour} o\'clock!");
        else if (windGust is not null)
            alerts.Add($"High wind gust speeds warning! Today the wind gust will blow at {windGust.WindGust} km/h starting at {windGust.Time.Hour} o\'clock!");

        var precipitation = weatherToday.FirstOrDefault(static w => w.PrecipitationProbability >= 40);
        if (precipitation is not null)
            alerts.Add($"Precipitation warning! Today the chance for precipitation will be {precipitation.PrecipitationProbability}% starting at {precipitation.Time.Hour} o\'clock!");

        return Ok(string.Join('\n', alerts));
    }
}
