package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

public class ThemeCreatorPanel extends WidgetContainer {
    public final GlTexture brushIcon = new GlTexture(new ClasspathResource("/icons/menu/new/brush.png"));
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final WidgetBounds headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 46.0f);
    public final IconLabelBadge titleBadge = new IconLabelBadge(this.brushIcon, Translation.clearText("Цветовые темы"));
    public boolean opened = false;

    @Override
    public void render(DrawCtx drawCtx) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        this.titleBadge.render(drawCtx);
        drawCtx.text(this.titleFont, Translation.clearText("Создание цветовой темы").effective(), 16, this.headerBounds.x(), this.headerBounds.y() + this.titleBadge.height() + 8.0f, colorStack115.computeColor(themePalette.text().tone(300).argb()));
        super.render(drawCtx);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.headerBounds.withSize(width() - 40.0f, 46.0f).withPosition(x() + 20.0f, y() + 16.0f);
        this.titleBadge.layout(layoutScaleContext);
        this.titleBadge.setPosition(this.headerBounds.x(), this.headerBounds.y());
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        super.animation(weightedEngine);
    }

    public void open() {
        this.opened = true;
    }

    public void close() {
        if (this.opened) {
            this.opened = false;
        }
    }

    @Override
    public float width() {
        return MenuWindow.MENU_WIDTH;
    }

    @Override
    public float height() {
        return MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT;
    }

    public boolean isOpened() {
        return this.opened;
    }
}
