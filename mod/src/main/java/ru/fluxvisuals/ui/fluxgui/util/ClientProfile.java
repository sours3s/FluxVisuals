package ru.fluxvisuals.ui.fluxgui.util;

/** Имя текущего игрока для карточки пользователя GUI FluxVisuals. */
public final class ClientProfile {
    private ClientProfile() {
    }

    public static String getUsername() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        return client != null && client.getSession() != null ? client.getSession().getUsername() : "Player";
    }
}
