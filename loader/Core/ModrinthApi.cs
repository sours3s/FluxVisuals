using System.Text.Json;
using System.Text.Json.Serialization;

namespace FluxVisualsLoader.Core;

/// <summary>Элемент магазина. Имена в camelCase — их читает JS-интерфейс.</summary>
public class ModItem
{
    [JsonPropertyName("id")] public string Id { get; set; } = "";
    [JsonPropertyName("slug")] public string Slug { get; set; } = "";
    [JsonPropertyName("projectUrl")] public string ProjectUrl { get; set; } = "";
    [JsonPropertyName("title")] public string Title { get; set; } = "";
    [JsonPropertyName("description")] public string Description { get; set; } = "";
    [JsonPropertyName("author")] public string Author { get; set; } = "";
    [JsonPropertyName("downloads")] public int Downloads { get; set; }
    [JsonPropertyName("iconUrl")] public string IconUrl { get; set; } = "";
    [JsonPropertyName("versionId")] public string? VersionId { get; set; }
    [JsonPropertyName("versionNumber")] public string? VersionNumber { get; set; }
    [JsonPropertyName("fileUrl")] public string? FileUrl { get; set; }
    [JsonPropertyName("fileName")] public string? FileName { get; set; }
    [JsonPropertyName("fileSize")] public long FileSize { get; set; }
}

/// <summary>Страница каталога: элементы + всего и текущий offset.</summary>
public class SearchPage
{
    [JsonPropertyName("items")] public List<ModItem> Items { get; set; } = new();
    [JsonPropertyName("total")] public int Total { get; set; }
    [JsonPropertyName("offset")] public int Offset { get; set; }
}

/// <summary>Категория каталога (для чипсов в UI).</summary>
public class CategoryInfo
{
    [JsonPropertyName("id")] public string Id { get; set; } = "";
    [JsonPropertyName("name")] public string Name { get; set; } = "";
    [JsonPropertyName("icon")] public string Icon { get; set; } = "";
    [JsonPropertyName("type")] public string Type { get; set; } = ""; // mod | resourcepack | shader
}

/// <summary>Доступная версия мода для установки.</summary>
public class VersionInfo
{
    [JsonPropertyName("versionId")] public string VersionId { get; set; } = "";
    [JsonPropertyName("versionNumber")] public string VersionNumber { get; set; } = "";
    [JsonPropertyName("gameVersion")] public string GameVersion { get; set; } = "";
    [JsonPropertyName("date")] public string Date { get; set; } = "";
    [JsonPropertyName("fileUrl")] public string FileUrl { get; set; } = "";
    [JsonPropertyName("fileName")] public string FileName { get; set; } = "";
    [JsonPropertyName("fileSize")] public long FileSize { get; set; }
    [JsonPropertyName("releaseType")] public string ReleaseType { get; set; } = "release";
}

/// <summary>Каталог Modrinth: категории, фильтры, версии, установка.</summary>
public static class ModrinthApi
{
    private const string Base = "https://api.modrinth.com/v2";
    private const int PageSize = 50;

    private static List<CategoryInfo>? _categories;
    private static readonly SemaphoreSlim CatLock = new(1, 1);

    public static string ProjectType(string type) => type switch
    {
        "resourcepack" => "resourcepack",
        "shader" => "shader",
        _ => "mod",
    };

