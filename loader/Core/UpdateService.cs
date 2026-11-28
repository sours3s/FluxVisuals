using System.Diagnostics;
using System.Net.Http;
using System.Reflection;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace FluxVisualsLoader.Core;

/// <summary>Автообновление лоадера: проверка GitHub Releases, скачивание и замена exe.</summary>
public static class UpdateService
{
    public static Version CurrentVersion =>
        Assembly.GetExecutingAssembly().GetName().Version ?? new Version(0, 0, 0);

    /// <summary>Временная папка обновления. Не лежит рядом с exe — рабочий стол остаётся чистым.</summary>
    public static string UpdateDir
    {
        get
        {
            string dir = Path.Combine(Path.GetTempPath(), "FluxVisuals", "update");
            Directory.CreateDirectory(dir);
            return dir;
        }
    }

    /// <summary>Версия и ссылка на лоадер берутся из админ-панели (/api/mod/version).
    /// Возвращает null, если обновлений нет или сервер недоступен.</summary>
    public static async Task<UpdateInfo?> CheckAsync(string authServerUrl, CancellationToken ct = default)
    {
        try
        {
            var info = await AuthService.GetModVersionAsync(authServerUrl);
            if (info == null || string.IsNullOrWhiteSpace(info.LoaderDownloadUrl) ||
                string.IsNullOrWhiteSpace(info.LoaderVersion) ||
                !Version.TryParse(info.LoaderVersion, out var version))
            {
                return null;
            }

            if (version <= CurrentVersion) return null;
            return new UpdateInfo(version, info.LoaderVersion, info.LoaderDownloadUrl);
        }
        catch (Exception ex)
        {
            LauncherLog.Warn($"Update check failed: {ex.Message}");
            return null;
        }
    }

    public static Task DownloadAsync(string url, string dest, Action<long, long?>? progress = null, CancellationToken ct = default)
        => Downloader.DownloadAsync(url, dest, progress, ct);

    /// <summary>Скачивает новый exe во временную папку и запускает скрытый updater.bat оттуда же:
    /// ждёт выхода текущего процесса, заменяет exe и перезапускает его. config.json не трогаем.</summary>
    public static void ApplyAndRestart(string currentExePath, string newExePath)
    {
        string dir = UpdateDir;
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
