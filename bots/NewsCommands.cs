using Discord;
using Discord.Interactions;

namespace FluxVisualsBots;

/// <summary>Публикация новостей через slash-команду. Команду может использовать только owner.</summary>
public sealed class NewsCommands : InteractionModuleBase<SocketInteractionContext>
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(60) };

    private readonly NewsPublisher _publisher;
    private readonly ulong _ownerId;

    public NewsCommands(NewsPublisher publisher)
    {
        _publisher = publisher;
        ulong.TryParse(Environment.GetEnvironmentVariable("DISCORD_OWNER_ID"), out _ownerId);
    }

    [SlashCommand("news", "Опубликовать сообщение с файлами в выбранном канале")]
    public async Task News(
        [Summary("channel", "Канал, куда опубликовать сообщение")] IChannel channel,
        [Summary("message", "Текст сообщения")] string message = "",
        [Summary("file1", "Скриншот или файл")] IAttachment? file1 = null,
        [Summary("file2", "Скриншот или файл")] IAttachment? file2 = null,
        [Summary("file3", "Скриншот или файл")] IAttachment? file3 = null,
        [Summary("file4", "Скриншот или файл")] IAttachment? file4 = null)
    {
        // Discord даёт 3 секунды на ответ — сначала подтверждаем, потом делаем работу.
        await DeferAsync(ephemeral: true);

        if (_ownerId == 0 || Context.User.Id != _ownerId)
        {
            await Respond("Эту команду может использовать только владелец бота.");
            return;
        }

        if (Context.Guild == null)
        {
            await Respond("Команда работает только на сервере.");
            return;
        }

        var files = new List<NewsFile>();
        foreach (var attachment in new[] { file1, file2, file3, file4 })
        {
            if (attachment == null) continue;
            if (attachment.Size > NewsPublisher.MaxFileBytes)
            {
                await Respond("Файл слишком большой — максимум 8 МБ на вложение.");
                return;
            }

            try
            {
                byte[] bytes = await Http.GetByteArrayAsync(attachment.Url);
                files.Add(new NewsFile(attachment.Filename, bytes));
            }
            catch (Exception)
            {
                await Respond("Не удалось скачать вложение. Попробуйте ещё раз.");
                return;
            }
        }

        string? error = _publisher.Validate(message, files);
        if (error != null)
        {
            await Respond(error);
            return;
        }

        PublishResult result = await _publisher.PublishAsync(channel.Id, message, files);
        await Respond(result.Ok ? $"Опубликовано в <#{channel.Id}>." : result.Error!);
    }

    private async Task Respond(string text) => await ModifyOriginalResponseAsync(m => m.Content = text);
}
