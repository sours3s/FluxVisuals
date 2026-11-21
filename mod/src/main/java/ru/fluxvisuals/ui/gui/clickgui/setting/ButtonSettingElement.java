package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.events.api.EventCallback;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.render.HighlightAnimation;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.LanguageChangeEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.function.Supplier;

public class ButtonSettingElement extends ModuleFrame {
    public static final int nameTextSize = 14;
    public static final int descriptionTextSize = 13;
    public static final int buttonTextSize = 12;
    public static final float descriptionGap = 3.0f;
    public static final float horizontalPadding = 10.0f;
    public static final float buttonHeight = 25.0f;
    public static final float buttonWidth = 101.0f;
    public final Translation name;
    public final Translation description;
    public final Translation buttonLabel;
    public Supplier<Boolean> visibilitySupplier;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final GlTexture clickIcon = new GlTexture(new ClasspathResource("/icons/menu/new/click.png"));
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final WidgetBounds buttonBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public float scaleFactor = 1.0f;
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float cachedWidth = -1.0f;
    public String cachedDescriptionText = null;
    public float descriptionHeight = 0.0f;
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescriptionText = null;
    };

    public ButtonSettingElement(Translation translation, Translation class254Var2, Translation class254Var3, Runnable runnable) {
        this.name = translation;
        this.description = class254Var2;
        this.buttonLabel = class254Var3;
        this.clickBehavior.clickCallback(runnable);
    }

    @Override
    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public void clearHighlight() {
        this.highlightAnimation.clearHighlight();
    }

    @Override
    public void draw(DrawCtx drawCtx, float f, float f2) {
        updateWrappedDescription(f);
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        float fY = y();
        float fValue = this.highlightAnimation.value();
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), fValue);
        int iComputeColor = colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb());
        int iComputeColor2 = colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb());
        int iComputeColor3 = colorStack115.computeColor(themePalette.text().tone(400).argb());
        int iComputeColor4 = colorStack115.computeColor(themePalette.text().tone(500).argb());
        int iInterpolate2 = colorStack115.interpolate(iComputeColor, colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.clickBehavior.hoverAnimation());
        int iInterpolate3 = colorStack115.interpolate(iComputeColor2, colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.clickBehavior.hoverAnimation());
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.titleFont, this.name.effective(), nameTextSize);
        float f3 = (f - buttonWidth) - horizontalPadding;
        float fX = x();
        float height = this.titleFont.getHeight(12.0f);
        float fTextWidthPhysical2 = drawCtx.textWidthPhysical(this.titleFont, this.buttonLabel.effective(), buttonTextSize);
        if (this.wrappedDescription != null || fTextWidthPhysical >= f3) {
            drawCtx.text(this.titleFont, this.name.effective(), nameTextSize, x(), fY, iInterpolate);
            if (this.wrappedDescription != null) {
                fY += this.titleFont.getHeight(14.0f) + descriptionGap;
                drawCtx.text(this.descriptionFont, this.wrappedDescription, descriptionTextSize, x(), fY, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), fValue));
            }
            this.buttonBounds.withPosition(x(), fY + (this.description != null ? this.descriptionHeight : this.titleFont.getHeight(14.0f)) + horizontalPadding).withSize(f, buttonHeight);
            float fY2 = this.buttonBounds.y() + (this.buttonBounds.height() / 2.0f);
            drawCtx.fillOutlinedRoundedRect(this.buttonBounds.x(), this.buttonBounds.y(), this.buttonBounds.width(), this.buttonBounds.height(), 6.0f, 2.5f, iInterpolate3, iInterpolate2);
            drawCtx.textureVerticalC(this.clickIcon, fX + horizontalPadding, fY2, this.clickIcon.width(), this.clickIcon.height(), iComputeColor4);
            drawCtx.text(this.titleFont, this.buttonLabel.effective(), buttonTextSize, (this.buttonBounds.x() + (this.buttonBounds.width() / 2.0f)) - (fTextWidthPhysical2 / 2.0f), fY2 - (height / 2.0f), iComputeColor3);
        } else {
            this.buttonBounds.withPosition((x() + f) - buttonWidth, y()).withSize(buttonWidth, buttonHeight);
            float fY3 = this.buttonBounds.y() + (this.buttonBounds.height() / 2.0f);
            drawCtx.text(this.titleFont, this.name.effective(), nameTextSize, x(), fY3 - (this.titleFont.getHeight(14.0f) / 2.0f), iInterpolate);
            drawCtx.fillOutlinedRoundedRect(this.buttonBounds.x(), this.buttonBounds.y(), this.buttonBounds.width(), this.buttonBounds.height(), 6.0f, 2.5f, iInterpolate3, iInterpolate2);
            float f4 = fX + horizontalPadding;
            drawCtx.textureVerticalC(this.clickIcon, f4, fY3, this.clickIcon.width(), this.clickIcon.height(), iComputeColor4);
            drawCtx.text(this.titleFont, this.buttonLabel.effective(), buttonTextSize, f4 + this.clickIcon.width() + 6.0f, fY3 - (height / 2.0f), iComputeColor3);
        }
        this.clickBehavior.setDimensions(x(), y(), f, height());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.clickIcon.setDimensions(buttonTextSize, buttonTextSize);
        this.scaleFactor = layoutScaleContext.scaleFactor();
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public void handleClose() {
        super.handleClose();
    }

    @Override
    public float height() {
        float fMax;
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateWrappedDescription(this.parent.width() - 18.0f);
        }
        float fMethod002 = measureTextWidth(this.name.effective(), nameTextSize);
        float fWidth = (this.parent.width() - buttonWidth) - horizontalPadding;
        if ((this.description == null || this.wrappedDescription == null) && fMethod002 < fWidth) {
            fMax = Math.max(this.titleFont.getHeight(14.0f), buttonHeight);
        } else {
            float height = 0.0f + this.titleFont.getHeight(14.0f);
            if (this.description != null && this.wrappedDescription != null) {
                height += descriptionGap + this.descriptionHeight;
            }
            fMax = height + 35.0f;
        }
        return fMax;
    }

    public float measureTextWidth(String str, int i) {
        if (this.scaleFactor <= 0.0f || this.scaleFactor == 1.0f) {
            return this.titleFont.getWidth(str, i);
        }
        return this.titleFont.getWidth(str, Math.max(1, Math.round(i * this.scaleFactor))) / this.scaleFactor;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        this.highlightAnimation.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void updateWrappedDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.cachedWidth = f;
            this.cachedDescriptionText = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.cachedWidth || strEffective == null || !strEffective.equals(this.cachedDescriptionText)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descriptionFont, descriptionTextSize);
            this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionTextSize);
            this.cachedWidth = f;
            this.cachedDescriptionText = strEffective;
            this.descriptionDirty = false;
        }
    }

    public Translation name() {
        return this.name;
    }

    public ButtonSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }
}
