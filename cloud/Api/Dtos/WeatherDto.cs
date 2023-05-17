using System.Text.Json.Serialization;

namespace Api.Dtos;

public sealed class WeatherDto
{
    public Hourly Hourly { get; set; } = null!;

    public IEnumerable<WeatherObj> GetWeatherObjects()
    {
        return Enumerable
            .Range(0, Hourly.Time.Length)
            .Select(i => new WeatherObj
            {
                Time = Hourly.Time[i],
                PrecipitationProbability = Hourly.PrecipitationProbability[i],
                Temperature = Hourly.Temperature[i],
                ApparentTemperature = Hourly.ApparentTemperature[i],
                WindSpeed = Hourly.WindSpeed[i],
                WindGust = Hourly.WindGusts[i],
            })
            .ToArray();
    }
}

public sealed class Hourly
{
    public DateTime[] Time { get; set; } = null!;
    [JsonPropertyName("precipitation_probability")]
    public int[] PrecipitationProbability { get; set; } = null!;
    [JsonPropertyName("temperature_2m")]
    public float[] Temperature { get; set; } = null!;
    [JsonPropertyName("apparent_temperature")]
    public float[] ApparentTemperature { get; set; } = null!;
    [JsonPropertyName("windspeed_10m")]
    public float[] WindSpeed { get; set; } = null!;
    [JsonPropertyName("windgusts_10m")]
    public float[] WindGusts { get; set; } = null!;
}
public sealed class WeatherObj
{
    public DateTime Time { get; set; }
    public int PrecipitationProbability { get; set; }
    public float Temperature { get; set; }
    public float ApparentTemperature { get; set; }
    public float WindSpeed { get; set; }
    public float WindGust { get; set; }
}