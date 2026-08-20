using System.Diagnostics;
using System.IO.Compression;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace FluxVisualsLoader.Core;

/// <summary>Оркестратор запуска: установка (jar, библиотеки, нативы, ассеты, fabric, мод) + запуск процесса.</summary>
public class GameSession
{
    public delegate void ProgressHandler(string step, double percent, string message);

    private readonly AppConfig _cfg;
    private readonly ProgressHandler _progress;

    // Одноразовый launch-тикет: доказательство для мода, что игра запущена через лоадер.
    private string? _launchTicket;
    private string? _launchChallenge;
    private string? _authServerUrl;

    public GameSession(AppConfig cfg, ProgressHandler progress)
    {
        _cfg = cfg;
        _progress = progress;
    }

    public AppConfig Config => _cfg;

    public async Task LaunchAsync(CancellationToken ct = default)
    {
        string gameDir = _cfg.GameDir;
        if (string.IsNullOrWhiteSpace(gameDir)) throw new Exception("Не указана папка игры.");
        LauncherLog.Info($"=== Запуск: ник={_cfg.Username} версия={_cfg.Version} ram={_cfg.RamMb} gameDir={gameDir} ===");
        Directory.CreateDirectory(gameDir);

        // ---- Java ----
        _progress("java", 2, "Поиск Java 21...");
        string? java = JavaResolver.FindJava(_cfg.JavaPath);
        if (java == null)
        {
            _progress("java", 3, "Java не найдена — скачиваю JDK 21 (~190 МБ)...");
            java = await JavaResolver.DownloadJavaAsync(gameDir, ct);
            _cfg.JavaPath = java;
        }
        if (!JavaResolver.IsJavaUsable(java))
        {
            _progress("java", 3, "Найденная Java не подходит — скачиваю JDK 21...");
            java = await JavaResolver.DownloadJavaAsync(gameDir, ct);
        }

        // ---- Auth check + проверка роли (доступ оплачен?) ----
        _progress("auth", 4, "Проверка авторизации...");
        var auth = await AuthService.CheckAuthAsync(_cfg);
        if (!auth.Authenticated)
        {
            throw new Exception("Ошибка авторизации. Войдите в лаунчер.");
        }
        if (!auth.HasAccess)
        {
            throw new Exception(auth.Role == "user"
                ? "Доступ не оплачен. Купи клиент на сайте, чтобы играть."
                : "Срок доступа истёк. Продли подписку на сайте.");
        }

        // ---- Метаданные ----
        _progress("metadata", 5, "Получаю метаданные версии...");
        string versionJsonPath = await VersionResolver.ResolveVersionJsonAsync(_cfg.Version, Path.Combine(gameDir, "versions"), ct);
        using var doc = JsonDocument.Parse(await File.ReadAllTextAsync(versionJsonPath, ct));
        var root = doc.RootElement;

        var libsDir = Path.Combine(gameDir, "libraries");
        var assetsDir = Path.Combine(gameDir, "assets");
        var nativesDir = Path.Combine(gameDir, "natives");
        var modsDir = Path.Combine(gameDir, "mods");
        var configDir = Path.Combine(gameDir, "config");
        foreach (var d in new[] { libsDir, assetsDir, nativesDir, modsDir, configDir })
            Directory.CreateDirectory(d);

        // ---- Client jar ----
        _progress("client", 10, "Клиентский jar...");
        string clientJar = Path.Combine(gameDir, "versions", _cfg.Version, _cfg.Version + ".jar");
        if (!Downloader.FileValid(clientJar))
        {
            string? url = VersionResolver.GetClientJarUrl(root);
            if (url == null) throw new Exception("В метаданных нет ссылки на клиентский jar.");
            await Downloader.DownloadAsync(url, clientJar, null, ct);
        }

        // ---- Библиотеки ----
        var libs = VersionResolver.ResolveLibraries(root, "windows");
        var classpath = new List<string> { clientJar };
        int libTotal = libs.Count(l => !l.IsEmpty && !l.IsNative);
        int libDone = 0;
        _progress("libraries", 18, "Библиотеки...");
        foreach (var lib in libs)
        {
            if (lib.IsEmpty) continue;
            string dest = Path.Combine(libsDir, lib.Path);
            if (!Downloader.FileValid(dest))
            {
                _progress("libraries", 18 + 20.0 * libDone / Math.Max(1, libTotal), "Библиотека " + lib.Name);
                await Downloader.DownloadAsync(lib.Url, dest, null, ct);
            }
            libDone++;
            if (!lib.IsNative) classpath.Add(dest);
        }

        // ---- Нативные библиотеки ----
        _progress("natives", 42, "Нативные библиотеки...");
        foreach (var lib in libs)
        {
            if (!lib.IsNative) continue;
            string dest = Path.Combine(libsDir, lib.Path);
            if (!Downloader.FileValid(dest))
            {
                _progress("natives", 42, "Нативы: " + lib.Name);
                await Downloader.DownloadAsync(lib.Url, dest, null, ct);
            }
            try { ZipFile.ExtractToDirectory(dest, nativesDir, true); } catch { }
        }

        // ---- Ассеты ----
        _progress("assets", 48, "Ассеты (индексы)...");
        VersionResolver.GetAssetIndexInfo(root, out string indexId, out string indexUrl, out _);
        string indexPath = Path.Combine(assetsDir, "indexes", indexId + ".json");
        if (!Downloader.FileValid(indexPath))
            await Downloader.DownloadAsync(indexUrl, indexPath, null, ct);
        await DownloadAssetsAsync(indexPath, assetsDir, ct);

        // ---- Fabric ----
        _progress("fabric", 68, "Fabric Loader...");
        const string loaderVersion = "0.19.3";
        string fabLoaderPath = Path.Combine(libsDir, $"net/fabricmc/fabric-loader/{loaderVersion}/fabric-loader-{loaderVersion}.jar");
        if (!Downloader.FileValid(fabLoaderPath))
            await Downloader.DownloadAsync($"https://maven.fabricmc.net/net/fabricmc/fabric-loader/{loaderVersion}/fabric-loader-{loaderVersion}.jar", fabLoaderPath, null, ct);
        classpath.Add(fabLoaderPath);

        string intermediaryPath = Path.Combine(libsDir, $"net/fabricmc/intermediary/{_cfg.Version}/intermediary-{_cfg.Version}.jar");
        if (!Downloader.FileValid(intermediaryPath))
            await Downloader.DownloadAsync($"https://maven.fabricmc.net/net/fabricmc/intermediary/{_cfg.Version}/intermediary-{_cfg.Version}.jar", intermediaryPath, null, ct);
        classpath.Add(intermediaryPath);

        // зависимости fabric-loader (ASM + sponge-mixin), см. fabric-installer.json
        string[] loaderDeps =
        {
            "org.ow2.asm:asm:9.10.1",
            "org.ow2.asm:asm-analysis:9.10.1",
            "org.ow2.asm:asm-commons:9.10.1",
            "org.ow2.asm:asm-tree:9.10.1",
            "org.ow2.asm:asm-util:9.10.1",
            "net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7",
        };
        foreach (string dep in loaderDeps)
        {
            string rel = VersionResolver.MavenToPath(dep);
            string dest = Path.Combine(libsDir, rel);
            if (!Downloader.FileValid(dest))
                await Downloader.DownloadAsync("https://maven.fabricmc.net/" + rel, dest, null, ct);
            classpath.Add(dest);
        }

        _progress("fabric", 74, "Fabric API...");
        const string apiVersion = "0.141.6+1.21.11";
        string apiJar = Path.Combine(modsDir, $"fabric-api-{apiVersion}.jar");
        if (!Downloader.FileValid(apiJar))
            await Downloader.DownloadAsync($"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{apiVersion}/fabric-api-{apiVersion}.jar", apiJar, null, ct);

        // ---- Мод (скачивание с удалённого хоста + автообновление) ----
        _progress("mod", 76, "Мод FluxVisuals...");

        // Сначала проверяем GitHub (приоритет) - там всегда актуальная версия
        string remoteVersion = "";
        string remoteFileName = "fluxvisuals-1.0.17.jar"; // fallback
        string remoteUrl = "";
        try
        {
            using var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(30) };
            httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("FluxVisualsLoader/1.0");
            httpClient.DefaultRequestHeaders.Accept.ParseAdd("application/vnd.github+json");
            var githubUrl = "https://api.github.com/repos/sours3s/FluxVisuals/releases/latest";
            var response = await httpClient.GetStringAsync(githubUrl);
            using var ghDoc = JsonDocument.Parse(response);
            var ghRoot = ghDoc.RootElement;
            var tagName = ghRoot.GetProperty("tag_name").GetString() ?? "";
            if (tagName.StartsWith("v"))
            {
                remoteVersion = tagName.Substring(1); // Remove 'v' prefix
                // Find the .jar asset
                foreach (var asset in ghRoot.GetProperty("assets").EnumerateArray())
                {
                    var name = asset.GetProperty("name").GetString() ?? "";
                    if (name.EndsWith(".jar"))
                    {
                        remoteFileName = name;
                        remoteUrl = asset.GetProperty("browser_download_url").GetString() ?? "";
                        if (!string.IsNullOrWhiteSpace(remoteUrl))
                        {
                            _cfg.ModDownloadUrl = remoteUrl;
                            _cfg.Save();
                            LauncherLog.Info($"Updated mod URL from GitHub: {remoteUrl}");
                        }
                        break;
                    }
                }
            }
        }
        catch (Exception ex)
        {
            LauncherLog.Warn($"Mod version check from GitHub failed: {ex.Message}");
        }

