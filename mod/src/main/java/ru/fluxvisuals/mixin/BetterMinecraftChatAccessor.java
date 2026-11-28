package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public interface BetterMinecraftChatAccessor {
    @Invoker("addVisibleMessage")
    void fluxvisuals$invokeAddVisibleMessage(ChatHudLine line);
}
