package ru.fluxvisuals.ui.gui.clickgui.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

public class HealthSorter implements TargetSorter {
    public static final Comparator<LivingEntity> healthComparator;

    @Override
    public List<LivingEntity> sort(List<LivingEntity> list, ClientPlayerEntity clientPlayerEntity) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        list.sort(healthComparator);
        return list;
    }

    static {
        ScoreboardHelper scoreboardHelper = ScoreboardHelper.INSTANCE;
        Objects.requireNonNull(scoreboardHelper);
        healthComparator = Comparator.comparingDouble(scoreboardHelper::getHealthBelowName);
    }
}
