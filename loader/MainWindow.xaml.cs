using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Threading;
using Microsoft.Web.WebView2.Core;
using Microsoft.Win32;

namespace FluxVisualsLoader;

public partial class MainWindow : Window
{
    private Core.CoreBackend _backend = null!;
    private string? _webDir;

    public MainWindow()
    {
        InitializeComponent();
        Loaded += OnLoaded;
        Closed += OnClosed;
    }

    private async void OnLoaded(object sender, RoutedEventArgs e)
    {
        // Диагностика сохранения настроек (баг: после обновления сбрасывался конфиг).
        Core.LauncherLog.Info($"Пути: processPath={Environment.ProcessPath}, baseDir={AppDomain.CurrentDomain.BaseDirectory}, " +
            $"config={Core.AppConfig.ConfigPath} (exists={File.Exists(Core.AppConfig.ConfigPath)})");

        // WebView2 UserDataFolder — в %LOCALAPPDATA%\FluxVisuals\WebView2 (не рядом с exe)
        string webViewDataDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "FluxVisuals", "WebView2");
        Directory.CreateDirectory(webViewDataDir);
        CleanupStaleTempDirs();
        var env = await CoreWebView2Environment.CreateAsync(userDataFolder: webViewDataDir);

        // Автообновление лоадера: проверка GitHub на старте. Если есть новая версия —
        // показываем окно «Обновление лоадера», качаем, заменяем exe и перезапускаемся.
        CheckForUpdate();

        try
        {
            await WebView.EnsureCoreWebView2Async(env);
        }
        catch (Exception ex)
        {
            MessageBox.Show("Не удалось инициализировать WebView2. Установите WebView2 Runtime.\n\n" + ex.Message,
                "FluxVisuals", MessageBoxButton.OK, MessageBoxImage.Error);
            return;
        }

        WebView.CoreWebView2.Settings.AreDefaultContextMenusEnabled = false;
        WebView.CoreWebView2.Settings.IsStatusBarEnabled = false;
#if DEBUG
        WebView.CoreWebView2.Settings.AreDevToolsEnabled = true;
#else
        WebView.CoreWebView2.Settings.AreDevToolsEnabled = false;
#endif
        WebView.CoreWebView2.Settings.IsZoomControlEnabled = false;

        // Извлекаем Web-файлы из Embedded Resources во временную папку (single-file)
        string webDir = ExtractWebFiles();
        _webDir = webDir;
        WebView.CoreWebView2.SetVirtualHostNameToFolderMapping("app.local", webDir,
            CoreWebView2HostResourceAccessKind.Allow);
        WebView.CoreWebView2.WebMessageReceived += OnWebMessage;
        WebView.CoreWebView2.Navigate("https://app.local/index.html");

