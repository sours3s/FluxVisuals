using System.Text.Json.Serialization;

namespace FluxVisualsLoader.Core;

/// <summary>Запись файла в «Моих файлах». Имена в camelCase — их читает JS-интерфейс.</summary>
public class ModEntry
{
    [JsonPropertyName("name")] public string Name { get; set; } = "";
    [JsonPropertyName("fileName")] public string FileName { get; set; } = "";
    [JsonPropertyName("path")] public string Path { get; set; } = "";
    [JsonPropertyName("enabled")] public bool Enabled { get; set; } = true;
    [JsonPropertyName("size")] public long Size { get; set; }
    [JsonPropertyName("type")] public string Type { get; set; } = "mod"; // mod | shader | resourcepack
    [JsonPropertyName("iconUrl")] public string IconUrl { get; set; } = "";
}

/// <summary>
/// Список файлов в модах/шейдерах/ресурспаках: включение/выключение переименованием в .disabled.
/// Работает и для файлов (.jar/.zip), и для распакованных папок шейдеров/РП.
/// </summary>
public static class ModManager
{
    public static List<ModEntry> ListFiles(string gameDir, IReadOnlyDictionary<string, string>? icons = null)
    {
        var result = new List<ModEntry>();
        AddFolder(result, Path.Combine(gameDir, "mods"), "mod", icons);
        AddFolder(result, Path.Combine(gameDir, "shaderpacks"), "shader", icons);
        AddFolder(result, Path.Combine(gameDir, "resourcepacks"), "resourcepack", icons);
        return result;
    }

    private static void AddFolder(List<ModEntry> result, string dir, string type, IReadOnlyDictionary<string, string>? icons)
    {
        if (!Directory.Exists(dir)) return;

        foreach (string file in Directory.EnumerateFiles(dir))
        {
            string fileName = Path.GetFileName(file);
            bool enabled = true;
            string display = fileName;
            if (fileName.EndsWith(".disabled"))
            {
                enabled = false;
                display = fileName[..^9];
            }

            // Моды — только .jar. Шейдеры/РП — .zip (и .mcpack).
            if (type == "mod" && !display.EndsWith(".jar", StringComparison.OrdinalIgnoreCase)) continue;
            if (type != "mod" && !display.EndsWith(".zip", StringComparison.OrdinalIgnoreCase)
                              && !display.EndsWith(".mcpack", StringComparison.OrdinalIgnoreCase)) continue;

            // Скрываем сам FluxVisuals из списка модов
            if (type == "mod" && display.Contains("fluxvisuals", StringComparison.OrdinalIgnoreCase)) continue;

            string icon = "";
            icons?.TryGetValue(fileName, out icon);
            result.Add(new ModEntry
            {
                Name = DisplayName(display, type),
                FileName = display,
                Path = file,
                Enabled = enabled,
                Size = new FileInfo(file).Length,
                Type = type,
                IconUrl = icon ?? "",
            });
        }

        // Распакованные папки шейдеров/ресурспаков
        if (type != "mod")
        {
            foreach (string d in Directory.EnumerateDirectories(dir))
            {
                string dirName = Path.GetFileName(d);
                bool enabled = !dirName.EndsWith(".disabled");
                string display = enabled ? dirName : dirName[..^9];
                if (display.StartsWith('.')) continue; // служебные папки
                string dicon = "";
                icons?.TryGetValue(display, out dicon);
                result.Add(new ModEntry
                {
                    Name = display,
                    FileName = display,
                    Path = d,
                    Enabled = enabled,
                    Size = 0,
                    Type = type,
                    IconUrl = dicon ?? "",
                });
            }
        }
    }

    private static string DisplayName(string display, string type)
    {
        if (type == "mod" && display.EndsWith(".jar", StringComparison.OrdinalIgnoreCase)
            && !display.StartsWith("fluxvisuals-", StringComparison.OrdinalIgnoreCase))
            return display[..^4];
        return Path.GetFileNameWithoutExtension(display);
    }

    public static void Toggle(string path, bool enable)
    {
        bool isDir = Directory.Exists(path);
        if (enable && path.EndsWith(".disabled"))
        {
            string dest = path[..^9];
            if (isDir) Directory.Move(path, dest); else File.Move(path, dest);
        }
        else if (!enable && !path.EndsWith(".disabled"))
        {
            if (isDir) Directory.Move(path, path + ".disabled"); else File.Move(path, path + ".disabled");
        }
    }

    public static void Remove(string path)
    {
        if (Directory.Exists(path)) Directory.Delete(path, true);
        else if (File.Exists(path)) File.Delete(path);
    }

    public static void Add(string srcFile, string gameDir)
    {
        string modsDir = Path.Combine(gameDir, "mods");
        Directory.CreateDirectory(modsDir);
        string dest = Path.Combine(modsDir, Path.GetFileName(srcFile));
        File.Copy(srcFile, dest, true);
    }
}
