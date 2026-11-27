using System.Net;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace FluxVisualsBots;

public sealed record CatalTurn(string Role, string Text);

public readonly record struct CatalResult(bool Ok, string Text)
{
    public static CatalResult Success(string text) => new(true, text);
    public static CatalResult Fail(string text) => new(false, text);
}

/// <summary>Общий доступ к Catal через защищённый серверный шлюз AuthServer.</summary>
public sealed class CatalClient
{
    private static readonly JsonSerializerOptions JsonOpts = new(JsonSerializerDefaults.Web)
    {
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
    };

    private readonly HttpClient _http = new() { Timeout = TimeSpan.FromSeconds(100) };
    private readonly string _baseUrl;
    private readonly string _secret;

    public CatalClient()
    {
        _baseUrl = (Environment.GetEnvironmentVariable("AUTH_SERVER_URL")
            ?? "https://fluxvisuals.fvls.workers.dev").TrimEnd('/');
        _secret = Environment.GetEnvironmentVariable("FLUXVISUALS_BOT_SECRET") ?? "";
    }

    public bool Configured => !string.IsNullOrWhiteSpace(_secret);

    public async Task<CatalResult> AskAsync(
        string message,
        IEnumerable<CatalTurn> history,
        string? image,
        ulong discordUserId)
    {
        if (!Configured)
            return CatalResult.Fail("Catal пока недоступен: не настроен доступ к серверу.");

        var payload = new
        {
            message,
            history = history.Select(t => new { role = t.Role, text = t.Text }).ToArray(),
            image,
            context = new { app = "discord" }
        };

        using var request = new HttpRequestMessage(HttpMethod.Post, _baseUrl + "/api/bot/ask")
        {
            Content = JsonContent.Create(payload, options: JsonOpts)
        };
        request.Headers.Add("X-Bot-Token", _secret);
        request.Headers.Add("X-Client-User-Id", discordUserId.ToString());

        HttpResponseMessage response;
        try
        {
            response = await _http.SendAsync(request);
        }
        catch (Exception)
        {
            return CatalResult.Fail("Catal сейчас не отвечает. Попробуйте ещё раз позже.");
        }

        using (response)
        {
            string body = await response.Content.ReadAsStringAsync();
            if (response.IsSuccessStatusCode)
            {
                try
                {
                    using var doc = JsonDocument.Parse(body);
                    if (doc.RootElement.TryGetProperty("answer", out var answer))
                    {
                        string? text = answer.GetString();
                        if (!string.IsNullOrWhiteSpace(text))
                            return CatalResult.Success(text.Trim());
                    }
                }
                catch { }
                return CatalResult.Fail("Catal вернул пустой ответ. Попробуйте ещё раз.");
            }

            string upstream = ReadError(body);
            string error = response.StatusCode switch
            {
                HttpStatusCode.Unauthorized => "Catal недоступен: проверьте настройку сервера.",
                HttpStatusCode.ServiceUnavailable => "Catal пока недоступен: ключ ИИ не задан на сервере.",
                HttpStatusCode.NotFound => "Catal ещё не развёрнут на сервере.",
                HttpStatusCode.TooManyRequests => "Слишком много вопросов. Подождите минуту.",
                _ => string.IsNullOrWhiteSpace(upstream)
                    ? "Catal недоступен. Попробуйте позже."
                    : "Catal недоступен: " + upstream
            };
            return CatalResult.Fail(error);
        }
    }

    private static string ReadError(string body)
    {
        try
        {
            using var doc = JsonDocument.Parse(body);
            if (doc.RootElement.TryGetProperty("error", out var error))
                return error.GetString() ?? "";
        }
        catch { }
        return string.IsNullOrWhiteSpace(body) ? "" : body[..Math.Min(body.Length, 180)];
    }
}
