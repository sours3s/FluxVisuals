package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import ru.fluxvisuals.ui.gui.clickgui.externall.animation.Animation;
import ru.fluxvisuals.ui.gui.clickgui.externall.animation.Easings;
import ru.fluxvisuals.ui.gui.clickgui.externall.colors.ColorUtil;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.font.Font;

public class RollingText {

    private final float slide;

    private String value = "";
    private String prev = "";

    private final Animation animation = new Animation();

    public RollingText() {
        this(4F);
    }

    public RollingText(float slide) {
        this.slide = slide;
    }

    public void set(String next) {
        if (next == null || next.equals(value)) return;

        prev = value;
        value = next;

        animation.set(0);
        animation.run(1, 0.18, Easings.QUAD_OUT);
    }

    public String get() {
        return value;
    }

    public float width(Font font, float size) {
        return font.getWidth(value, size);
    }

    public void draw(Font font, float x, float y, float size, int color) {
        animation.update();

        float t = animation.get();

        float cx = x;

        for (int i = 0; i < value.length(); i++) {
            String ch = value.substring(i, i + 1);

            int j = prev.length() - (value.length() - i);
            String old = j >= 0 && j < prev.length() ? prev.substring(j, j + 1) : null;

            if (t >= 1F || ch.equals(old)) {
                font.draw(ch, cx, y, size, color);
            } else {
                font.draw(ch, cx, y + slide * (1 - t), size, ColorUtil.multAlpha(color, t));

                if (old != null) font.draw(old, cx, y - slide * t, size, ColorUtil.multAlpha(color, 1 - t));
            }

            cx += font.getWidth(ch, size);
        }
    }
}

