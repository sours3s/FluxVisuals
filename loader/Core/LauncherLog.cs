using System.Diagnostics;
using System.IO;

namespace FluxVisualsLoader.Core;

/// <summary>Простой файловый логгер лаунчера (dist/launcher.log) для диагностики запуска.</summary>
public static class LauncherLog
{
    private static readonly object Lock = new();
    private static string? _logPath;

    public static string LogPath
    {
        get
        {
            if (_logPath != null) return _logPath;
            // Используем папку на рабочем столе для логов (надёжнее в single-file режиме)
            string desktop = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
            _logPath = Path.Combine(desktop, "FluxVisualsLauncher.log");
            return _logPath;
        }
    }

    public static void Info(string message)
    {
        Write("INFO", message);
    }

    public static void Error(string message, Exception? ex = null)
    {
        Write("ERROR", message + (ex != null ? "\n" + ex : ""));
    }

    public static void Warn(string message)
    {
        Write("WARN", message);
    }

    private static void Write(string level, string message)
    {
        try
        {
            lock (Lock)
            {
                File.AppendAllText(LogPath, $"[{DateTime.Now:HH:mm:ss}] [{level}] {message}\n");
            }
        }
        catch { }
    }
}
