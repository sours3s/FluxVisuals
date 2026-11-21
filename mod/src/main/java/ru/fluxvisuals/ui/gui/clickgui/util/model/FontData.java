package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.util.render.FontMetrics;
import ru.fluxvisuals.ui.gui.clickgui.util.render.KerningPair;
import ru.fluxvisuals.ui.gui.clickgui.config.SpriteRenderConfig;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FontData {

    @SerializedName("atlas")
    public SpriteRenderConfig atlas;

    @SerializedName("metrics")
    public FontMetrics metrics;

    @SerializedName("glyphs")
    public List<GlyphData> glyphs;

    @SerializedName("kerning")
    public List<KerningPair> kernings;

    public SpriteRenderConfig atlas() {
        return this.atlas;
    }

    public FontMetrics metrics() {
        return this.metrics;
    }

    public List<GlyphData> glyphs() {
        return this.glyphs;
    }

    public List<KerningPair> kernings() {
        return this.kernings;
    }
}
