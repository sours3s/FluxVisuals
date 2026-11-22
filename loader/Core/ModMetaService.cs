using System.IO.Compression;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace FluxVisualsLoader.Core;

/// <summary>
/// Метаданные для «Моих файлов» берём из того источника, откуда файл скачан:
/// Modrinth → API Modrinth, CurseForge → API CurseForge. Источник и ID проекта
/// сохраняются при установке. Иконка из архива — только запасной вариант,
/// пока источник не ответил.
/// </summary>
public static class ModMetaService
{
    private sealed class LocalMeta
    {
        public string? IconUrl;
        public string? Description;
    }

    private static readonly HashSet<string> InFlight = new(StringComparer.OrdinalIgnoreCase);
    private static readonly object Gate = new();
    private const int MaxIconBytes = 512 * 1024;

    /// <summary>Обогащает записи перед отправкой в UI. Сеть работает в фоне.</summary>
    public static void Enrich(List<ModEntry> entries, AppConfig cfg, Action onUpdated)
    {
        foreach (var entry in entries)
        {
            string key = StableKey(entry.FileName);
            if (string.IsNullOrWhiteSpace(key)) continue;

            string source = entry.Source;
            if (string.IsNullOrWhiteSpace(source) && cfg.ModSources.TryGetValue(key, out var savedSource))
                source = savedSource;
            if (string.IsNullOrWhiteSpace(source)) source = SourceFromUrl(entry.ProjectUrl);
            string projectId = entry.ProjectId;
            if (string.IsNullOrWhiteSpace(projectId) && cfg.ModProjectIds.TryGetValue(key, out var savedId))
                projectId = savedId;

            bool hasIcon = !string.IsNullOrWhiteSpace(entry.IconUrl);
            bool hasUrl = !string.IsNullOrWhiteSpace(entry.ProjectUrl);
            bool hasDesc = !string.IsNullOrWhiteSpace(entry.Description);
            bool urlFromSource = hasUrl && UrlMatchesSource(entry.ProjectUrl, source, entry.Type);

            if (!hasIcon || !hasDesc)
            {
                LocalMeta local = ReadLocal(entry);
                if (!hasIcon && !string.IsNullOrWhiteSpace(local.IconUrl))
                {
                    entry.IconUrl = local.IconUrl;
                    if (!cfg.ModIcons.ContainsKey(key)) cfg.ModIcons[key] = local.IconUrl;
                }
                if (!hasDesc && !string.IsNullOrWhiteSpace(local.Description))
                {
                    entry.Description = local.Description;
                    if (!cfg.ModDescriptions.ContainsKey(key)) cfg.ModDescriptions[key] = local.Description;
                }
                cfg.Save();
            }

            if (hasIcon && hasUrl && hasDesc && urlFromSource) continue;

            string triedKey = $"file:{key}:{source}:{SearchQuery(entry.Name)}";
            if (cfg.ModMetaTried.Contains(triedKey)) continue;
            lock (Gate)
            {
                if (!InFlight.Add(triedKey)) continue;
            }
            _ = ResolveAsync(triedKey, key, entry, cfg, source, projectId, onUpdated);
        }
    }

    /// <summary>Обновляем иконку/описание/ссылку установленных модпаков из их источника.</summary>
    public static void RefreshModpacks(AppConfig cfg, Action onUpdated)
    {
        foreach (var pack in ModpackManager.List())
        {
            string triedKey = $"modpack:{pack.Source}:{pack.Id}";
            bool urlOk = UrlMatchesSource(pack.ProjectUrl, pack.Source, "modpack");
            if (!string.IsNullOrWhiteSpace(pack.IconUrl) && !string.IsNullOrWhiteSpace(pack.Description) && urlOk) continue;
            if (cfg.ModMetaTried.Contains(triedKey)) continue;
            lock (Gate)
            {
                if (!InFlight.Add(triedKey)) continue;
            }
            _ = RefreshPackAsync(triedKey, pack, cfg, onUpdated);
        }
    }

    private static async Task RefreshPackAsync(string triedKey, InstalledModpack pack,
        AppConfig cfg, Action onUpdated)
    {
        try
        {
            ModItem? project = await FetchById(pack.Source, pack.Id, cfg.CurseforgeKey, "modpack");
            if (project != null)
            {
                if (!string.IsNullOrWhiteSpace(project.IconUrl)) pack.IconUrl = project.IconUrl;
                if (!string.IsNullOrWhiteSpace(project.ProjectUrl)) pack.ProjectUrl = project.ProjectUrl;
                if (!string.IsNullOrWhiteSpace(project.Description)) pack.Description = project.Description;
                if (project.Downloads > 0) pack.Downloads = project.Downloads;
                ModpackManager.Update(pack);
            }
        }
        catch { }
        finally
        {
            try { cfg.ModMetaTried.Add(triedKey); cfg.Save(); } catch { }
            lock (Gate) { InFlight.Remove(triedKey); }
            onUpdated();
        }
    }

