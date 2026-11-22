using Discord;
using Discord.Interactions;

namespace FluxVisualsBots;

/// <summary>Публикация новостей через slash-команду. Команду может использовать только owner.</summary>
public sealed class NewsCommands : InteractionModuleBase<SocketInteractionContext>
{
    private readonly ulong _ownerId;

    public NewsCommands()
    {
        ulong.TryParse(Environment.GetEnvironmentVariable("DISCORD_OWNER_ID"), out _ownerId);
    }

    [SlashCommand("news", "Опубликовать сообщение в выбранном канале")]
    public async Task News(
        [Summary("channel", "Канал, куда опубликовать сообщение")] IChannel channel,
        [Summary("message", "Текст сообщения")] string message)
    {
        if (_ownerId == 0 || Context.User.Id != _ownerId)
        {
            await RespondAsync("Эту команду может использовать только владелец бота.", ephemeral: true);
            return;
        }

        var guild = Context.Guild;
        if (guild == null)
        {
            await RespondAsync("Команда работает только на сервере.", ephemeral: true);
            return;
        }

        var target = guild.GetTextChannel(channel.Id);
        if (target == null)
        {
            await RespondAsync("Канал не найден или бот не может писать в него.", ephemeral: true);
            return;
        }

        message = (message ?? "").Trim();
        if (message.Length == 0)
        {
            await RespondAsync("Сообщение пустое.", ephemeral: true);
            return;
        }
        if (message.Length > 1900)
        {
            await RespondAsync("Сообщение слишком длинное — максимум 1900 символов.", ephemeral: true);
            return;
        }

        await target.SendMessageAsync(message);
        await RespondAsync($"Опубликовано в {target.Mention}.", ephemeral: true);
    }
}
