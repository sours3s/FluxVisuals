using System.Text.Json;
using System.Text.Json.Nodes;

namespace FluxVisualsLoader.Core;

/// <summary>IPC-мост: команды из JS-интерфейса → C# и события обратно в UI.</summary>
public class CoreBackend
{
    private readonly MainWindow _window;
    private AppConfig _cfg;
    private GameSession? _session;
    private readonly object _downloadPostLock = new();
    private DateTime _lastDownloadPost = DateTime.MinValue;

    public CoreBackend(MainWindow window)
    {
        _window = window;
        _cfg = AppConfig.Load();
    }

    public async Task Handle(string json)
    {
        try
        {
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            string cmd = root.GetProperty("cmd").GetString() ?? "";
            LauncherLog.Info($"Команда UI: {cmd}");
            switch (cmd)
            {
                case "getState": SendState(); break;
                case "saveConfig": SaveConfig(root); break;
                case "login": await LoginAsync(root); break;
                case "logout": await LogoutAsync(); break;
                case "register": await RegisterAsync(root); break;
                case "checkAuth": await CheckAuthAsync(); break;
                case "getModVersion": await GetModVersionAsync(); break;
                case "launch": await LaunchAsync(root); break;
                case "pickFolder": PickFolder(root); break;
                case "pickJava": PickJava(); break;
                case "openPath": OpenPath(root); break;
                case "minimize": _window.Dispatcher.Invoke(_window.Minimize); break;
                case "toggleMaximize": _window.Dispatcher.Invoke(_window.ToggleMaximize); break;
                case "close": _window.Dispatcher.Invoke(_window.Close); break;
                case "drag": _window.Dispatcher.Invoke(_window.Drag); break;
                case "search": await SearchAsync(root); break;
                case "catalogMeta": await CatalogMetaAsync(root); break;
                case "versions": await VersionsAsync(root); break;
                case "openUrl": OpenUrl(root); break;
                case "install": await InstallAsync(root); break;
                case "installModpack": await InstallModpackAsync(root); break;
                case "removeModpack": RemoveModpack(root); break;
                case "listMods": SendMods(); break;
                case "toggleMod": ToggleMod(root); break;
                case "removeMod": RemoveMod(root); break;
                case "addMod": await AddModAsync(root); break;
                case "aiStatus": await AiStatusAsync(); break;
                case "aiAsk": await AiAskAsync(root); break;
            }
        }
        catch (Exception ex)
        {
            Post(new { ev = "error", message = ex.Message });
        }
    }

    // ---------- состояние ----------

    private void SendState()
    {
        SendMods();
        string? java = JavaResolver.FindJava(_cfg.JavaPath);
        LauncherLog.Info($"Состояние: java={(java ?? "НЕ НАЙДЕНА")} gameDir={_cfg.GameDir} (существует={Directory.Exists(_cfg.GameDir)})");
        // Секреты не отдаём в WebView: токен и API-ключ остаются только в C#.
        var safe = JsonNode.Parse(JsonSerializer.Serialize(_cfg))!.AsObject();
        safe.Remove("authToken");
        safe.Remove("curseforgeKey");
        Post(new
        {
            ev = "state",
            config = safe,
            java = java != null,
            gameExists = Directory.Exists(_cfg.GameDir),
        });
    }

    private void SendMods()
    {
        // Список файлов в mods, resourcepacks, shaderpacks для проверки «Установлено»
        var installed = new List<string>();
        string gameDir = _cfg.GameDir;
        foreach (string dir in new[] { Path.Combine(gameDir, "mods"), Path.Combine(gameDir, "resourcepacks"), Path.Combine(gameDir, "shaderpacks") })
        {
            if (!Directory.Exists(dir)) continue;
            foreach (string f in Directory.EnumerateFiles(dir))
                installed.Add(Path.GetFileNameWithoutExtension(f).ToLowerInvariant());
            // распакованные папки ресурспаков/шейдеров тоже «установлены»
            foreach (string d in Directory.EnumerateDirectories(dir))
                installed.Add(Path.GetFileName(d).ToLowerInvariant());
        }
        var files = ModManager.ListFiles(_cfg.GameDir, _cfg.ModIcons, _cfg.ModUrls, _cfg.ModDescriptions,
            _cfg.ModSources, _cfg.ModProjectIds);
        ModMetaService.Enrich(files, _cfg, SendMods);
        Post(new { ev = "files", files, installed });
        ModMetaService.RefreshModpacks(_cfg, SendMods);
        var packs = ModpackManager.List();
        Post(new { ev = "modpacks", modpacks = packs, installedIds = packs.Select(p => p.Id).ToList() });
    }

