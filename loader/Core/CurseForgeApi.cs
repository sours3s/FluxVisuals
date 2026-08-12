using System.Text.Json;

namespace FluxVisualsLoader.Core;

/// <summary>Каталог CurseForge: категории, фильтры, версии, установка. Требует API-ключ.</summary>
public static class CurseForgeApi
{
    private const string Base = "https://api.curseforge.com/v1";
    private const int GameId = 432; // Minecraft
    private const int PageSize = 50;

    private static List<CategoryInfo>? _categories;
    private static readonly SemaphoreSlim CatLock = new(1, 1);

    public static int ClassId(string type) => type switch
    {
        "resourcepack" => 12,
        "shader" => 6552,
        _ => 6, // mods
    };

    private static string TypeOf(int classId) => classId switch
    {
        12 => "resourcepack",
        6552 => "shader",
        _ => "mod",
    };

    private static int? LoaderId(string loader) => loader switch
    {
        "fabric" => 4,
        "forge" => 1,
        "neoforge" => 6,
        "quilt" => 5,
        _ => null,
    };

    public static async Task<SearchPage> SearchAsync(string query, string type, string categoryId, string loader, string sort, string apiKey, int offset, CancellationToken ct = default)
    {
        // у CurseForge «index» — номер страницы (offset / pageSize)
        int pageIndex = Math.Max(0, offset / PageSize);
        string url = $"{Base}/mods/search?gameId={GameId}&classId={ClassId(type)}&index={pageIndex}&pageSize={PageSize}";

        // Фильтр версии игры — только для модов. Шейдеры/ресурспаки ищем без него.
        if (type == "mod")
            url += "&gameVersion=1.21.11";

        if (int.TryParse(categoryId, out int cat) && cat > 0) url += $"&categoryId={cat}";
        // Loader filter applies only to mods. Resource packs/shaders do not have modLoaderType.
        if (type == "mod" && LoaderId(loader) is int ld) url += $"&modLoaderType={ld}";
        url += sort == "updated" ? "&sortField=3&sortOrder=desc" : "&sortField=2&sortOrder=desc";
        if (!string.IsNullOrWhiteSpace(query))
            url += $"&searchFilter={Uri.EscapeDataString(query)}";

        var page = new SearchPage { Offset = offset };
        string? json = await Downloader.GetStringAsync(url, apiKey, ct);
        if (json == null) return page;
        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;
        page.Total = root.GetProperty("pagination").GetProperty("totalCount").GetInt32();
        foreach (var m in root.GetProperty("data").EnumerateArray())
        {
            string slug = m.TryGetProperty("slug", out var s) ? s.GetString() ?? "" : "";
            page.Items.Add(new ModItem
            {
                Id = m.GetProperty("id").GetInt32().ToString(),
                Slug = slug,
                ProjectUrl = string.IsNullOrEmpty(slug) ? "" : $"https://www.curseforge.com/minecraft/mc-mods/{slug}",
                Title = m.GetProperty("name").GetString() ?? "",
                Description = m.TryGetProperty("summary", out var sum) ? sum.GetString() ?? "" : "",
                Author = m.TryGetProperty("authors", out var au) && au.GetArrayLength() > 0
                    ? au[0].GetProperty("name").GetString() ?? "" : "",
                Downloads = m.TryGetProperty("downloadCount", out var dc) ? dc.GetInt32() : 0,
                IconUrl = m.TryGetProperty("logo", out var lg) && lg.ValueKind == JsonValueKind.Object
                    ? lg.GetProperty("url").GetString() ?? "" : "",
            });
        }
        return page;
    }

    public static async Task<List<CategoryInfo>> FetchCategoriesAsync(string apiKey, CancellationToken ct = default)
    {
        if (_categories != null) return _categories;
        await CatLock.WaitAsync(ct);
        try
        {
            if (_categories != null) return _categories;
            var list = new List<CategoryInfo>();
            string? json = await Downloader.GetStringAsync($"{Base}/categories?gameId={GameId}", apiKey, ct);
            if (json != null)
            {
                using var doc = JsonDocument.Parse(json);
                foreach (var c in doc.RootElement.GetProperty("data").EnumerateArray())
                {
                    int classId = c.GetProperty("classId").GetInt32();
                    string type = TypeOf(classId);
                    if (type == "mod" && classId != 6) continue; // для модов берём только корневые (без addons)
                    list.Add(new CategoryInfo
                    {
                        Id = c.GetProperty("id").GetInt32().ToString(),
                        Name = c.GetProperty("name").GetString() ?? "",
                        Type = type,
                    });
                }
            }
            _categories = list;
            return list;
        }
        finally { CatLock.Release(); }
    }

