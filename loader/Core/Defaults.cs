using System.Text;

namespace FluxVisualsLoader.Core;

/// <summary>Скрытые дефолтные URL (GitHub, сервер). Собираются из частей, чтобы не светить
/// полные ссылки в бинарнике — поиск по бинарнику не находит github/onrender.</summary>
public static class Defaults
{
    public static string ModDownloadUrl => Build(
        "https://", "github", ".com/sour", "s3s/Flux", "Visuals/releases/",
        "download/", "v1.0.17/", "fluxvisuals-", "1.0.17.jar");

    public static string AuthServerUrl => Build(
        "https://flux", "visuals-se", "rver.onre", "nder.com");

    public static string UpdateCheckUrl => Build(
        "https://", "api.github", ".com/repos/", "sours3s/Flux", "Visuals/releases?per_page=30");

    private static string Build(params string[] parts)
    {
        var sb = new StringBuilder();
        foreach (var p in parts) sb.Append(p);
        return sb.ToString();
    }
}
