package ru.fluxvisuals.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // Получаем экземпляр PlayerEntity
        PlayerEntity player = (PlayerEntity)(Object)this;

        // Здесь можно добавить логику обновления игрока каждый тик
        // Например, проверка состояния игрока или выполнение кастомных действий
    }

    // @Inject(method = "jump", at = @At("HEAD"))
    // private void onJump(CallbackInfo ci) {
    //     // Отключено: несовпадение маппингов (jump vs method_6043)
    //     // Получаем экземпляр PlayerEntity
    //     PlayerEntity player = (PlayerEntity)(Object)this;
    //
    //     // Здесь можно добавить логику прыжка игрока
    //     // Например, добавление кастомных эффектов при прыжке
    // }
}