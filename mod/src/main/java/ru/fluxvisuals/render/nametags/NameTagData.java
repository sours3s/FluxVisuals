package ru.fluxvisuals.render.nametags;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

/** CPU mirror of the std140 block shared by the ported NameTags shaders. */
final class NameTagData {
    static final String UNIFORM_NAME = "NameTagData";

    static final int FONT_PARAMS = 0;
    static final int BLUR_PARAMS = 1;
    static final int RECT_SIZE = 2;
    static final int RECT_CONTENT = 3;
    static final int CORNER_RADIUS = 4;
    static final int FILL_COLOR = 5;
    static final int BORDER_COLOR = 6;
    static final int SHADOW_COLOR = 7;
    static final int SHADOW_PARAMS = 8;
    static final int SEG_CENTERS_A = 9;
    static final int SEG_CENTERS_B = 10;
    static final int SEG_HALF_WIDTHS_A = 11;
    static final int SEG_HALF_WIDTHS_B = 12;
    static final int COUNTS = 13;
    static final int BOX_CENTER_A = 14;
    static final int BOX_HALF_SIZE_A = 15;
    static final int BOX_RADII_0 = 16;
    static final int BOX_RADII_1 = 17;
    static final int FILLET_RADIUS = 18;
    static final int TEXTURE_PARAMS = 19;
    static final int TEXTURE_SMOOTH = 20;
    static final int VIEW_BOX = 21;
    static final int SVG_PARAMS = 22;
    static final int SVG_COLOR = 23;
    static final int GLASS_PARAMS_0 = 24;
    static final int GLASS_PARAMS_1 = 25;
    static final int GLASS_PARAMS_2 = 26;
    static final int GLASS_PARAMS_3 = 27;
    static final int GLASS_TINT = 28;
    static final int GLASS_GLOW_COLOR = 29;
    static final int RESOLUTION_PARAMS = 30;
    static final int MISC_PARAMS = 31;
    static final int SVG_SEGMENTS = 32;

    private static final int SVG_SEGMENT_COUNT = 256;
    private static final int FIELD_COUNT = SVG_SEGMENTS + SVG_SEGMENT_COUNT;
    static final int BUFFER_SIZE = FIELD_COUNT * 16;

    private final ByteBuffer buffer = MemoryUtil.memAlloc(BUFFER_SIZE);

    void reset() {
        buffer.clear();
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer.put((byte) 0);
        }
        buffer.flip();
    }

    void vec4(int field, float x, float y, float z, float w) {
        int offset = field * 16;
        buffer.putFloat(offset, x);
        buffer.putFloat(offset + 4, y);
        buffer.putFloat(offset + 8, z);
        buffer.putFloat(offset + 12, w);
    }

    void ivec4(int field, int x, int y, int z, int w) {
        int offset = field * 16;
        buffer.putInt(offset, x);
        buffer.putInt(offset + 4, y);
        buffer.putInt(offset + 8, z);
        buffer.putInt(offset + 12, w);
    }

    void rgba(int field, int argb) {
        vec4(field,
              ((argb >> 16) & 0xFF) / 255.0F,
              ((argb >> 8) & 0xFF) / 255.0F,
              (argb & 0xFF) / 255.0F,
              ((argb >>> 24) & 0xFF) / 255.0F);
    }

    void segment(int index, float x1, float y1, float x2, float y2) {
        if (index >= 0 && index < SVG_SEGMENT_COUNT) {
            vec4(SVG_SEGMENTS + index, x1, y1, x2, y2);
        }
    }

    ByteBuffer data() {
        buffer.position(0);
        buffer.limit(BUFFER_SIZE);
        return buffer;
    }

    void close() {
        MemoryUtil.memFree(buffer);
    }
}
