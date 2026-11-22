using Discord;
using Discord.WebSocket;

namespace FluxVisualsBots;

/// <summary>
/// Веб-панель публикации новостей. Доступна только у news-бота и только по токену.
/// Токен: NEWS_ADMIN_TOKEN (или FLUXVISUALS_BOT_SECRET в качестве запасного).
/// </summary>
public static class NewsAdmin
{
    public static void MapNewsAdmin(this WebApplication app, string mode)
    {
        if (mode != "news") return;

        string token = Environment.GetEnvironmentVariable("NEWS_ADMIN_TOKEN")
                       ?? Environment.GetEnvironmentVariable("FLUXVISUALS_BOT_SECRET")
                       ?? "";

        app.MapGet("/news", () => Results.Content(NewsAdminPage.Html, "text/html; charset=utf-8"));

        app.MapGet("/api/news/status", (HttpContext ctx, DiscordSocketClient client) =>
        {
            if (!Authorized(ctx, token)) return Unauthorized();
            return Results.Json(new
            {
                ok = true,
                connected = client.ConnectionState == ConnectionState.Connected,
                connection = client.ConnectionState.ToString(),
                user = client.CurrentUser?.Username,
                guilds = client.Guilds.Count,
                maxFiles = NewsPublisher.MaxFiles,
                maxFileBytes = NewsPublisher.MaxFileBytes,
                maxChars = NewsPublisher.MaxMessageChars
            });
        });

        app.MapGet("/api/news/channels", (HttpContext ctx, DiscordSocketClient client) =>
        {
            if (!Authorized(ctx, token)) return Unauthorized();
            if (client.ConnectionState != ConnectionState.Connected)
                return Results.Json(new { ok = false, error = "Бот не подключён к Discord." }, statusCode: 503);

            var channels = new List<object>();
            foreach (var guild in client.Guilds)
            {
                var self = client.CurrentUser == null ? null : guild.GetUser(client.CurrentUser.Id);
                if (self == null) continue;

                foreach (var channel in guild.TextChannels.OrderBy(c => c.Position))
                {
                    if (!self.GetPermissions(channel).SendMessages) continue;
                    channels.Add(new
                    {
                        id = channel.Id.ToString(),
                        name = channel.Name,
                        guild = guild.Name,
                        category = channel.Category?.Name ?? ""
                    });
                }
            }
            return Results.Json(new { ok = true, channels });
        });

        app.MapPost("/api/news/publish", async (HttpContext ctx, NewsPublisher publisher) =>
        {
            if (!Authorized(ctx, token)) return Unauthorized();

            var form = await ctx.Request.ReadFormAsync();
            if (!ulong.TryParse(form["channel"].ToString(), out ulong channelId))
                return Results.Json(new { ok = false, error = "Выберите канал." }, statusCode: 400);

            var files = new List<NewsFile>();
            foreach (var file in form.Files.Take(NewsPublisher.MaxFiles + 1))
            {
                using var buffer = new MemoryStream();
                await file.CopyToAsync(buffer);
                files.Add(new NewsFile(Path.GetFileName(file.FileName), buffer.ToArray()));
            }

            string? error = publisher.Validate(form["message"].ToString(), files);
            if (error != null)
                return Results.Json(new { ok = false, error }, statusCode: 400);

            PublishResult result = await publisher.PublishAsync(channelId, form["message"].ToString(), files);
            if (!result.Ok)
                return Results.Json(new { ok = false, error = result.Error }, statusCode: 400);

            return Results.Json(new
            {
                ok = true,
                guild = result.Guild,
                channel = result.Channel,
                messageId = result.MessageId.ToString()
            });
        });
    }

    private static bool Authorized(HttpContext ctx, string token)
    {
        if (token.Length == 0) return false;
        string? supplied = ctx.Request.Headers["X-Admin-Token"].ToString();
        return string.Equals(supplied, token, StringComparison.Ordinal);
    }

    private static IResult Unauthorized() =>
        Results.Json(new { ok = false, error = "Неверный токен. Откройте /news и введите токен администратора." }, statusCode: 401);
}
