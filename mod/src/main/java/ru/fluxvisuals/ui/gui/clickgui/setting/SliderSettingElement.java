package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.events.api.EventCallback;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.HighlightAnimation;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.LanguageChangeEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MouseButtonAction;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.type.SettingUnit;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.joml.Vector4f;

public class SliderSettingElement extends ModuleFrame {
    public static final float p = 3.0f;
    public static final float q = 12.0f;
    public static final int titleFontSize = 14;
    public static final int descFontSize = 13;
    public static final float r = 3.0f;
    public static final float s = 7.0f;
    public static final float t = 1.5f;
    public static final float u = 2.0f;
    public final Translation name;
    public final Translation description;
    public final SettingUnit unit;
    public final float v;
    public final float z;
    public final float A;
    public float currentValue;
    public Supplier<Float> valueSupplier;

    public Supplier<Boolean> visibleCondition;
    public Consumer<Float> changeConsumer;
    public float B;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descFont = Fonts.INTER_MEDIUM.get();
    public final AnimatedFloat valueAnimation = new AnimatedFloat(140, Easings.LINEAR);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public String wrappedDescription = null;
    public boolean dragging = false;
    public final WidgetBounds trackBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds interactionBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final ToggleAnimator hoverAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public boolean descriptionDirty = true;
    public float lastLayoutWidth = -1.0f;
    public String lastDescription = null;
    public float descriptionHeight = 0.0f;
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        this.descriptionDirty = true;
        this.lastLayoutWidth = -1.0f;
        this.lastDescription = null;
    };

    public SliderSettingElement(Translation translation, Translation class254Var2, SettingUnit settingUnit, float f, float f2, float f3) {
        this.name = translation;
        this.description = class254Var2;
        this.unit = settingUnit;
        this.v = f;
        this.z = f2;
        this.A = f3;
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
        updateDescription(f);
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        float fY = y();
        String str = (this.currentValue % 1.0f == 0.0f ? String.valueOf((int) this.currentValue) : String.format("%.2f", Float.valueOf(this.currentValue))) + " " + this.unit.format(this.currentValue);
        String strEffective = this.name.effective();
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.titleFont, str, 12);
        float fTextWidthPhysical2 = drawCtx.textWidthPhysical(this.titleFont, strEffective, titleFontSize);
        float fX = (x() + f) - fTextWidthPhysical;
        float f3 = fX - 10.0f;
        boolean z = x() + fTextWidthPhysical2 > f3;
        int iComputeColor = colorStack115.computeColor(themePalette.accent().argb());
        String strCutoff = StringUtil.cutoff(strEffective, x() + fTextWidthPhysical2, f3, fTextWidthPhysical2, z);
        float fValue = this.highlightAnimation.value();
        drawCtx.text(this.titleFont, strCutoff, titleFontSize, x(), fY, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), fValue));
        drawCtx.text(this.titleFont, str, 12, fX, fY + 1.0f, iComputeColor);
        float f4 = 0.0f;
        if (this.wrappedDescription != null) {
            fY += this.titleFont.getHeight(14.0f) + 3.0f;
            drawCtx.text(this.descFont, this.wrappedDescription, descFontSize, x(), fY, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), fValue));
            f4 = this.descriptionHeight;
        }
        this.trackBounds.withPosition(x(), fY + (this.description != null ? f4 : this.titleFont.getHeight(14.0f)) + q).withSize(f, 3.0f);
        float f5 = 5.0f + u;
        float fX2 = this.trackBounds.x() - f5;
        float fWidth = this.trackBounds.width() + (f5 * u);
        float fY2 = this.trackBounds.y() - f5;
        float fHeight = this.trackBounds.height() + (f5 * u);
        this.interactionBounds.withPosition(x(), y()).withSize(f, height());
        int iComputeColor2 = colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb());
        int iInterpolate = colorStack115.interpolate(iComputeColor2, colorStack115.autoBrightenDarken(iComputeColor2, 0.01f), this.hoverAnimator);
        int iDarkenedInterpolatedColor = colorStack115.darkenedInterpolatedColor(iComputeColor, 0.1f, this.hoverAnimator);
        float fClamp = MathUtil.clamp((this.valueAnimation.animatedValue() - this.z) / (this.v - this.z), 0.0f, 1.0f);
        float f6 = 3.5f + t;
        float fX3 = this.trackBounds.x() + f6;
        float fMax = Math.max(0.0f, this.trackBounds.width() - (f6 * u));
        float f7 = (fX3 - 1.0f) + (fClamp * fMax);
        float fY3 = this.trackBounds.y() + (this.trackBounds.height() / u);
        drawCtx.fillRoundedRect(this.trackBounds.x(), this.trackBounds.y(), this.trackBounds.width(), this.trackBounds.height(), new Vector4f(u, u, u, u), iInterpolate);
        drawCtx.fillRoundedRect(this.trackBounds.x(), this.trackBounds.y(), fClamp * fMax, this.trackBounds.height(), new Vector4f(u, u, u, u), iComputeColor);
        drawCtx.circle(f7, fY3, 3.5f + t, iDarkenedInterpolatedColor);
        drawCtx.circle(f7, fY3, 3.5f, iComputeColor2);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (super.handleInput(inputEventContext, z)) {
            return true;
        }
        if (z) {
            return false;
        }
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (!(inputEvent691 instanceof MouseButtonInput)) {
            if (!(inputEventContext.inputEvent() instanceof CursorMoveInput)) {
                return false;
            }
            if (this.dragging) {
                float fMethod001 = roundToStep(positionToValue(this.z, this.v, this.trackBounds.x(), this.trackBounds.width(), inputEventContext.logicalMousePosition().x()), this.A);
                if (fMethod001 != this.currentValue) {
                    this.currentValue = fMethod001;
                    if (this.changeConsumer != null) {
                        this.changeConsumer.accept(Float.valueOf(this.currentValue));
                    }
                }
            }
            boolean z2 = inputEventContext.inArea(this.interactionBounds.x(), this.interactionBounds.y(), this.interactionBounds.width(), this.interactionBounds.height()) || this.dragging;
            this.hoverAnimator.state(z2);
            return z2;
        }
        MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
        if (mouseButtonInput.button() != 0) {
            return false;
        }
        MouseButtonAction mouseButtonAction = mouseButtonInput.action();
        if (!mouseButtonAction.press() || !inputEventContext.inArea(this.interactionBounds.x(), this.interactionBounds.y(), this.interactionBounds.width(), this.interactionBounds.height())) {
            if (!mouseButtonAction.release() || !this.dragging) {
                return false;
            }
            this.dragging = false;
            return true;
        }
        this.dragging = true;
        float fMethod002 = roundToStep(positionToValue(this.z, this.v, this.trackBounds.x(), this.trackBounds.width(), inputEventContext.logicalMousePosition().x()), this.A);
        if (fMethod002 == this.currentValue) {
            return true;
        }
        this.currentValue = fMethod002;
        if (this.changeConsumer == null) {
            return true;
        }
        this.changeConsumer.accept(Float.valueOf(this.currentValue));
        return true;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.valueSupplier != null) {
            Float f = this.valueSupplier.get();
            if (Math.abs(f.floatValue() - this.B) > 1.0E-6f) {
                this.currentValue = f.floatValue();
                this.B = f.floatValue();
            }
        }
        if (this.visibleCondition != null) {
            visible(this.visibleCondition.get().booleanValue());
        }
        this.highlightAnimation.animate(weightedEngine);
        this.valueAnimation.destination(this.currentValue);
        this.valueAnimation.animate(weightedEngine);
        this.hoverAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        this.dragging = false;
        super.handleClose();
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescription(this.parent.width());
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += 3.0f + this.descriptionHeight;
        }
        return height + 18.0f;
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.lastLayoutWidth = f;
            this.lastDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.lastLayoutWidth || strEffective == null || !strEffective.equals(this.lastDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descFont, descFontSize);
            this.descriptionHeight = this.descFont.getHeightWithLineBreaks(this.wrappedDescription, descFontSize);
            this.lastLayoutWidth = f;
            this.lastDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    public float positionToValue(float f, float f2, float f3, float f4, double d) {
        return (Math.clamp((float) ((d - ((double) f3)) / ((double) f4)), 0.0f, 1.0f) * (f2 - f)) + f;
    }

    public float roundToStep(float f, float f2) {
        return MathUtil.clamp(Math.round(f / f2) * f2, this.z, this.v);
    }

    public Translation name() {
        return this.name;
    }

    public SliderSettingElement currentValueSupplier(Supplier<Float> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public SliderSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }

    public SliderSettingElement onValueChanged(Consumer<Float> consumer) {
        this.changeConsumer = consumer;
        return this;
    }
}
