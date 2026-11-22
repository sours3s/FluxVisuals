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
    [JsonPropertyName("projectUrl")] public string ProjectUrl { get; set; } = "";
    [JsonPropertyName("description")] public string Description { get; set; } = "";
    [JsonPropertyName("source")] public string Source { get; set; } = "";
    [JsonPropertyName("projectId")] public string ProjectId { get; set; } = "";
}

/// <summary>
/// Список файлов в модах/шейдерах/ресурспаках: включение/выключение переименованием в .disabled.
/// Работает и для файлов (.jar/.zip), и для распакованных папок шейдеров/РП.
/// </summary>
public static class ModManager
{
    public static List<ModEntry> ListFiles(string gameDir, IReadOnlyDictionary<string, string>? icons = null,
        IReadOnlyDictionary<string, string>? urls = null, IReadOnlyDictionary<string, string>? descriptions = null,
        IReadOnlyDictionary<string, string>? sources = null, IReadOnlyDictionary<string, string>? projectIds = null)
    {
        var result = new List<ModEntry>();
        AddFolder(result, Path.Combine(gameDir, "mods"), "mod", icons, urls, descriptions, sources, projectIds);
        AddFolder(result, Path.Combine(gameDir, "shaderpacks"), "shader", icons, urls, descriptions, sources, projectIds);
        AddFolder(result, Path.Combine(gameDir, "resourcepacks"), "resourcepack", icons, urls, descriptions, sources, projectIds);
        return result;
    }

    private static void AddFolder(List<ModEntry> result, string dir, string type,
        IReadOnlyDictionary<string, string>? icons, IReadOnlyDictionary<string, string>? urls,
        IReadOnlyDictionary<string, string>? descriptions, IReadOnlyDictionary<string, string>? sources,
        IReadOnlyDictionary<string, string>? projectIds)
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

            // Метаданные сохраняются без суффикса .disabled — при выключении/включении
            // иконка и ссылка не должны пропадать.
            Lookup(icons, fileName, display, out string? icon);
            Lookup(urls, fileName, display, out string? url);
            Lookup(descriptions, fileName, display, out string? description);
            Lookup(sources, fileName, display, out string? source);
            Lookup(projectIds, fileName, display, out string? projectId);
            result.Add(new ModEntry
            {
                Name = DisplayName(display, type),
                FileName = display,
                Path = file,
                Enabled = enabled,
                Size = new FileInfo(file).Length,
                Type = type,
                IconUrl = icon ?? "",
                ProjectUrl = url ?? "",
                Description = description ?? "",
                Source = source ?? "",
                ProjectId = projectId ?? "",
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
                Lookup(icons, dirName, display, out string? dicon);
                Lookup(urls, dirName, display, out string? durl);
                Lookup(descriptions, dirName, display, out string? ddesc);
                Lookup(sources, dirName, display, out string? dsource);
                Lookup(projectIds, dirName, display, out string? dprojectId);
                result.Add(new ModEntry
                {
                    Name = display,
                    FileName = display,
                    Path = d,
                    Enabled = enabled,
                    Size = 0,
                    Type = type,
                    IconUrl = dicon ?? "",
                    ProjectUrl = durl ?? "",
                    Description = ddesc ?? "",
                    Source = dsource ?? "",
                    ProjectId = dprojectId ?? "",
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

    private static void Lookup(IReadOnlyDictionary<string, string>? dict, string rawName, string display,
        out string? value)
    {
        value = null;
        if (dict == null) return;
        if (dict.TryGetValue(rawName, out var raw) && !string.IsNullOrWhiteSpace(raw)) { value = raw; return; }
        if (dict.TryGetValue(display, out var normal) && !string.IsNullOrWhiteSpace(normal)) value = normal;
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

    public static void Add(string srcFile, string gameDir, string type = "mod")
    {
        string subDir = type switch
        {
            "shader" => "shaderpacks",
            "resourcepack" => "resourcepacks",
            _ => "mods",
        };
        string destDir = Path.Combine(gameDir, subDir);
        Directory.CreateDirectory(destDir);
        string dest = Path.Combine(destDir, Path.GetFileName(srcFile));
        File.Copy(srcFile, dest, true);
    }

    /// <summary>Добавить распакованный шейдер/ресурспак целиком.</summary>
    public static void AddFolder(string srcDir, string gameDir, string type)
    {
        string subDir = type == "shader" ? "shaderpacks" : "resourcepacks";
        string name = Path.GetFileName(srcDir.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar));
        string dest = Path.Combine(gameDir, subDir, name);
        if (Directory.Exists(dest)) Directory.Delete(dest, true);
        Directory.CreateDirectory(Path.GetDirectoryName(dest)!);
        CopyTree(srcDir, dest);
    }

    private static void CopyTree(string src, string dest)
    {
        Directory.CreateDirectory(dest);
        foreach (string file in Directory.EnumerateFiles(src))
            File.Copy(file, Path.Combine(dest, Path.GetFileName(file)), true);
        foreach (string dir in Directory.EnumerateDirectories(src))
            CopyTree(dir, Path.Combine(dest, Path.GetFileName(dir)));
    }
}