        _backend = new Core.CoreBackend(this);
    }

    private void OnWebMessage(object? sender, CoreWebView2WebMessageReceivedEventArgs e)
    {
        string json = e.WebMessageAsJson;
        _ = _backend.Handle(json);
    }

    /// <summary>Проверяет наличие новой версии лоадера и, если она есть, показывает окно обновления.
    /// Ошибки сети/доступа к GitHub не блокируют запуск — продолжаем со старой версией.</summary>
    private async void CheckForUpdate()
    {
        try
        {
            var cfg = Core.AppConfig.Load();
            if (!cfg.UpdateCheckEnabled) return;

            var update = await Core.UpdateService.CheckAsync(cfg.UpdateCheckUrl);
            if (update == null) return;

            string newExe = Path.Combine(Core.UpdateService.UpdateDir, "FluxVisualsLoader.update.exe");

            var win = new UpdateWindow(update, newExe) { Owner = this };
            win.ShowDialog();
            if (win.ShouldRestart)
            {
                // updater.bat уже запущен (скрыто) — заменяет exe и перезапускает. Выходим.
                Application.Current.Shutdown();
            }
        }
        catch (Exception ex)
        {
            Core.LauncherLog.Warn($"Update flow error: {ex.Message}");
        }
    }

    /// <summary>Удаляет временные папки WebView2 от прошлых запусков (старше одного дня).</summary>
    private static void CleanupStaleTempDirs()
    {
        try
        {
            string root = Path.Combine(Path.GetTempPath(), "FluxVisuals");
            if (!Directory.Exists(root)) return;
            foreach (string dir in Directory.EnumerateDirectories(root, "web_*"))
            {
                var info = new DirectoryInfo(dir);
                if (DateTime.Now - info.CreationTime > TimeSpan.FromDays(1))
                    Directory.Delete(dir, true);
            }
        }
        catch { }
    }

    private void OnClosed(object? sender, EventArgs e)
    {
        try
        {
            if (!string.IsNullOrEmpty(_webDir) && Directory.Exists(_webDir))
                Directory.Delete(_webDir, true);
        }
        catch { }
    }

    private static string ExtractWebFiles()
    {
        string webDir = Path.Combine(Path.GetTempPath(), "FluxVisuals", "web_" + Environment.ProcessId);
        Directory.CreateDirectory(webDir);
        Directory.CreateDirectory(Path.Combine(webDir, "img"));
        Directory.CreateDirectory(Path.Combine(webDir, "js"));
        Directory.CreateDirectory(Path.Combine(webDir, "css"));

        var asm = System.Reflection.Assembly.GetExecutingAssembly();
        FluxVisualsLoader.Core.LauncherLog.Info($"Extracting resources from assembly: {asm.FullName}");
        FluxVisualsLoader.Core.LauncherLog.Info($"Available resources: {string.Join(", ", asm.GetManifestResourceNames())}");

        ExtractResource("Web.index.html", Path.Combine(webDir, "index.html"));
        ExtractResource("Web.app.js", Path.Combine(webDir, "js", "app.js"));
        ExtractResource("Web.style.css", Path.Combine(webDir, "css", "style.css"));

        // Лого и аватар Catal из Embedded Resources
        string logoDst = Path.Combine(webDir, "img", "logo.png");
        ExtractResource("Resources.logo.png", logoDst);
        string catalDst = Path.Combine(webDir, "img", "catal.png");
        ExtractResource("Web.img.catal.png", catalDst);

        FluxVisualsLoader.Core.LauncherLog.Info($"Web files extracted to: {webDir}");
        FluxVisualsLoader.Core.LauncherLog.Info($"Files: {string.Join(", ", Directory.GetFiles(webDir, "*", SearchOption.AllDirectories))}");

        return webDir;
    }

    private static void ExtractResource(string name, string path)
    {
        // В single-file режиме используем GetExecutingAssembly вместо typeof(MainWindow).Assembly
        var asm = System.Reflection.Assembly.GetExecutingAssembly();
        using var stream = asm.GetManifestResourceStream(name);
        if (stream != null)
        {
            using var fs = File.Create(path);
            stream.CopyTo(fs);
        }
    }

    private static string ReadEmbeddedResource(string name)
    {
        var asm = System.Reflection.Assembly.GetExecutingAssembly();
        using var stream = asm.GetManifestResourceStream(name);
        if (stream == null) throw new FileNotFoundException($"Embedded resource not found: {name}");
        using var reader = new StreamReader(stream);
        return reader.ReadToEnd();
    }

    public void PostToWeb(string json)
    {
        Dispatcher.BeginInvoke(DispatcherPriority.Background, () =>
            WebView.CoreWebView2?.PostWebMessageAsJson(json));
    }

    // ---- окно ----

    // DragMove() можно вызывать только во время нажатия ЛКМ в WPF, а WebView2
    // перехватывает мышь — отсюда «можно вызвать только DragMove» и окно не двигалось.
    // Поэтому тащим окно через Win32 HTCAPTION: это работает поверх WebView2.
    [DllImport("user32.dll")] private static extern bool ReleaseCapture();
    [DllImport("user32.dll", CharSet = CharSet.Auto)] private static extern IntPtr SendMessage(
        IntPtr hWnd, uint msg, IntPtr wParam, IntPtr lParam);
    private const uint WM_NCLBUTTONDOWN = 0x00A1;
    private const int HTCAPTION = 2;

    public void Drag()
    {
        try
        {
            if (WindowState == WindowState.Maximized) WindowState = WindowState.Normal;
            IntPtr hwnd = new WindowInteropHelper(this).Handle;
            ReleaseCapture();
            SendMessage(hwnd, WM_NCLBUTTONDOWN, (IntPtr)HTCAPTION, IntPtr.Zero);
        }
        catch
        {
            try { DragMove(); } catch { /* если и этот путь недоступен — просто игнорируем */ }
        }
    }

    private void DragStrip_PreviewMouseLeftButtonDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        if (e.ClickCount == 2) ToggleMaximize();
        else Drag();
        e.Handled = true;
    }

    public void Minimize() => WindowState = WindowState.Minimized;

    public void ToggleMaximize()
        => WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;

    public string? PickFolder(string? initialDir = null, string title = "Выберите папку")
    {
        var dlg = new OpenFolderDialog { Title = title };
        if (!string.IsNullOrEmpty(initialDir) && Directory.Exists(initialDir))
            dlg.InitialDirectory = initialDir;
        return dlg.ShowDialog(this) == true ? dlg.FolderName : null;
    }

    public string? PickFile(string? initialDir = null, string title = "Выберите мод (.jar)",
        string filter = "Minecraft mods (*.jar)|*.jar")
    {
        var dlg = new OpenFileDialog { Title = title, Filter = filter };
        if (!string.IsNullOrEmpty(initialDir) && Directory.Exists(initialDir))
            dlg.InitialDirectory = initialDir;
        return dlg.ShowDialog(this) == true ? dlg.FileName : null;
    }

    /// <summary>Файловый диалог для выбранной вкладки. Выбор «архив/папка» делается внутри WebView.</summary>
    public string? PickAddFile(string type, string initialDir, string mode = "file")
    {
        string typeName = type switch
        {
            "shader" => "шейдер",
            "resourcepack" => "ресурспак",
            "modpack" => "модпак",
            _ => "мод",
        };

        if (mode == "folder" && (type is "shader" or "resourcepack"))
        {
            if (!Directory.Exists(initialDir)) Directory.CreateDirectory(initialDir);
            return PickFolder(initialDir, "Выберите папку: " + typeName);
        }

        string filter = type switch
        {
            "shader" => "Шейдеры (*.zip;*.mcpack)|*.zip;*.mcpack",
            "resourcepack" => "Ресурспаки (*.zip;*.mcpack)|*.zip;*.mcpack",
            "modpack" => "Модпаки (*.mrpack;*.zip)|*.mrpack;*.zip",
            _ => "Minecraft mods (*.jar)|*.jar",
        };
        return PickFile(initialDir, "Выберите " + typeName + ":", filter);
    }

    public string? PickJava()
    {
        var dlg = new OpenFileDialog { Title = "Выберите java.exe", Filter = "Java (java.exe)|java.exe" };
        string javaDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "Java");
        if (Directory.Exists(javaDir)) dlg.InitialDirectory = javaDir;
        return dlg.ShowDialog(this) == true ? dlg.FileName : null;
    }

    /// <summary>Открывает URL в браузере по умолчанию.</summary>
    public void OpenUrl(string url)
    {
        try { System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(url) { UseShellExecute = true }); }
        catch { }
    }

    /// <summary>Открывает путь в проводнике; для файла — выделяет его (explorer /select).</summary>
    public void OpenPath(string path, bool selectFile = false)
    {
        try
        {
            if (selectFile && File.Exists(path))
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo("explorer.exe", $"/select,\"{path}\""));
            else if (Directory.Exists(path))
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo("explorer.exe", $"\"{path}\""));
            else if (File.Exists(path))
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo("explorer.exe", $"/select,\"{path}\""));
        }
        catch { }
    }
}
