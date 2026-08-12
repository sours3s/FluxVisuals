using System.Windows;

namespace FluxVisualsLoader;

public partial class App : Application
{
    public static Core.CoreBackend Backend = null!;

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        DispatcherUnhandledException += (_, args) =>
        {
            try
            {
                File.AppendAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "crash.log"),
                    $"[{DateTime.Now:O}] {args.Exception}\n\n");
            }
            catch { }
        };
    }
}
