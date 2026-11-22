package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import java.util.Objects;
import net.minecraft.item.ItemStack;

public final class Cooldowns$CooldownRow {
    ItemStack stack;
    String name;
    String timer;
    float progress;

    public Cooldowns$CooldownRow(ItemStack stack, String name, String timer, float progress) {
        this.stack = stack;
        this.name = name;
        this.timer = timer;
        this.progress = progress;
    }

    public ItemStack stack() {
        return this.stack;
    }

    public String name() {
        return this.name;
    }

    public String timer() {
        return this.timer;
    }

    public float progress() {
        return this.progress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof Cooldowns$CooldownRow other)
                ? false
                : Objects.equals(this.stack, other.stack)
                    && Objects.equals(this.name, other.name)
                    && Objects.equals(this.timer, other.timer)
                    && Objects.equals(this.progress, other.progress);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stack, this.name, this.timer, this.progress);
    }

    @Override
    public String toString() {
        return "Cooldowns$CooldownRow[stack=" + this.stack + ", name=" + this.name + ", timer=" + this.timer + ", progress=" + this.progress + "]";
    }
}
