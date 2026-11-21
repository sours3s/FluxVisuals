package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public class IconLabelBadge extends AbstractWidget {
    public final MsdfFont font = Fonts.INTER_EXTRA_BOLD.get();
    static final int textSize = 9;
    static final float padding = 6.0f;
    public final Translation label;
    public final GlTexture icon;
    public float width;
    public Integer baseColor;
    public Integer textColor;

    public IconLabelBadge(GlTexture glTexture, Translation translation) {
        this.label = translation;
        this.icon = glTexture;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.colorStack();
        int iIntValue = this.baseColor != null ? this.baseColor.intValue() : themePalette.accent().argb();
        int iIntValue2 = this.textColor != null ? this.textColor.intValue() : iIntValue;
        float height = this.font.getHeight(9.0f);
        float fHeight = height() / 2.0f;
        drawCtx.fillRoundedRect(x(), y(), width(), height(), 5.0f, colorStack115.computeColor(iIntValue, 0.15f));
        drawCtx.textureVerticalC(this.icon, x() + padding, y() + fHeight, 10, 10, colorStack115.computeColor(iIntValue2));
        drawCtx.text(this.font, this.label.effective().toUpperCase(), textSize, x() + padding + 10.0f + 4.0f, (y() + fHeight) - (height / 2.0f), colorStack115.computeColor(iIntValue2));
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.width = 26.0f + layoutScaleContext.textWidthPhysical(this.font, this.label.effective().toUpperCase(), textSize);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return 19.0f;
    }

    public void setBaseColor(Integer num) {
        this.baseColor = num;
    }

    public void setTextColor(Integer num) {
        this.textColor = num;
    }
}
