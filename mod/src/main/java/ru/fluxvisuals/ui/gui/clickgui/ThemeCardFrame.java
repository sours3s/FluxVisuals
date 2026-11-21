package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ConfigOrigin;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ThemeMode;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.ArrayList;
import java.util.List;

public class ThemeCardFrame extends AbstractFrame {
    public final GlTexture themeIcon;
    public final GlTexture smileIcon;
    public final GlTexture checkmarkIcon;
    public final GlTexture calendarIcon;
    static final GlTexture loadIcon = new GlTexture(new ClasspathResource("/icons/menu/new/click.png"));
    static final GlTexture copyIcon = new GlTexture(new ClasspathResource("/icons/menu/new/copy.png"));
    static final GlTexture starIcon = new GlTexture(new ClasspathResource("/icons/menu/new/star.png"));
    static final GlTexture editIcon = new GlTexture(new ClasspathResource("/icons/menu/new/pencil.png"));
    static final GlTexture deleteIcon = new GlTexture(new ClasspathResource("/icons/menu/new/trash.png"));
    public final MsdfFont boldFont;
    public final MsdfFont semiboldFont;
    public final AnimatedFloat xAnimation;
    public final AnimatedFloat yAnimation;
    public final WidgetBounds iconBounds;
    public final ToggleAnimator selectedAnimator;
    public final ToggleAnimator favoriteAnimator;
    public final ToggleAnimator hoverAnimator;
    public final ToggleAnimator visibilityAnimator;
    public final WidgetBounds checkBounds;
    public final ThemeCard themeContext;
    public final IconLabelBadge modeBadge;
    public final List<IconButtonWidget> actionButtons;
    public final IconButtonWidget authorButton;
    public List<Integer> paletteColors;
    public boolean pendingHide;
    public boolean selected;
    public boolean favorite;
    public boolean positionInitialized;
    public boolean menuDragging;

    public ThemeCardFrame(ThemeCard themeCard) {
        super(new FrameElementColumn(0.0f, 0.0f));
        this.themeIcon = new GlTexture(new ClasspathResource("/icons/menu/new/tabs/themes.png"));
        this.smileIcon = new GlTexture(new ClasspathResource("/icons/menu/new/smile.png"));
        this.checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
        this.calendarIcon = new GlTexture(new ClasspathResource("/icons/menu/new/calendar.png"));
        this.boldFont = Fonts.INTER_BOLD.get();
        this.semiboldFont = Fonts.INTER_SEMIBOLD.get();
        this.xAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.yAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.iconBounds = new WidgetBounds(0.0f, 0.0f, 36.0f, 36.0f);
        this.selectedAnimator = new ToggleAnimator(100, Easings.LINEAR);
        this.favoriteAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
        this.hoverAnimator = new ToggleAnimator(100, Easings.LINEAR);
        this.visibilityAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
        this.checkBounds = new WidgetBounds(0.0f, 0.0f, 19.0f, 19.0f);
        this.actionButtons = new ArrayList();
        this.paletteColors = new ArrayList();
        this.selected = false;
        this.favorite = false;
        this.themeContext = themeCard;
        this.modeBadge = new IconLabelBadge(this.themeIcon, themeCard.type() == ThemeMode.LIGHT ? Translation.clearText("Светлая тема") : Translation.clearText("Тёмная тема"));
        this.paletteColors = extractPaletteColors(themeCard.palette());
        this.authorButton = new IconButtonWidget(this.smileIcon, () -> {
        }, 12.0f, 12.0f).tooltip(Translation.clearText("Вы являетесь автором этой темы"), this.smileIcon);
        this.actionButtons.addAll(List.of(new IconButtonWidget(loadIcon, this::selectExclusive, 14.0f, 14.0f).tooltip(Translation.clearText("Загрузить тему"), loadIcon), new IconButtonWidget(copyIcon, () -> {
        }, 14.0f, 14.0f).tooltip(Translation.clearText("Скопировать Cloud ID темы"), copyIcon), new IconButtonWidget(starIcon, this::invertFavorite, 14.0f, 14.0f).tooltip(Translation.clearText("Добавить тему в избранные"), starIcon), new IconButtonWidget(editIcon, () -> {
        }, 14.0f, 14.0f).tooltip(Translation.clearText("Редактировать тему"), editIcon), new IconButtonWidget(deleteIcon, () -> {
        }, 14.0f, 14.0f).tooltip(Translation.clearText("Удалить тему"), deleteIcon)));
        addChild(this.authorButton);
        this.actionButtons.forEach((v1) -> {
            addChild(v1);
        });
    }

