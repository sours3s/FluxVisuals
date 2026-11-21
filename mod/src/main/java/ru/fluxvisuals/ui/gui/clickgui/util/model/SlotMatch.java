package ru.fluxvisuals.ui.gui.clickgui.util.model;

import net.minecraft.item.ItemStack;

public final class SlotMatch {
    public final ScopedSlot slotReference;
    public final ItemStack stack;

    public SlotMatch(ScopedSlot scopedSlot, ItemStack itemStack) {
        this.slotReference = scopedSlot;
        this.stack = itemStack;
    }

    public boolean found() {
        return this.slotReference != null;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "slotReference=" + this.slotReference + ", " + "stack=" + this.stack + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.slotReference, this.stack);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SlotMatch)) return false;
        SlotMatch o = (SlotMatch) obj;
        return java.util.Objects.equals(this.slotReference, o.slotReference) && java.util.Objects.equals(this.stack, o.stack);
    }
public ScopedSlot slotReference() {
        return this.slotReference;
    }

    public ItemStack stack() {
        return this.stack;
    }
}