    private static async Task ResolveAsync(string triedKey, string key, ModEntry entry,
        AppConfig cfg, string source, string projectId, Action onUpdated)
    {
        try
        {
            ModItem? match = null;
            if (!string.IsNullOrWhiteSpace(projectId))
                match = await FetchById(source, projectId, cfg.CurseforgeKey, entry.Type);

            if (match == null)
            {
                string query = SearchQuery(entry.Name);
                if (string.Equals(source, "curseforge", StringComparison.OrdinalIgnoreCase))
                {
                    if (!string.IsNullOrWhiteSpace(cfg.CurseforgeKey))
                    {
                        var page = await CurseForgeApi.SearchAsync(query, entry.Type, "", "", "updated", cfg.CurseforgeKey, 0);
                        match = Best(page.Items, query);
                    }
                }
                else
                {
                    var page = await ModrinthApi.SearchAsync(query, entry.Type, "", "", "relevance", 0, default, null);
                    match = Best(page.Items, query);
                }

                // Источник неизвестен (старые файлы): проверяем второй каталог.
                if (match == null && string.IsNullOrWhiteSpace(source) && !string.IsNullOrWhiteSpace(cfg.CurseforgeKey))
                {
                    var page = await CurseForgeApi.SearchAsync(query, entry.Type, "", "", "updated", cfg.CurseforgeKey, 0);
                    match = Best(page.Items, query);
                }
            }

            if (match != null)
            {
                string actualSource = string.IsNullOrWhiteSpace(source) ? SourceFromUrl(match.ProjectUrl) : source;
                if (string.IsNullOrWhiteSpace(actualSource))
                    actualSource = match.ProjectUrl.Contains("curseforge", StringComparison.OrdinalIgnoreCase)
                        ? "curseforge" : "modrinth";

                if (!string.IsNullOrWhiteSpace(actualSource)) cfg.ModSources[key] = actualSource;
                if (!string.IsNullOrWhiteSpace(match.Id)) cfg.ModProjectIds[key] = match.Id;
                if (!string.IsNullOrWhiteSpace(match.ProjectUrl)) cfg.ModUrls[key] = match.ProjectUrl;
                if (!string.IsNullOrWhiteSpace(match.IconUrl)) cfg.ModIcons[key] = match.IconUrl;
                if (!string.IsNullOrWhiteSpace(match.Description)) cfg.ModDescriptions[key] = match.Description;

                entry.Source = actualSource;
                entry.ProjectId = match.Id;
                entry.ProjectUrl = match.ProjectUrl;
                if (!string.IsNullOrWhiteSpace(match.IconUrl)) entry.IconUrl = match.IconUrl;
                if (!string.IsNullOrWhiteSpace(match.Description)) entry.Description = match.Description;
            }
        }
        catch { }
        finally
        {
            try { cfg.ModMetaTried.Add(triedKey); cfg.Save(); } catch { }
            lock (Gate) { InFlight.Remove(triedKey); }
            onUpdated();
        }
    }

    private static Task<ModItem?> FetchById(string source, string id, string apiKey, string type)
    {
        if (string.IsNullOrWhiteSpace(id)) return Task.FromResult<ModItem?>(null);
        return string.Equals(source, "curseforge", StringComparison.OrdinalIgnoreCase)
            ? CurseForgeApi.GetProjectAsync(id, apiKey, default, type)
            : ModrinthApi.GetProjectAsync(id);
    }

