using System.Diagnostics;
using System.Net.Http;
using System.Reflection;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace FluxVisualsLoader.Core;

/// <summary>Автообновление лоадера: проверка GitHub Releases, скачивание и замена exe.</summary>
public static class UpdateService
{
    /// <summary>URL проверки обновлений собирается из частей, чтобы не светить ссылку в бинарнике.</summary>
    public static string DefaultUpdateCheckUrl => Defaults.UpdateCheckUrl;

    private static readonly HttpClient Http = new()
    {
        // Короткий таймаут: если GitHub недоступен — просто запускаемся со старой версией.
        Timeout = TimeSpan.FromSeconds(15)
    };

    static UpdateService()
    {
        Http.DefaultRequestHeaders.UserAgent.ParseAdd("FluxVisualsLoader/1.0");
        Http.DefaultRequestHeaders.Accept.ParseAdd("application/vnd.github+json");
    }

    public static Version CurrentVersion =>
        Assembly.GetExecutingAssembly().GetName().Version ?? new Version(0, 0, 0);

    /// <summary>Находит последний релиз с ассетом FluxVisualsLoader.exe и сравнивает с текущей версией.
    /// Возвращает null, если обновлений нет (или не удалось проверить).</summary>
    public static async Task<UpdateInfo?> CheckAsync(string updateCheckUrl, CancellationToken ct = default)
    {
        try
        {
            using var resp = await Http.GetAsync(updateCheckUrl, ct);
            if (!resp.IsSuccessStatusCode) return null;
            string json = await resp.Content.ReadAsStringAsync(ct);
            using var doc = JsonDocument.Parse(json);

            Version? best = null;
            string? bestUrl = null;
            string? bestTag = null;

            foreach (var rel in doc.RootElement.EnumerateArray())
            {
                // Тег релиза: v1.0.1.loader (или v1.0.0.0.loader). Версия — первая цифровая часть.
                string tag = rel.TryGetProperty("tag_name", out var t) ? t.GetString() ?? "" : "";
                var m = Regex.Match(tag, @"v?(\d+(?:\.\d+){1,3})");
                if (!m.Success || !Version.TryParse(m.Groups[1].Value, out var ver)) continue;

                // Интересует только релиз, где лежит FluxVisualsLoader.exe.
                if (!rel.TryGetProperty("assets", out var assets)) continue;
                string? url = null;
                foreach (var a in assets.EnumerateArray())
                {
                    string name = a.TryGetProperty("name", out var n) ? n.GetString() ?? "" : "";
                    if (!name.Equals("FluxVisualsLoader.exe", StringComparison.OrdinalIgnoreCase)) continue;
                    url = a.TryGetProperty("browser_download_url", out var u) ? u.GetString() : null;
                    break;
                }
                if (url == null) continue;

                if (best == null || ver > best)
                {
                    best = ver;
                    bestUrl = url;
                    bestTag = tag;
                }
            }

            if (best == null || bestUrl == null) return null;
            if (best <= CurrentVersion) return null; // уже последняя версия
            return new UpdateInfo(best, bestTag ?? "", bestUrl);
        }
        catch (Exception ex)
        {
            LauncherLog.Warn($"Update check failed: {ex.Message}");
            return null;
        }
    }

    public static Task DownloadAsync(string url, string dest, Action<long, long?>? progress = null, CancellationToken ct = default)
        => Downloader.DownloadAsync(url, dest, progress, ct);

    /// <summary>Скачивает новый exe рядом с текущим и запускает скрытый updater.bat:
    /// ждёт выхода текущего процесса, заменяет exe и перезапускает его. config.json не трогаем.</summary>
    public static void ApplyAndRestart(string currentExePath, string newExePath)
    {
        string dir = Path.GetDirectoryName(currentExePath)!;
        string exeName = Path.GetFileName(currentExePath);
        string batPath = Path.Combine(dir, "updater.bat");

        string bat =
            "@echo off\r\n" +
            "title FluxVisuals Updater\r\n" +
            "timeout /t 2 /nobreak >nul\r\n" +
            "set \"DST=" + currentExePath + "\"\r\n" +
            "set \"NEW=" + newExePath + "\"\r\n" +
            "set /a n=0\r\n" +
            ":retry\r\n" +
            "move /Y \"%NEW%\" \"%DST%\" >nul 2>&1\r\n" +
            "if not errorlevel 1 goto done\r\n" +
            "taskkill /F /IM " + exeName + " >nul 2>&1\r\n" +
            "timeout /t 1 /nobreak >nul\r\n" +
            "set /a n+=1\r\n" +
            "if %n% lss 30 goto retry\r\n" +
            ":done\r\n" +
            "start \"\" \"%DST%\"\r\n" +
            "del \"%~f0\"\r\n";

        try { File.WriteAllText(batPath, bat); }
        catch (Exception ex)
        {
            LauncherLog.Error($"Failed to write updater.bat: {ex.Message}");
            return;
        }

        Process.Start(new ProcessStartInfo(batPath)
        {
            UseShellExecute = true,
            WindowStyle = ProcessWindowStyle.Hidden,
            WorkingDirectory = dir
        });
    }
}

public record UpdateInfo(Version Version, string Tag, string DownloadUrl);
