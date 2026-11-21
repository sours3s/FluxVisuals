package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.HighlightAnimation;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class DualToggleElement extends ModuleFrame {
    static final int LABEL_SIZE = 14;
    static final int BUTTON_TEXT_SIZE = 9;
    static final float TOGGLE_HEIGHT = 22.0f;
    public final Translation labelText;
    public final Translation leftText;
    public final Translation rightText;
    public Consumer<Boolean> onToggleCallback;
    public Supplier<Boolean> visibleSupplier;
    public final MsdfFont labelFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont buttonFont = Fonts.INTER_EXTRA_BOLD.get();
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final ToggleAnimator toggleAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public boolean leftActive = false;

    public DualToggleElement(Translation translation, Translation class254Var2, Translation class254Var3) {
        this.labelText = translation;
        this.leftText = class254Var2;
        this.rightText = class254Var3;
        this.clickBehavior.clickCallback(this::toggle);
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
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        float fX = (x() + f) - 110.0f;
        float fHeight = height() / 2.0f;
        float fY = y();
        drawCtx.text(this.labelFont, this.labelText.effective(), LABEL_SIZE, x(), (y() + fHeight) - (this.labelFont.getHeight(14.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(200).argb()));
        float f3 = 110.0f / 2.0f;
        drawCtx.fillRoundedRect(fX + (f3 * (1.0f - this.toggleAnimation.smoothAnimation())), fY, f3, TOGGLE_HEIGHT, 5.0f, colorStack115.computeColor(themePalette.accent().argb()));
        drawCtx.fillOutlinedRoundedRect(fX, fY, 110.0f, TOGGLE_HEIGHT, 6.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb(), 0));
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.buttonFont, this.leftText.effective(), BUTTON_TEXT_SIZE);
        float fTextWidthPhysical2 = drawCtx.textWidthPhysical(this.buttonFont, this.rightText.effective(), BUTTON_TEXT_SIZE);
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.white(), this.toggleAnimation.smoothAnimation());
        int iInterpolate2 = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.white(), 1.0f - this.toggleAnimation.smoothAnimation());
        drawCtx.text(this.buttonFont, this.leftText.effective(), BUTTON_TEXT_SIZE, (fX + (f3 / 2.0f)) - (fTextWidthPhysical / 2.0f), (fY + 11.0f) - (this.buttonFont.getHeight(9.0f) / 2.0f), iInterpolate);
        drawCtx.text(this.buttonFont, this.rightText.effective(), BUTTON_TEXT_SIZE, ((fX + f3) + (f3 / 2.0f)) - (fTextWidthPhysical2 / 2.0f), (fY + 11.0f) - (this.buttonFont.getHeight(9.0f) / 2.0f), iInterpolate2);
        this.clickBehavior.setDimensions(fX, fY, 110.0f, TOGGLE_HEIGHT);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickBehavior.animate(weightedEngine);
        this.toggleAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public float height() {
        return Math.max(this.labelFont.getHeight(14.0f), TOGGLE_HEIGHT);
    }

    public void toggle() {
        this.leftActive = !this.leftActive;
        this.toggleAnimation.state(this.leftActive);
        if (this.onToggleCallback != null) {
            this.onToggleCallback.accept(Boolean.valueOf(this.leftActive));
        }
    }

    public void setState(boolean z) {
        this.leftActive = z;
        this.toggleAnimation.state(z);
    }

    public boolean isLeftActive() {
        return this.leftActive;
    }

    public void setOnToggleCallback(Consumer<Boolean> consumer) {
        this.onToggleCallback = consumer;
    }

    public void setVisibleSupplier(Supplier<Boolean> supplier) {
        this.visibleSupplier = supplier;
    }
}
