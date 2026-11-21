package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.ColorPickerWindow;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorSwatchButton;
import ru.fluxvisuals.ui.gui.clickgui.util.ColorUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorValue;
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
import ru.fluxvisuals.ui.gui.clickgui.util.StencilBufferUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorSettingElement extends ModuleFrame {
    static final int nameTextSize = 14;
    static final int descriptionTextSize = 13;
    static final int hexTextSize = 10;
    static final float descriptionGap = 3.0f;
    static final float horizontalPadding = 10.0f;
    static final float barHeight = 25.0f;
    static final float verticalPadding = 10.0f;
    static final float swatchRadius = 15.0f;
    static final float swatchHoverScale = 1.5f;

    static final long copiedDurationMs = 1000;
    public final List<ColorSwatchButton> swatches;
    public final Translation name;
    public final Translation description;
    public String wrappedDescription;
    public String cachedDescriptionText;
    public float descriptionHeight;
    public Supplier<Boolean> visibilitySupplier;

    public Consumer<Integer> colorCallback;
    public Consumer<Float> alphaCallback;
    public final Supplier<Integer> colorSupplier;
    public final Supplier<Float> alphaSupplier;
    public Runnable commitAction;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final MsdfFont hexFont = Fonts.INTER_EXTRA_BOLD.get();
    public final GlTexture brushIcon = new GlTexture(new ClasspathResource("/icons/menu/new/brush.png"));
    public final GlTexture checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
    public final GlTexture previewTexture = new GlTexture(new ClasspathResource("/icons/menu/new/container.png"));
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        markDescriptionDirty();
    };
    public final ClickableBehavior barClickable = new ClickableBehavior();
    public final ClickableBehavior hexClickable = new ClickableBehavior();
    public final ToggleAnimator copiedAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator hexAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator copyAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
    public boolean copied = false;

    public long copiedTime = 0;
    public float hue = 0.0f;
    public float saturation = 1.0f;
    public float brightness = 1.0f;
    public float alpha = 1.0f;
    public ColorSwatchButton selectedSwatch = null;
    public float cachedWidth = -1.0f;
    public boolean descriptionDirty = true;
    public boolean pickerOpen = false;

    public ColorPickerWindow pickerWindow = null;

    public ColorSettingElement(Translation translation, Translation class254Var2, Supplier<Integer> supplier, Supplier<Float> supplier2) {
        this.name = translation;
        this.description = class254Var2;
        this.colorSupplier = supplier;
        this.alphaSupplier = supplier2;
        initFromSuppliers(supplier, supplier2);
        this.swatches = createSwatches();
        bindSwatchCallbacks();
        bindCopyCallback();
        this.barClickable.clickCallback(() -> {
            ColorPickerWindow colorPickerWindow = (ColorPickerWindow) FluxVisuals.INSTANCE.windowController().getWindow(ColorPickerWindow.class);
            if (colorPickerWindow == null) {
                ColorPickerWindow class773Var2 = new ColorPickerWindow(this.hue, this.saturation, this.brightness, this.alpha);
                FluxVisuals.INSTANCE.windowController().newWindow(class773Var2);
                class773Var2.colorConsumer(num -> {
                    if (this.colorCallback != null) {
                        this.colorCallback.accept(num);
                    }
                    float[] fArrRGBtoHSB = Color.RGBtoHSB(ColorUtil.red(num.intValue()), ColorUtil.green(num.intValue()), ColorUtil.blue(num.intValue()), (float[]) null);
                    this.hue = fArrRGBtoHSB[0];
                    this.saturation = fArrRGBtoHSB[1];
                    this.brightness = fArrRGBtoHSB[2];
                });
                class773Var2.applyCallback(() -> {
                    if (this.commitAction != null) {
                        this.commitAction.run();
                    }
                });
                class773Var2.alphaConsumer(f -> {
                    this.alpha = f.floatValue();
                    if (this.alphaCallback != null) {
                        this.alphaCallback.accept(Float.valueOf(this.alpha));
                    }
                });
                this.pickerWindow = class773Var2;
                this.pickerOpen = true;
                class773Var2.openPicker();
                return;
            }
            colorPickerWindow.updateColor(this.hue, this.saturation, this.brightness, this.alpha);
            colorPickerWindow.colorConsumer(num2 -> {
                if (this.colorCallback != null) {
                    this.colorCallback.accept(num2);
                }
                float[] fArrRGBtoHSB = Color.RGBtoHSB(ColorUtil.red(num2.intValue()), ColorUtil.green(num2.intValue()), ColorUtil.blue(num2.intValue()), (float[]) null);
                this.hue = fArrRGBtoHSB[0];
                this.saturation = fArrRGBtoHSB[1];
                this.brightness = fArrRGBtoHSB[2];
            });
            colorPickerWindow.applyCallback(() -> {
                if (this.commitAction != null) {
                    this.commitAction.run();
                }
            });
            colorPickerWindow.alphaConsumer(f2 -> {
                this.alpha = f2.floatValue();
                if (this.alphaCallback != null) {
                    this.alphaCallback.accept(Float.valueOf(this.alpha));
                }
            });
            if (colorPickerWindow.opened()) {
                colorPickerWindow.closePicker();
            } else {
                colorPickerWindow.openPicker();
            }
            this.pickerWindow = colorPickerWindow;
            this.pickerOpen = true;
        });
    }

    public void initFromSuppliers(Supplier<Integer> supplier, Supplier<Float> supplier2) {
        int iIntValue = supplier.get().intValue();
        float[] fArrRGBtoHSB = Color.RGBtoHSB(ColorUtil.red(iIntValue), ColorUtil.green(iIntValue), ColorUtil.blue(iIntValue), (float[]) null);
        this.hue = fArrRGBtoHSB[0];
        this.saturation = fArrRGBtoHSB[1];
        this.brightness = fArrRGBtoHSB[2];
        this.alpha = supplier2.get().floatValue();
    }

    public void syncFromSupplier() {
        if (this.pickerOpen) {
            return;
        }
        int iIntValue = this.colorSupplier.get().intValue();
        float fFloatValue = this.alphaSupplier.get().floatValue();
        int iHSBtoRGB = Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215;
        int i = iIntValue & 16777215;
        int iMethod001 = (int) (clamp01(this.alpha) * 255.0f);
        int iMethod002 = (int) (clamp01(fFloatValue) * 255.0f);
        if (iHSBtoRGB != i || Math.abs(iMethod001 - iMethod002) > 1) {
            float[] fArrRGBtoHSB = Color.RGBtoHSB(ColorUtil.red(iIntValue), ColorUtil.green(iIntValue), ColorUtil.blue(iIntValue), (float[]) null);
            this.hue = fArrRGBtoHSB[0];
            this.saturation = fArrRGBtoHSB[1];
            this.brightness = fArrRGBtoHSB[2];
            this.alpha = fFloatValue;
            updateSelectedSwatch();
        }
    }

    public List<ColorSwatchButton> createSwatches() {
        return List.of(new ColorSwatchButton(ThemePalette.mistyBlue.argb()), new ColorSwatchButton(ThemePalette.golden.argb()), new ColorSwatchButton(ThemePalette.violet.argb()), new ColorSwatchButton(ColorValue.fromHex("C5C6C8").argb()), new ColorSwatchButton(ThemePalette.red.argb()), new ColorSwatchButton(ThemePalette.mint.argb()), new ColorSwatchButton(FluxVisuals.INSTANCE.theme().palette().accent().argb()));
    }

    public void bindSwatchCallbacks() {
        this.swatches.forEach(colorSwatchButton -> {
            colorSwatchButton.clickable.clickCallback(() -> {
                selectSwatch(colorSwatchButton);
            });
        });
    }

    public void selectSwatch(ColorSwatchButton colorSwatchButton) {
        this.selectedSwatch = colorSwatchButton;
        if (this.colorCallback != null) {
            this.colorCallback.accept(Integer.valueOf(colorSwatchButton.color()));
        }
        applySwatchColor(colorSwatchButton);
        ColorPickerWindow colorPickerWindow = (ColorPickerWindow) FluxVisuals.INSTANCE.windowController().getWindow(ColorPickerWindow.class);
        if (colorPickerWindow != null) {
            colorPickerWindow.updateColor(this.hue, this.saturation, this.brightness, this.alpha);
        }
        if (this.commitAction != null) {
            this.commitAction.run();
        }
    }

    public void applySwatchColor(ColorSwatchButton colorSwatchButton) {
        float[] fArrRGBtoHSB = Color.RGBtoHSB(ColorUtil.red(colorSwatchButton.color()), ColorUtil.green(colorSwatchButton.color()), ColorUtil.blue(colorSwatchButton.color()), (float[]) null);
        this.hue = fArrRGBtoHSB[0];
        this.saturation = fArrRGBtoHSB[1];
        this.brightness = fArrRGBtoHSB[2];
        int iAlpha = ColorUtil.alpha(colorSwatchButton.color());
        this.alpha = (iAlpha == 0 && (colorSwatchButton.color() >>> 24) == 0) ? 1.0f : iAlpha / 255.0f;
        if (this.alphaCallback != null) {
            this.alphaCallback.accept(Float.valueOf(this.alpha));
        }
    }

    public void bindCopyCallback() {
        this.hexClickable.clickCallback(() -> {
            StringUtil.copyToClipboard(String.format("#%06X", Integer.valueOf(Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215)));
            this.copied = true;
            this.copiedTime = System.currentTimeMillis();
        });
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
        drawBar(drawCtx, f, drawDescription(drawCtx, drawName(drawCtx, y())) + 10.0f);
        this.barClickable.setDimensions(x(), y(), f, height());
    }

    public void drawBar(DrawCtx drawCtx, float f, float f2) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        drawCtx.fillOutlinedRoundedRect(x(), f2, f, barHeight, 6.0f, 2.5f, colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.barClickable.hoverAnimation()), colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.barClickable.hoverAnimation()));
        float fMethod025 = drawSwatches(drawCtx, colorStack115, drawDivider(drawCtx, colorStack115, drawPreview(drawCtx, colorStack115, f2), f2) + 10.0f + 9.0f, f2);
        drawDivider(drawCtx, colorStack115, fMethod025, f2);
        drawHexLabel(drawCtx, themePalette, colorStack115, fMethod025, f2, f);
    }

    public float drawPreview(DrawCtx drawCtx, ColorStack colorStack, float f) {
        float fX = x() + 14.0f;
        int iComputeColor = colorStack.computeColor(Color.HSBtoRGB(this.hue, this.saturation, this.brightness), this.alpha);
        drawCtx.texture(this.previewTexture, x(), f, 40.0f, barHeight, colorStack.interpolate(iComputeColor, colorStack.autoBrightenDarken(iComputeColor, 0.2f), this.barClickable.hoverAnimation().smoothAnimation()));
        return fX + this.brushIcon.width() + 12.0f;
    }

    public float drawDivider(DrawCtx drawCtx, ColorStack colorStack, float f, float f2) {
        drawCtx.fillRect(f, f2, 1.0f, barHeight, colorStack.computeColor(drawCtx.theme().palette().surfaceOutline().tone(400).argb()));
        return f;
    }

    public float drawSwatches(DrawCtx drawCtx, ColorStack colorStack, float f, float f2) {
        int i = 0;
        while (i < this.swatches.size()) {
            drawSwatch(drawCtx, colorStack, this.swatches.get(i), f, f2);
            f += i < this.swatches.size() - 1 ? 19.0f : 18.0f;
            i++;
        }
        return f;
    }

    public void drawSwatch(DrawCtx drawCtx, ColorStack colorStack, ColorSwatchButton colorSwatchButton, float f, float f2) {
        float f3 = f2 + 12.5f;
        drawCtx.circle(f, f3, (10.0f + Math.max(colorSwatchButton.clickable.hoverAnimation().smoothAnimation() * swatchHoverScale, colorSwatchButton.selectAnim.smoothAnimation() * 2.0f)) / 2.0f, colorStack.computeColor(colorSwatchButton.color()));
        colorSwatchButton.clickable.setDimensions(f - (19.0f / 2.0f), f2, 19.0f, barHeight);
        if (colorSwatchButton == this.selectedSwatch) {
            drawCtx.textureVerticalC(this.checkmarkIcon, f - 5.0f, f3, this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack.computeColor(drawCtx.theme().palette().surfaceBackground().tone(900).argb(), colorSwatchButton.selectAnim.smoothAnimation()));
        }
    }

    public void drawHexLabel(DrawCtx drawCtx, ThemePalette themePalette, ColorStack colorStack, float f, float f2, float f3) {
        float fX = (x() + f3) - f;
        this.hexClickable.setDimensions(f, f2, fX, barHeight);
        String str = String.format("#%06X", Integer.valueOf(Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215));
        float height = (f2 + 12.5f) - (this.hexFont.getHeight(10.0f) / 2.0f);
        float f4 = f + (fX / 2.0f);
        int iComputeColor = colorStack.computeColor(themePalette.text().tone(300).argb());
        if (this.copied) {
            drawCenteredText(drawCtx, "Copied", f4, height, colorStack.computeColor(iComputeColor, this.copiedAnimator.smoothAnimation()));
        } else if (this.hexClickable.hoverAnimation().smoothAnimation() <= 0.01f) {
            drawCenteredText(drawCtx, str, f4, height, colorStack.computeColor(iComputeColor, this.hexAnimator.smoothAnimation()));
        } else {
            drawCenteredText(drawCtx, str, f4, height, colorStack.computeColor(iComputeColor, this.hexAnimator.smoothAnimation()));
            drawCenteredText(drawCtx, "Copy", f4, height, colorStack.computeColor(iComputeColor, this.copyAnimator.smoothAnimation()));
        }
    }

    public void drawCenteredText(DrawCtx drawCtx, String str, float f, float f2, int i) {
        drawCtx.text(this.hexFont, str, hexTextSize, (f - (drawCtx.textWidthPhysical(this.hexFont, str, hexTextSize) / 2.0f)) - 2.0f, f2, i);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.brushIcon.setDimensions(12, 12);
        this.checkmarkIcon.setDimensions(hexTextSize, hexTextSize);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = this.hexClickable.handleInput(inputEventContext, z);
        Iterator<ColorSwatchButton> it = this.swatches.iterator();
        while (it.hasNext()) {
            zHandleInput |= it.next().clickable.handleInput(inputEventContext, zHandleInput);
        }
        boolean zHandleInput2 = zHandleInput | this.barClickable.handleInput(inputEventContext, zHandleInput);
        return zHandleInput2 | super.handleInput(inputEventContext, zHandleInput2);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        if (this.pickerWindow != null && !this.pickerWindow.opened()) {
            this.pickerWindow = null;
            this.pickerOpen = false;
        }
        syncFromSupplier();
        updateSelectedSwatch();
        this.barClickable.animate(weightedEngine);
        updateCopiedState();
        animateSwatches(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void updateCopiedState() {
        if (!this.copied || System.currentTimeMillis() - this.copiedTime <= copiedDurationMs) {
            return;
        }
        this.copied = false;
    }

    public void animateSwatches(WeightedEngine weightedEngine) {
        boolean z = this.hexClickable.hoverAnimation().smoothAnimation() > 0.01f;
        this.hexAnimator.state((z || this.copied) ? false : true);
        this.copyAnimator.state(z && !this.copied);
        this.copiedAnimator.state(this.copied);
        this.hexClickable.animate(weightedEngine);
        this.hexAnimator.animate(weightedEngine);
        this.copyAnimator.animate(weightedEngine);
        this.copiedAnimator.animate(weightedEngine);
        this.swatches.forEach(colorSwatchButton -> {
            colorSwatchButton.selectAnim.state(this.selectedSwatch == colorSwatchButton);
            colorSwatchButton.clickable.animate(weightedEngine);
            colorSwatchButton.selectAnim.animate(weightedEngine);
        });
    }

    @Override
    public void handleClose() {
        super.handleClose();
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.selectedSwatch != null) {
            updateWrappedDescription(width());
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += descriptionGap + this.descriptionHeight;
        }
        return height + 10.0f + barHeight;
    }

    public float drawName(DrawCtx drawCtx, float f) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        drawCtx.text(this.titleFont, this.name.effective(), nameTextSize, x(), f, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), this.highlightAnimation.value()));
        return f + this.titleFont.getHeight(14.0f);
    }

    public float drawDescription(DrawCtx drawCtx, float f) {
        if (this.wrappedDescription == null) {
            return f;
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), this.highlightAnimation.value());
        float f2 = f + descriptionGap;
        drawCtx.text(this.descriptionFont, this.wrappedDescription, descriptionTextSize, x(), f2, iInterpolate);
        return f2 + this.descriptionHeight;
    }

    public void markDescriptionDirty() {
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescriptionText = null;
    }

    public void updateWrappedDescription(float f) {
        if (this.description == null) {
            clearWrappedDescription(f);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionCacheValid(f, strEffective)) {
            return;
        }
        computeWrappedDescription(f, strEffective == null ? "" : strEffective);
    }

    public void updateSelectedSwatch() {
        int iMethod033 = currentColor();
        ColorSwatchButton colorSwatchButton = null;
        for (ColorSwatchButton class824Var2 : this.swatches) {
            if (class824Var2.color() == iMethod033) {
                colorSwatchButton = class824Var2;
                break;
            }
        }
        if (this.selectedSwatch != colorSwatchButton) {
            this.selectedSwatch = colorSwatchButton;
            Iterator<ColorSwatchButton> it = this.swatches.iterator();
            while (it.hasNext()) {
                ColorSwatchButton next = it.next();
                next.selectAnim.state(next == this.selectedSwatch);
            }
        }
    }

    public void clearWrappedDescription(float f) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.cachedWidth = f;
        this.cachedDescriptionText = null;
    }

    public boolean isDescriptionCacheValid(float f, String str) {
        return !this.descriptionDirty && f == this.cachedWidth && str != null && str.equals(this.cachedDescriptionText);
    }

    public int currentColor() {
        return ((((int) (clamp01(this.alpha) * 255.0f)) & StencilBufferUtil.STENCIL_MASK) << 24) | (Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215);
    }

    public static float clamp01(float f) {
        if (f < 0.0f) {
            return 0.0f;
        }
        return Math.min(f, 1.0f);
    }

    public void computeWrappedDescription(float f, String str) {
        this.wrappedDescription = StringUtil.formatTextToFitWidth(str, f, this.descriptionFont, descriptionTextSize);
        this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionTextSize);
        this.cachedWidth = f;
        this.cachedDescriptionText = str;
        this.descriptionDirty = false;
    }

    public Translation name() {
        return this.name;
    }

    public ColorSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }

    public ColorSettingElement colorConsumer(Consumer<Integer> consumer) {
        this.colorCallback = consumer;
        return this;
    }

    public ColorSettingElement alphaConsumer(Consumer<Float> consumer) {
        this.alphaCallback = consumer;
        return this;
    }

    public ColorSettingElement commitRunnable(Runnable runnable) {
        this.commitAction = runnable;
        return this;
    }
}
