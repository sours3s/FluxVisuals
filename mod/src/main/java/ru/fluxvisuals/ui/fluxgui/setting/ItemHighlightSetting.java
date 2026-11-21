package ru.fluxvisuals.ui.fluxgui.setting;

import net.minecraft.item.Item;

public class ItemHighlightSetting extends Setting<Boolean> {
    private final Item item;
    private int color;

    public ItemHighlightSetting(String name, Item item, int color) {
        super(name, true);
        this.item = item;
        this.color = color;
    }

    public Item getItem() {
        return this.item;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void toggle() {
        this.setValue(!this.getValue());
    }
}
