package ru.fluxvisuals.ui.fluxgui.command;

/** Вывод системных сообщений GUI FluxVisuals в чат. */
public final class CommandManager {
    private CommandManager() {
    }

    public static void addChatMessage(String message) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        }
    }
}
