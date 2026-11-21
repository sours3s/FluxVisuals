package ru.fluxvisuals.ui.gui.clickgui.util;

import java.util.List;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

public interface TargetSorter {
    List<LivingEntity> sort(List<LivingEntity> list, ClientPlayerEntity clientPlayerEntity);

    default TargetSorter then(TargetSorter targetSorter) {
        return (list, clientPlayerEntity) -> {
            return targetSorter.sort(sort(list, clientPlayerEntity), clientPlayerEntity);
        };
    }
}
