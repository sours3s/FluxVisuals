using Discord;
using Discord.Interactions;
using Discord.WebSocket;
using FluxVisualsBots;

string mode = (Environment.GetEnvironmentVariable("BOT_MODE") ?? "news").Trim().ToLowerInvariant();
if (mode is not ("news" or "support"))
{
    Console.Error.WriteLine("BOT_MODE должен быть news или support.");
    return;
}

var builder = WebApplication.CreateBuilder(args);

// Веб-панель принимает до 4 файлов по 8 МБ.
builder.WebHost.ConfigureKestrel(o => o.Limits.MaxRequestBodySize = 40_000_000);

// Новостному боту не нужно читать сообщения — не запрашиваем привилегированный интент.
// Support-бот читает вопросы в тикетах, поэтому ему нужны GuildMessages + MessageContent.
GatewayIntents intents = GatewayIntents.Guilds;
if (mode == "support")
    intents |= GatewayIntents.GuildMessages | GatewayIntents.MessageContent | GatewayIntents.DirectMessages;

builder.Services.AddSingleton(new DiscordSocketConfig
{
    GatewayIntents = intents,
    AlwaysDownloadUsers = false,
    MessageCacheSize = 20
});
builder.Services.AddSingleton<DiscordSocketClient>();
builder.Services.AddSingleton(provider =>
    new InteractionService(provider.GetRequiredService<DiscordSocketClient>()));
builder.Services.AddSingleton<CatalClient>();
builder.Services.AddSingleton<NewsPublisher>();
builder.Services.AddSingleton<BotWorker>();
builder.Services.AddHostedService(provider => provider.GetRequiredService<BotWorker>());

var app = builder.Build();

// Render Free засыпает через ~15 минут. UptimeRobot дёргает /ping каждые 5 минут.
app.MapGet("/ping", () => Results.Text("ok"));
app.MapGet("/", () => Results.Text($"FluxVisuals bot: {mode}"));
app.MapGet("/bot-status", (DiscordSocketClient client) => Results.Json(new
{
    connection = client.ConnectionState.ToString(),
    user = client.CurrentUser?.Username,
    userId = client.CurrentUser?.Id.ToString(),
    guilds = client.Guilds.Count
}));

if (mode == "news")
    app.MapNewsAdmin(mode);

app.Run();
