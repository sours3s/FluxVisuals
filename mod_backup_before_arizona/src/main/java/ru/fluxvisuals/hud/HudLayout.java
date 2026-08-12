package ru.fluxvisuals.hud;

import net.minecraft.client.gui.DrawContext;

import java.util.EnumMap;
import java.util.function.Consumer;

/** Раскладка HUD: стеки у четырёх углов экрана, чтобы элементы не перекрывались. */
public final class HudLayout {
    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    public static final HudLayout INSTANCE = new HudLayout();

    private final EnumMap<Anchor, Float> cursors = new EnumMap<>(Anchor.class);
    private int screenWidth;
    private int screenHeight;
    private final int padding = 4;
    private final int gap = 4;

    private HudLayout() {}

    public void reset(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        cursors.put(Anchor.TOP_LEFT, (float) padding);
        cursors.put(Anchor.TOP_RIGHT, (float) padding);
        cursors.put(Anchor.BOTTOM_LEFT, (float) (height - padding));
        cursors.put(Anchor.BOTTOM_RIGHT, (float) (height - padding));
    }

    /** Рисует элемент у якоря и отступает под следующий. drawer получает [x, y] верхнего левого угла. */
    public void push(DrawContext g, Anchor anchor, float width, float height, Consumer<float[]> drawer) {
        float y = cursors.get(anchor);
        float x;
        switch (anchor) {
            case TOP_RIGHT -> x = screenWidth - padding - width;
            case BOTTOM_RIGHT -> x = screenWidth - padding - width;
            default -> x = padding;
        }
        drawer.accept(new float[]{x, y});
        switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> cursors.put(anchor, y + height + gap);
            case BOTTOM_LEFT, BOTTOM_RIGHT -> cursors.put(anchor, y - height - gap);
        }
    }

    public static Anchor fromIndex(int index) {
        return switch (index) {
            case 1 -> Anchor.TOP_RIGHT;
            case 2 -> Anchor.BOTTOM_LEFT;
            case 3 -> Anchor.BOTTOM_RIGHT;
            default -> Anchor.TOP_LEFT;
        };
    }
}