    private void SaveConfig(JsonElement root)
    {
        if (root.TryGetProperty("config", out var cfgEl))
        {
            var updated = cfgEl.Deserialize<AppConfig>();
            if (updated != null)
            {
                // обычные пользователи не могут менять ватермарку — она равна их нику
                if (!_cfg.IsAdmin) updated.ClientName = updated.Username;
                ApplyAuthFields(updated);
                _cfg = updated;
            }
        }
        _cfg.Save();
        Post(new { ev = "configSaved" });
    }

    /// <summary>JS-интерфейс не знает токен (если логин произошёл после загрузки состояния),
    /// поэтому при применении конфига из UI сохраняем авторизационные поля из текущего _cfg.</summary>
    private void ApplyAuthFields(AppConfig incoming)
    {
        incoming.AuthToken = _cfg.AuthToken;
        incoming.IsAdmin = _cfg.IsAdmin;
        incoming.AuthServerUrl = _cfg.AuthServerUrl;
        incoming.CurseforgeKey = _cfg.CurseforgeKey;
        incoming.Role = _cfg.Role;
        incoming.HasAccess = _cfg.HasAccess;
        incoming.AccessExpiresAt = _cfg.AccessExpiresAt;
        incoming.Username = _cfg.Username;
        incoming.Uid = _cfg.Uid;
        incoming.McNickname = _cfg.McNickname;
        incoming.ModIcons = _cfg.ModIcons;
        incoming.ModUrls = _cfg.ModUrls;
        incoming.ModDescriptions = _cfg.ModDescriptions;
        incoming.ModMetaTried = _cfg.ModMetaTried;
        incoming.ModSources = _cfg.ModSources;
        incoming.ModProjectIds = _cfg.ModProjectIds;
    }

    // ---------- запуск ----------

    private async Task LaunchAsync(JsonElement root)
    {
        if (root.TryGetProperty("config", out var cfgEl))
        {
            var updated = cfgEl.Deserialize<AppConfig>();
            if (updated != null)
            {
                if (!_cfg.IsAdmin) updated.ClientName = updated.Username;
                ApplyAuthFields(updated);
                _cfg = updated;
            }
        }
        _cfg.Save();

        _session = new GameSession(_cfg, (step, percent, message) =>
            Post(new { ev = "progress", step, percent = Math.Round(percent, 1), message }));

        await Task.Run(async () =>
        {
            try
            {
                await _session.LaunchAsync();
                Post(new { ev = "launchState", state = "launched" });
            }
            catch (Exception ex)
            {
                Post(new { ev = "launchState", state = "error", message = ex.Message });
            }
        });
    }

    // ---------- папки ----------

    private void PickFolder(JsonElement root)
    {
        string key = root.GetProperty("key").GetString() ?? "";
        string? path = _window.PickFolder();
        if (path == null) return;
        if (key == "gameDir") _cfg.GameDir = path;
        else if (key == "java") _cfg.JavaPath = path;
        _cfg.Save();
        Post(new { ev = "folderPicked", key, path });
    }

    private void PickJava()
    {
        string? path = _window.PickJava();
        if (path == null) return;
        _cfg.JavaPath = path;
        _cfg.Save();
        Post(new { ev = "folderPicked", key = "java", path });
    }

