using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using FluxVisualsLoader.Core;

namespace FluxVisualsLoader.Core;

/// <summary>Сервис авторизации через AuthServer (JWT + HWID).</summary>
public static class AuthService
{
    private static readonly HttpClient _http = new()
    {
        // 90 сек: на бесплатном Render сервер «засыпает» и просыпается 30-60 сек
        Timeout = TimeSpan.FromSeconds(90)
    };

    /// <summary>Ретрай до 3 попыток: спящий Render на первый запрос отвечает таймаутом,
    /// повтор уже попадает в тёплый сервер.</summary>
    private static async Task<T> WithRetryAsync<T>(Func<Task<T>> action)
    {
        Exception? last = null;
        for (int attempt = 1; attempt <= 3; attempt++)
        {
            try
            {
                return await action();
            }
            catch (Exception ex)
            {
                last = ex;
                if (attempt == 3) break;
                await Task.Delay(3000);
            }
        }
        throw last ?? new HttpRequestException();
    }

    public static async Task<AuthCheckResult> CheckAuthAsync(AppConfig cfg)
    {
        if (string.IsNullOrWhiteSpace(cfg.AuthToken))
            return new AuthCheckResult { Authenticated = false };

        try
        {
            var url = $"{cfg.AuthServerUrl.TrimEnd('/')}/api/auth/verify";

            var req = new HttpRequestMessage(HttpMethod.Get, url);
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", cfg.AuthToken);

            var resp = await WithRetryAsync(() => _http.SendAsync(req));
            if (resp.IsSuccessStatusCode)
            {
                var json = await resp.Content.ReadAsStringAsync();
                using var doc = JsonDocument.Parse(json);
                var root = doc.RootElement;
                string role = root.TryGetProperty("role", out var r) ? r.GetString() ?? "user" : "user";
                bool hasAccess = root.TryGetProperty("hasAccess", out var h) && h.GetBoolean();
                DateTime? exp = root.TryGetProperty("accessExpiresAt", out var e) && e.ValueKind == JsonValueKind.String
                    ? DateTime.TryParse(e.GetString(), out var dt) ? dt : null : null;

                int? uid = root.TryGetProperty("uid", out var uidEl) && uidEl.ValueKind == JsonValueKind.Number
                    ? uidEl.GetInt32() : null;

                return new AuthCheckResult
                {
                    Authenticated = true,
                    Username = root.TryGetProperty("username", out var un) ? un.GetString() : null,
                    Role = role,
                    IsAdmin = role == "admin",
                    HasAccess = hasAccess,
                    AccessExpiresAt = exp,
                    Uid = uid
                };
            }
            else if (resp.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                // Token expired or invalid
                cfg.AuthToken = "";
                cfg.Save();
            }
        }
        catch (Exception ex)
        {
            LauncherLog.Error($"Auth check failed: {ex.Message}");
        }

        return new AuthCheckResult { Authenticated = false };
    }

    public static async Task<AuthResult?> LoginAsync(string username, string password, string authServerUrl)
    {
        try
        {
            var hwid = GetHwid();
            var payload = JsonSerializer.Serialize(new { username, password, hwid });
            var content = new StringContent(payload, Encoding.UTF8, "application/json");

            var url = $"{authServerUrl.TrimEnd('/')}/api/auth/login";
            var resp = await WithRetryAsync(() => _http.PostAsync(url, content));

            if (resp.IsSuccessStatusCode)
            {
                var json = await resp.Content.ReadAsStringAsync();
                using var doc = JsonDocument.Parse(json);
                var root = doc.RootElement;
                string role = root.TryGetProperty("role", out var r) ? r.GetString() ?? "user" : "user";
                bool hasAccess = root.TryGetProperty("hasAccess", out var h) && h.GetBoolean();
                DateTime? exp = root.TryGetProperty("accessExpiresAt", out var e) && e.ValueKind == JsonValueKind.String
                    ? DateTime.TryParse(e.GetString(), out var dt) ? dt : null : null;

                int? uid = root.TryGetProperty("uid", out var uidEl) && uidEl.ValueKind == JsonValueKind.Number
                    ? uidEl.GetInt32() : null;

                return new AuthResult
                {
                    Token = root.GetProperty("token").GetString() ?? "",
                    Username = root.GetProperty("username").GetString() ?? "",
                    Role = role,
                    IsAdmin = role == "admin",
                    HasAccess = hasAccess,
                    AccessExpiresAt = exp,
                    Uid = uid
                };
            }
            else
            {
                var err = await resp.Content.ReadAsStringAsync();
                return new AuthResult { Error = err };
            }
        }
        catch (Exception ex)
        {
            return new AuthResult { Error = ex.Message };
        }
    }