    public static async Task<SearchPage> SearchAsync(string query, string type, string category, string loader, string sort, int offset, CancellationToken ct = default)
    {
        string facets = BuildFacets(type, category, loader);
        string index = sort switch
        {
            "downloads" => "downloads",
            "updated" => "updated",
            "newest" => "newest",
            "follows" => "follows",
            _ => "relevance",
        };
        string url = $"{Base}/search?limit={PageSize}&offset={offset}&facets={Uri.EscapeDataString(facets)}&index={index}";
        if (!string.IsNullOrWhiteSpace(query))
            url += $"&query={Uri.EscapeDataString(query)}";

        // Фильтр версии игры — только для модов.
        // Шейдеры/ресурспаки ищем без него: они не привязаны к версии.
        if (type == "mod")
        {
            string gameVersion = "1.21.11";
            url += $"&game_versions=[\"{gameVersion}\"]";
        }

        var page = new SearchPage { Offset = offset };
        string? json = await Downloader.GetStringAsync(url, ct: ct);
        if (json == null) return page;
        using var doc = JsonDocument.Parse(json);
        page.Total = doc.RootElement.GetProperty("total_hits").GetInt32();
        foreach (var h in doc.RootElement.GetProperty("hits").EnumerateArray())
        {
            string slug = h.TryGetProperty("slug", out var s) ? s.GetString() ?? "" : "";
            page.Items.Add(new ModItem
            {
                Id = h.GetProperty("project_id").GetString() ?? "",
                Slug = slug,
                ProjectUrl = string.IsNullOrEmpty(slug) ? "" : $"https://modrinth.com/project/{slug}",
                Title = h.GetProperty("title").GetString() ?? "",
                Description = Truncate(h.GetProperty("description").GetString() ?? "", 120),
                Author = h.TryGetProperty("author", out var a) ? a.GetString() ?? "" : "",
                Downloads = h.TryGetProperty("downloads", out var d) ? d.GetInt32() : 0,
                IconUrl = h.TryGetProperty("icon_url", out var i) && i.ValueKind == JsonValueKind.String ? i.GetString() ?? "" : "",
            });
        }
        return page;
    }

    public static async Task<List<CategoryInfo>> FetchCategoriesAsync(CancellationToken ct = default)
    {
        if (_categories != null) return _categories;
        await CatLock.WaitAsync(ct);
        try
        {
            if (_categories != null) return _categories;
            var list = new List<CategoryInfo>();
            string? json = await Downloader.GetStringAsync($"{Base}/tag/category", ct: ct);
            if (json != null)
            {
                using var doc = JsonDocument.Parse(json);
                foreach (var c in doc.RootElement.EnumerateArray())
                {
                    string type = c.GetProperty("project_type").GetString() ?? "";
                    if (type != "mod" && type != "resourcepack" && type != "shader") continue;
                    string name = c.GetProperty("name").GetString() ?? "";
                    list.Add(new CategoryInfo
                    {
                        Id = name,
                        Name = name,
                        Icon = c.TryGetProperty("icon", out var ic) ? ic.GetString() ?? "" : "",
                        Type = type,
                    });
                }
            }
            _categories = list;
            return list;
        }
        finally { CatLock.Release(); }
    }

    /// <summary>Версии проекта для выбранной версии игры + загрузчика, свежие сверху.</summary>
    public static async Task<List<VersionInfo>> VersionsAsync(string projectId, string gameVersion, string loader, CancellationToken ct = default)
    {
        var result = new List<VersionInfo>();
        string? json = await Downloader.GetStringAsync($"{Base}/project/{projectId}/version", ct: ct);
        if (json == null) return result;
        using var doc = JsonDocument.Parse(json);
        foreach (var v in doc.RootElement.EnumerateArray())
        {
            bool gvMatch = v.GetProperty("game_versions").EnumerateArray().Any(g => g.GetString() == gameVersion);
            bool ldMatch = v.GetProperty("loaders").EnumerateArray().Any(l => l.GetString() == loader);
            if (!gvMatch || !ldMatch) continue;
            var files = v.GetProperty("files");
            if (files.GetArrayLength() == 0) continue;
            var file = files[0];
            result.Add(new VersionInfo
            {
                VersionId = v.GetProperty("id").GetString(),
                VersionNumber = v.TryGetProperty("version_number", out var vn) ? vn.GetString() : "",
                GameVersion = gameVersion,
                Date = v.TryGetProperty("date_published", out var dp) ? dp.GetString() ?? "" : "",
                FileUrl = file.GetProperty("url").GetString(),
                FileName = file.GetProperty("filename").GetString(),
                FileSize = file.TryGetProperty("size", out var s) ? s.GetInt64() : 0,
                ReleaseType = v.TryGetProperty("version_type", out var vt) ? vt.GetString() ?? "release" : "release",
            });
        }
        // свежие сверху, release优先
        result.Sort((a, b) => {
            int ta = a.ReleaseType == "release" ? 0 : a.ReleaseType == "beta" ? 1 : 2;
            int tb = b.ReleaseType == "release" ? 0 : b.ReleaseType == "beta" ? 1 : 2;
            if (ta != tb) return ta - tb;
            return string.Compare(b.Date, a.Date, StringComparison.Ordinal);
        });
        return result;
    }

