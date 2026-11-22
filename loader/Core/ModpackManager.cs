using System.IO;
using System.IO.Compression;
using System.Security.Cryptography;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace FluxVisualsLoader.Core;

/// <summary>Установленный модпак. Хранится отдельно от config.json, чтобы конфиг не раздувался.</summary>
public class InstalledModpack
{
    [JsonPropertyName("id")] public string Id { get; set; } = "";
    [JsonPropertyName("source")] public string Source { get; set; } = "modrinth";
    [JsonPropertyName("title")] public string Title { get; set; } = "";
    [JsonPropertyName("iconUrl")] public string IconUrl { get; set; } = "";
    [JsonPropertyName("projectUrl")] public string ProjectUrl { get; set; } = "";
    [JsonPropertyName("description")] public string Description { get; set; } = "";
    [JsonPropertyName("versionId")] public string VersionId { get; set; } = "";
    [JsonPropertyName("versionNumber")] public string VersionNumber { get; set; } = "";
    [JsonPropertyName("downloads")] public int Downloads { get; set; }
    [JsonPropertyName("fileCount")] public int FileCount { get; set; }
    [JsonPropertyName("files")] public List<string> Files { get; set; } = new();
    [JsonPropertyName("installedAt")] public DateTime InstalledAt { get; set; } = DateTime.Now;
}

internal sealed class InstallPlan
{
    public string Path { get; set; } = "";
    public string Url { get; set; } = "";
    public long Size { get; set; }
    public string Sha1 { get; set; } = "";
    public string Sha512 { get; set; } = "";
    public string? ApiKey { get; set; }
    public ZipArchiveEntry? Entry { get; set; }
}

/// <summary>Каталог установленных модпаков + установщик .mrpack (Modrinth) и manifest.json (CurseForge).</summary>
public static class ModpackManager
{
    private const string ModrinthBase = "https://api.modrinth.com/v2";
    private const string CurseForgeBase = "https://api.curseforge.com/v1";

    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    /// <summary>Прогресс: done bytes, total bytes, текущий файл.</summary>
    public delegate void ProgressHandler(long done, long total, string file);

