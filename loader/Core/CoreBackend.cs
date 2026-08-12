using System.Text.Json;

namespace FluxVisualsLoader.Core;

/// <summary>IPC-мост: команды из JS-интерфейса → C# и события обратно в UI.</summary>
public class CoreBackend
{
    private readonly MainWindow _window;
    private AppConfig _cfg;
    private GameSession? _session;

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
                case "listMods": SendMods(); break;
                case "toggleMod": ToggleMod(root); break;
                case "removeMod": RemoveMod(root); break;
                case "addMod": await AddModAsync(); break;
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
        Post(new
        {
            ev = "state",
            config = _cfg,
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
        Post(new { ev = "files", files = ModManager.ListFiles(_cfg.GameDir, _cfg.ModIcons), installed });
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
        List<VersionInfo> versions = source == "curseforge"
            ? await CurseForgeApi.VersionsAsync(id, _cfg.Version, type == "mod" ? loader : "", _cfg.CurseforgeKey)
            : await ModrinthApi.VersionsAsync(id, _cfg.Version, type == "mod" ? loader : "");
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

        // если юзер выбрал конкретную версию — она уже в item.fileUrl
        ModItem? resolved = item;
        if (string.IsNullOrEmpty(resolved.FileUrl))
        {
            bool isMod = type == "mod";
            resolved = source switch
            {
                // шейдеры/ресурспаки: последний релиз без привязки к версии игры
                "curseforge" when !isMod => await CurseForgeApi.ResolveLatestAnyAsync(item.Id, _cfg.CurseforgeKey),
                "curseforge" => await CurseForgeApi.ResolveLatestAsync(item.Id, _cfg.Version, "fabric", _cfg.CurseforgeKey),
                _ when !isMod => await ModrinthApi.ResolveLatestAnyAsync(item.Id),
                _ => await ModrinthApi.ResolveLatestAsync(item.Id, _cfg.Version, "fabric"),
            };
        }
        if (resolved == null || string.IsNullOrEmpty(resolved.FileUrl))
        {
            Post(new { ev = "installResult", ok = false, message = "Не удалось найти версию для установки" });
            return;
        }

        // иконку несёт только элемент поиска — переносим её на выбранную версию
        if (string.IsNullOrEmpty(resolved.IconUrl) && !string.IsNullOrEmpty(item.IconUrl))
            resolved.IconUrl = item.IconUrl;

        await Task.Run(async () =>
        {
            try
            {
                string dest = source switch
                {
                    "curseforge" => await CurseForgeApi.DownloadFileAsync(resolved, dir),
                    _ => await ModrinthApi.DownloadFileAsync(resolved, dir),
                };
                Post(new { ev = "installResult", ok = true, file = Path.GetFileName(dest) });
                // запоминаем иконку источника для «Моих файлов»
                string destName = Path.GetFileName(dest);
                if (!string.IsNullOrEmpty(resolved.IconUrl) && !string.IsNullOrEmpty(destName))
                {
                    _cfg.ModIcons[destName] = resolved.IconUrl;
                    _cfg.Save();
                }
                SendMods();
            }
            catch (Exception ex)
            {
                Post(new { ev = "installResult", ok = false, message = ex.Message });
            }
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

    private async Task AddModAsync()
    {
        string modsDir = Path.Combine(_cfg.GameDir, "mods");
        string? file = _window.PickFile(modsDir);
        if (file == null) return;
        ModManager.Add(file, _cfg.GameDir);
        SendMods();
    }

    // ---------- авторизация ----------

    private async Task LoginAsync(JsonElement root)
    {
        string username = root.GetProperty("username").GetString() ?? "";
        string password = root.GetProperty("password").GetString() ?? "";
        string authUrl = root.TryGetProperty("authUrl", out var au) ? au.GetString() ?? _cfg.AuthServerUrl : _cfg.AuthServerUrl;

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

    // ---------- транспорт ----------

    public void Post(object obj)
    {
        _window.PostToWeb(JsonSerializer.Serialize(obj));
    }
}
