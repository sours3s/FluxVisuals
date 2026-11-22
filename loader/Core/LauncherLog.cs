using System.IO;

namespace FluxVisualsLoader.Core;

/// <summary>Файловый логгер лаунчера. Пишет только в %LOCALAPPDATA%\FluxVisuals\logs —
/// рядом с exe и на Рабочем столе не создаётся ни одного файла.</summary>
public static class LauncherLog
{
    private const long MaxLogBytes = 1024 * 1024;
    private static readonly object Lock = new();
    private static string? _logPath;

    /// <summary>Каталог логов лоадера (создаётся при первом обращении).</summary>
    public static string LogDir
    {
        get
        {
            string dir = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "FluxVisuals", "logs");
            Directory.CreateDirectory(dir);
            return dir;
        }
    }

    public static string LogPath
    {
        get
        {
            if (_logPath != null) return _logPath;
            _logPath = Path.Combine(LogDir, "FluxVisualsLauncher.log");
            return _logPath;
        }
    }

    public static void Info(string message) => Write("INFO", message);
    public static void Error(string message, Exception? ex = null) => Write("ERROR", message + (ex != null ? "\n" + ex : ""));
    public static void Warn(string message) => Write("WARN", message);

    private static void Write(string level, string message)
    {
        try
        {
            lock (Lock)
            {
                RotateIfNeeded();
                File.AppendAllText(LogPath, $"[{DateTime.Now:HH:mm:ss}] [{level}] {message}\n");
            }
        }
        catch { }
    }

    private static void RotateIfNeeded()
    {
        try
        {
            var fi = new FileInfo(LogPath);
            if (!fi.Exists || fi.Length < MaxLogBytes) return;
            string archive = Path.ChangeExtension(LogPath, ".1.log");
            if (File.Exists(archive)) File.Delete(archive);
            File.Move(LogPath, archive);
        }
        catch { }
    }
}
