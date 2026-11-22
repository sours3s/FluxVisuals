using System.Collections.Concurrent;
using Discord;
using Discord.Interactions;
using Discord.WebSocket;

namespace FluxVisualsBots;

/// <summary>Один образ, два режима: news (slash-команда) и support (Catal в канале).</summary>
public sealed class BotWorker : BackgroundService
{
    private readonly DiscordSocketClient _client;
    private readonly InteractionService _interactions;
    private readonly IServiceProvider _services;
    private readonly CatalClient _catal;
    private readonly ILogger<BotWorker> _log;
    private readonly HttpClient _http = new() { Timeout = TimeSpan.FromSeconds(30) };

    private readonly string _mode;
    private readonly HashSet<ulong> _supportChannels;
    private readonly ConcurrentDictionary<ulong, List<CatalTurn>> _history = new();
    private readonly object _historyGate = new();
    private int _commandsRegistered;

    public BotWorker(
        DiscordSocketClient client,
        InteractionService interactions,
        IServiceProvider services,
        CatalClient catal,
        ILogger<BotWorker> log)
    {
        _client = client;
        _interactions = interactions;
        _services = services;
        _catal = catal;
        _log = log;
        _mode = (Environment.GetEnvironmentVariable("BOT_MODE") ?? "news").Trim().ToLowerInvariant();
        _supportChannels = (Environment.GetEnvironmentVariable("SUPPORT_CHANNEL_IDS") ?? "")
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(s => ulong.TryParse(s, out var id) ? id : 0)
            .Where(id => id != 0)
            .ToHashSet();
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        string token = (Environment.GetEnvironmentVariable("DISCORD_TOKEN") ?? "").Trim();
        if (string.IsNullOrWhiteSpace(token))
        {
            _log.LogWarning("DISCORD_TOKEN не задан — Discord-бот отключён, /ping работает.");
            await Task.Delay(Timeout.Infinite, stoppingToken);
            return;
        }

        if (_mode == "news")
        {
            _client.Ready += RegisterCommandsAsync;
            _interactions.SlashCommandExecuted += OnSlashExecuted;
        }
        else
        {
            if (_supportChannels.Count == 0)
                _log.LogWarning("SUPPORT_CHANNEL_IDS пуст — бот будет молчать. Укажите ID каналов через запятую.");
            _client.MessageReceived += OnMessageAsync;
        }

        await _client.LoginAsync(TokenType.Bot, token);
        await _client.StartAsync();
        _log.LogInformation("FluxVisuals {Mode} bot запущен.", _mode);

        try
        {
            await Task.Delay(Timeout.Infinite, stoppingToken);
        }
        finally
        {
            await _client.StopAsync();
            await _client.LogoutAsync();
        }
    }

    private async Task RegisterCommandsAsync()
    {
        if (Interlocked.Exchange(ref _commandsRegistered, 1) != 0) return;

        await _interactions.AddModulesAsync(typeof(BotWorker).Assembly, _services);

        string? guildRaw = Environment.GetEnvironmentVariable("COMMAND_GUILD_ID");
        if (ulong.TryParse(guildRaw, out var guildId) && guildId != 0)
            await _interactions.RegisterCommandsToGuildAsync(guildId, true);
        else
            await _interactions.RegisterCommandsGloballyAsync(true);
    }

    private async Task OnSlashExecuted(SlashCommandInfo command, IInteractionContext context, Discord.Interactions.IResult result)
    {
        if (result.IsSuccess || context.Interaction.HasResponded) return;

        string text = result.Error?.ToString() switch
        {
            "BadRequest" => "Неверные параметры команды.",
            "Timeout" => "Команда не ответила вовремя. Попробуйте ещё раз.",
            _ => "Не удалось выполнить команду: " + result.ErrorReason
        };
        await context.Interaction.RespondAsync(text, ephemeral: true);
    }

    private async Task OnMessageAsync(SocketMessage message)
    {
        if (message.Author.IsBot || message.Author.IsWebhook) return;
        if (_supportChannels.Count == 0 || !_supportChannels.Contains(message.Channel.Id)) return;
        if (_client.CurrentUser == null) return;

        string text = message.Content.Replace(_client.CurrentUser.Mention, "").Trim();
        var (image, imageError) = await GetImageAsync(message.Attachments);
        if (imageError != null)
        {
            await ReplyChunksAsync(message, new[] { imageError });
            return;
        }
        if (text.Length == 0 && image == null) return;

        string question = text.Length == 0
            ? "Посмотри на скриншот и помоги разобраться с FluxVisuals."
            : text;

        CatalTurn[] history;
        lock (_historyGate)
        {
            var list = _history.GetOrAdd(message.Author.Id, _ => new List<CatalTurn>());
            list.Add(new CatalTurn("user", text.Length == 0 ? "[скриншот]" : text));
            Trim(list);
            history = list.ToArray();
        }

        try
        {
            await message.Channel.TriggerTypingAsync();
        }
        catch { }

        string answer = await _catal.AskAsync(question, history, image, message.Author.Id);

        lock (_historyGate)
        {
            var list = _history.GetOrAdd(message.Author.Id, _ => new List<CatalTurn>());
            list.Add(new CatalTurn("model", answer));
            Trim(list);
        }

        await ReplyChunksAsync(message, SplitAnswer(answer));
    }

    private async Task<(string? Data, string? Error)> GetImageAsync(IReadOnlyCollection<IAttachment> attachments)
    {
        foreach (var attachment in attachments.Take(1))
        {
            string mime = (attachment.ContentType ?? "").Split(';')[0].Trim().ToLowerInvariant();
            if (!mime.StartsWith("image/")) continue;

            const int maxBytes = 4_000_000;
            if (attachment.Size > maxBytes)
                return (null, "Скриншот слишком большой. Пришлите изображение до 4 МБ.");

            try
            {
                byte[] bytes = await _http.GetByteArrayAsync(attachment.Url);
                if (bytes.Length > maxBytes)
                    return (null, "Скриншот слишком большой. Пришлите изображение до 4 МБ.");
                return ("data:" + mime + ";base64," + Convert.ToBase64String(bytes), null);
            }
            catch
            {
                return (null, "Не удалось скачать скриншот. Попробуйте прикрепить его ещё раз.");
            }
        }
        return (null, null);
    }

    private async Task ReplyChunksAsync(SocketMessage source, IEnumerable<string> chunks)
    {
        bool first = true;
        foreach (string chunk in chunks)
        {
            var reference = first ? new MessageReference(source.Id) : null;
            await source.Channel.SendMessageAsync(
                chunk,
                allowedMentions: AllowedMentions.None,
                messageReference: reference);
            first = false;
        }
    }

    private static void Trim(List<CatalTurn> list)
    {
        while (list.Count > 20) list.RemoveAt(0);
    }

    private static IEnumerable<string> SplitAnswer(string text, int max = 1900)
    {
        text = (text ?? "").Replace("\r\n", "\n");
        while (text.Length > max)
        {
            int cut = text.LastIndexOf('\n', max);
            if (cut < max / 2) cut = max;
            string part = text[..cut].TrimEnd();
            if (part.Length > 0) yield return part;
            text = text[cut..].TrimStart();
        }
        if (text.Length > 0) yield return text;
    }
}