    @Override
    public float contentHeight() {
        return 0.0f;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        colorStack115.push();
        colorStack115.alpha(this.visibilityAnimator.smoothAnimation());
        float fSmoothAnimation = 0.0f;
        if (this.selected) {
            fSmoothAnimation = 1.0f;
        } else if (!this.selectedAnimator.isZero()) {
            fSmoothAnimation = this.selectedAnimator.smoothAnimation();
        } else if (!this.hoverAnimator.isZero()) {
            fSmoothAnimation = this.hoverAnimator.smoothAnimation();
        }
        if (fSmoothAnimation > 0.0f) {
            colorStack115.push();
            colorStack115.alpha(fSmoothAnimation);
            drawCtx.fillOutlinedRoundedRect(x(), y(), width(), height() + contentHeight(), 10.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(800).argb()));
            colorStack115.pop();
        }
        drawCtx.fillOutlinedRoundedRect(this.iconBounds.x(), this.iconBounds.y(), this.iconBounds.width(), this.iconBounds.height(), 10.0f, 2.5f, colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb(), 0), colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), this.selectedAnimator.smoothAnimation()), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()));
        drawCtx.textureVerticalC(this.themeIcon, (this.iconBounds.x() + (this.iconBounds.width() / 2.0f)) - (this.themeIcon.width() / 2.0f), this.iconBounds.y() + (this.iconBounds.height() / 2.0f), this.themeIcon.width(), this.themeIcon.height(), colorStack115.interpolate(colorStack115.computeColor(this.themeContext.kind() == ConfigOrigin.OFFICIAL ? themePalette.accent().argb() : themePalette.text().tone(600).argb()), colorStack115.computeColor(ThemePalette.golden.argb()), this.favoriteAnimator.smoothAnimation()));
        int iComputeColor = colorStack115.computeColor(themePalette.text().tone(300).argb());
        float fX = this.iconBounds.x() + this.iconBounds.width() + 8.0f;
        drawCtx.texture(this.calendarIcon, fX, this.iconBounds.y() + 2.0f, this.calendarIcon.width(), this.calendarIcon.height(), colorStack115.computeColor(themePalette.text().tone(700).argb()));
        drawCtx.text(this.boldFont, this.themeContext.date(), 10, fX + this.calendarIcon.width() + 4.0f, this.iconBounds.y() + 2.0f, colorStack115.computeColor(themePalette.text().tone(700).argb()));
        if (this.themeContext.author().name().equals(FluxVisuals.INSTANCE.userSession().username())) {
            drawCtx.circle(fX + this.calendarIcon.width() + 4.0f + drawCtx.textWidthPhysical(this.boldFont, this.themeContext.date(), 10) + 6.0f + 2.0f, this.iconBounds.y() + 2.0f + (this.boldFont.getHeight(10.0f) / 2.0f), 1.0f, colorStack115.computeColor(themePalette.text().tone(700).argb()));
            this.authorButton.setColor(themePalette.text().tone(400).argb());
            this.authorButton.render(drawCtx);
            this.authorButton.setPosition(fX + this.calendarIcon.width() + 4.0f + drawCtx.textWidthPhysical(this.boldFont, this.themeContext.date(), 10) + 6.0f + 2.0f + 6.0f, this.iconBounds.y() + 2.0f);
            drawCtx.texture(this.smileIcon, fX + this.calendarIcon.width() + 4.0f + drawCtx.textWidthPhysical(this.boldFont, this.themeContext.date(), 10) + 6.0f + 2.0f + 6.0f, this.iconBounds.y() + 2.0f, this.smileIcon.width(), this.smileIcon.height(), colorStack115.computeColor(themePalette.text().tone(400).argb()));
        }
        float fX2 = ((x() + width()) - 10.0f) - drawCtx.textWidthPhysical(this.semiboldFont, this.themeContext.author().name(), 12);
        drawCtx.text(this.semiboldFont, this.themeContext.author().name(), 12, fX2, (this.iconBounds.y() + (this.iconBounds.height() / 2.0f)) - (this.semiboldFont.getHeight(12.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(300).argb()));
        drawCtx.roundedTexture(this.themeContext.author().avatar(), (fX2 - 6.0f) - 20.0f, (this.iconBounds.y() + (this.iconBounds.height() / 2.0f)) - 10.0f, 20.0f, 20.0f, 6.0f, colorStack115.white());
        float f = (((fX2 - 6.0f) - 20.0f) - 20.0f) - fX;
        if (drawCtx.textWidthPhysical(this.boldFont, this.themeContext.name(), 13) > f) {
            int i = iComputeColor & 16777215;
            String str = "";
            for (int length = this.themeContext.name().length(); length > 0; length--) {
                String strSubstring = this.themeContext.name().substring(0, length);
                if (drawCtx.textWidthPhysical(this.boldFont, strSubstring, 13) <= f) {
                    str = strSubstring;
                    break;
                }
            }
            drawCtx.textWithHorizontalGradient(this.boldFont, str, 13, fX, this.iconBounds.y() + 2.0f + this.boldFont.getHeight(10.0f) + 4.0f, iComputeColor, i);
        } else {
            drawCtx.text(this.boldFont, this.themeContext.name(), 13, fX, this.iconBounds.y() + 2.0f + this.boldFont.getHeight(10.0f) + 4.0f, iComputeColor);
        }
        boolean z = this.themeContext.type() == ThemeMode.LIGHT;
        colorStack115.push();
        colorStack115.alpha(this.hoverAnimator.state() ? 0.0f : 1.0f - this.hoverAnimator.smoothAnimation());
        this.modeBadge.setBaseColor(Integer.valueOf(z ? colorStack115.computeColor(14343394, 0.1f) : colorStack115.computeColor(6316390, 0.1f)));
        this.modeBadge.setTextColor(Integer.valueOf(z ? colorStack115.computeColor(themePalette.text().tone(200).argb()) : colorStack115.computeColor(themePalette.text().tone(500).argb())));
        this.modeBadge.render(drawCtx);
        colorStack115.pop();
        colorStack115.push();
        colorStack115.alpha(this.selectedAnimator.smoothAnimation());
        drawCtx.fillRoundedRect(this.checkBounds.x(), this.checkBounds.y(), this.checkBounds.width(), this.checkBounds.height(), 5.0f, colorStack115.computeColor(themePalette.accent().argb(), 0.15f));
        drawCtx.textureVerticalCHorizontalC(this.checkmarkIcon, this.checkBounds.x() + (this.checkBounds.width() / 2.0f), this.checkBounds.y() + (this.checkBounds.height() / 2.0f), this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack115.computeColor(themePalette.accent().argb()));
        colorStack115.pop();
        colorStack115.push();
        colorStack115.alpha(this.hoverAnimator.smoothAnimation());
        for (int i2 = 0; i2 < this.actionButtons.size(); i2++) {
            if (!this.selected || i2 != 0) {
                this.actionButtons.get(i2).render(drawCtx);
            }
        }
        colorStack115.pop();
        float fX3 = (x() + width()) - 10.0f;
        float fY = this.modeBadge.y() + (this.modeBadge.height() / 2.0f);
        for (int size = this.paletteColors.size() - 1; size >= 0; size--) {
            drawCtx.circle(fX3, fY, 5.0f, colorStack115.computeColor(this.paletteColors.get(size).intValue()));
            fX3 -= 16.0f;
        }
        colorStack115.pop();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.checkmarkIcon.setDimensions(14, 14);
        this.themeIcon.setDimensions(14, 14);
        this.calendarIcon.setDimensions(11, 11);
        this.smileIcon.setDimensions(12, 12);
        this.iconBounds.withPosition(x() + 10.0f, y() + 10.0f);
        this.modeBadge.layout(layoutScaleContext);
        this.modeBadge.setPosition(this.iconBounds.x() + ((this.checkBounds.width() + 3.0f) * this.selectedAnimator.animation()), ((y() + height()) - 10.0f) - this.modeBadge.height());
        this.checkBounds.withPosition(this.iconBounds.x(), ((y() + height()) - 10.0f) - this.checkBounds.height());
        super.layout(layoutScaleContext);
        float fX = this.modeBadge.x() + (5.0f * this.selectedAnimator.animation());
        float fY = (this.modeBadge.y() + (this.modeBadge.height() / 2.0f)) - 7.0f;
        for (int i = 0; i < this.actionButtons.size(); i++) {
            IconButtonWidget iconButtonWidget = this.actionButtons.get(i);
            if (!this.selected || i != 0) {
                iconButtonWidget.setPosition(fX, fY);
                fX += iconButtonWidget.width() + 10.0f;
            }
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.positionInitialized) {
            this.xAnimation.animate(weightedEngine);
            this.yAnimation.animate(weightedEngine);
            setPosition(this.xAnimation.animatedValue(), this.yAnimation.animatedValue());
        }
        this.selectedAnimator.state(this.selected);
        this.favoriteAnimator.state(this.favorite);
        this.selectedAnimator.animate(weightedEngine);
        this.favoriteAnimator.animate(weightedEngine);
        this.hoverAnimator.animate(weightedEngine);
        this.visibilityAnimator.animate(weightedEngine);
        if (this.pendingHide && this.visibilityAnimator.isZero()) {
            this.pendingHide = false;
            if (visible()) {
                visible(false);
                FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
            }
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = z;
        if (inputEventContext.inputEvent() instanceof CursorMoveInput) {
            this.hoverAnimator.state(inputEventContext.inArea(x(), y() + 3.0f, width(), height() - (3.0f * 2.0f)) && !z);
        }
        boolean z3 = z2;
        for (int i = 0; i < this.actionButtons.size(); i++) {
            IconButtonWidget iconButtonWidget = this.actionButtons.get(i);
            if (this.selected && i == 0 && inputEventContext.inArea(iconButtonWidget.x(), iconButtonWidget.y(), iconButtonWidget.width(), iconButtonWidget.height())) {
                z3 = true;
            }
            if (iconButtonWidget.handleInput(inputEventContext, z3)) {
                z2 = true;
                z3 = true;
            }
        }
        if (this.authorButton.handleInput(inputEventContext, z2)) {
            z2 = true;
        }
        return z2;
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.menuDragging = z;
        if (z && this.positionInitialized) {
            this.xAnimation.set(x());
            this.yAnimation.set(y());
            this.xAnimation.destination(x());
            this.yAnimation.destination(y());
        }
    }

    @Override
    public float width() {
        return 281.0f;
    }

    @Override
    public float height() {
        return 85.0f;
    }

    public void updateFilterVisibility(boolean z) {
        if (!z) {
            if (visible()) {
                this.visibilityAnimator.state(false);
                this.pendingHide = true;
                return;
            }
            return;
        }
        this.pendingHide = false;
        if (!visible()) {
            visible(true);
            FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
        }
        this.visibilityAnimator.state(true);
    }

    public void targetPosition(float f, float f2, boolean z) {
        if (!this.positionInitialized || !z) {
            this.positionInitialized = true;
            snapTo(f, f2);
        } else if (this.menuDragging) {
            snapTo(f, f2);
        } else {
            this.xAnimation.destination(f);
            this.yAnimation.destination(f2);
        }
    }

    public void snapTo(float f, float f2) {
        setPosition(f, f2);
        this.xAnimation.set(f);
        this.yAnimation.set(f2);
    }

    public void invertFavorite() {
        this.favorite = !this.favorite;
        FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
    }

    public void selectExclusive() {
        MenuTabElement menuTabElement = FluxVisuals.INSTANCE.tabsController().current();
        for (AbstractFrame abstractFrame : menuTabElement.frames()) {
            if (abstractFrame instanceof ThemeCardFrame) {
                ((ThemeCardFrame) abstractFrame).selected(false);
            }
        }
        selected(true);
        menuTabElement.markFramesDirty();
    }

    public List<Integer> extractPaletteColors(ThemePalette themePalette) {
        return List.of(Integer.valueOf(themePalette.accent().argb()), Integer.valueOf(themePalette.accentBright().argb()), Integer.valueOf(themePalette.favorite().argb()), Integer.valueOf(themePalette.surfaceBackground().tone(400).argb()), Integer.valueOf(themePalette.surfaceBackground().tone(700).argb()), Integer.valueOf(themePalette.text().tone(200).argb()), Integer.valueOf(themePalette.text().tone(500).argb()));
    }

    public ThemeCard themeContext() {
        return this.themeContext;
    }

    public ThemeCardFrame selected(boolean z) {
        this.selected = z;
        return this;
    }

    public boolean selected() {
        return this.selected;
    }

    public boolean favorite() {
        return this.favorite;
    }
}