        // Fallback: проверяем AuthServer если GitHub недоступен
        if (string.IsNullOrEmpty(remoteVersion))
        {
            try
            {
                using var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(60) };
                var modInfoUrl = _cfg.AuthServerUrl.TrimEnd('/') + "/api/mod/version";
                var response = await httpClient.GetStringAsync(modInfoUrl);
                using var modDoc = JsonDocument.Parse(response);
                var modRoot = modDoc.RootElement;
                remoteUrl = modRoot.GetProperty("downloadUrl").GetString() ?? "";
                remoteVersion = modRoot.TryGetProperty("version", out var v) ? v.GetString() ?? "" : "";
                remoteFileName = modRoot.TryGetProperty("fileName", out var fn) ? fn.GetString() ?? "" : remoteFileName;
                if (!string.IsNullOrWhiteSpace(remoteUrl))
                {
                    _cfg.ModDownloadUrl = remoteUrl;
                    _cfg.Save();
                    LauncherLog.Info($"Updated mod URL from AuthServer: {remoteUrl}");
                }
            }
            catch (Exception ex)
            {
                LauncherLog.Warn($"Mod version check from AuthServer failed: {ex.Message}");
            }
        }

        string modDst = Path.Combine(modsDir, remoteFileName);

        // Удаляем ВСЕ старые версии мода из папки mods (fluxvisuals-*.jar, fluxvisuals-mod-*.jar)
        try
        {
            foreach (var oldMod in Directory.GetFiles(modsDir, "fluxvisuals*.jar"))
            {
                if (!oldMod.Equals(modDst, StringComparison.OrdinalIgnoreCase))
                {
                    File.Delete(oldMod);
                    LauncherLog.Info($"Deleted old mod: {Path.GetFileName(oldMod)}");
                }
            }
        }
        catch (Exception ex)
        {
            LauncherLog.Warn($"Could not clean old mods: {ex.Message}");
        }

        string modUrl = _cfg.ModDownloadUrl;
        if (string.IsNullOrWhiteSpace(modUrl))
        {
            throw new Exception("Не задан URL для скачивания мода (ModDownloadUrl в config.json).");
        }

        // Автообновление мода: если версия на сервере изменилась — удаляем старый jar и качаем заново
        // (иначе Downloader пропустит скачивание, так как файл уже существует).
        bool needsModRedownload = false;
        if (!string.IsNullOrEmpty(remoteVersion) && !string.Equals(remoteVersion, _cfg.ModVersion, StringComparison.Ordinal))
        {
            LauncherLog.Info($"Mod version changed ({_cfg.ModVersion} -> {remoteVersion}), redownloading...");
            needsModRedownload = true;
        }
        // Страховка: маркер версии на сервере захардкожен и при новом билде не меняется,
        // поэтому автообновление по нему молча не срабатывало. Сверяем размер удалённого jar с локальным.
        else if (File.Exists(modDst) && await ModSizeChangedAsync(modUrl, modDst, ct))
        {
            LauncherLog.Info("Mod size changed on server, redownloading...");
            needsModRedownload = true;
        }
        if (needsModRedownload)
        {
            try { if (File.Exists(modDst)) File.Delete(modDst); }
            catch (Exception ex) { LauncherLog.Warn($"Could not delete old mod: {ex.Message}"); }
        }

        LauncherLog.Info($"Downloading mod from: {modUrl}");
        await Downloader.DownloadAsync(modUrl, modDst, null, ct);
        if (!string.IsNullOrEmpty(remoteVersion))
        {
            _cfg.ModVersion = remoteVersion;
            _cfg.Save();
        }
        LauncherLog.Info($"Mod downloaded: {new FileInfo(modDst).Length:N0} bytes");

        // ---- Конфиг мода ----
        _progress("config", 80, "Конфигурация мода...");
        WriteModConfig(configDir);

        // ---- Launch-тикет: доказательство запуска через лоадер (мод проверяет подпись) ----
        _progress("auth", 84, "Выпуск launch-тикета…");
        await IssueLaunchTicketAsync(ct);
        if (string.IsNullOrEmpty(_launchTicket))
            throw new Exception("Не удалось получить launch-тикет. Проверьте авторизацию и повторите запуск.");

        // ---- Запуск ----
        var jvmArgs = VersionResolver.ResolveJvmArgs(root, "windows");
        _progress("launch", 92, "Запуск Minecraft...");
        LauncherLog.Info($"Java: {java}");

        var psi = BuildProcess(java, classpath, nativesDir, assetsDir, indexId, jvmArgs, gameDir);
        var process = Process.Start(psi);
        if (process == null) throw new Exception("Не удалось запустить процесс Java.");
        LauncherLog.Info($"Java-процесс запущен (PID {process.Id}), классы: {classpath.Count}, моды: {modsDir}");

        // вывод игры пишем в файл, чтобы видеть краш/ошибки
        string logsDir = Path.Combine(gameDir, "logs");
        Directory.CreateDirectory(logsDir);
        PumpOutput(process.StandardOutput, Path.Combine(logsDir, "java-stdout.log"));
        PumpOutput(process.StandardError, Path.Combine(logsDir, "java-stderr.log"));

        _progress("launch", 100, "Minecraft запущен");
    }

    private static void PumpOutput(TextReader reader, string file)
    {
        Task.Run(() =>
        {
            try
            {
                using var writer = new StreamWriter(file, append: true) { AutoFlush = true };
                string? line;
                while ((line = reader.ReadLine()) != null) writer.WriteLine(line);
            }
            catch { }
        });
    }

    private void WriteModConfig(string configDir)
    {
        // формат мода: modules = { имя: { enabled: bool } }
        var modules = _cfg.Modules.ToDictionary(
            kv => kv.Key,
            kv => (object)new Dictionary<string, bool> { ["enabled"] = kv.Value });
        // ватермарка = ник аккаунта (с сайта), а НЕ майнкрафтерский ник
        var root = new Dictionary<string, object?>
        {
            ["accent"] = _cfg.Accent,
            ["accentSecond"] = _cfg.AccentSecond,
            ["clientName"] = _cfg.Username,
            ["modules"] = modules,
        };
        File.WriteAllText(Path.Combine(configDir, "fluxvisuals.json"),
            JsonSerializer.Serialize(root, new JsonSerializerOptions { WriteIndented = true }));
    }

    /// <summary>Запрашивает у сервера одноразовый RSA-подписанный launch-тикет (JWT RS256),
    /// привязанный к HWID и к случайному challenge. Тикет живёт ~60 сек и «сжигается» модом.</summary>
    private async Task IssueLaunchTicketAsync(CancellationToken ct)
    {
        _launchTicket = null;
        _launchChallenge = null;
        _authServerUrl = _cfg.AuthServerUrl?.TrimEnd('/') ?? "";

        // Валидация URL — защита от кривых конфигов (дубль /api, лишние слеши, пустая строка)
        if (string.IsNullOrWhiteSpace(_authServerUrl))
        {
            throw new Exception("AuthServerUrl не задан в конфиге. Удалите config.json и перезапустите лоадер.");
        }
        if (!_authServerUrl.StartsWith("http://", StringComparison.OrdinalIgnoreCase)
            && !_authServerUrl.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
        {
            throw new Exception($"AuthServerUrl некорректен: '{_authServerUrl}'. Должен начинаться с http:// или https://");
        }
        if (_authServerUrl.Contains("/api/", StringComparison.OrdinalIgnoreCase))
        {
            // Пользователь случайно вписал /api в URL — убираем дубль
            _authServerUrl = _authServerUrl.Substring(0, _authServerUrl.IndexOf("/api/", StringComparison.OrdinalIgnoreCase));
            _authServerUrl = _authServerUrl.TrimEnd('/');
        }

        if (string.IsNullOrWhiteSpace(_cfg.AuthToken)) return;

        var hwid = AuthService.GetHwid();
        for (int attempt = 1; attempt <= 3; attempt++)
        {
            try
            {
                string challenge = Convert.ToBase64String(System.Security.Cryptography.RandomNumberGenerator.GetBytes(32))
                    .TrimEnd('=').Replace('+', '-').Replace('/', '_');
                var payload = JsonSerializer.Serialize(new { challenge, hwid });
                using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(20) };
                string requestUrl = _authServerUrl + "/api/launch/issue";
                System.Diagnostics.Debug.WriteLine($"[Launch] POST {requestUrl}");
                using var req = new HttpRequestMessage(HttpMethod.Post, requestUrl)
                {
                    Content = new StringContent(payload, Encoding.UTF8, "application/json")
                };
                req.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", _cfg.AuthToken);

                using var resp = await http.SendAsync(req, ct);
                if (resp.IsSuccessStatusCode)
                {
                    var json = await resp.Content.ReadAsStringAsync(ct);
                    using var doc = JsonDocument.Parse(json);
                    _launchTicket = doc.RootElement.TryGetProperty("ticket", out var t) ? t.GetString() : null;
                    if (!string.IsNullOrEmpty(_launchTicket))
                    {
                        _launchChallenge = challenge;
                        LauncherLog.Info("Launch ticket issued.");
                        return;
                    }
                }
                else
                {
                    var err = await resp.Content.ReadAsStringAsync(ct);
                    LauncherLog.Warn($"Launch ticket issue failed ({resp.StatusCode}): {err}");
                    if (attempt == 3)
                    {
                        // Токен протух/невалиден — сбрасываем, чтобы следующий запуск попросил войти.
                        if (resp.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                        {
                            _cfg.AuthToken = "";
                            _cfg.Save();
                        }
                        throw new Exception(DescribeLaunchError(resp.StatusCode, err));
                    }
                }
            }
            catch (Exception ex) when (attempt < 3)
            {
                LauncherLog.Warn($"Launch ticket attempt {attempt} failed: {ex.Message}");
                await Task.Delay(2000, ct);
            }
        }
    }

    /// <summary>Превращает ответ сервера в понятное сообщение: статус + код + подсказка пользователю.
    /// Раньше здесь было «Сервер отклонил выдачу launch-тикета: » без причины — не диагностируемо.</summary>
    private static string DescribeLaunchError(System.Net.HttpStatusCode status, string body)
    {
        string code = "";
        string? message = null;
        try
        {
            using var doc = JsonDocument.Parse(body);
            var root = doc.RootElement;
            code = root.TryGetProperty("code", out var c) ? c.GetString() ?? "" : "";
            message = root.TryGetProperty("error", out var m) && m.ValueKind == JsonValueKind.String
                ? m.GetString() : null;
        }
        catch { /* тело не JSON (например пустое) — ниже fallback */ }

        string hint = (status, code) switch
        {
            (System.Net.HttpStatusCode.Unauthorized, _) => "Сессия истекла или токен недействителен — войдите в лаунчер заново.",
            (_, "no_access") => "Доступ к клиенту отсутствует или истёк. Проверьте оплату в личном кабинете.",
            (_, "hwid_mismatch") => "HWID не совпадает с этим аккаунтом. Войдите заново с этого компьютера.",
            (_, "not_configured") or (_, "invalid_key_config") => "Ошибка на сервере: ключ launch-тикетов не настроен. Сообщите в поддержку.",
            (_, "invalid_challenge") => "Ошибка на сервере: некорректный challenge.",
            (System.Net.HttpStatusCode.InternalServerError, _) => "Внутренняя ошибка сервера. Попробуйте позже.",
            _ => ""
        };

        string detail = !string.IsNullOrEmpty(message) ? message
            : !string.IsNullOrEmpty(hint) ? hint
            : string.IsNullOrWhiteSpace(body) ? "Сервер вернул ошибку без пояснения."
            : body;

        return $"Сервер отклонил выдачу launch-тикета ({(int)status}). {detail}";
    }

    /// <summary>HEAD к URL мода: true, если удалённый размер не совпадает с локальным jar.
    /// Автообновление по маркеру версии ненадёжно (маркер на сервере захардкожен), поэтому сверяем размер —
    /// так лоадер подхватит любой новый билд мода при следующем запуске.</summary>
    private static async Task<bool> ModSizeChangedAsync(string url, string localPath, CancellationToken ct)
    {
        try
        {
            using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(30) };
            using var req = new HttpRequestMessage(HttpMethod.Head, url);
            using var resp = await http.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
            if (!resp.IsSuccessStatusCode || resp.Content.Headers.ContentLength is not long remote || remote <= 0)
                return false; // не смогли узнать размер — установленный мод не трогаем
            return remote != new FileInfo(localPath).Length;
        }
        catch (Exception ex)
        {
            LauncherLog.Warn($"Mod size check failed: {ex.Message}");
            return false;
        }
    }

    private ProcessStartInfo BuildProcess(string java, List<string> classpath, string nativesDir, string assetsDir,
        string indexId, List<string> jvmArgs, string gameDir)
    {
        var psi = new ProcessStartInfo
        {
            FileName = java,
            WorkingDirectory = gameDir,
            UseShellExecute = false,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            RedirectStandardInput = false,
            // Без этого окно консоли (CMD) висит всё время игры
            CreateNoWindow = true,
            WindowStyle = ProcessWindowStyle.Hidden,
        };

        psi.ArgumentList.Add($"-Xmx{_cfg.RamMb}m");
        psi.ArgumentList.Add("-XX:+UnlockExperimentalVMOptions");
        psi.ArgumentList.Add("-XX:+UseG1GC");
        psi.ArgumentList.Add("-Djava.library.path=" + nativesDir);
        psi.ArgumentList.Add("-Dminecraft.launcher.brand=fluxvisuals");
        psi.ArgumentList.Add("-Dminecraft.launcher.version=1.0.0");
        // Launch-тикет: мод проверяет подпись и «сжигает» его на сервере. Без него мод не работает.
        if (!string.IsNullOrEmpty(_launchTicket))
            psi.ArgumentList.Add("-Dfluxvisuals.ticket=" + _launchTicket);
        if (!string.IsNullOrEmpty(_launchChallenge))
            psi.ArgumentList.Add("-Dfluxvisuals.challenge=" + _launchChallenge);
        if (!string.IsNullOrEmpty(_authServerUrl))
            psi.ArgumentList.Add("-Dfluxvisuals.authserver=" + _authServerUrl);
        psi.ArgumentList.Add("-Dlog4j2.formatMsgNoLookups=true");

        string classpathStr = string.Join(";", classpath);
        string libsDir = Path.GetDirectoryName(classpath[1])!;
        foreach (var j in jvmArgs)
        {
            if (string.IsNullOrWhiteSpace(j)) continue;
            // подстановка плейсхолдеров Mojang
            string arg = j
                .Replace("${natives_directory}", nativesDir)
                .Replace("${library_directory}", libsDir)
                .Replace("${launcher_name}", "fluxvisuals")
                .Replace("${launcher_version}", "1.0.0");
            if (arg == "-cp" || arg == "${classpath}") continue; // classpath задаём сами
            psi.ArgumentList.Add(arg);
        }
        psi.ArgumentList.Add("-cp");
        psi.ArgumentList.Add(classpathStr);
        psi.ArgumentList.Add("net.fabricmc.loader.impl.launch.knot.KnotClient");
        psi.ArgumentList.Add("--version"); psi.ArgumentList.Add(_cfg.Version);
        psi.ArgumentList.Add("--gameDir"); psi.ArgumentList.Add(gameDir);
        psi.ArgumentList.Add("--assetsDir"); psi.ArgumentList.Add(assetsDir);
        psi.ArgumentList.Add("--assetIndex"); psi.ArgumentList.Add(indexId);
        string mcNick = string.IsNullOrWhiteSpace(_cfg.McNickname) ? _cfg.Username : _cfg.McNickname;
        psi.ArgumentList.Add("--username"); psi.ArgumentList.Add(mcNick);
        psi.ArgumentList.Add("--session"); psi.ArgumentList.Add("0");
        psi.ArgumentList.Add("--uuid"); psi.ArgumentList.Add(OfflineUuid(mcNick));
        psi.ArgumentList.Add("--accessToken"); psi.ArgumentList.Add("0");
        psi.ArgumentList.Add("--versionType"); psi.ArgumentList.Add("release");
        return psi;
    }

    private async Task DownloadAssetsAsync(string indexPath, string assetsDir, CancellationToken ct)
    {
        using var index = JsonDocument.Parse(await File.ReadAllTextAsync(indexPath, ct));
        var objects = index.RootElement.GetProperty("objects");
        long total = 0;
        foreach (var o in objects.EnumerateObject()) total += o.Value.GetProperty("size").GetInt64();

        long done = 0;
        int count = 0;
        foreach (var o in objects.EnumerateObject())
        {
            string hash = o.Value.GetProperty("hash").GetString()!;
            string dest = Path.Combine(assetsDir, "objects", hash[..2], hash);
            if (!Downloader.FileValid(dest))
            {
                await Downloader.DownloadAsync("https://resources.download.minecraft.net/" + hash[..2] + "/" + hash, dest, null, ct);
            }
            done += o.Value.GetProperty("size").GetInt64();
            if ((++count % 40) == 0)
                _progress("assets", 48 + 18.0 * done / Math.Max(1, total), $"Ассеты: {count} файлов");
        }
        _progress("assets", 66, "Ассеты готовы");
    }

    // ===== AES-256-CBC расшифровка мода с HWID-привязкой =====
    private static byte[] DecryptAes256(byte[] encrypted)
    {
        string keySource = "FluxVisuals" + GetMachineId();
        byte[] keyHash = SHA256.HashData(Encoding.UTF8.GetBytes(keySource));
        byte[] key = keyHash.AsSpan(0, 32).ToArray();
        byte[] ivHash = SHA256.HashData(Encoding.UTF8.GetBytes("FluxVisualsIV" + GetMachineId()));
        byte[] iv = ivHash.AsSpan(0, 16).ToArray();

        using Aes aes = Aes.Create();
        aes.Key = key;
        aes.IV = iv;
        aes.Mode = CipherMode.CBC;
        aes.Padding = PaddingMode.PKCS7;

        using MemoryStream ms = new();
        using CryptoStream cs = new(ms, aes.CreateDecryptor(), CryptoStreamMode.Write);
        cs.Write(encrypted, 0, encrypted.Length);
        cs.FlushFinalBlock();
        return ms.ToArray();
    }

    private static string GetMachineId()
    {
        try { return Environment.MachineName + Environment.OSVersion.ToString().Replace(" ", ""); }
        catch { return "DefaultMachine"; }
    }

    // ===== Anti-tamper: проверка целостности exe =====
    public static bool VerifyIntegrity()
    {
        try
        {
            string exePath = Environment.ProcessPath ?? "";
            if (string.IsNullOrEmpty(exePath) || !File.Exists(exePath)) return true;
            byte[] fileBytes = File.ReadAllBytes(exePath);
            byte[] hash = SHA256.HashData(fileBytes);
            // Если hash изменился — кто-то правил файл. Можно логировать.
            return true; // TODO: сравнить с stored hash при необходимости
        }
        catch { return true; }
    }

    private static string OfflineUuid(string username)
    {
        var bytes = System.Security.Cryptography.MD5.HashData(
            System.Text.Encoding.UTF8.GetBytes("OfflinePlayer:" + username));
        bytes[6] = (byte)((bytes[6] & 0x0f) | 0x30);
        bytes[8] = (byte)((bytes[8] & 0x3f) | 0x80);
        Array.Reverse(bytes, 0, 4);
        Array.Reverse(bytes, 4, 2);
        Array.Reverse(bytes, 6, 2);
        return new Guid(bytes).ToString();
    }
}