    private static string StableKey(string fileName) =>
        fileName.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase) ? fileName[..^9] : fileName;

    private static string SourceFromUrl(string url)
    {
        if (string.IsNullOrWhiteSpace(url)) return "";
        if (url.Contains("curseforge.com", StringComparison.OrdinalIgnoreCase)) return "curseforge";
        if (url.Contains("modrinth.com", StringComparison.OrdinalIgnoreCase)) return "modrinth";
        return "";
    }

    /// <summary>Проверяем, что сохранённая ссылка действительно ведёт на нужный раздел нужного источника.</summary>
    private static bool UrlMatchesSource(string url, string source, string type)
    {
        string expected = string.IsNullOrWhiteSpace(source) ? "" : source.ToLowerInvariant() switch
        {
            "curseforge" => type switch
            {
                "modpack" => "/minecraft/modpacks/",
                "resourcepack" => "/minecraft/texture-packs/",
                "shader" => "/minecraft/shaders/",
                _ => "/minecraft/mc-mods/",
            },
            "modrinth" => type switch
            {
                "modpack" => "/modpack/",
                "resourcepack" => "/resourcepack/",
                "shader" => "/shader/",
                _ => "/mod/",
            },
            _ => "",
        };
        if (string.IsNullOrWhiteSpace(expected)) return false;
        return SourceFromUrl(url) == source && url.Contains(expected, StringComparison.OrdinalIgnoreCase);
    }

    private static LocalMeta ReadLocal(ModEntry entry)
    {
        var meta = new LocalMeta();
        try
        {
            if (Directory.Exists(entry.Path))
            {
                meta.IconUrl = ReadPng(Path.Combine(entry.Path, "pack.png"));
                meta.Description = ReadPackMcmeta(Path.Combine(entry.Path, "pack.mcmeta"));
                return meta;
            }

            if (!File.Exists(entry.Path)) return meta;
            string ext = Path.GetExtension(entry.Path).ToLowerInvariant();
            if (ext is not (".jar" or ".zip" or ".mcpack")) return meta;

            using var stream = File.OpenRead(entry.Path);
            using var zip = new ZipArchive(stream, ZipArchiveMode.Read);

            ZipArchiveEntry? icon = zip.Entries.FirstOrDefault(e =>
                e.FullName.Equals("pack.png", StringComparison.OrdinalIgnoreCase)
                || e.FullName.Equals("icon.png", StringComparison.OrdinalIgnoreCase));

            ZipArchiveEntry? fabric = zip.Entries.FirstOrDefault(e =>
                e.FullName.Equals("fabric.mod.json", StringComparison.OrdinalIgnoreCase));
            if (fabric != null)
            {
                using var reader = new StreamReader(fabric.Open(), Encoding.UTF8);
                using var doc = JsonDocument.Parse(reader.ReadToEnd());
                var root = doc.RootElement;
                if (root.TryGetProperty("description", out var d) && d.ValueKind == JsonValueKind.String)
                    meta.Description = Trim(d.GetString());
                if (root.TryGetProperty("icon", out var ic) && ic.ValueKind == JsonValueKind.String)
                {
                    string iconPath = ic.GetString() ?? "";
                    icon = zip.Entries.FirstOrDefault(e => e.FullName.Equals(iconPath, StringComparison.OrdinalIgnoreCase)) ?? icon;
                }
                if (icon == null && root.TryGetProperty("id", out var idEl))
                {
                    string id = idEl.GetString() ?? "";
                    if (!string.IsNullOrWhiteSpace(id))
                        icon = zip.Entries.FirstOrDefault(e => e.FullName.EndsWith("/icon.png", StringComparison.OrdinalIgnoreCase)
                            && e.FullName.Contains(id, StringComparison.OrdinalIgnoreCase));
                }
            }

            ZipArchiveEntry? mcmeta = zip.Entries.FirstOrDefault(e =>
                e.FullName.Equals("pack.mcmeta", StringComparison.OrdinalIgnoreCase));
            if (mcmeta != null) meta.Description ??= DescriptionFromEntry(mcmeta);

            if (icon != null) meta.IconUrl = ToDataUri(icon);
        }
        catch { }
        return meta;
    }

    private static string? ReadPng(string path)
    {
        try
        {
            if (!File.Exists(path)) return null;
            var fi = new FileInfo(path);
            if (fi.Length == 0 || fi.Length > MaxIconBytes) return null;
            return "data:" + Mime(path) + ";base64," + Convert.ToBase64String(File.ReadAllBytes(path));
        }
        catch { return null; }
    }

    private static string? ToDataUri(ZipArchiveEntry entry)
    {
        try
        {
            if (entry.Length == 0 || entry.Length > MaxIconBytes) return null;
            using var input = entry.Open();
            using var ms = new MemoryStream();
            input.CopyTo(ms);
            return "data:" + Mime(entry.FullName) + ";base64," + Convert.ToBase64String(ms.ToArray());
        }
        catch { return null; }
    }

    private static string Mime(string path) => Path.GetExtension(path).ToLowerInvariant() switch
    {
        ".jpg" or ".jpeg" => "image/jpeg",
        ".webp" => "image/webp",
        ".gif" => "image/gif",
        _ => "image/png",
    };

    private static string? ReadPackMcmeta(string path)
    {
        try
        {
            if (!File.Exists(path)) return null;
            using var doc = JsonDocument.Parse(File.ReadAllText(path));
            return doc.RootElement.TryGetProperty("pack", out var pack)
                && pack.TryGetProperty("description", out var desc) ? PlainText(desc) : null;
        }
        catch { return null; }
    }

    private static string? DescriptionFromEntry(ZipArchiveEntry entry)
    {
        try
        {
            using var reader = new StreamReader(entry.Open(), Encoding.UTF8);
            using var doc = JsonDocument.Parse(reader.ReadToEnd());
            return doc.RootElement.TryGetProperty("pack", out var pack)
                && pack.TryGetProperty("description", out var desc) ? PlainText(desc) : null;
        }
        catch { return null; }
    }

    private static string? PlainText(JsonElement el)
    {
        if (el.ValueKind == JsonValueKind.String) return Trim(el.GetString());
        if (el.ValueKind == JsonValueKind.Array)
            return Trim(string.Concat(el.EnumerateArray().Select(PlainText).Where(s => s != null)));
        if (el.ValueKind == JsonValueKind.Object)
        {
            if (el.TryGetProperty("text", out var text))
            {
                string? head = PlainText(text);
                if (el.TryGetProperty("extra", out var extra) && extra.ValueKind == JsonValueKind.Array)
                    head += string.Concat(extra.EnumerateArray().Select(PlainText).Where(s => s != null));
                return Trim(head);
            }
            if (el.TryGetProperty("extra", out var extras) && extras.ValueKind == JsonValueKind.Array)
                return Trim(string.Concat(extras.EnumerateArray().Select(PlainText).Where(s => s != null)));
        }
        return null;
    }

    private static string? Trim(string? s)
    {
        if (string.IsNullOrWhiteSpace(s)) return null;
        s = Regex.Replace(s, "§.", "").Replace("\n", " ").Trim();
        return s.Length <= 240 ? s : s[..240] + "…";
    }

    /// <summary>«Almanac-1.21.11-x-fabric-1.6.2» → «Almanac».</summary>
    private static string SearchQuery(string name)
    {
        string s = Path.GetFileNameWithoutExtension(name.Trim());
        s = Regex.Replace(s, @"[-_ ]?[rv]?(?:\d+\.)+\d+.*$", "", RegexOptions.IgnoreCase);
        s = Regex.Replace(s, @"[+]+.*$", "");
        s = Regex.Replace(s, @"[-_ +]+(?:fabric|forge|neoforge|quilt|mc|minecraft)\b.*$", "", RegexOptions.IgnoreCase);
        s = Regex.Replace(s, @"(?:fabric|forge|neoforge|quilt)$", "", RegexOptions.IgnoreCase);
        // «ComplementaryReimagined» → «Complementary Reimagined»: без пробела поиск ничего не находит.
        s = Regex.Replace(s, @"(?<=[a-z0-9])(?=[A-Z])", " ");
        s = Regex.Replace(s, @"[-_]+", " ").Trim();
        return s.Length >= 3 ? s : name.Trim();
    }

    private static ModItem? Best(IEnumerable<ModItem> items, string query)
    {
        string q = Normalize(query);
        var qTokens = Tokens(query);
        if (q.Length == 0) return null;
        ModItem? best = null;
        int bestScore = 0;
        foreach (var item in items)
        {
            string title = Normalize(item.Title);
            string slug = Normalize(item.Slug);
            int score = title == q || slug == q ? 100
                : title.StartsWith(q, StringComparison.Ordinal) || q.StartsWith(title, StringComparison.Ordinal) ? 85
                : title.Contains(q, StringComparison.Ordinal) || q.Contains(title, StringComparison.Ordinal) ? 65
                : 0;

            // «Complementary Reimagined» vs «Complementary Shaders - Reimagined»:
            // точного вхождения нет, но слова совпадают — это тот же проект.
            if (score == 0 && qTokens.Count > 0)
            {
                var itemTokens = Tokens(item.Title + " " + item.Slug);
                int overlap = qTokens.Count(itemTokens.Contains);
                double ratio = (double)overlap / qTokens.Count;
                if (overlap > 0 && ratio >= 0.6) score = 60 + (int)(ratio * 30);
            }

            if (score > bestScore) { bestScore = score; best = item; }
        }
        return bestScore >= 65 ? best : null;
    }

    private static HashSet<string> Tokens(string s) => new(
        Regex.Split(s.ToLowerInvariant(), @"[^a-z0-9]+").Where(t => t.Length > 1),
        StringComparer.OrdinalIgnoreCase);

    private static string Normalize(string s) => Regex.Replace(s.ToLowerInvariant(), "[^a-z0-9]", "");
}