    public static async Task<ModItem?> ResolveLatestAsync(string projectId, string gameVersion, string loader, CancellationToken ct = default)
    {
        string url = $"{Base}/project/{projectId}/version?game_versions=[\"{gameVersion}\"]&loaders=[\"{loader}\"]";
        string? json = await Downloader.GetStringAsync(url, ct: ct);
        if (json == null) return null;
        using var doc = JsonDocument.Parse(json);
        foreach (var v in doc.RootElement.EnumerateArray())
        {
            var files = v.GetProperty("files");
            if (files.GetArrayLength() == 0) continue;
            var file = files[0];
            return new ModItem
            {
                Id = projectId,
                Title = v.TryGetProperty("name", out var n) ? n.GetString() ?? "" : "",
                VersionId = v.GetProperty("id").GetString(),
                VersionNumber = v.TryGetProperty("version_number", out var vn) ? vn.GetString() : "",
                FileUrl = file.GetProperty("url").GetString(),
                FileName = file.GetProperty("filename").GetString(),
                FileSize = file.TryGetProperty("size", out var s) ? s.GetInt64() : 0,
            };
        }
        return null;
    }

    /// <summary>Последний релиз проекта БЕЗ фильтра версии игры/загрузчика (для шейдеров и ресурспаков).</summary>
    public static async Task<ModItem?> ResolveLatestAnyAsync(string projectId, CancellationToken ct = default)
    {
        string? json = await Downloader.GetStringAsync($"{Base}/project/{projectId}/version", ct: ct);
        if (json == null) return null;
        using var doc = JsonDocument.Parse(json);
        ModItem? fallback = null;
        foreach (var v in doc.RootElement.EnumerateArray())
        {
            // предпочитаем release; если их нет — отдаём любую (иначе «нет версии» на бета-проектах)
            string releaseType = v.TryGetProperty("version_type", out var vt) ? vt.GetString() ?? "release" : "release";
            var files = v.GetProperty("files");
            if (files.GetArrayLength() == 0) continue;
            // берём primary-файл, иначе первый
            var file = files[0];
            foreach (var f in files.EnumerateArray())
                if (f.TryGetProperty("primary", out var p) && p.ValueKind == JsonValueKind.True) { file = f; break; }
            var item = new ModItem
            {
                Id = projectId,
                Title = v.TryGetProperty("name", out var n) ? n.GetString() ?? "" : "",
                VersionId = v.GetProperty("id").GetString(),
                VersionNumber = v.TryGetProperty("version_number", out var vn) ? vn.GetString() : "",
                FileUrl = file.GetProperty("url").GetString(),
                FileName = file.GetProperty("filename").GetString(),
                FileSize = file.TryGetProperty("size", out var s) ? s.GetInt64() : 0,
            };
            if (releaseType == "release") return item;
            fallback ??= item;
        }
        return fallback;
    }

    public static async Task<string> DownloadFileAsync(ModItem item, string modsDir, CancellationToken ct = default)
    {
        if (string.IsNullOrEmpty(item.FileUrl) || string.IsNullOrEmpty(item.FileName)) throw new Exception("Нет файла для загрузки.");
        Directory.CreateDirectory(modsDir);
        string dest = Path.Combine(modsDir, item.FileName);
        await Downloader.DownloadAsync(item.FileUrl, dest, null, ct);
        return dest;
    }

    private static string BuildFacets(string type, string category, string loader)
    {
        // Формат facets Modrinth: [["условие"],["условие"]] — AND между массивами, OR внутри массива.
        // Примечание: фильтр loaders:fabric в текущем API возвращает 0, поэтому загрузчик
        // модов фильтруем через категорию (categories:fabric и т.п.) — она гарантированно работает.
        var facets = new List<string> { $"project_type:{ProjectType(type)}" };
        if (!string.IsNullOrEmpty(category)) facets.Add($"categories:{category}");
        // Loader filter only applies to mods. Resource packs and shaders do not have Fabric/Forge loaders.
        if (!string.IsNullOrEmpty(loader) && type == "mod")
        {
            string loaderCat = loader switch
            {
                "fabric" => "fabric",
                "forge" => "forge",
                "neoforge" => "neoforge",
                "quilt" => "quilt",
                _ => "fabric",
            };
            facets.Add($"categories:{loaderCat}");
        }
        return "[" + string.Join(",", facets.Select(f => $"[\"{f}\"]")) + "]";
    }

    private static string Truncate(string s, int max) => s.Length <= max ? s : s[..max] + "…";
}
