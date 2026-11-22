using Discord;
using Discord.Interactions;

namespace FluxVisualsBots;

/// <summary>Публикация новостей через slash-команду. Команду может использовать только owner.</summary>
public sealed class NewsCommands : InteractionModuleBase<SocketInteractionContext>
{
    private const int MaxFileBytes = 8_000_000;
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(60) };

    private readonly ulong _ownerId;

    public NewsCommands()
    {
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

        var guild = Context.Guild;
        if (guild == null)
        {
            await Respond("Команда работает только на сервере.");
            return;
        }

        var target = guild.GetTextChannel(channel.Id);
        if (target == null)
        {
            await Respond("Канал не найден или бот не может писать в него.");
            return;
        }

        message = (message ?? "").Trim();
        if (message.Length > 1900)
        {
            await Respond("Текст слишком длинный — максимум 1900 символов.");
            return;
        }

        var files = new List<FileAttachment>();
        foreach (var attachment in new[] { file1, file2, file3, file4 })
        {
            if (attachment == null) continue;
            if (attachment.Size > MaxFileBytes)
            {
                await Respond("Файл слишком большой — максимум 8 МБ на вложение.");
                return;
            }

            try
            {
                byte[] bytes = await Http.GetByteArrayAsync(attachment.Url);
                if (bytes.Length > MaxFileBytes)
                {
                    await Respond("Файл слишком большой — максимум 8 МБ на вложение.");
                    return;
                }
                files.Add(new FileAttachment(new MemoryStream(bytes), attachment.Filename));
            }
            catch (Exception)
            {
                await Respond("Не удалось скачать вложение. Попробуйте ещё раз.");
                return;
            }
        }

        if (message.Length == 0 && files.Count == 0)
        {
            await Respond("Нужно указать текст или прикрепить файл.");
            return;
        }

        try
        {
            if (files.Count > 0)
                await target.SendFilesAsync(files, message, allowedMentions: AllowedMentions.None);
            else
                await target.SendMessageAsync(message, allowedMentions: AllowedMentions.None);

            await Respond($"Опубликовано в {target.Mention}.");
        }
        catch (Exception ex)
        {
            await Respond("Не удалось опубликовать сообщение: " + ex.Message);
        }
    }

    private async Task Respond(string text)
    {
        await ModifyOriginalResponseAsync(m => m.Content = text);
    }
}