    public static async Task<List<VersionInfo>> VersionsAsync(string modId, string gameVersion, string loader, string apiKey, CancellationToken ct = default)
    {
        var result = new List<VersionInfo>();
        string url = $"{Base}/mods/{modId}/files?gameVersion={Uri.EscapeDataString(gameVersion)}&pageSize=50";
        string? json = await Downloader.GetStringAsync(url, apiKey, ct);
        if (json == null) return result;
        using var doc = JsonDocument.Parse(json);
        foreach (var f in doc.RootElement.GetProperty("data").EnumerateArray())
        {
            // Фильтруем по загрузчику если указан
            if (!string.IsNullOrEmpty(loader) && f.TryGetProperty("gameVersions", out var gvs))
            {
                bool hasLoader = false;
                foreach (var gv in gvs.EnumerateArray())
                {
                    string? gvStr = gv.GetString();
                    if (gvStr != null && gvStr.Equals(loader, StringComparison.OrdinalIgnoreCase))
                    {
                        hasLoader = true;
                        break;
                    }
                }
                if (!hasLoader) continue;
            }

            string fileId = f.GetProperty("id").GetInt32().ToString();
            string releaseType = f.TryGetProperty("releaseType", out var rt) ? rt.GetInt32() switch
            {
                1 => "release",
                2 => "beta",
                3 => "alpha",
                _ => "release"
            } : "release";

            result.Add(new VersionInfo
            {
                VersionId = fileId,
                VersionNumber = f.TryGetProperty("displayName", out var dn) ? dn.GetString() ?? "" : "",
                GameVersion = gameVersion,
                Date = f.TryGetProperty("fileDate", out var fd) ? fd.GetString() ?? "" : "",
                FileUrl = $"https://www.curseforge.com/api/v1/mods/{modId}/files/{fileId}/download",
                FileName = f.TryGetProperty("fileName", out var fn) ? fn.GetString() ?? "" : "",
                FileSize = f.TryGetProperty("fileLength", out var s) ? s.GetInt64() : 0,
                ReleaseType = releaseType,
            });
        }
        // Сортируем: release优先, потом по дате
        result.Sort((a, b) => {
            int ta = a.ReleaseType == "release" ? 0 : a.ReleaseType == "beta" ? 1 : 2;
            int tb = b.ReleaseType == "release" ? 0 : b.ReleaseType == "beta" ? 1 : 2;
            if (ta != tb) return ta - tb;
            return string.Compare(b.Date, a.Date, StringComparison.Ordinal);
        });
        return result;
    }

    public static async Task<ModItem?> ResolveLatestAsync(string modId, string gameVersion, string loader, string apiKey, CancellationToken ct = default)
    {
        var versions = await VersionsAsync(modId, gameVersion, loader, apiKey, ct);
        var v = versions.FirstOrDefault(x => x.ReleaseType == "release") ?? versions.FirstOrDefault();
        if (v == null) return null;
        return new ModItem
        {
            Id = modId,
            Title = v.VersionNumber,
            VersionId = v.VersionId,
            VersionNumber = v.VersionNumber,
            FileUrl = v.FileUrl,
            FileName = v.FileName,
            FileSize = v.FileSize,
        };
    }

    /// <summary>Последний релиз БЕЗ фильтра версии игры (для шейдеров и ресурспаков).</summary>
    public static async Task<ModItem?> ResolveLatestAnyAsync(string modId, string apiKey, CancellationToken ct = default)
    {
        string url = $"{Base}/mods/{modId}/files?pageSize=50";
        string? json = await Downloader.GetStringAsync(url, apiKey, ct);
        if (json == null) return null;
        using var doc = JsonDocument.Parse(json);
        ModItem? fallback = null;
        foreach (var f in doc.RootElement.GetProperty("data").EnumerateArray())
        {
            string releaseType = f.TryGetProperty("releaseType", out var rt) ? rt.GetInt32() switch
            {
                1 => "release", 2 => "beta", _ => "alpha"
            } : "release";
            string fileId = f.GetProperty("id").GetInt32().ToString();
            var item = new ModItem
            {
                Id = modId,
                VersionId = fileId,
                VersionNumber = f.TryGetProperty("displayName", out var dn) ? dn.GetString() ?? "" : "",
                FileUrl = $"https://www.curseforge.com/api/v1/mods/{modId}/files/{fileId}/download",
                FileName = f.TryGetProperty("fileName", out var fn) ? fn.GetString() ?? "" : "",
                FileSize = f.TryGetProperty("fileLength", out var s) ? s.GetInt64() : 0,
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
}
