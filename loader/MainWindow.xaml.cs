using System.Windows;
using System.Windows.Threading;
using Microsoft.Web.WebView2.Core;
using Microsoft.Win32;

namespace FluxVisualsLoader;

public partial class MainWindow : Window
{
    private Core.CoreBackend _backend = null!;

    public MainWindow()
    {
        InitializeComponent();
        Loaded += OnLoaded;
    }

    private async void OnLoaded(object sender, RoutedEventArgs e)
    {
        // Диагностика сохранения настроек (баг: после обновления сбрасывался конфиг).
        Core.LauncherLog.Info($"Пути: processPath={Environment.ProcessPath}, baseDir={AppDomain.CurrentDomain.BaseDirectory}, " +
            $"config={Core.AppConfig.ConfigPath} (exists={File.Exists(Core.AppConfig.ConfigPath)})");

        // Автообновление лоадера: проверка GitHub на старте. Если есть новая версия —
        // показываем окно «Обновление лоадера», качаем, заменяем exe и перезапускаемся.
        CheckForUpdate();

        try
        {
            await WebView.EnsureCoreWebView2Async();
        }
        catch (Exception ex)
        {
            MessageBox.Show("Не удалось инициализировать WebView2. Установите WebView2 Runtime.\n\n" + ex.Message,
                "FluxVisuals", MessageBoxButton.OK, MessageBoxImage.Error);
            return;
        }

        WebView.CoreWebView2.Settings.AreDefaultContextMenusEnabled = false;
        WebView.CoreWebView2.Settings.IsStatusBarEnabled = false;
        WebView.CoreWebView2.Settings.AreDevToolsEnabled = true;
        WebView.CoreWebView2.Settings.IsZoomControlEnabled = false;

        // Извлекаем Web-файлы из Embedded Resources во временную папку (single-file)
        string webDir = ExtractWebFiles();
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

            string exeDir = Path.GetDirectoryName(Environment.ProcessPath ?? AppDomain.CurrentDomain.BaseDirectory)
                            ?? AppDomain.CurrentDomain.BaseDirectory;
            string newExe = Path.Combine(exeDir, "FluxVisualsLoader.update.exe");

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

        // Лого из Embedded Resources
        string logoDst = Path.Combine(webDir, "img", "logo.png");
        ExtractResource("Resources.logo.png", logoDst);

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

    public void Drag() => DragMove();

    public void Minimize() => WindowState = WindowState.Minimized;

    public void ToggleMaximize()
        => WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;

    public string? PickFolder()
    {
        var dlg = new OpenFolderDialog { Title = "Выберите папку" };
        return dlg.ShowDialog(this) == true ? dlg.FolderName : null;
    }

    public string? PickFile(string? initialDir = null)
    {
        var dlg = new OpenFileDialog { Title = "Выберите мод (.jar)", Filter = "Minecraft mods (*.jar)|*.jar" };
        if (!string.IsNullOrEmpty(initialDir) && Directory.Exists(initialDir))
            dlg.InitialDirectory = initialDir;
        return dlg.ShowDialog(this) == true ? dlg.FileName : null;
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
