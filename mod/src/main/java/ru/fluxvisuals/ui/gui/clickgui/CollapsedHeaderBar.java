package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

public class CollapsedHeaderBar extends WidgetContainer {
    public final Widget unfoldButton;
    public final GlTexture searchIcon = new GlTexture(new ClasspathResource("/icons/menu/new/search.png"));
    public final GlTexture languageIcon = new GlTexture(new ClasspathResource("/icons/menu/new/language.png"));
    public final GlTexture unfoldIcon = new GlTexture(new ClasspathResource("/icons/menu/new/unfold.png"));
    public final GlTexture logoIcon = new GlTexture(new ClasspathResource("/icons/menu/new/client_logo.png"));
    public final Widget languageButton = new IconButtonWidget(this.languageIcon, () -> {
        FluxVisuals.INSTANCE.menuWindow().contentArea().requestNextLanguage();
    }, 13.0f, 13.0f);
    public final Widget searchButton = new IconButtonWidget(this.searchIcon, () -> {
        FluxVisuals.INSTANCE.menuWindow().searchContainer().invert();
    }, 13.0f, 13.0f);

    public CollapsedHeaderBar(Runnable runnable) {
        this.unfoldButton = new IconButtonWidget(this.unfoldIcon, runnable, 13.0f, 13.0f);
        addChild(new CollapsedTabBar());
        addChild(this.unfoldButton);
        addChild(this.languageButton);
        addChild(this.searchButton);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.colorStack();
        drawCtx.roundedTexture(FluxVisuals.INSTANCE.userSession().texture(), ((x() + width()) - 16.0f) - 22.0f, (y() + (height() / 2.0f)) - 11.0f, 22.0f, 22.0f, 6.0f, colorStack115.white());
        drawCtx.roundedTexture(this.logoIcon, x() + 16.0f, (y() + (height() / 2.0f)) - (this.logoIcon.height() / 2), this.logoIcon.width(), this.logoIcon.height(), 6.0f, colorStack115.white());
        super.render(drawCtx);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.COLLAPSED_HEADER_HEIGHT);
        this.logoIcon.setDimensions(26, 26);
        float fX = ((((x() + width()) - 16.0f) - 22.0f) - 16.0f) - this.languageButton.width();
        this.languageButton.setPosition(fX, (y() + (height() / 2.0f)) - (this.languageButton.height() / 2.0f));
        float fWidth2 = fX - (this.languageButton.width() + 14.0f);
        this.searchButton.setPosition(fWidth2, (y() + (height() / 2.0f)) - (this.searchButton.height() / 2.0f));
        this.unfoldButton.setPosition(fWidth2 - (this.searchButton.width() + 14.0f), (y() + (height() / 2.0f)) - (this.unfoldButton.height() / 2.0f));
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        super.animation(weightedEngine);
    }
}
