using Discord;
using Discord.WebSocket;

namespace FluxVisualsBots;

/// <summary>Файл новости, уже прочитанный в память.</summary>
public sealed record NewsFile(string Name, byte[] Bytes);

/// <summary>Результат публикации новости.</summary>
public sealed record PublishResult(bool Ok, string? Guild, string? Channel, ulong MessageId, string? Error)
{
    public static PublishResult Success(string? guild, string? channel, ulong id) => new(true, guild, channel, id, null);
    public static PublishResult Failure(string error) => new(false, null, null, 0, error);
}

/// <summary>
/// Единая точка публикации новостей — используется и slash-командой, и веб-панелью.
/// </summary>
public sealed class NewsPublisher
{
    public const int MaxFileBytes = 8_000_000;
    public const int MaxMessageChars = 1900;
    public const int MaxFiles = 4;

    // Разрешаем только @everyone / @here. Роли и пользователи из текста не пингуются.
    private static readonly AllowedMentions Mentions = new(AllowedMentionTypes.Everyone);

    private readonly DiscordSocketClient _client;
    private readonly ILogger<NewsPublisher> _log;

    public NewsPublisher(DiscordSocketClient client, ILogger<NewsPublisher> log)
    {
        _client = client;
        _log = log;
    }

    /// <summary>Возвращает текст ошибки или null, если параметры корректны.</summary>
    public string? Validate(string message, IReadOnlyCollection<NewsFile> files)
    {
        message = (message ?? "").Trim();
        if (message.Length > MaxMessageChars)
            return $"Текст слишком длинный — максимум {MaxMessageChars} символов.";
        if (files.Count > MaxFiles)
            return $"Можно прикрепить не больше {MaxFiles} файлов.";
        if (files.Any(f => f.Bytes.Length == 0))
            return "Один из файлов пустой.";
        if (files.Any(f => f.Bytes.Length > MaxFileBytes))
            return $"Файл слишком большой — максимум {MaxFileBytes / 1_000_000} МБ на вложение.";
        if (message.Length == 0 && files.Count == 0)
            return "Нужно указать текст или прикрепить файл.";
        return null;
    }

    public async Task<PublishResult> PublishAsync(ulong channelId, string message, IReadOnlyList<NewsFile> files)
    {
        if (_client.GetChannel(channelId) is not ITextChannel channel)
            return PublishResult.Failure("Канал не найден или бот не может писать в него.");

        var attachments = files.Select(f => new FileAttachment(new MemoryStream(f.Bytes), f.Name)).ToList();

        try
        {
            IUserMessage sent = attachments.Count > 0
                ? await channel.SendFilesAsync(attachments, message.Trim(), allowedMentions: Mentions)
                : await channel.SendMessageAsync(message.Trim(), allowedMentions: Mentions);

            _log.LogInformation(
                "Новость опубликована: {Guild} / #{Channel}, сообщение {MessageId}",
                channel.Guild?.Name, channel.Name, sent.Id);

            return PublishResult.Success(channel.Guild?.Name, channel.Name, sent.Id);
        }
        catch (Exception ex)
        {
            _log.LogError(ex, "Не удалось опубликовать новость в канале {ChannelId}", channelId);
            return PublishResult.Failure("Не удалось опубликовать сообщение: " + ex.Message);
        }
    }
}