    public static string StatePath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "FluxVisuals", "modpacks.json");

    public static List<InstalledModpack> List()
    {
        try
        {
            if (!File.Exists(StatePath)) return new();
            var list = JsonSerializer.Deserialize<List<InstalledModpack>>(File.ReadAllText(StatePath)) ?? new();
            return list.OrderByDescending(p => p.InstalledAt).ToList();
        }
        catch { return new(); }
    }

    private static void Save(List<InstalledModpack> packs)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(StatePath)!);
        File.WriteAllText(StatePath, JsonSerializer.Serialize(packs, JsonOptions));
    }

    public static async Task<InstalledModpack> InstallAsync(
        string source, ModItem item, string gameDir, string apiKey, ProgressHandler report, CancellationToken ct = default)
    {
        string modrinth = string.Equals(source, "modrinth", StringComparison.OrdinalIgnoreCase)
            ? "modrinth" : "curseforge";

        // 1. Разрешаем конкретную версию (выбранную в UI или последнюю релизную).
        string url = item.SelectedVersion?.FileUrl ?? item.FileUrl ?? "";
        string fileName = item.SelectedVersion?.FileName ?? item.FileName ?? "modpack.mrpack";
        string versionId = item.SelectedVersion?.VersionId ?? item.VersionId ?? "";
        string versionNumber = item.SelectedVersion?.VersionNumber ?? item.VersionNumber ?? "";
        long archiveSize = item.SelectedVersion?.FileSize ?? item.FileSize;

        if (string.IsNullOrWhiteSpace(url))
        {
            ModItem? latest = modrinth == "modrinth"
                ? await ModrinthApi.ResolveLatestAsync(item.Id, "1.21.11", "fabric", ct)
                : await CurseForgeApi.ResolveLatestAsync(item.Id, "1.21.11", "fabric", apiKey, ct);
            if (latest == null || string.IsNullOrWhiteSpace(latest.FileUrl))
                throw new Exception("Не удалось найти версию модпака");
            url = latest.FileUrl!;
            fileName = latest.FileName ?? fileName;
            versionId = latest.VersionId ?? versionId;
            versionNumber = latest.VersionNumber ?? versionNumber;
            archiveSize = latest.FileSize;
        }

        string workDir = Path.Combine(Path.GetTempPath(), "FluxVisuals", "modpacks");
        Directory.CreateDirectory(workDir);
        string safeName = SanitizeFileName(fileName);
        string archive = Path.Combine(workDir, $"{SanitizeFileName(item.Id)}_{SanitizeFileName(versionId)}_{safeName}");
        if (File.Exists(archive)) File.Delete(archive);

        try
        {
            // 2. Качаем сам архив модпака с настоящим прогрессом в байтах.
            report(0, archiveSize > 0 ? archiveSize : 0, safeName);
            await Downloader.DownloadAsync(url, archive,
                (done, total) => report(done, total ?? archiveSize, safeName), ct,
                modrinth == "curseforge" ? apiKey : null);

            // 3. Читаем манифест и строим план установки.
            using var zip = ZipFile.OpenRead(archive);
            var plan = modrinth == "modrinth"
                ? await BuildModrinthPlanAsync(zip, modrinth, ct)
                : await BuildCurseForgePlanAsync(zip, apiKey, ct);

            var totalByPath = plan.ToDictionary(p => p.Path, p => p.Size > 0 ? p.Size : 0);
            var doneByPath = plan.ToDictionary(p => p.Path, _ => 0L);
            var progressLock = new object();
            string currentFile = safeName;

            void Aggregated(string file)
            {
                currentFile = file;
                long done = 0, total = 0;
                lock (progressLock)
                {
                    foreach (var v in doneByPath.Values) done += v;
                    foreach (var v in totalByPath.Values) total += v;
                }
                report(done, total, file);
            }

            // 4. Оверрайды — небольшие файлы, распаковываем последовательно.
            foreach (var entry in plan.Where(p => p.Entry != null))
            {
                ct.ThrowIfCancellationRequested();
                doneByPath[entry.Path] = entry.Size;
                totalByPath[entry.Path] = entry.Size;
                await ExtractEntryAsync(entry, gameDir, ct);
                Aggregated(Path.GetFileName(entry.Path));
            }

            // 5. Моды/ресурсы — параллельно, но с ограничением, чтобы не уронить API.
            var downloads = plan.Where(p => p.Entry == null && !string.IsNullOrWhiteSpace(p.Url)).ToList();
            await Parallel.ForEachAsync(downloads,
                new ParallelOptions { MaxDegreeOfParallelism = 4, CancellationToken = ct },
                async (p, token) =>
                {
                    await EnsureRemoteFileAsync(p, gameDir, apiKey, token,
                        (done, total) =>
                        {
                            lock (progressLock)
                            {
                                doneByPath[p.Path] = done;
                                if (total > 0) totalByPath[p.Path] = total;
                            }
                            Aggregated(Path.GetFileName(p.Path));
                        });
                });

            foreach (var p in plan)
            {
                doneByPath[p.Path] = totalByPath[p.Path];
                Aggregated(Path.GetFileName(p.Path));
            }

            var installed = new InstalledModpack
            {
                Id = item.Id,
                Source = modrinth,
                Title = item.Title,
                IconUrl = item.IconUrl,
                ProjectUrl = item.ProjectUrl,
                Description = item.Description,
                VersionId = versionId,
                VersionNumber = versionNumber,
                Downloads = item.Downloads,
                FileCount = plan.Count,
                Files = plan.Select(p => p.Path).ToList(),
                InstalledAt = DateTime.Now,
            };

            var all = List();
            all.RemoveAll(p => p.Id == installed.Id && p.Source == installed.Source);
            all.Insert(0, installed);
            Save(all);
            return installed;
        }
        finally
        {
            try { if (File.Exists(archive)) File.Delete(archive); } catch { }
        }
    }

    public static void Uninstall(InstalledModpack pack, string gameDir)
    {
        foreach (string rel in pack.Files)
        {
            try
            {
                string full = SafePath(gameDir, rel);
                if (File.Exists(full)) File.Delete(full);
            }
            catch { }
        }
        var all = List();
        all.RemoveAll(p => p.Id == pack.Id && p.Source == pack.Source && p.VersionId == pack.VersionId);
        Save(all);
    }

    // ---------- манифесты ----------

    private static async Task<List<InstallPlan>> BuildModrinthPlanAsync(
        ZipArchive zip, string source, CancellationToken ct)
    {
        var result = new List<InstallPlan>();
        var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var indexEntry = zip.GetEntry("modrinth.index.json")
            ?? throw new Exception("В архиве нет modrinth.index.json — это не .mrpack модпак");

        using (var stream = indexEntry.Open())
        using (var doc = await JsonDocument.ParseAsync(stream, cancellationToken: ct))
        {
            var root = doc.RootElement;
            if (!root.TryGetProperty("files", out var files)) return result;
            foreach (var f in files.EnumerateArray())
            {
                string rel = f.TryGetProperty("path", out var pe) ? pe.GetString() ?? "" : "";
                string url = f.TryGetProperty("downloads", out var de) && de.GetArrayLength() > 0
                    ? de[0].GetString() ?? "" : "";
                if (!IsInstallable(rel, seen) || string.IsNullOrWhiteSpace(url)) continue;
                result.Add(new InstallPlan
                {
                    Path = rel,
                    Url = url,
                    Size = f.TryGetProperty("fileSize", out var se) ? se.GetInt64() : 0,
                    Sha1 = ReadHash(f, "sha1"),
                    Sha512 = ReadHash(f, "sha512"),
                });
            }

            // Зависимости .mrpack: отдельные версии Modrinth, которых может не быть в files.
            if (root.TryGetProperty("dependencies", out var deps)
                && deps.TryGetProperty("modrinth", out var mrDeps))
            {
                foreach (var dep in mrDeps.EnumerateObject())
                {
                    ct.ThrowIfCancellationRequested();
                    string versionId = dep.Name;
                    if (string.IsNullOrWhiteSpace(versionId)) continue;
                    try
                    {
                        string? json = await Downloader.GetStringAsync($"{ModrinthBase}/version/{versionId}", ct: ct);
                        if (json == null) continue;
                        using var vdoc = JsonDocument.Parse(json);
                        var v = vdoc.RootElement;
                        if (!v.TryGetProperty("files", out var vf) || vf.GetArrayLength() == 0) continue;
                        var file = vf[0];
                        foreach (var candidate in vf.EnumerateArray())
                            if (candidate.TryGetProperty("primary", out var pr) && pr.ValueKind == JsonValueKind.True)
                            { file = candidate; break; }
                        string name = file.TryGetProperty("filename", out var fn) ? fn.GetString() ?? "" : "";
                        string rel = "mods/" + name;
                        string url = file.TryGetProperty("url", out var u) ? u.GetString() ?? "" : "";
                        if (!IsInstallable(rel, seen) || string.IsNullOrWhiteSpace(url)) continue;
                        result.Add(new InstallPlan
                        {
                            Path = rel,
                            Url = url,
                            Size = file.TryGetProperty("size", out var s) ? s.GetInt64() : 0,
                            Sha1 = ReadHash(file, "sha1"),
                            Sha512 = ReadHash(file, "sha512"),
                        });
                    }
                    catch { }
                }
            }
        }

        AddOverrides(zip, result, seen);
        return result;
    }

    private static async Task<List<InstallPlan>> BuildCurseForgePlanAsync(
        ZipArchive zip, string apiKey, CancellationToken ct)
    {
        var result = new List<InstallPlan>();
        var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var manifestEntry = zip.GetEntry("manifest.json")
            ?? zip.Entries.FirstOrDefault(e => e.Name.Equals("manifest.json", StringComparison.OrdinalIgnoreCase))
            ?? throw new Exception("В архиве нет manifest.json — это не модпак CurseForge");

        using (var stream = manifestEntry.Open())
        using (var doc = await JsonDocument.ParseAsync(stream, cancellationToken: ct))
        {
            if (!doc.RootElement.TryGetProperty("files", out var files)) return result;
            var requests = new List<(string ProjectId, string FileId)>();
            foreach (var f in files.EnumerateArray())
            {
                bool required = !f.TryGetProperty("required", out var rq) || rq.ValueKind != JsonValueKind.False;
                if (!required) continue;
                string projectId = f.TryGetProperty("projectID", out var pid) ? pid.GetInt32().ToString() : "";
                string fileId = f.TryGetProperty("fileID", out var fid) ? fid.GetInt32().ToString() : "";
                if (string.IsNullOrWhiteSpace(projectId) || string.IsNullOrWhiteSpace(fileId)) continue;
                requests.Add((projectId, fileId));
            }

            var meta = await FetchCurseForgeFilesAsync(requests, apiKey, ct);
            foreach (var req in requests)
            {
                ct.ThrowIfCancellationRequested();
                if (!meta.TryGetValue(req.FileId, out var m)) m = new FileMeta();
                string name = string.IsNullOrWhiteSpace(m.FileName) ? $"{req.ProjectId}-{req.FileId}.jar" : m.FileName!;
                string rel = "mods/" + name;
                if (!IsInstallable(rel, seen)) continue;
                string directUrl = string.IsNullOrWhiteSpace(m.DownloadUrl)
                    ? $"https://www.curseforge.com/api/v1/mods/{req.ProjectId}/files/{req.FileId}/download"
                    : m.DownloadUrl;
                result.Add(new InstallPlan
                {
                    Path = rel,
                    Url = directUrl,
                    Size = m.Size,
                    Sha1 = m.Sha1,
                    ApiKey = string.IsNullOrWhiteSpace(m.DownloadUrl) ? apiKey : null,
                });
            }
        }

        AddOverrides(zip, result, seen);
        return result;
    }

    private sealed class FileMeta
    {
        public string? FileName { get; set; }
        public string DownloadUrl { get; set; } = "";
        public long Size { get; set; }
        public string Sha1 { get; set; } = "";
    }

    private static async Task<Dictionary<string, FileMeta>> FetchCurseForgeFilesAsync(
        List<(string ProjectId, string FileId)> requests, string apiKey, CancellationToken ct)
    {
        var result = new Dictionary<string, FileMeta>(StringComparer.OrdinalIgnoreCase);
        if (requests.Count == 0) return result;

        try
        {
            var ids = requests.Select(r => int.TryParse(r.FileId, out int v) ? v : 0).Where(v => v > 0).Distinct().ToArray();
            string body = JsonSerializer.Serialize(new { fileIds = ids });
            string? json = await Downloader.PostStringAsync($"{CurseForgeBase}/mods/files", body, apiKey, ct);
            if (json != null)
            {
                using var doc = JsonDocument.Parse(json);
                foreach (var f in doc.RootElement.GetProperty("data").EnumerateArray())
                {
                    string id = f.GetProperty("id").GetInt32().ToString();
                    result[id] = new FileMeta
                    {
                        FileName = f.TryGetProperty("fileName", out var fn) ? fn.GetString() ?? "" : "",
                        Size = f.TryGetProperty("fileLength", out var fl) ? fl.GetInt64() : 0,
                        DownloadUrl = f.TryGetProperty("downloadUrl", out var du) ? du.GetString() ?? "" : "",
                        Sha1 = ReadCurseForgeSha1(f),
                    };
                }
                if (result.Count > 0) return result;
            }
        }
        catch { }

        // Fallback: по одному файлу (если batch endpoint недоступен).
        foreach (var req in requests)
        {
            try
            {
                string? json = await Downloader.GetStringAsync(
                    $"{CurseForgeBase}/mods/{req.ProjectId}/files/{req.FileId}", apiKey, ct);
                if (json == null) continue;
                using var doc = JsonDocument.Parse(json);
                var f = doc.RootElement.GetProperty("data");
                result[req.FileId] = new FileMeta
                {
                    FileName = f.TryGetProperty("fileName", out var fn) ? fn.GetString() ?? "" : "",
                    Size = f.TryGetProperty("fileLength", out var fl) ? fl.GetInt64() : 0,
                    DownloadUrl = f.TryGetProperty("downloadUrl", out var du) ? du.GetString() ?? "" : "",
                    Sha1 = ReadCurseForgeSha1(f),
                };
            }
            catch { }
        }
        return result;
    }

    private static void AddOverrides(ZipArchive zip, List<InstallPlan> plan, HashSet<string> seen)
    {
        foreach (var entry in zip.Entries)
        {
            if (entry.FullName.EndsWith('/')) continue;
            string prefix = null!;
            foreach (string candidate in new[] { "overrides/", "client-overrides/" })
            {
                if (entry.FullName.StartsWith(candidate, StringComparison.OrdinalIgnoreCase))
                { prefix = candidate; break; }
            }
            if (prefix == null) continue;
            string rel = entry.FullName[prefix.Length..].Replace('\\', '/');
            if (!IsInstallable(rel, seen)) continue;
            plan.Add(new InstallPlan { Path = rel, Entry = entry, Size = entry.Length });
        }
    }

    // ---------- файлы ----------

    private static async Task EnsureRemoteFileAsync(InstallPlan p, string gameDir, string apiKey,
        CancellationToken ct, Action<long, long> progress)
    {
        string dest = SafePath(gameDir, p.Path);
        Directory.CreateDirectory(Path.GetDirectoryName(dest)!);

        if (await HashMatchesAsync(dest, p, ct))
        {
            progress(p.Size > 0 ? p.Size : new FileInfo(dest).Length, p.Size > 0 ? p.Size : new FileInfo(dest).Length);
            return;
        }
        if (File.Exists(dest)) File.Delete(dest);

        string tmp = dest + ".part";
        if (File.Exists(tmp)) File.Delete(tmp);
        await Downloader.DownloadAsync(p.Url, tmp, (done, total) => progress(done, total ?? p.Size), ct, p.ApiKey ?? apiKey);
        if (!await HashMatchesAsync(tmp, p, ct))
            throw new Exception($"Хэш файла не совпал: {Path.GetFileName(dest)}");
        File.Move(tmp, dest, true);
        progress(p.Size > 0 ? p.Size : new FileInfo(dest).Length, p.Size > 0 ? p.Size : new FileInfo(dest).Length);
    }

    private static async Task ExtractEntryAsync(InstallPlan p, string gameDir, CancellationToken ct)
    {
        if (p.Entry == null) return;
        string dest = SafePath(gameDir, p.Path);
        Directory.CreateDirectory(Path.GetDirectoryName(dest)!);
        await using var src = p.Entry.Open();
        await using var dst = new FileStream(dest, FileMode.Create, FileAccess.Write, FileShare.None, 81920, true);
        await src.CopyToAsync(dst, ct);
    }

    private static async Task<bool> HashMatchesAsync(string path, InstallPlan p, CancellationToken ct)
    {
        if (!File.Exists(path)) return false;
        var fi = new FileInfo(path);
        if (fi.Length <= 0) return false;
        if (string.IsNullOrWhiteSpace(p.Sha1) && string.IsNullOrWhiteSpace(p.Sha512)) return true;

        if (!string.IsNullOrWhiteSpace(p.Sha1))
        {
            await using var s = File.OpenRead(path);
            var hash = await SHA1.HashDataAsync(s, ct);
            if (!Convert.ToHexString(hash).Equals(p.Sha1.Trim(), StringComparison.OrdinalIgnoreCase)) return false;
        }
        if (!string.IsNullOrWhiteSpace(p.Sha512))
        {
            await using var s = File.OpenRead(path);
            var hash = await SHA512.HashDataAsync(s, ct);
            if (!Convert.ToHexString(hash).Equals(p.Sha512.Trim(), StringComparison.OrdinalIgnoreCase)) return false;
        }
        return true;
    }

    private static string SafePath(string gameDir, string relative)
    {
        string root = Path.GetFullPath(gameDir);
        string normalized = relative.Replace('\\', Path.DirectorySeparatorChar).Replace('/', Path.DirectorySeparatorChar);
        string full = Path.GetFullPath(Path.Combine(root, normalized));
        if (!full.StartsWith(root + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase)
            && !full.Equals(root, StringComparison.OrdinalIgnoreCase))
            throw new Exception("Путь файла модпака выходит за пределы папки игры: " + relative);
        return full;
    }

    private static bool IsInstallable(string relative, HashSet<string> seen)
    {
        if (string.IsNullOrWhiteSpace(relative)) return false;
        relative = relative.Replace('\\', '/').TrimStart('/');
        if (relative.Contains("..", StringComparison.Ordinal)) return false;
        if (Path.GetFileName(relative).StartsWith("fluxvisuals", StringComparison.OrdinalIgnoreCase)) return false;
        return seen.Add(relative);
    }

    private static string ReadCurseForgeSha1(JsonElement file)
    {
        if (!file.TryGetProperty("hashes", out var hashes)) return "";
        foreach (var h in hashes.EnumerateArray())
        {
            if (h.TryGetProperty("algo", out var algo) && algo.GetInt32() == 1
                && h.TryGetProperty("value", out var v))
                return v.GetString() ?? "";
        }
        return "";
    }

    private static string ReadHash(JsonElement obj, string name)
        => obj.TryGetProperty("hashes", out var hashes) && hashes.TryGetProperty(name, out var h)
            ? h.GetString() ?? "" : "";

    private static string SanitizeFileName(string name)
    {
        foreach (char c in Path.GetInvalidFileNameChars()) name = name.Replace(c, '_');
        return string.IsNullOrWhiteSpace(name) ? "modpack" : name;
    }
}
