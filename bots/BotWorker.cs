using System.Collections.Concurrent;
using System.Text.RegularExpressions;
using Discord;
using Discord.Interactions;
using Discord.WebSocket;

namespace FluxVisualsBots;

/// <summary>Один образ, два режима: news (slash-команда) и support (Catal в канале).</summary>
public sealed class BotWorker : BackgroundService
{
    private const string Greeting =
        "Привет! Я Catal — ИИ-саппорт FluxVisuals. Помогу с вопросами и ошибками по клиенту, лаунчеру, модам и аккаунту. " +
        "Опишите проблему или прикрепите скриншот. Если нужен живой человек — напишите «позови саппорта».";

    private static readonly Regex IntroPrefix = new(
        @"^\s*(?:Привет[!,.]?\s*)?(?:Я\s*[—–-]\s*)?Catal\s*[—–-]\s*ИИ[\s-]?саппорт\s+FluxVisuals\.?\s*",
        RegexOptions.IgnoreCase | RegexOptions.Compiled);
    private static readonly Regex CallVerb = new(
        @"\b(позови(те)?|вызови(те)?|зови(те)?|пригласи(те)?|нужен|нужна|подключи(те)?|call|ping|tag|mention)\b",
        RegexOptions.IgnoreCase | RegexOptions.Compiled);
    private static readonly Regex SupportTarget = new(
        @"\b(саппорт\w*|поддерж\w*|жив\w*\s+человек\w*|человек\w*|support|staff|админ\w*|модератор\w*)\b",
        RegexOptions.IgnoreCase | RegexOptions.Compiled);

    private readonly DiscordSocketClient _client;
    private readonly InteractionService _interactions;
    private readonly IServiceProvider _services;
    private readonly CatalClient _catal;
    private readonly ILogger<BotWorker> _log;
    private readonly HttpClient _http = new() { Timeout = TimeSpan.FromSeconds(30) };

    private readonly string _mode;
    private readonly HashSet<ulong> _supportChannels;
    private readonly HashSet<ulong> _supportCategories;
    private readonly Regex? _supportNamePattern;
    private readonly ulong _supportRoleId;
    private readonly ConcurrentDictionary<ulong, List<CatalTurn>> _history = new();
    private readonly ConcurrentDictionary<ulong, byte> _greeted = new();
    private readonly ConcurrentDictionary<ulong, byte> _channelGreeted = new();
    private readonly ConcurrentDictionary<ulong, byte> _pendingGreetings = new();
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
        _supportChannels = ParseIds("SUPPORT_CHANNEL_IDS");
        _supportCategories = ParseIds("SUPPORT_CATEGORY_IDS");
        ulong.TryParse(Environment.GetEnvironmentVariable("SUPPORT_ROLE_ID"), out _supportRoleId);

        // Ticket Tool создаёт каналы вида ticket-1234. По умолчанию берём их по имени,
        // а точные ID категории можно задать через SUPPORT_CATEGORY_IDS.
        string pattern = Environment.GetEnvironmentVariable("SUPPORT_CHANNEL_PATTERN") ?? "^ticket";
        if (!string.IsNullOrWhiteSpace(pattern))
            _supportNamePattern = new Regex(pattern, RegexOptions.IgnoreCase | RegexOptions.Compiled);
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

        _client.Log += msg =>
        {
            if (msg.Severity is LogSeverity.Warning or LogSeverity.Error or LogSeverity.Critical)
                _log.LogWarning("Discord: {Message}", msg.ToString());
            return Task.CompletedTask;
        };
        _client.Disconnected += ex =>
        {
            _log.LogWarning(ex, "Discord-соединение потеряно, бот попробует переподключиться");
            return Task.CompletedTask;
        };

        if (_mode == "news")
        {
            _client.Ready += RegisterCommandsAsync;
            _client.Ready += () => _client.SetStatusAsync(UserStatus.Online);
            _interactions.SlashCommandExecuted += OnSlashExecuted;
        }
        else
        {
            if (_supportChannels.Count == 0 && _supportCategories.Count == 0 && _supportNamePattern == null)
                _log.LogWarning("Не заданы SUPPORT_CHANNEL_IDS / SUPPORT_CATEGORY_IDS / SUPPORT_CHANNEL_PATTERN — бот будет молчать.");
            if (_supportRoleId == 0)
                _log.LogWarning("SUPPORT_ROLE_ID не задан — по просьбе «позови саппорта» бот ответит без упоминания роли.");
            _client.MessageReceived += OnMessageAsync;
            _client.ChannelCreated += OnChannelCreatedAsync;
        }

