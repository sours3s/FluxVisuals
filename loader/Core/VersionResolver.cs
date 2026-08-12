using System.Text.Json;

namespace FluxVisualsLoader.Core;

public class LibSpec
{
    public string Name = "";
    public string Path = "";
    public string Url = "";
    public bool IsNative;
    public bool IsEmpty; // библиотека без артефакта (например, только правила)
}

/// <summary>Разрешает метаданные версии: client jar, libraries, assets, natives, jvm-аргументы.</summary>
public static class VersionResolver
{
    private const string ManifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    public static async Task<string?> ResolveVersionJsonAsync(string gameVersion, string cacheDir, CancellationToken ct = default)
    {
        Directory.CreateDirectory(cacheDir);
        string manifestPath = Path.Combine(cacheDir, "version_manifest_v2.json");
        if (!Downloader.FileValid(manifestPath))
            await Downloader.DownloadAsync(ManifestUrl, manifestPath, null, ct);

        using var manifest = JsonDocument.Parse(await File.ReadAllTextAsync(manifestPath, ct));
        string? versionUrl = null;
        foreach (var v in manifest.RootElement.GetProperty("versions").EnumerateArray())
        {
            if (v.GetProperty("id").GetString() == gameVersion)
            {
                versionUrl = v.GetProperty("url").GetString();
                break;
            }
        }
        if (versionUrl == null) throw new Exception($"Версия {gameVersion} не найдена в манифесте.");

        string versionDir = Path.Combine(cacheDir, gameVersion);
        Directory.CreateDirectory(versionDir);
        string versionJsonPath = Path.Combine(versionDir, $"{gameVersion}.json");
        if (!Downloader.FileValid(versionJsonPath))
            await Downloader.DownloadAsync(versionUrl, versionJsonPath, null, ct);
        return versionJsonPath;
    }

    public static string? GetClientJarUrl(JsonElement versionJson)
        => versionJson.GetProperty("downloads").GetProperty("client").GetProperty("url").GetString();

    public static string? GetAssetIndexInfo(JsonElement versionJson, out string indexId, out string indexUrl, out string? indexSha1)
    {
        var ai = versionJson.GetProperty("assetIndex");
        indexId = ai.GetProperty("id").GetString() ?? "unknown";
        indexUrl = ai.GetProperty("url").GetString() ?? "";
        indexSha1 = ai.TryGetProperty("sha1", out var s) ? s.GetString() : null;
        return indexId;
    }

    /// <summary>Разрешает библиотеки для текущей ОС (Windows). Возвращает (путь, url).</summary>
    public static List<LibSpec> ResolveLibraries(JsonElement versionJson, string os)
    {
        var result = new List<LibSpec>();
        foreach (var lib in versionJson.GetProperty("libraries").EnumerateArray())
        {
            bool allow = RulesAllow(lib, os);
            if (!allow) continue;

            string? natives = null;
            if (lib.TryGetProperty("natives", out var nativesObj) && nativesObj.TryGetProperty("windows", out var nw))
                natives = nw.GetString();

            var spec = new LibSpec();
            if (lib.TryGetProperty("downloads", out var downloads))
            {
                if (downloads.TryGetProperty("artifact", out var art))
                {
                    spec.Path = art.GetProperty("path").GetString() ?? "";
                    spec.Url = art.GetProperty("url").GetString() ?? "";
                    spec.Name = lib.GetProperty("name").GetString() ?? "";
                }
                if (natives != null && downloads.TryGetProperty("classifiers", out var cls) &&
                    cls.TryGetProperty(natives, out var nativeArt))
                {
                    spec.Path = nativeArt.GetProperty("path").GetString() ?? "";
                    spec.Url = nativeArt.GetProperty("url").GetString() ?? "";
                    spec.Name = lib.GetProperty("name").GetString() ?? "";
                    spec.IsNative = true;
                }
            }
            else if (lib.TryGetProperty("url", out var urlEl))
            {
                // legacy: имя → путь
                string name = lib.GetProperty("name").GetString() ?? "";
                string path = MavenToPath(name);
                spec.Name = name;
                spec.Path = path;
                spec.Url = urlEl.GetString() + path;
            }

            if (string.IsNullOrEmpty(spec.Path))
            {
                spec.IsEmpty = true;
            }
            result.Add(spec);
        }
        return result;
    }

    public static List<string> ResolveJvmArgs(JsonElement versionJson, string os)
    {
        var args = new List<string>();
        if (!versionJson.TryGetProperty("arguments", out var argsEl) || !argsEl.TryGetProperty("jvm", out var jvm))
            return args;
        foreach (var item in jvm.EnumerateArray())
        {
            if (item.ValueKind == JsonValueKind.String)
            {
                args.Add(item.GetString()!);
            }
            else if (item.TryGetProperty("rules", out _))
            {
                if (RulesAllow(item, os))
                {
                    var val = item.GetProperty("value");
                    if (val.ValueKind == JsonValueKind.String) args.Add(val.GetString()!);
                    else foreach (var s in val.EnumerateArray()) args.Add(s.GetString()!);
                }
            }
        }
        return args;
    }

    /// <summary>Правила Mojang: если у элемента есть rules, он разрешён только если применилось allow-правило.
    /// Неприменившееся правило пропускается; если ни одно не применилось — элемент НЕ включается.</summary>
    private static bool RulesAllow(JsonElement element, string os)
    {
        if (!element.TryGetProperty("rules", out var rules)) return true;
        foreach (var rule in rules.EnumerateArray())
        {
            string action = rule.GetProperty("action").GetString() ?? "allow";
            bool applies = true;
            if (rule.TryGetProperty("os", out var osRule))
            {
                if (osRule.TryGetProperty("name", out var name))
                    applies = name.GetString() == os;
                if (applies && osRule.TryGetProperty("arch", out var arch))
                    applies = arch.GetString() == CurrentArch();
            }
            if (applies)
                return action == "allow";
        }
        return false;
    }

    private static string CurrentArch()
        => Environment.Is64BitOperatingSystem ? "x86_64" : "x86";

    public static string MavenToPath(string name)
    {
        string[] parts = name.Split(':');
        if (parts.Length < 3) return name.Replace(':', '/');
        string group = parts[0].Replace('.', '/');
        string artifact = parts[1];
        string version = parts[2];
        string classifier = parts.Length > 3 ? "-" + parts[3] : "";
        return $"{group}/{artifact}/{version}/{artifact}-{version}{classifier}.jar";
    }
}
