package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.type.TargetSortMode;

import java.util.Set;

public class TargetSorters {
    public static final DistanceSorter DISTANCE = new DistanceSorter();
    public static final FovSorter FOV = new FovSorter();
    public static final HealthSorter HEALTH = new HealthSorter();

    public static TargetSorter of(Set<TargetSortMode> set) {
        if (set == null || set.isEmpty() || set.size() == TargetSortMode.values().length) {
            return FOV.then(HEALTH).then(DISTANCE);
        }
        TargetSorter targetSorter = null;
        if (set.contains(TargetSortMode.BY_FOV)) {
            targetSorter = FOV;
        }
        if (set.contains(TargetSortMode.BY_HEALTH)) {
            targetSorter = targetSorter == null ? HEALTH : targetSorter.then(HEALTH);
        }
        if (set.contains(TargetSortMode.BY_DISTANCE)) {
            targetSorter = targetSorter == null ? DISTANCE : targetSorter.then(DISTANCE);
        }
        return targetSorter == null ? FOV.then(HEALTH).then(DISTANCE) : targetSorter;
    }
}
