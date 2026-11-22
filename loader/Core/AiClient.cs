using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace FluxVisualsLoader.Core;

/// <summary>Обмен с Catal через AuthServer. Ключ ИИ живёт только на сервере —
/// в лаунчер и WebView он никогда не попадает.</summary>
public static class AiClient
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(120) };
    private static readonly JsonSerializerOptions JsonOpts = new(JsonSerializerDefaults.Web);

    public sealed record StatusResult(bool Available, string? Error);
    public sealed record AskResult(string? Answer, string? Model, string? Error);

    public static async Task<StatusResult> StatusAsync(AppConfig cfg, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(cfg.AuthToken))
            return new StatusResult(false, "Войдите в аккаунт, чтобы использовать Catal.");

        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Get, Url(cfg, "api/ai/status"));
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", cfg.AuthToken);
            using var resp = await Http.SendAsync(req, ct);
            var body = await resp.Content.ReadAsStringAsync(ct);
            if (!resp.IsSuccessStatusCode)
                return new StatusResult(false, ReadError(body, resp.StatusCode));

            using var doc = JsonDocument.Parse(body);
            var root = doc.RootElement;
            bool available = root.TryGetProperty("available", out var a) && a.GetBoolean();
            return new StatusResult(available, available ? null : "Catal пока недоступен на сервере.");
        }
        catch (Exception ex)
        {
            return new StatusResult(false, "Не удалось проверить доступность Catal: " + ex.Message);
        }
    }

    public static async Task<AskResult> AskAsync(
        AppConfig cfg,
        string message,
        JsonElement history,
        string? image,
        JsonElement context,
        CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(cfg.AuthToken))
            return new AskResult(null, null, "Войдите в аккаунт, чтобы написать Catal.");

        var payload = new Dictionary<string, object?>
        {
            ["message"] = message,
            ["history"] = history.ValueKind == JsonValueKind.Array ? history : new List<string>(),
            ["image"] = image,
            ["context"] = context.ValueKind == JsonValueKind.Object ? context : new Dictionary<string, string>()
        };

        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Post, Url(cfg, "api/ai/ask"))
            {
                Content = new StringContent(JsonSerializer.Serialize(payload, JsonOpts), Encoding.UTF8, "application/json")
            };
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", cfg.AuthToken);
            using var resp = await Http.SendAsync(req, ct);
            var body = await resp.Content.ReadAsStringAsync(ct);
            if (!resp.IsSuccessStatusCode)
                return new AskResult(null, null, ReadError(body, resp.StatusCode));

            using var doc = JsonDocument.Parse(body);
            var root = doc.RootElement;
            string answer = root.TryGetProperty("answer", out var ans) ? ans.GetString() ?? "" : "";
            string? model = root.TryGetProperty("model", out var m) ? m.GetString() : null;
            if (string.IsNullOrWhiteSpace(answer))
                return new AskResult(null, model, "Catal вернул пустой ответ. Попробуйте ещё раз.");
            return new AskResult(answer, model, null);
        }
        catch (Exception ex)
        {
            return new AskResult(null, null, "Не удалось связаться с Catal: " + ex.Message);
        }
    }

    private static string Url(AppConfig cfg, string path)
    {
        string baseUrl = string.IsNullOrWhiteSpace(cfg.AuthServerUrl)
            ? Defaults.AuthServerUrl
            : cfg.AuthServerUrl.TrimEnd('/');
        return baseUrl + "/" + path;
    }

    private static string ReadError(string body, System.Net.HttpStatusCode status)
    {
        try
        {
            using var doc = JsonDocument.Parse(body);
            if (doc.RootElement.TryGetProperty("error", out var e))
                return e.GetString() ?? "Ошибка сервера";
        }
        catch { }
        return status switch
        {
            System.Net.HttpStatusCode.Unauthorized => "Войдите в аккаунт заново.",
            System.Net.HttpStatusCode.NotFound => "Catal ещё не развёрнут на сервере. Обновление сервера может занять несколько минут.",
            System.Net.HttpStatusCode.ServiceUnavailable => "Catal пока недоступен: на сервере не задан ключ ИИ.",
            _ => "Сервер Catal недоступен (" + (int)status + "). Попробуйте позже."
        };
    }
}
