package ru.fluxvisuals.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/entity/player/ItemCooldownManager$Entry")
public interface ItemCooldownManagerEntryAccessor {
    @Accessor("startTick")
    int getStartTick();

    @Accessor("endTick")
    int getEndTick();
}