        // Неверный токен не должен ронять хост: /ping обязан отвечать даже при сбое Discord.
        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await _client.LoginAsync(TokenType.Bot, token);
                await _client.StartAsync();
                _log.LogInformation("FluxVisuals {Mode} bot запущен.", _mode);
                break;
            }
            catch (Exception ex)
            {
                _log.LogWarning(ex, "Не удалось подключиться к Discord. Повтор через 30 секунд, /ping работает.");
                try
                {
                    await Task.Delay(TimeSpan.FromSeconds(30), stoppingToken);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
            }
        }

        try
        {
            await Task.Delay(Timeout.Infinite, stoppingToken);
        }
        finally
        {
            if (_client.ConnectionState == ConnectionState.Connected)
            {
                await _client.StopAsync();
                await _client.LogoutAsync();
            }
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
        try
        {
            await HandleMessageAsync(message);
        }
        catch (Exception ex)
        {
            _log.LogError(ex, "Ошибка обработки сообщения {MessageId}", message.Id);
        }
    }

    private async Task HandleMessageAsync(SocketMessage message)
    {
        if (message.Author.IsBot || message.Author.IsWebhook) return;

        bool supported = IsSupportChannel(message.Channel);
        _log.LogInformation(
            "Сообщение {MessageId} в канале {ChannelName} ({ChannelId}), подходит={Supported}",
            message.Id, message.Channel.Name, message.Channel.Id, supported);
        if (!supported) return;
        if (_client.CurrentUser == null) return;

        bool pending = _pendingGreetings.ContainsKey(message.Channel.Id);
        if (pending && message.Author.IsBot && message.Author.Id != _client.CurrentUser.Id)
        {
            // Это сообщение Ticket Tool — отвечаем сразу после него.
            if (_pendingGreetings.TryRemove(message.Channel.Id, out _))
            {
                await Task.Delay(400);
                await SendGreetingAsync(message.Channel, message.Channel.Id);
            }
        }

        bool channelHadGreeting = _channelGreeted.ContainsKey(message.Channel.Id) || pending;
        if (!pending && _channelGreeted.TryAdd(message.Channel.Id, 0))
            await SendGreetingAsync(message.Channel, message.Channel.Id);

        string text = message.Content.Replace(_client.CurrentUser.Mention, "").Trim();

        if (IsSupportCall(text))
        {
            await CallSupportAsync(message);
            return;
        }

        var (image, imageError) = await GetImageAsync(message.Attachments);
        if (imageError != null)
        {
            await ReplyChunksAsync(message, new[] { imageError + "\n\nЕсли хотите, напишите «позови саппорта» — я упомяну роль поддержки в этом тикете." });
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

        _log.LogInformation("Запрос к Catal от {UserId}, символов={Length}, скриншот={HasImage}",
            message.Author.Id, question.Length, image != null);
        CatalResult result;
        try
        {
            result = await _catal.AskAsync(question, history, image, message.Author.Id);
        }
        catch (Exception ex)
        {
            _log.LogError(ex, "Неожиданная ошибка при запросе к Catal");
            result = CatalResult.Fail("Catal недоступен. Попробуйте позже.");
        }
        _log.LogInformation("Ответ Catal: ok={Ok}, символов={Length}", result.Ok, result.Text.Length);

        bool first = _greeted.TryAdd(message.Author.Id, 0) && !channelHadGreeting;
        string answer = IntroPrefix.Replace(result.Text, "").Trim();
        if (answer.Length == 0) answer = result.Text.Trim();

        if (!result.Ok)
            answer += "\n\nЕсли хотите, напишите «позови саппорта» — я упомяну роль поддержки в этом тикете.";
        if (first)
            answer = "Catal — ИИ-саппорт FluxVisuals.\n\n" + answer;

        if (result.Ok)
        {
            lock (_historyGate)
            {
                var list = _history.GetOrAdd(message.Author.Id, _ => new List<CatalTurn>());
                list.Add(new CatalTurn("model", answer));
                Trim(list);
            }
        }

        _log.LogInformation("Отправка ответа в канал {ChannelId}", message.Channel.Id);
        await ReplyChunksAsync(message, SplitAnswer(answer));
    }

    private async Task CallSupportAsync(SocketMessage message)
    {
        if (_supportRoleId == 0)
        {
            await ReplyChunksAsync(message, new[] {
                "Позвал саппорта, но роль поддержки не настроена. Сообщите администратору."
            });
            return;
        }

        await message.Channel.SendMessageAsync(
            MentionUtils.MentionRole(_supportRoleId) + " — пользователь просит помощи в этом тикете.",
            allowedMentions: AllowedMentions.All,
            messageReference: new MessageReference(message.Id));
    }

    private async Task OnChannelCreatedAsync(SocketChannel channel)
    {
        if (channel is not ITextChannel text || !IsSupportChannel(text)) return;
        if (!_pendingGreetings.TryAdd(channel.Id, 0)) return;

        // Ticket Tool пишет «Welcome» сразу после создания канала. Ждём его сообщение,
        // а если оно не пришло — отправляем приветствие сами через 12 секунд.
        _log.LogInformation("Новый тикет {ChannelId}, ждём сообщение Ticket Tool", channel.Id);
        _ = Task.Run(async () =>
        {
            await Task.Delay(TimeSpan.FromSeconds(12));
            if (_pendingGreetings.TryRemove(channel.Id, out _))
                await SendGreetingAsync(text, channel.Id);
        });
    }

    private async Task SendGreetingAsync(IMessageChannel channel, ulong channelId)
    {
        for (int attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                await channel.SendMessageAsync(Greeting, allowedMentions: AllowedMentions.None);
                _channelGreeted.TryAdd(channelId, 0);
                _log.LogInformation("Приветствие отправлено в тикет {ChannelId}", channelId);
                return;
            }
            catch (Exception ex)
            {
                if (attempt == 2)
                    _log.LogWarning(ex, "Не удалось поприветствовать тикет {ChannelId}", channelId);
                else
                    await Task.Delay(1500);
            }
        }
    }

    private static bool IsSupportCall(string text)
    {
        return CallVerb.IsMatch(text) && SupportTarget.IsMatch(text);
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

    private bool IsSupportChannel(IMessageChannel channel)
    {
        if (_supportChannels.Contains(channel.Id)) return true;

        if (channel is ITextChannel text)
        {
            if (text.CategoryId is ulong categoryId && _supportCategories.Contains(categoryId))
                return true;
            if (_supportNamePattern?.IsMatch(text.Name) == true)
                return true;
        }

        return false;
    }

    private static HashSet<ulong> ParseIds(string variable)
    {
        return (Environment.GetEnvironmentVariable(variable) ?? "")
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(s => ulong.TryParse(s, out var id) ? id : 0)
            .Where(id => id != 0)
            .ToHashSet();
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
