package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ConfigFilter;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;


public final class ThemeFilterIndicator {
    public final ConfigFilter filter;
    public final String label;
    public final ToggleAnimator animation;

    ThemeFilterIndicator(ConfigFilter configFilter, String str) {
        this(configFilter, str, new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC));
    }

    public ThemeFilterIndicator(ConfigFilter configFilter, String str, ToggleAnimator toggleAnimator) {
        this.filter = configFilter;
        this.label = str;
        this.animation = toggleAnimator;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "filter=" + this.filter + ", " + "label=" + this.label + ", " + "animation=" + this.animation + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.filter, this.label, this.animation);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ThemeFilterIndicator)) return false;
        ThemeFilterIndicator o = (ThemeFilterIndicator) obj;
        return java.util.Objects.equals(this.filter, o.filter) && java.util.Objects.equals(this.label, o.label) && java.util.Objects.equals(this.animation, o.animation);
    }
public ConfigFilter filter() {
        return this.filter;
    }

    public String label() {
        return this.label;
    }

    public ToggleAnimator animation() {
        return this.animation;
    }
}
