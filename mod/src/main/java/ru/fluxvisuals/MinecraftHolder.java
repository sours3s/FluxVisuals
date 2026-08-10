package ru.fluxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public interface MinecraftHolder {
    MinecraftClient mc = MinecraftClient.getInstance();
    Window window = MinecraftClient.getInstance().getWindow();
}