    private void OpenPath(JsonElement root)
    {
        string key = root.GetProperty("key").GetString() ?? "";
        string? path = key switch
        {
            "gameDir" => _cfg.GameDir,
            "java" => _cfg.JavaPath,
            "mods" => Path.Combine(_cfg.GameDir, "mods"),
            "shaderpacks" => Path.Combine(_cfg.GameDir, "shaderpacks"),
            "resourcepacks" => Path.Combine(_cfg.GameDir, "resourcepacks"),
            "modpacks" => _cfg.GameDir,
            _ => null,
        };
        if (string.IsNullOrWhiteSpace(path)) return;
        _window.OpenPath(path, selectFile: key == "java");
    }

    // ---------- магазины модов ----------

    private async Task SearchAsync(JsonElement root)
    {
        string source = root.GetProperty("source").GetString() ?? "modrinth";
        string query = root.TryGetProperty("query", out var q) ? q.GetString() ?? "" : "";
        string type = root.TryGetProperty("type", out var t) ? t.GetString() ?? "mod" : "mod";
        string category = root.TryGetProperty("category", out var c) ? c.GetString() ?? "" : "";
        string loader = root.TryGetProperty("loader", out var l) ? l.GetString() ?? "fabric" : "fabric";
        string sort = root.TryGetProperty("sort", out var so) ? so.GetString() ?? "" : "";
        int offset = root.TryGetProperty("offset", out var o) ? o.GetInt32() : 0;
        int seq = root.TryGetProperty("seq", out var s) ? s.GetInt32() : 0;

        SearchPage page = source switch
        {
            "curseforge" => await CurseForgeApi.SearchAsync(query, type, category, loader, sort, _cfg.CurseforgeKey, offset),
            _ => await ModrinthApi.SearchAsync(query, type, category, loader, sort, offset),
        };
        Post(new { ev = "searchResult", source, seq, items = page.Items, total = page.Total, offset = page.Offset });
    }

    private async Task CatalogMetaAsync(JsonElement root)
    {
        string source = root.GetProperty("source").GetString() ?? "modrinth";
        string type = root.TryGetProperty("type", out var t) ? t.GetString() ?? "mod" : "mod";
        List<CategoryInfo> cats = source == "curseforge"
            ? await CurseForgeApi.FetchCategoriesAsync(_cfg.CurseforgeKey)
            : await ModrinthApi.FetchCategoriesAsync();
        Post(new { ev = "catalogMeta", source, categories = cats.Where(c => c.Type == type).ToList() });
    }

    private async Task VersionsAsync(JsonElement root)
    {
        string source = root.GetProperty("source").GetString() ?? "modrinth";
        string id = root.GetProperty("id").GetString() ?? "";
        if (string.IsNullOrEmpty(id)) return;
        string type = root.TryGetProperty("type", out var t) ? t.GetString() ?? "mod" : "mod";
        string loader = "fabric";
        string versionLoader = type == "mod" || type == "modpack" ? loader : "";
        List<VersionInfo> versions = source == "curseforge"
            ? await CurseForgeApi.VersionsAsync(id, _cfg.Version, versionLoader, _cfg.CurseforgeKey)
            : await ModrinthApi.VersionsAsync(id, _cfg.Version, versionLoader);
        Post(new { ev = "versions", id, versions });
    }

    private void OpenUrl(JsonElement root)
    {
        string url = root.GetProperty("url").GetString() ?? "";
        if (string.IsNullOrEmpty(url)) return;
        _window.OpenUrl(url);
    }

