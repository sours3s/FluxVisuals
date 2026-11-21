package ru.fluxvisuals.ui.fluxgui.setting;

import java.util.function.Supplier;
import net.minecraft.item.Item;

public class ItemScaleSetting extends Setting<Boolean> {
    private final Item item;

    public ItemScaleSetting(String name, Item item) {
        super(name, false);
        this.item = item;
    }

    public ItemScaleSetting(String name, Item item, Supplier<Boolean> visibility) {
        super(name, false, visibility);
        this.item = item;
    }

    public Item getItem() {
        return this.item;
    }

    public void toggle() {
        this.setValue(!this.getValue());
    }
}
