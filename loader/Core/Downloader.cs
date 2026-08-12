using System.IO;
using System.Net;
using System.Net.Http;

namespace FluxVisualsLoader.Core;

/// <summary>HTTP-загрузчик с прогрессом и кэшем валидных файлов.</summary>
public static class Downloader
{
    private static readonly HttpClient Http = CreateClient();

    private static HttpClient CreateClient()
    {
        var client = new HttpClient(new SocketsHttpHandler
        {
            AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
            MaxConnectionsPerServer = 8,
        });
        client.Timeout = TimeSpan.FromMinutes(10);
        return client;
    }

    public static bool FileValid(string path)
    {
        try { return File.Exists(path) && new FileInfo(path).Length > 0; }
        catch { return false; }
    }

    /// <summary>Скачивает URL в dest (через temp), пропускает, если файл уже валиден.</summary>
    public static async Task DownloadAsync(string url, string dest, Action<long, long?>? progress = null, CancellationToken ct = default)
    {
        if (FileValid(dest)) return;
        Directory.CreateDirectory(Path.GetDirectoryName(dest)!);
        string tmp = dest + ".part";

        using var resp = await Http.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, ct);
        resp.EnsureSuccessStatusCode();
        long total = resp.Content.Headers.ContentLength ?? -1;

        await using var src = await resp.Content.ReadAsStreamAsync(ct);
        byte[] buffer = new byte[81920];
        long written = 0;
        int read;
        await using (var dst = new FileStream(tmp, FileMode.Create, FileAccess.Write, FileShare.None, 81920, true))
        {
            while ((read = await src.ReadAsync(buffer, ct)) > 0)
            {
                await dst.WriteAsync(buffer.AsMemory(0, read), ct);
                written += read;
                progress?.Invoke(written, total >= 0 ? total : null);
            }
            await dst.FlushAsync(ct);
        } // закрываем поток ДО File.Move (иначе Windows не даст переименовать открытый файл)

        // ретраи на случай антивируса/занятости
        for (int attempt = 0; ; attempt++)
        {
            try
            {
                File.Move(tmp, dest, true);
                break;
            }
            catch (IOException) when (attempt < 10)
            {
                await Task.Delay(300, ct);
            }
        }
    }

    public static async Task<string?> GetStringAsync(string url, string? apiKey = null, CancellationToken ct = default)
    {
        using var req = new HttpRequestMessage(HttpMethod.Get, url);
        if (!string.IsNullOrWhiteSpace(apiKey))
            req.Headers.Add("x-api-key", apiKey);
        using var resp = await Http.SendAsync(req, ct);
        if (!resp.IsSuccessStatusCode) return null;
        return await resp.Content.ReadAsStringAsync(ct);
    }

    public static async Task<Stream> GetStreamAsync(string url, string? apiKey = null, CancellationToken ct = default)
    {
        using var req = new HttpRequestMessage(HttpMethod.Get, url);
        if (!string.IsNullOrWhiteSpace(apiKey))
            req.Headers.Add("x-api-key", apiKey);
        var resp = await Http.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
        resp.EnsureSuccessStatusCode();
        return await resp.Content.ReadAsStreamAsync(ct);
    }
}
