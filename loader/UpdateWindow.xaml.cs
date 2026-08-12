using System.Windows;
using FluxVisualsLoader.Core;

namespace FluxVisualsLoader;

/// <summary>Окно «Обновление лоадера»: показывает прогресс скачивания и применяет обновление.</summary>
public partial class UpdateWindow : Window
{
    private readonly string _downloadUrl;
    private readonly string _newExePath;
    private bool _restart;

    public UpdateWindow(UpdateInfo info, string newExePath)
    {
        InitializeComponent();
        _downloadUrl = info.DownloadUrl;
        _newExePath = newExePath;
        MessageText.Text = $"Доступна версия {info.Version} (текущая {UpdateService.CurrentVersion}). Скачиваем обновление…";
        Loaded += OnLoadedAsync;
    }

    public bool ShouldRestart => _restart;

    private async void OnLoadedAsync(object sender, RoutedEventArgs e)
    {
        try
        {
            await UpdateService.DownloadAsync(_downloadUrl, _newExePath,
                (done, total) => Dispatcher.Invoke(() =>
                {
                    double pct = total.HasValue && total.Value > 0 ? 100.0 * done / total.Value : 0;
                    Progress.Value = Math.Min(100, pct);
                    PercentText.Text = $"{(int)pct}% · {done / 1048576.0:F0} МБ";
                }));

            TitleText.Text = "Применяю обновление…";
            MessageText.Text = "Скачано. Сейчас лоадер заменит себя и перезапустится.";
            Progress.Value = 100;
            PercentText.Text = "100%";

            // Запускаем скрытый updater.bat (заменит exe и перезапустит), затем выходим из приложения.
            _restart = true;
            UpdateService.ApplyAndRestart(Environment.ProcessPath ?? _newExePath, _newExePath);
            Close();
        }
        catch (Exception ex)
        {
            MessageText.Text = "Не удалось обновить лоадер: " + ex.Message;
            SkipButton.Visibility = Visibility.Visible;
        }
    }

    private void OnSkip(object sender, RoutedEventArgs e)
    {
        Close();
    }
}