    private async Task InstallAsync(JsonElement root)
    {
        string source = root.GetProperty("source").GetString() ?? "modrinth";
        var item = root.GetProperty("item").Deserialize<ModItem>() ?? throw new Exception("Нет данных о моде");
        string type = root.TryGetProperty("type", out var t) ? t.GetString() ?? "mod" : "mod";
        string dir = type switch
        {
            "resourcepack" => Path.Combine(_cfg.GameDir, "resourcepacks"),
            "shader" => Path.Combine(_cfg.GameDir, "shaderpacks"),
            _ => Path.Combine(_cfg.GameDir, "mods"),
        };

        // Раньше выбранная в UI версия терялась: ModItem не имел поля selectedVersion.
        ModItem resolved = item;
        if (item.SelectedVersion != null && !string.IsNullOrWhiteSpace(item.SelectedVersion.FileUrl))
        {
            resolved = new ModItem
            {
                Id = item.Id,
                ProjectUrl = item.ProjectUrl,
                Title = item.Title,
                Description = item.Description,
                Author = item.Author,
                Downloads = item.Downloads,
                IconUrl = item.IconUrl,
                VersionId = item.SelectedVersion.VersionId,
                VersionNumber = item.SelectedVersion.VersionNumber,
                FileUrl = item.SelectedVersion.FileUrl,
                FileName = item.SelectedVersion.FileName,
                FileSize = item.SelectedVersion.FileSize,
            };
        }
        else if (string.IsNullOrEmpty(resolved.FileUrl))
        {
            bool isMod = type == "mod";
            resolved = source switch
            {
                "curseforge" when !isMod => await CurseForgeApi.ResolveLatestAnyAsync(item.Id, _cfg.CurseforgeKey)
                    ?? throw new Exception("Не удалось найти версию для установки"),
                "curseforge" => await CurseForgeApi.ResolveLatestAsync(item.Id, _cfg.Version, "fabric", _cfg.CurseforgeKey)
                    ?? throw new Exception("Не удалось найти версию для установки"),
                _ when !isMod => await ModrinthApi.ResolveLatestAnyAsync(item.Id)
                    ?? throw new Exception("Не удалось найти версию для установки"),
                _ => await ModrinthApi.ResolveLatestAsync(item.Id, _cfg.Version, "fabric")
                    ?? throw new Exception("Не удалось найти версию для установки"),
            };
        }
        // API с разрешённой версией возвращает только файл — сохраняем иконку,
        // описание и ссылку проекта из карточки каталога, иначе в «Моих файлах»
        // карточки остаются без иконок и кнопки сайта.
        if (string.IsNullOrEmpty(resolved.IconUrl)) resolved.IconUrl = item.IconUrl;
        if (string.IsNullOrEmpty(resolved.ProjectUrl)) resolved.ProjectUrl = item.ProjectUrl;
        if (string.IsNullOrEmpty(resolved.Description)) resolved.Description = item.Description;
        if (string.IsNullOrEmpty(resolved.Author)) resolved.Author = item.Author;
        if (resolved.Downloads == 0) resolved.Downloads = item.Downloads;
        if (string.IsNullOrEmpty(resolved.Title)) resolved.Title = item.Title;

        if (string.IsNullOrEmpty(resolved.FileUrl))
        {
            Post(new { ev = "installResult", ok = false, message = "Не удалось найти версию для установки" });
            return;
        }

        string downloadId = $"{source}:{item.Id}:{type}";
        string fileName = Path.GetFileName(resolved.FileName ?? "");
        if (string.IsNullOrWhiteSpace(fileName) && Uri.TryCreate(resolved.FileUrl, UriKind.Absolute, out var uri))
            fileName = Path.GetFileName(uri.LocalPath);
        if (string.IsNullOrWhiteSpace(fileName)) fileName = "file";

        await Task.Run(async () =>
        {
            try
            {
                Directory.CreateDirectory(dir);
                string dest = Path.Combine(dir, fileName);
                await Downloader.DownloadAsync(resolved.FileUrl!, dest,
                    (done, total) => PostDownloadProgress(downloadId, item.Title, done,
                        total ?? resolved.FileSize, fileName),
                    default, source == "curseforge" ? _cfg.CurseforgeKey : null);

                Post(new { ev = "downloadDone", id = downloadId, ok = true });
                Post(new { ev = "installResult", ok = true, file = fileName });
                if (!string.IsNullOrEmpty(resolved.IconUrl) || !string.IsNullOrEmpty(resolved.ProjectUrl)
                    || !string.IsNullOrEmpty(resolved.Description))
                {
                    if (!string.IsNullOrEmpty(resolved.IconUrl)) _cfg.ModIcons[fileName] = resolved.IconUrl;
                    if (!string.IsNullOrEmpty(resolved.ProjectUrl)) _cfg.ModUrls[fileName] = resolved.ProjectUrl;
                    if (!string.IsNullOrEmpty(resolved.Description)) _cfg.ModDescriptions[fileName] = resolved.Description;
                    // Источник и ID проекта нужны, чтобы иконку/описание/ссылку брать
                    // из того же каталога, откуда файл скачан, а не из первого попавшегося.
                    if (!string.IsNullOrEmpty(source)) _cfg.ModSources[fileName] = source;
                    if (!string.IsNullOrEmpty(item.Id)) _cfg.ModProjectIds[fileName] = item.Id;
                    _cfg.Save();
                }
                SendMods();
            }
            catch (Exception ex)
            {
                Post(new { ev = "downloadDone", id = downloadId, ok = false, message = ex.Message });
                Post(new { ev = "installResult", ok = false, message = ex.Message });
            }
        });
    }