    public static async Task<bool> RegisterAsync(string username, string password, bool isAdmin, string authServerUrl, string adminToken)
    {
        try
        {
            var payload = JsonSerializer.Serialize(new { username, password, isAdmin });
            var content = new StringContent(payload, Encoding.UTF8, "application/json");

            var url = $"{authServerUrl.TrimEnd('/')}/api/auth/register";
            var req = new HttpRequestMessage(HttpMethod.Post, url) { Content = content };
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", adminToken);

            var resp = await WithRetryAsync(() => _http.SendAsync(req));
            return resp.IsSuccessStatusCode;
        }
        catch
        {
            return false;
        }
    }

    public static async Task<ModVersionInfo?> GetModVersionAsync(string authServerUrl)
    {
        try
        {
            var url = $"{authServerUrl.TrimEnd('/')}/api/mod/version";
            var json = await WithRetryAsync(() => _http.GetStringAsync(url));
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            return new ModVersionInfo
            {
                DownloadUrl = root.GetProperty("downloadUrl").GetString() ?? "",
                Version = root.GetProperty("version").GetString() ?? "",
                FileName = root.GetProperty("fileName").GetString() ?? ""
            };
        }
        catch
        {
            return null;
        }
    }

    public static string GetHwid()
    {
        try
        {
            // Stable HWID: Motherboard + BIOS + CPU ID
            var sb = new StringBuilder();
            using (var searcher = new System.Management.ManagementObjectSearcher("SELECT SerialNumber FROM Win32_BaseBoard"))
            {
                foreach (var obj in searcher.Get()) sb.Append(obj["SerialNumber"]?.ToString() ?? "");
            }
            using (var searcher = new System.Management.ManagementObjectSearcher("SELECT ProcessorId FROM Win32_Processor"))
            {
                foreach (var obj in searcher.Get()) sb.Append(obj["ProcessorId"]?.ToString() ?? "");
            }
            using (var searcher = new System.Management.ManagementObjectSearcher("SELECT SerialNumber FROM Win32_BIOS"))
            {
                foreach (var obj in searcher.Get()) sb.Append(obj["SerialNumber"]?.ToString() ?? "");
            }

            var hash = System.Security.Cryptography.SHA256.HashData(Encoding.UTF8.GetBytes(sb.ToString()));
            var hwid = Convert.ToHexString(hash)[..32];
            LauncherLog.Info($"HWID (WMI): {hwid}");
            return hwid;
        }
        catch (Exception ex)
        {
            var hwid = Environment.MachineName + Environment.OSVersion.Version;
            LauncherLog.Warn($"HWID fallback (WMI failed: {ex.Message}): {hwid}");
            return hwid;
        }
    }
}

public record AuthResult
{
    public string Token { get; init; } = "";
    public string Username { get; init; } = "";
    public string Role { get; init; } = "user";
    public bool IsAdmin { get; init; }
    public bool HasAccess { get; init; }
    public DateTime? AccessExpiresAt { get; init; }
    public int? Uid { get; init; }
    public string? Error { get; init; }
    public bool Success => string.IsNullOrEmpty(Error);
}

public record AuthCheckResult
{
    public bool Authenticated { get; init; }
    public string? Username { get; init; }
    public string Role { get; init; } = "user";
    public bool IsAdmin { get; init; }
    public bool HasAccess { get; init; }
    public DateTime? AccessExpiresAt { get; init; }
    public int? Uid { get; init; }
}

public record ModVersionInfo
{
    public string DownloadUrl { get; init; } = "";
    public string Version { get; init; } = "";
    public string FileName { get; init; } = "";
}