package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class EmojiRender {
    private static final int COLUMNS = 8;
    private static final int ROWS = 8;
    private static final Identifier ATLAS = Identifier.of("fluxvisuals", "textures/gui/emoji_atlas.png");
    private static final int[] CODEPOINTS = new int[]{
        128512,
        128515,
        128516,
        128513,
        128518,
        128517,
        128514,
        129315,
        128522,
        128519,
        128578,
        128579,
        128521,
        128524,
        128525,
        129392,
        128536,
        128535,
        128537,
        128538,
        128523,
        128539,
        128540,
        129322,
        129320,
        129488,
        129299,
        128526,
        129321,
        129395,
        128527,
        128530,
        128542,
        128532,
        128543,
        128533,
        128577,
        128547,
        128534,
        128555,
        128553,
        129402,
        128546,
        128557,
        128548,
        128544,
        128545,
        129324,
        129327,
        128563,
        129397,
        129398,
        128561,
        128552,
        128560,
        128549,
        128531,
        129303,
        129300,
        129325,
        129323,
        129317,
        128566,
        128528
    };
    private static final Map<Integer, Integer> INDEX_BY_CODEPOINT = new HashMap<>();

    private EmojiRender() {
    }

    public static int count() {
        return CODEPOINTS.length;
    }

    public static int codepointAt(int index) {
        return CODEPOINTS[index];
    }

    public static String valueAt(int index) {
        return new String(Character.toChars(CODEPOINTS[index]));
    }

    public static boolean contains(int codepoint) {
        return INDEX_BY_CODEPOINT.containsKey(codepoint);
    }

    public static boolean isVariationSelector(int codepoint) {
        return codepoint == 65038 || codepoint == 65039;
    }

    public static float advance(float textSize) {
        return textSize * 0.92F;
    }

    public static void draw(DrawContext context, int codepoint, float x, float y, float size, int color) {
        Integer index = INDEX_BY_CODEPOINT.get(codepoint);
        if (index != null && !(size <= 0.0F)) {
            int col = index % 8;
            int row = index / 8;
            float u0 = col / 8.0F;
            float v0 = row / 8.0F;
            float u1 = (col + 1.0F) / 8.0F;
            float v1 = (row + 1.0F) / 8.0F;
            Gui.texQuad(context, ATLAS, x, y, x + size, y + size, u0, v0, u1, v1, color);
        }
    }

    static {
        for (int index = 0; index < CODEPOINTS.length; index++) {
            INDEX_BY_CODEPOINT.put(CODEPOINTS[index], index);
        }
    }
}