    private async Task InstallModpackAsync(JsonElement root)
    {
        string source = root.GetProperty("source").GetString() ?? "modrinth";
        var item = root.GetProperty("item").Deserialize<ModItem>() ?? throw new Exception("Нет данных о модпаке");
        string downloadId = $"{source}:{item.Id}:modpack";

        await Task.Run(async () =>
        {
            try
            {
                var pack = await ModpackManager.InstallAsync(source, item, _cfg.GameDir, _cfg.CurseforgeKey,
                    (done, total, file) => PostDownloadProgress(downloadId, item.Title, done, total, file));
                Post(new { ev = "downloadDone", id = downloadId, ok = true });
                Post(new { ev = "installResult", ok = true, file = $"Модпак «{pack.Title}»" });
                SendMods();
            }
            catch (Exception ex)
            {
                Post(new { ev = "downloadDone", id = downloadId, ok = false, message = ex.Message });
                Post(new { ev = "installResult", ok = false, message = ex.Message });
            }
        });
    }

    private void RemoveModpack(JsonElement root)
    {
        string id = root.GetProperty("id").GetString() ?? "";
        var pack = ModpackManager.List().FirstOrDefault(p => p.Id == id);
        if (pack == null) return;
        ModpackManager.Uninstall(pack, _cfg.GameDir);
        SendMods();
        Post(new { ev = "installResult", ok = true, file = $"Модпак «{pack.Title}» удалён" });
    }

    /// <summary>Присылает реальный прогресс загрузки в кнопку-монитор (с лёгким троттлингом).</summary>
    private void PostDownloadProgress(string id, string title, long done, long total, string file)
    {
        lock (_downloadPostLock)
        {
            if (DateTime.Now - _lastDownloadPost < TimeSpan.FromMilliseconds(120)) return;
            _lastDownloadPost = DateTime.Now;
        }
        double percent = total > 0 ? Math.Min(100, 100.0 * done / total) : 0;
        Post(new
        {
            ev = "downloadProgress",
            id,
            title,
            file,
            done,
            total,
            percent = Math.Round(percent, 1),
        });
    }

    // ---------- мои моды ----------

    private void ToggleMod(JsonElement root)
    {
        string file = root.GetProperty("file").GetString() ?? "";
        bool enabled = root.TryGetProperty("enabled", out var e) && e.GetBoolean();
        ModManager.Toggle(file, enabled);
        SendMods();
    }

    private void RemoveMod(JsonElement root)
    {
        string file = root.GetProperty("file").GetString() ?? "";
        ModManager.Remove(file);
        SendMods();
    }

