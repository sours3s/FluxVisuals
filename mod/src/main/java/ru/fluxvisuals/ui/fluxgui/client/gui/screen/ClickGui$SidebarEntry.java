package ru.fluxvisuals.ui.fluxgui.client.gui.screen;

import ru.fluxvisuals.ui.fluxgui.module.Category;
import java.util.Objects;

public final class ClickGui$SidebarEntry {
    Category category;
    String icon;
    String headerIcon;
    float headerScale;

    public ClickGui$SidebarEntry(Category category, String icon, String headerIcon, float headerScale) {
        this.category = category;
        this.icon = icon;
        this.headerIcon = headerIcon;
        this.headerScale = headerScale;
    }

    public Category category() {
        return this.category;
    }

    public String icon() {
        return this.icon;
    }

    public String headerIcon() {
        return this.headerIcon;
    }

    public float headerScale() {
        return this.headerScale;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof ClickGui$SidebarEntry other)
                ? false
                : Objects.equals(this.category, other.category)
                    && Objects.equals(this.icon, other.icon)
                    && Objects.equals(this.headerIcon, other.headerIcon)
                    && Objects.equals(this.headerScale, other.headerScale);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.category, this.icon, this.headerIcon, this.headerScale);
    }

    @Override
    public String toString() {
        return "ClickGui$SidebarEntry[category="
            + this.category
            + ", icon="
            + this.icon
            + ", headerIcon="
            + this.headerIcon
            + ", headerScale="
            + this.headerScale
            + "]";
    }
}
