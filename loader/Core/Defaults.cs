using System.Text;

namespace FluxVisualsLoader.Core;

/// <summary>Адрес сервера. Собирается из частей, чтобы не светить полную ссылку в бинарнике.
/// Ссылки на мод и лоадер задаются только в админ-панели и приходят через /api/mod/version.</summary>
public static class Defaults
{
    public static string AuthServerUrl => Build(
        "https://", "fluxvisuals", ".fvls.", "workers.dev");

    /// <summary>Старый адрес из конфигов, оставшихся после Render.</summary>
    public static bool IsLegacyAuthServerUrl(string? url) =>
        !string.IsNullOrWhiteSpace(url) &&
        url.Contains(Build("fluxvisuals", "-server", ".onrender", ".com"), StringComparison.OrdinalIgnoreCase);

    /// <summary>Версия берётся из ссылки, заданной в админ-панели.</summary>
    public static string VersionFromUrl(string? url)
    {
        if (string.IsNullOrWhiteSpace(url) || !Uri.TryCreate(url, UriKind.Absolute, out var uri))
            return "";
        string path = uri.AbsolutePath;
        var match = System.Text.RegularExpressions.Regex.Match(
            path, @"(?:^|/)v?(\d+(?:\.\d+){1,3})(?:[-_.][^/]*)?(?:/|$)");
        if (!match.Success)
            match = System.Text.RegularExpressions.Regex.Match(
                path, @"[-_]v?(\d+(?:\.\d+){1,3})(?=[-_.]|$)");
        return match.Success ? match.Groups[1].Value : "";
    }

    /// <summary>API-ключ CurseForge по умолчанию. Пользователь может переопределить его в config.json.</summary>
    public static string CurseforgeApiKey => "$2a$10$JwFPGI2MAXdjVaGX.XWjduKm8fED605B6YqnsY8YsIpVM178ofZam";

    private static string Build(params string[] parts)
    {
        var sb = new StringBuilder();
        foreach (var p in parts) sb.Append(p);
        return sb.ToString();
    }
}
