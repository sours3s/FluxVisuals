using System.Text.Json;
using System.Text.Json.Serialization;

namespace FluxVisualsLoader.Core;

/// <summary>Настройки лаунчера (сохраняются в config.json рядом с exe).
/// Имена в camelCase, чтобы совпадать с JS-интерфейсом — иначе настройки «спадают».</summary>
public class AppConfig
{
    [JsonPropertyName("username")] public string Username { get; set; } = "Player";
    [JsonPropertyName("uid")] public int? Uid { get; set; }
    [JsonPropertyName("isAdmin")] public bool IsAdmin { get; set; } = false;
    [JsonPropertyName("role")] public string Role { get; set; } = "user";
    [JsonPropertyName("hasAccess")] public bool HasAccess { get; set; } = false;
    [JsonPropertyName("accessExpiresAt")] public DateTime? AccessExpiresAt { get; set; }
    [JsonPropertyName("ramMb")] public int RamMb { get; set; } = 4096;
    [JsonPropertyName("version")] public string Version { get; set; } = "1.21.11";
    [JsonPropertyName("gameDir")] public string GameDir { get; set; } = @"C:\FluxVisuals\game";
    [JsonPropertyName("javaPath")] public string JavaPath { get; set; } = "";
    [JsonPropertyName("curseforgeKey")] public string CurseforgeKey { get; set; } = "";
    [JsonPropertyName("clientName")] public string ClientName { get; set; } = "FluxVisuals";
    /// <summary>Никнейм Minecraft, с которым пользователь заходит на сервера (отдельно от ника аккаунта).</summary>
    [JsonPropertyName("mcNickname")] public string McNickname { get; set; } = "";
    [JsonPropertyName("accent")] public int Accent { get; set; } = unchecked((int)0xFFA855F7);
    [JsonPropertyName("accentSecond")] public int AccentSecond { get; set; } = unchecked((int)0xFFEC4899);
    [JsonPropertyName("modules")] public Dictionary<string, bool> Modules { get; set; } = new();
    [JsonPropertyName("modDownloadUrl")] public string ModDownloadUrl { get; set; } = "https://github.com/sours3s/FluxVisuals/releases/download/v1.0.12/fluxvisuals-mod-1.0.12.jar";
    [JsonPropertyName("modVersion")] public string ModVersion { get; set; } = "";
    /// <summary>Иконки установленных файлов: fileName -> URL иконки (для «Моих файлов»).</summary>
    [JsonPropertyName("modIcons")] public Dictionary<string, string> ModIcons { get; set; } = new();
    [JsonPropertyName("authServerUrl")] public string AuthServerUrl { get; set; } = "https://fluxvisuals-server.onrender.com";
    [JsonPropertyName("authToken")] public string AuthToken { get; set; } = "";
    [JsonPropertyName("updateCheckUrl")] public string UpdateCheckUrl { get; set; } = UpdateService.DefaultUpdateCheckUrl;
    [JsonPropertyName("updateCheckEnabled")] public bool UpdateCheckEnabled { get; set; } = true;

    /// <summary>config.json всегда лежит рядом с запущенным exe (Environment.ProcessPath).
    /// Это единственный стабильный путь в single-file сборке и он переживает автообновление:
    /// updater.bat заменяет exe на том же месте, поэтому config.json остаётся на месте.</summary>
    public static string ConfigPath
    {
        get
        {
            string? exeDir = Path.GetDirectoryName(Environment.ProcessPath);
            if (!string.IsNullOrEmpty(exeDir)) return Path.Combine(exeDir, "config.json");
            return Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "config.json");
        }
    }

    /// <summary>Путь конфига внутри папки игры — авторитетная копия, переживает перенос exe.</summary>
    public string GameConfigPath =>
        string.IsNullOrWhiteSpace(GameDir) ? "" : Path.Combine(GameDir, "config.json");

    public static AppConfig Load()
    {
        AppConfig? cfg = null;
        try
        {
            if (File.Exists(ConfigPath))
                cfg = JsonSerializer.Deserialize<AppConfig>(File.ReadAllText(ConfigPath));
        }
        catch { }

        // Миграция со старых версий: config.json мог лежать в AppDomain.BaseDirectory.
        if (cfg == null)
        {
            try
            {
                string legacyPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "config.json");
                if (!string.Equals(Path.GetFullPath(legacyPath), Path.GetFullPath(ConfigPath), StringComparison.OrdinalIgnoreCase)
                    && File.Exists(legacyPath))
                    cfg = JsonSerializer.Deserialize<AppConfig>(File.ReadAllText(legacyPath));
            }
            catch { }
        }
        cfg ??= new AppConfig();

        // Авторитетный конфиг лежит в папке игры (сохраняется вместе с игрой).
        // Auth-поля переносим из exe-конфига, чтобы не разлогинило при расхождении.
        try
        {
            string gamePath = cfg.GameConfigPath;
            if (!string.IsNullOrWhiteSpace(gamePath) && File.Exists(gamePath))
            {
                var gc = JsonSerializer.Deserialize<AppConfig>(File.ReadAllText(gamePath));
                if (gc != null)
                {
                    gc.AuthToken = cfg.AuthToken;
                    gc.AuthServerUrl = cfg.AuthServerUrl;
                    gc.IsAdmin = cfg.IsAdmin;
                    gc.Role = cfg.Role;
                    gc.HasAccess = cfg.HasAccess;
                    gc.Username = cfg.Username;
                    gc.Uid = cfg.Uid;
                    gc.AccessExpiresAt = cfg.AccessExpiresAt;
                    return gc;
                }
            }
        }
        catch { }
        return cfg;
    }

    public void Save()
    {
        var options = new JsonSerializerOptions { WriteIndented = true };
        string json = JsonSerializer.Serialize(this, options);
        try { File.WriteAllText(ConfigPath, json); } catch { }
        // Зеркалим в папку игры — конфиг переживает обновление/перенос лоадера.
        try
        {
            string gamePath = GameConfigPath;
            if (!string.IsNullOrWhiteSpace(gamePath)
                && !string.Equals(Path.GetFullPath(gamePath), Path.GetFullPath(ConfigPath), StringComparison.OrdinalIgnoreCase))
            {
                Directory.CreateDirectory(GameDir);
                File.WriteAllText(gamePath, json);
            }
        }
        catch { }
    }
}
