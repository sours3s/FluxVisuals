using System.Diagnostics;
using System.IO.Compression;

namespace FluxVisualsLoader.Core;

/// <summary>Поиск или скачивание Java 21 для запуска игры.</summary>
public static class JavaResolver
{
    private static readonly string[] Candidates =
    {
        @"C:\Program Files\Java\jdk-21\bin\java.exe",
        @"C:\Program Files\Java\jdk-22\bin\java.exe",
        @"C:\Program Files\Java\jdk-23\bin\java.exe",
        @"C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe",
        @"C:\Program Files\Microsoft\jdk-21*\bin\java.exe",
        @"C:\Program Files\Zulu\zulu-21*\bin\java.exe",
    };

    public static string? FindJava(string? configured)
    {
        if (!string.IsNullOrWhiteSpace(configured) && File.Exists(configured))
            return configured;

        foreach (string pattern in Candidates)
        {
            if (!pattern.Contains('*'))
            {
                if (File.Exists(pattern)) return pattern;
            }
            else
            {
                string dir = Path.GetDirectoryName(pattern)!;
                if (Directory.Exists(dir))
                {
                    var match = Directory.EnumerateFiles(dir, Path.GetFileName(pattern)).FirstOrDefault();
                    if (match != null) return match;
                }
            }
        }

        string? javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrWhiteSpace(javaHome))
        {
            string j = Path.Combine(javaHome, "bin", "java.exe");
            if (File.Exists(j)) return j;
        }
        return null;
    }

    /// <summary>Скачивает JDK21 (Adoptium) в gameDir/java/java21 и возвращает путь к java.exe.</summary>
    public static async Task<string> DownloadJavaAsync(string gameDir, CancellationToken ct)
    {
        string javaDir = Path.Combine(gameDir, "java", "java21");
        string javaExe = Path.Combine(javaDir, "bin", "java.exe");
        if (File.Exists(javaExe)) return javaExe;

        Directory.CreateDirectory(Path.GetDirectoryName(javaDir)!);
        string zipPath = Path.Combine(Path.GetTempPath(), "fluxvisuals-jdk21.zip");
        const string url = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse";

        await Downloader.DownloadAsync(url, zipPath, null, ct);
        if (File.Exists(javaDir))
        {
            try { Directory.Delete(javaDir, true); } catch { }
        }
        string extractDir = Path.Combine(Path.GetDirectoryName(javaDir)!, "_extract");
        if (Directory.Exists(extractDir)) { try { Directory.Delete(extractDir, true); } catch { } }
        Directory.CreateDirectory(extractDir);
        ZipFile.ExtractToDirectory(zipPath, extractDir);
        // найти вложенную папку jdk-*
        string inner = Directory.EnumerateDirectories(extractDir).FirstOrDefault() ?? extractDir;
        Directory.CreateDirectory(javaDir);
        CopyDir(inner, javaDir);
        try { Directory.Delete(extractDir, true); } catch { }
        return javaExe;
    }

    public static bool IsJavaUsable(string javaExe)
    {
        try
        {
            var psi = new ProcessStartInfo(javaExe, "-version") { UseShellExecute = false, RedirectStandardError = true, RedirectStandardOutput = true };
            using var p = Process.Start(psi);
            if (p == null) return false;
            string outText = p.StandardError.ReadToEnd() + p.StandardOutput.ReadToEnd();
            p.WaitForExit(8000);
            return outText.Contains("version \"21");
        }
        catch { return false; }
    }

    private static void CopyDir(string src, string dst)
    {
        foreach (string dir in Directory.EnumerateDirectories(src, "*", SearchOption.AllDirectories))
            Directory.CreateDirectory(dir.Replace(src, dst));
        foreach (string file in Directory.EnumerateFiles(src, "*", SearchOption.AllDirectories))
            File.Copy(file, file.Replace(src, dst), true);
    }
}
