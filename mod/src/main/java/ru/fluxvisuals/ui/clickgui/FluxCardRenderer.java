package ru.fluxvisuals.ui.clickgui;

import ru.fluxvisuals.ui.clickgui.FluxClickGuiModel.Module;

public final class FluxCardRenderer {
    private static final int WHITE = 0xFFF7F4EC, TEXT = 0xFFD9D5C9, MUTED = 0xFF898A85;

    public static float baseHeight(Module m, float s) {
        return (m.description != null && !m.description.isBlank()) ? 38 * s : 28 * s;
    }

    public static void draw(UiDrawList ui, Module m, float x, float y, float w, float h, float s,
                            int accent, boolean favorite, boolean expanded, boolean over,
                            String bindLabel, boolean binding, float dt) {
        float baseH = baseHeight(m, s);
        float p = m.animation.update(dt, 16);

        int bgColor = over ? 0xFF181818 : 0xFF131313;
        int outlineColor = m.enabled ? accent : (over ? 0xFF2D2D2D : 0xFF202020);

        ui.roundedRect(x, y, w, h, 6 * s, bgColor)
          .outlineRoundedRect(x, y, w, h, 6 * s, 0.6f * s, outlineColor);

        // Star / favorite
        ui.icon(x + 10 * s, y + 12 * s, 8.5f * s, favorite ? accent : (over ? 0xFF555555 : 0xFF353535), 0xE838);

        // Module name
        int nameColor = m.enabled ? WHITE : (over ? TEXT : 0xFFB0AEA6);
        ui.strongText(x + 22 * s, y + 12 * s, 8.2f * s, nameColor, m.name);

        // Expand / Settings button (right-most)
        if (!m.settings.isEmpty()) {
            int expandColor = expanded ? accent : (over ? 0xFF888888 : 0xFF4E4E4E);
            ui.icon(x + w - 10 * s, y + 12 * s, 8 * s, expandColor, expanded ? 0xE5CE : 0xE5CF);
        }

        // Keybind button
        float bindW = Math.max(22 * s, (bindLabel.length() * 5.2f + 8) * s);
        float bindX = x + w - (m.settings.isEmpty() ? 10 * s : 22 * s) - bindW;
        int bindBg = binding ? 0xFF3D2518 : 0xFF1E1E1E;
        int bindTextCol = binding ? accent : (bindLabel.equals("BIND") ? 0xFF555555 : MUTED);
        ui.roundedRect(bindX, y + 6 * s, bindW, 12 * s, 3 * s, bindBg)
          .centeredText(bindX + bindW * 0.5f, y + 12 * s, 5.8f * s, bindTextCol, binding ? "..." : bindLabel, false);

        // Description
        if (m.description != null && !m.description.isBlank()) {
            String desc = m.description;
            if (desc.length() > 38) {
                desc = desc.substring(0, 35) + "...";
            }
            ui.text(x + 12 * s, y + 25 * s, 6.8f * s, MUTED, desc);
        }
    }

    private FluxCardRenderer() {}
}