    private async Task AddModAsync(JsonElement root)
    {
        string type = root.TryGetProperty("type", out var t) ? t.GetString() ?? "mod" : "mod";
        string mode = root.TryGetProperty("mode", out var md) ? md.GetString() ?? "file" : "file";
        string initialDir = type switch
        {
            "shader" => Path.Combine(_cfg.GameDir, "shaderpacks"),
            "resourcepack" => Path.Combine(_cfg.GameDir, "resourcepacks"),
            "modpack" => Path.Combine(_cfg.GameDir, "mods"),
            _ => Path.Combine(_cfg.GameDir, "mods"),
        };
        string? path = _window.PickAddFile(type, initialDir, mode);
        if (string.IsNullOrWhiteSpace(path)) return;

        // Распакованный шейдер/ресурспак — копируем всей папкой.
        if (Directory.Exists(path))
        {
            if (type is "shader" or "resourcepack") ModManager.AddFolder(path, _cfg.GameDir, type);
            SendMods();
            return;
        }

        if (type == "modpack")
        {
            string downloadId = "local:" + Path.GetFileName(path);
            await Task.Run(async () =>
            {
                try
                {
                    string source = ModpackManager.DetectSource(path);
                    var item = new ModItem
                    {
                        Id = downloadId,
                        Title = Path.GetFileNameWithoutExtension(path),
                        FileName = Path.GetFileName(path),
                        FileUrl = "",
                    };
                    var pack = await ModpackManager.InstallAsync(source, item, _cfg.GameDir, _cfg.CurseforgeKey,
                        (done, total, file) => PostDownloadProgress(downloadId, item.Title, done, total, file),
                        default, path);
                    Post(new { ev = "downloadDone", id = downloadId, ok = true });
                    Post(new { ev = "installResult", ok = true, file = $"Модпак «{pack.Title}»" });
                    SendMods();
                }
                catch (Exception ex)
                {
                    Post(new { ev = "downloadDone", id = downloadId, ok = false, message = ex.Message });
                    Post(new { ev = "error", message = ex.Message });
                }
            });
            return;
        }

        ModManager.Add(path, _cfg.GameDir, type);
        SendMods();
    }

    // ---------- авторизация ----------

    private async Task LoginAsync(JsonElement root)
    {
        string username = root.GetProperty("username").GetString() ?? "";
        string password = root.GetProperty("password").GetString() ?? "";
        // Сервер авторизации берётся только из конфигурации лаунчера.
        string authUrl = _cfg.AuthServerUrl;

        var result = await AuthService.LoginAsync(username, password, authUrl);
        if (result?.Success == true)
        {
            _cfg.Username = result.Username;
            _cfg.AuthToken = result.Token;
            _cfg.AuthServerUrl = authUrl;
            _cfg.IsAdmin = result.IsAdmin;
            _cfg.Role = result.Role;
            _cfg.HasAccess = result.HasAccess;
            _cfg.AccessExpiresAt = result.AccessExpiresAt;
            _cfg.Uid = result.Uid;
            // Minecraft-ник по умолчанию = ник аккаунта (пользователь может сменить в настройках)
            if (string.IsNullOrWhiteSpace(_cfg.McNickname)) _cfg.McNickname = result.Username;
            _cfg.Save();
            SendState(); // обновляем state.config в UI (чтобы там был актуальный токен)
            Post(new { ev = "authSuccess", username = result.Username, isAdmin = result.IsAdmin, role = result.Role, hasAccess = result.HasAccess, accessExpiresAt = result.AccessExpiresAt, uid = result.Uid });
        }
        else
        {
            Post(new { ev = "authError", message = result?.Error ?? "Login failed" });
        }
    }

    private async Task LogoutAsync()
    {
        _cfg.AuthToken = "";
        _cfg.Save();
        Post(new { ev = "authLogout" });
    }

    private async Task RegisterAsync(JsonElement root)
    {
        string username = root.GetProperty("username").GetString() ?? "";
        string password = root.GetProperty("password").GetString() ?? "";
        bool isAdmin = root.TryGetProperty("isAdmin", out var ia) && ia.GetBoolean();
        string authUrl = root.TryGetProperty("authUrl", out var au) ? au.GetString() ?? _cfg.AuthServerUrl : _cfg.AuthServerUrl;
        string adminToken = _cfg.AuthToken;

        bool ok = await AuthService.RegisterAsync(username, password, isAdmin, authUrl, adminToken);
        if (ok)
            Post(new { ev = "registerSuccess", username });
        else
            Post(new { ev = "registerError", message = "Registration failed" });
    }

    private async Task CheckAuthAsync()
    {
        var auth = await AuthService.CheckAuthAsync(_cfg);
        if (auth.Authenticated)
        {
            _cfg.Uid = auth.Uid;
            _cfg.Save();
            Post(new
            {
                ev = "authChecked",
                username = auth.Username ?? _cfg.Username,
                authenticated = true,
                role = auth.Role,
                isAdmin = auth.IsAdmin,
                hasAccess = auth.HasAccess,
                accessExpiresAt = auth.AccessExpiresAt,
                uid = auth.Uid
            });
        }
        else
            Post(new { ev = "authChecked", authenticated = false });
    }

