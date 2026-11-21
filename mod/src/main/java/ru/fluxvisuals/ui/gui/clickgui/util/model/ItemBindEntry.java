package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import net.minecraft.item.ItemStack;

public class ItemBindEntry {
    public int count;
    public boolean active;
    public String keyText = "";
    public ItemStack stack = ItemStack.EMPTY;
    public final AnimatedFloat animation = new AnimatedFloat(250, Easings.LINEAR);

    public ItemBindEntry() {
    }

    public void animate(WeightedEngine weightedEngine) {
        this.animation.destination(this.active ? 1.0f : 0.0f);
        this.animation.animate(weightedEngine);
    }
}