    private async Task GetModVersionAsync()
    {
        var info = await AuthService.GetModVersionAsync(_cfg.AuthServerUrl);
        if (info != null)
            Post(new { ev = "modVersion", downloadUrl = info.DownloadUrl, version = info.Version, fileName = info.FileName });
        else
            Post(new { ev = "modVersion", error = "Failed to check mod version" });
    }

    // ---------- Catal: ИИ-помощник ----------

    private async Task AiStatusAsync()
    {
        if (string.IsNullOrWhiteSpace(_cfg.AuthToken))
        {
            Post(new { ev = "aiStatus", available = false, reason = "Войдите в аккаунт, чтобы использовать Catal." });
            return;
        }

        var result = await AiClient.StatusAsync(_cfg);
        Post(new { ev = "aiStatus", available = result.Available, reason = result.Error });
    }

    private async Task AiAskAsync(JsonElement root)
    {
        string message = root.TryGetProperty("message", out var m) ? m.GetString() ?? "" : "";
        if (string.IsNullOrWhiteSpace(message))
        {
            Post(new { ev = "aiError", message = "Напишите вопрос." });
            return;
        }

        var history = root.TryGetProperty("history", out var h) && h.ValueKind == JsonValueKind.Array
            ? h.Clone()
            : JsonSerializer.SerializeToElement(Array.Empty<string>());
        string? image = root.TryGetProperty("image", out var im) && im.ValueKind == JsonValueKind.String
            ? im.GetString()
            : null;

        // Диагностику собираем сами — JS не может подсунуть фейковый контекст или токен.
        var context = BuildAiContext();
        Post(new { ev = "aiThinking", busy = true });
        var result = await AiClient.AskAsync(_cfg, message, history, image, context);
        Post(new { ev = "aiThinking", busy = false });
        if (result.Error != null)
            Post(new { ev = "aiError", message = result.Error });
        else
            Post(new { ev = "aiAnswer", answer = result.Answer, model = result.Model });
    }

    private JsonElement BuildAiContext()
    {
        string version = GetType().Assembly.GetName().Version?.ToString() ?? "unknown";
        var ctx = new JsonObject
        {
            ["launcherVersion"] = version,
            ["modVersion"] = _cfg.ModVersion,
            ["role"] = _cfg.Role,
            ["hasAccess"] = _cfg.HasAccess,
            ["gameDirExists"] = Directory.Exists(_cfg.GameDir),
            ["javaFound"] = JavaResolver.FindJava(_cfg.JavaPath) != null,
            ["ramMb"] = _cfg.RamMb
        };

        var installed = new List<string>();
        foreach (string dir in new[] { "mods", "resourcepacks", "shaderpacks" })
        {
            string path = Path.Combine(_cfg.GameDir, dir);
            if (!Directory.Exists(path)) continue;
            try
            {
                foreach (string f in Directory.EnumerateFiles(path).Take(30))
                    installed.Add(Path.GetFileName(f));
                foreach (string d in Directory.EnumerateDirectories(path).Take(10))
                    installed.Add(Path.GetFileName(d) + "/");
            }
            catch { }
        }
        ctx["installedFiles"] = installed.Count == 0 ? "(пусто)" : string.Join(", ", installed.Take(40));

        try
        {
            string log = LauncherLog.LogPath;
            if (File.Exists(log))
                ctx["lastLog"] = string.Join(" | ", File.ReadLines(log).TakeLast(12));
            string crash = Path.Combine(LauncherLog.LogDir, "crash.log");
            if (File.Exists(crash))
                ctx["lastCrash"] = string.Join(" | ", File.ReadLines(crash).TakeLast(8));
        }
        catch { }

        using var doc = JsonDocument.Parse(ctx.ToJsonString());
        return doc.RootElement.Clone();
    }

    // ---------- транспорт ----------

    public void Post(object obj)
    {
        _window.PostToWeb(JsonSerializer.Serialize(obj));
    }
}
