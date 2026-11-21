package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorValue;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PixelPoint;
import ru.fluxvisuals.ui.gui.clickgui.util.StencilBufferUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.joml.Vector4f;

public class ColorPickerWindow extends AbstractWindow {
    public final GlTexture clickIcon;
    public final GlTexture plusIcon;
    public final GlTexture checkmarkIcon;
    public final MsdfFont font;
    static final int animationDuration = 350;
    static final float cornerRadius = 8.0f;
    static final float outlineWidth = 2.5f;
    static final float padding = 12.0f;
    static final float svBoxWidth = 192.0f;
    static final float svBoxHeight = 140.0f;
    static final float barWidth = 6.0f;
    static final float rgbaRowHeight = 26.0f;
    static final float separatorWidth = 1.5f;
    static final float headerHeight = 12.0f;
    static final float labelColumnWidth = 60.0f;
    static final float valueColumnWidth = 40.0f;
    static final float margin = 10.0f;
    static final float swatchRadius = 4.0f;
    static final float half = 2.0f;
    static final float baseHeight = 288.0f;
    static final float rowHeightIncrement = 23.0f;
    static final int maxSwatches = 29;
    static final float snapThreshold = 50.0f;
    static final float pointerSize = 4.0f;
    public final WidgetBounds svBounds;
    public final WidgetBounds hueBounds;
    public final WidgetBounds alphaBounds;

    public final WidgetBounds addSwatchBounds;
    public final WidgetBounds dragBarBounds;
    public final ToggleAnimator openAnimator;
    public List<ColorSwatch> swatches;
    public float hue;
    public float saturation;
    public float brightness;
    public float alpha;
    public boolean draggingSV;

    public boolean draggingHue;

    public boolean draggingAlpha;

    public boolean draggingSide;
    public boolean onRightSide;
    public final ClickableBehavior addSwatchClickable;

    public final LoadingButtonWidget closeButton;
    public final MenuWindow menuWindow;

    public ColorSwatch selectedSwatch;
    public Consumer<Integer> colorCallback;
    public Consumer<Float> alphaCallback;

    public Runnable applyAction;

    public ColorPickerWindow(float f, float f2, float f3, float f4) {
        super(248.0f, computeHeight(20));
        this.clickIcon = new GlTexture(new ClasspathResource("/icons/menu/new/click.png"));
        this.plusIcon = new GlTexture(new ClasspathResource("/icons/menu/new/plus.png"));
        this.checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
        this.font = Fonts.INTER_EXTRA_BOLD.get();
        this.svBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.hueBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.alphaBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.addSwatchBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.dragBarBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.openAnimator = new ToggleAnimator(animationDuration, Easings.EASE_IN_OUT_CUBIC);
        this.swatches = null;
        this.onRightSide = true;
        this.addSwatchClickable = new ClickableBehavior();
        this.selectedSwatch = null;
        int iArgb = FluxVisuals.INSTANCE.theme().palette().accent().argb();
        if (this.swatches == null) {
            this.swatches = new ArrayList(Arrays.asList(new ColorSwatch(iArgb, true), new ColorSwatch(ColorValue.fromHex("E7BC37").argb(), true), new ColorSwatch(ColorValue.fromHex("8673FA").argb(), true), new ColorSwatch(ColorValue.fromHex("B8FF8C").argb(), true), new ColorSwatch(ColorValue.fromHex("C34040").argb(), true), new ColorSwatch(ColorValue.fromHex("65677A").argb(), true), new ColorSwatch(ColorValue.fromHex("D6D6E6").argb(), true), new ColorSwatch(ColorValue.fromHex("59B35C").argb(), true), new ColorSwatch(ColorValue.fromHex("EDCA5C").argb(), true), new ColorSwatch(ColorValue.fromHex("568CDD").argb(), true), new ColorSwatch(ColorValue.fromHex("FF6158").argb(), true), new ColorSwatch(ColorValue.fromHex("af52de").argb(), true), new ColorSwatch(ThemePalette.mint.argb(), true), new ColorSwatch(ThemePalette.lightOrange.argb(), true), new ColorSwatch(ThemePalette.golden.argb(), true), new ColorSwatch(ColorValue.fromHex("575AC6").argb(), true), new ColorSwatch(ColorValue.fromHex("7AD7C1").argb(), true), new ColorSwatch(ColorValue.fromHex("F2A65A").argb(), true), new ColorSwatch(ColorValue.fromHex("6A9FB5").argb(), true)));
        }
        this.closeButton = new LoadingButtonWidget(Lang.CLOSE, Math.round(width() - 24.0f), 25, this::onCloseClicked, 7.0f, true);
        this.hue = f;
        this.saturation = f2;
        this.brightness = f3;
        this.alpha = f4;
        this.menuWindow = FluxVisuals.INSTANCE.menuWindow();
        this.clickIcon.setDimensions(12, 12);
        this.addSwatchClickable.clickCallback(() -> {
            if (this.swatches.size() < maxSwatches) {
                addSwatch();
            }
        });
        for (ColorSwatch colorSwatch : this.swatches) {
            colorSwatch.clickable().clickCallback(() -> {
                selectSwatch(colorSwatch);
            });
            if (!colorSwatch.system()) {
                colorSwatch.clickable().rightClickCallback(() -> {
                    removeSwatch(colorSwatch);
                });
            }
        }
        addChild(this.closeButton);
    }

    public void selectSwatch(ColorSwatch colorSwatch) {
        this.selectedSwatch = colorSwatch;
        int iColor = colorSwatch.color();
        int i = (iColor >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i2 = (iColor >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i3 = iColor & StencilBufferUtil.STENCIL_MASK;
        int i4 = (iColor >> 24) & StencilBufferUtil.STENCIL_MASK;
        float[] fArrRGBtoHSB = Color.RGBtoHSB(i, i2, i3, (float[]) null);
        this.hue = fArrRGBtoHSB[0];
        this.saturation = fArrRGBtoHSB[1];
        this.brightness = fArrRGBtoHSB[2];
        this.alpha = i4 / 255.0f;
        colorSwatch.selectAnim().state(true);
        emitColorAndAlpha();
    }

    public void removeSwatch(ColorSwatch colorSwatch) {
        if (colorSwatch.system()) {
            return;
        }
        if (this.selectedSwatch == colorSwatch) {
            clearSelection();
        }
        this.swatches.remove(colorSwatch);
        setSize(width(), computeHeight(this.swatches.size() + 1));
    }

    public void onCloseClicked() {
        closePicker();
    }

    public void updateColor(float f, float f2, float f3, float f4) {
        this.hue = f;
        this.saturation = f2;
        this.brightness = f3;
        this.alpha = f4;
    }

    public static float computeHeight(int i) {
        int iCeil = (int) Math.ceil(((double) i) / ((double) 10));
        return iCeil <= 2 ? baseHeight : baseHeight + ((iCeil - 2) * rowHeightIncrement);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        updatePosition();
        updateBounds();
        super.layout(layoutScaleContext);
    }

    void updatePosition() {
        if (this.onRightSide) {
            setPosition(this.menuWindow.x() + MenuWindow.MENU_WIDTH + 0.0f + margin, this.menuWindow.y());
        } else {
            setPosition((this.menuWindow.x() - width()) - margin, this.menuWindow.y());
        }
    }

    void updateBounds() {
        this.svBounds.withPosition(x() + 12.0f, y() + 12.0f).withSize(svBoxWidth, svBoxHeight);
        this.hueBounds.withSize(barWidth, svBoxHeight).withPosition(((x() + width()) - 28.0f) - barWidth, this.svBounds.y());
        this.alphaBounds.withSize(barWidth, svBoxHeight).withPosition(((x() + width()) - 12.0f) - barWidth, this.svBounds.y());
        this.closeButton.setPosition(x() + 12.0f, ((y() + height()) - 12.0f) - this.closeButton.height());
        this.dragBarBounds.withPosition(x(), y()).withSize(width(), 12.0f);
        this.checkmarkIcon.setDimensions(12, 12);
        this.clickIcon.setDimensions(12, 12);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        this.closeButton.baseColor(themePalette.surfaceBackground().tone(600).argb());
        this.closeButton.textColor(themePalette.text().tone(600).argb());
        this.closeButton.outlineColor(themePalette.surfaceOutline().tone(400).argb());
        if (this.openAnimator.isZero()) {
            return;
        }
        colorStack115.push();
        if (!this.openAnimator.isOne()) {
            colorStack115.alpha(this.openAnimator.smoothAnimation());
        }
        drawCtx.fillOutlinedRoundedRect(x(), y(), width(), height(), cornerRadius, outlineWidth, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb()));
        if (this.draggingSide) {
            drawCtx.fillRoundedRect(this.onRightSide ? (this.menuWindow.x() - 4.0f) - margin : this.menuWindow.x() + MenuWindow.MENU_WIDTH + 0.0f + margin, this.menuWindow.y(), 4.0f, height(), half, colorStack115.computeColor(themePalette.accent().argb()));
        }
        int iMethod009 = renderSaturationBrightness(drawCtx, colorStack115);
        renderHueBar(drawCtx, colorStack115);
        renderAlphaBar(drawCtx, iMethod009, colorStack115);
        float fMethod011 = renderRgbaRow(drawCtx, colorStack115, themePalette) + 12.0f;
        if (this.swatches != null) {
            for (int i = 0; i < this.swatches.size(); i++) {
                ColorSwatch colorSwatch = this.swatches.get(i);
                float fX = this.svBounds.x() + ((i % 10) * (14.0f + 9.0f));
                float f = fMethod011 + ((i / 10) * (14.0f + 9.0f));
                drawCtx.circle(fX + (14.0f / half), f + (14.0f / half), (14.0f + (colorSwatch.clickable.hoverAnimation().smoothAnimation() * 1.3f)) / half, colorStack115.computeColor(colorSwatch.color()));
                colorSwatch.clickable().determineFromLastDrawEngineOperation(drawCtx);
                if (this.selectedSwatch == colorSwatch && colorSwatch.selectAnim().smoothAnimation() > 0.0f) {
                    colorStack115.push();
                    colorStack115.alpha(colorSwatch.selectAnim().smoothAnimation());
                    drawCtx.texture(this.checkmarkIcon, (fX + (14.0f / half)) - (this.checkmarkIcon.width() / 2), (f + (14.0f / half)) - (this.checkmarkIcon.height() / 2), this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb()));
                    colorStack115.pop();
                }
            }
            if (this.swatches.size() < maxSwatches) {
                int size = this.swatches.size();
                float fX2 = this.svBounds.x() + ((size % 10) * (14.0f + 9.0f));
                float f2 = fMethod011 + ((size / 10) * (14.0f + 9.0f));
                float fSmoothAnimation = this.addSwatchClickable.hoverAnimation().smoothAnimation() * 1.3f;
                this.addSwatchBounds.withPosition(fX2, f2).withSize(14.0f, 14.0f);
                drawCtx.circle(fX2 + (14.0f / half), f2 + (14.0f / half), (14.0f + fSmoothAnimation) / half, colorStack115.computeColor(themePalette.text().tone(500).argb()));
                this.addSwatchClickable.determineFromLastDrawEngineOperation(drawCtx);
                drawCtx.texture(this.plusIcon, (fX2 + (14.0f / half)) - (this.plusIcon.width() / 2), ((f2 + (14.0f / half)) - (this.plusIcon.height() / 2)) - 0.1f, this.plusIcon.width(), this.plusIcon.height(), -1);
            }
        }
        this.closeButton.render(drawCtx);
        colorStack115.pop();
    }

    float renderRgbaRow(DrawCtx drawCtx, ColorStack colorStack, ThemePalette themePalette) {
        float fY = this.hueBounds.y() + this.hueBounds.height() + 12.0f;
        drawCtx.fillOutlinedRoundedRect(this.svBounds.x(), fY, width() - 24.0f, rgbaRowHeight, barWidth, outlineWidth, colorStack.computeColor(themePalette.surfaceOutline().tone(300).argb()), colorStack.computeColor(themePalette.surfaceBackground().tone(900).argb(), 0.0f));
        float fX = this.svBounds.x() + 1.0f;
        drawCtx.fillRoundedRect(fX, fY + 1.0f, 58.0f, 24.0f, new Vector4f(0.0f, 0.0f, barWidth, barWidth), colorStack.computeColor(themePalette.surfaceBackground().tone(600).argb()));
        drawCtx.textureVerticalC(this.clickIcon, this.svBounds.x() + cornerRadius, fY + 13.0f, this.clickIcon.width(), this.clickIcon.height(), colorStack.computeColor(themePalette.text().tone(400).argb()));
        drawCtx.text(this.font, "RGBA", 10, this.svBounds.x() + cornerRadius + this.clickIcon.width() + 4.0f, (fY + 13.0f) - (this.font.getHeight(margin) / half), colorStack.computeColor(themePalette.text().tone(400).argb()));
        float f = (fX + labelColumnWidth) - 3.0f;
        drawSeparator(drawCtx, colorStack, themePalette, f, fY);
        float f2 = f + 1.0f;
        int iHSBtoRGB = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        int[] iArr = {(iHSBtoRGB >> 16) & StencilBufferUtil.STENCIL_MASK, (iHSBtoRGB >> 8) & StencilBufferUtil.STENCIL_MASK, iHSBtoRGB & StencilBufferUtil.STENCIL_MASK, (int) (clamp01(this.alpha) * 255.0f)};
        for (int i = 0; i < iArr.length; i++) {
            String strValueOf = String.valueOf(iArr[i]);
            drawCtx.text(this.font, strValueOf, 10, f2 + ((valueColumnWidth - drawCtx.textWidthPhysical(this.font, strValueOf, 10)) / half), (fY + 13.0f) - (this.font.getHeight(margin) / half), colorStack.computeColor(themePalette.text().tone(400).argb()));
            f2 += valueColumnWidth;
            if (i != iArr.length - 1) {
                drawSeparator(drawCtx, colorStack, themePalette, f2, fY);
                f2 += 1.0f;
            }
        }
        return fY + rgbaRowHeight;
    }

    void drawSeparator(DrawCtx drawCtx, ColorStack colorStack, ThemePalette themePalette, float f, float f2) {
        drawCtx.fillRect(f, f2 + 1.0f, separatorWidth, 24.0f, colorStack.computeColor(themePalette.surfaceOutline().tone(500).argb()));
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (!opened() || this.openAnimator.isZero()) {
            return false;
        }
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (inputEvent691 instanceof CursorMoveInput) {
            PixelPoint pixelPoint = inputEventContext.logicalMousePosition();
            if (handleDrag(pixelPoint) || handleSideDrag(pixelPoint)) {
                return true;
            }
        }
        if (inputEvent691 instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
            if (mouseButtonInput.button() == 0) {
                if (mouseButtonInput.action().release()) {
                    boolean z2 = this.draggingSV || this.draggingHue || this.draggingAlpha || this.draggingSide;
                    if (this.draggingSide) {
                        this.draggingSide = false;
                    }
                    this.draggingAlpha = false;
                    this.draggingHue = false;
                    this.draggingSV = false;
                    return z2;
                }
                if (inputEventContext.inArea(this.dragBarBounds.x(), this.dragBarBounds.y(), this.dragBarBounds.width(), this.dragBarBounds.height())) {
                    this.draggingSide = true;
                    return true;
                }
                if (inputEventContext.inArea(this.svBounds.x(), this.svBounds.y(), this.svBounds.width(), this.svBounds.height())) {
                    this.draggingSV = true;
                    return true;
                }
                if (inputEventContext.inArea(this.hueBounds.x(), this.hueBounds.y(), this.hueBounds.width(), this.hueBounds.height())) {
                    this.draggingHue = true;
                    return true;
                }
                if (inputEventContext.inArea(this.alphaBounds.x(), this.alphaBounds.y(), this.alphaBounds.width(), this.alphaBounds.height())) {
                    this.draggingAlpha = true;
                    return true;
                }
            }
        }
        if (!this.draggingAlpha && !this.draggingSV && !this.draggingHue && !this.draggingSide) {
            Iterator<ColorSwatch> it = this.swatches.iterator();
            while (it.hasNext()) {
                if (it.next().clickable().handleInput(inputEventContext, z)) {
                    return true;
                }
            }
            if (this.addSwatchClickable.handleInput(inputEventContext, z)) {
                return true;
            }
        }
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimator.animate(weightedEngine);
        this.addSwatchClickable.animate(weightedEngine);
        for (ColorSwatch colorSwatch : this.swatches) {
            colorSwatch.clickable().animate(weightedEngine);
            colorSwatch.selectAnim().animate(weightedEngine);
        }
        super.animation(weightedEngine);
    }

    boolean handleDrag(PixelPoint pixelPoint) {
        if (this.draggingSV) {
            updateSaturationBrightness(pixelPoint.x(), pixelPoint.y());
            clearSelection();
            emitColorAndAlpha();
            return true;
        }
        if (this.draggingHue) {
            updateHue(pixelPoint.y());
            clearSelection();
            emitColorAndAlpha();
            return true;
        }
        if (!this.draggingAlpha) {
            return false;
        }
        updateAlpha(pixelPoint.y());
        emitColorAndAlpha();
        return true;
    }

    boolean handleSideDrag(PixelPoint pixelPoint) {
        if (!this.draggingSide) {
            return false;
        }
        float fX = this.menuWindow.x() + ((MenuWindow.MENU_WIDTH + 0.0f) / half);
        float fX2 = pixelPoint.x();
        if (this.onRightSide) {
            if (fX2 >= fX - snapThreshold) {
                return true;
            }
            this.onRightSide = false;
            updatePosition();
            updateBounds();
            return true;
        }
        if (fX2 <= fX + snapThreshold) {
            return true;
        }
        this.onRightSide = true;
        updatePosition();
        updateBounds();
        return true;
    }

    public void clearSelection() {
        if (this.selectedSwatch != null) {
            this.selectedSwatch.selectAnim().state(false);
            this.selectedSwatch = null;
        }
    }

    int renderSaturationBrightness(DrawCtx drawCtx, ColorStack colorStack) {
        int iComputeColor = colorStack.computeColor(Color.HSBtoRGB(this.hue, 1.0f, 1.0f));
        LayoutScaleContext layoutScaleContext = drawCtx.layoutContext();
        drawCtx.fillGradientRoundedRect(this.svBounds.x(), this.svBounds.y(), this.svBounds.width(), this.svBounds.height(), 4, colorStack.white(), iComputeColor, iComputeColor, colorStack.white());
        drawCtx.fillGradientRoundedRect(this.svBounds.x(), this.svBounds.y(), this.svBounds.width(), this.svBounds.height(), 4, colorStack.computeColor(0, 1.0f), colorStack.computeColor(0, 1.0f), colorStack.transparent(), colorStack.transparent());
        drawCtx.drawEngine().hollowCircle(drawCtx.matrixStack().peek().getPositionMatrix(), layoutScaleContext.toPhysical(this.svBounds.x() + clamp(this.saturation * this.svBounds.width(), barWidth, this.svBounds.width() - barWidth)), layoutScaleContext.toPhysical(this.svBounds.y() + clamp((1.0f - this.brightness) * this.svBounds.height(), barWidth, this.svBounds.height() - barWidth)), layoutScaleContext.toPhysical(barWidth), layoutScaleContext.toPhysical(outlineWidth), colorStack.white());
        return iComputeColor;
    }

    void renderHueBar(DrawCtx drawCtx, ColorStack colorStack) {
        float fHeight = this.hueBounds.height() - (3.0f * half);
        for (int i = 0; i < fHeight; i++) {
            drawCtx.circle(this.hueBounds.x() + 3.0f, this.hueBounds.y() + i + 3.0f, 3.0f, colorStack.HSBtoRGB(i / fHeight, 1.0f, 1.0f));
        }
        drawCtx.fillRoundedRect((this.hueBounds.x() + (this.hueBounds.width() / half)) - 5.0f, this.hueBounds.y() + (this.hue * (this.hueBounds.height() - 4.0f)), margin, 4.0f, half, colorStack.white());
    }

    void renderAlphaBar(DrawCtx drawCtx, int i, ColorStack colorStack) {
        int iTransparent = i & colorStack.transparent();
        int iComputeColor = i | colorStack.computeColor(0);
        LayoutScaleContext layoutScaleContext = drawCtx.layoutContext();
        drawCtx.drawEngine().roundedChecker(drawCtx.matrixStack().peek().getPositionMatrix(), layoutScaleContext.toPhysical(this.alphaBounds.x()), layoutScaleContext.toPhysical(this.alphaBounds.y()), layoutScaleContext.toPhysical(this.alphaBounds.width()), layoutScaleContext.toPhysical(this.alphaBounds.height()), 3.0f, half, colorStack.computeColor(0.7f, 39, 39, 40), colorStack.computeColor(0.7f, 14, 14, 15));
        drawCtx.fillGradientRoundedRect(this.alphaBounds.x(), this.alphaBounds.y(), this.alphaBounds.width(), this.alphaBounds.height(), 3, iTransparent, iTransparent, iComputeColor, iComputeColor);
        drawCtx.fillRoundedRect((this.alphaBounds.x() + (this.alphaBounds.width() / half)) - 5.0f, this.alphaBounds.y() + (this.alphaBounds.height() * (1.0f - clamp01(this.alpha))), margin, 4.0f, half, colorStack.white());
    }

    void updateSaturationBrightness(float f, float f2) {
        this.saturation = clamp01((f - this.svBounds.x()) / this.svBounds.width());
        this.brightness = 1.0f - clamp01((f2 - this.svBounds.y()) / this.svBounds.height());
    }

    void updateHue(float f) {
        this.hue = clamp01((f - this.hueBounds.y()) / this.hueBounds.height());
    }

    void updateAlpha(float f) {
        this.alpha = 1.0f - clamp01((f - this.alphaBounds.y()) / this.alphaBounds.height());
    }

    void emitColor() {
        if (this.colorCallback == null) {
            return;
        }
        this.colorCallback.accept(Integer.valueOf(Color.HSBtoRGB(this.hue, this.saturation, this.brightness)));
    }

    void emitAlpha() {
        if (this.alphaCallback == null) {
            return;
        }
        this.alphaCallback.accept(Float.valueOf(this.alpha));
    }

    void addSwatch() {
        if (this.swatches == null || this.swatches.size() >= maxSwatches) {
            return;
        }
        ColorSwatch colorSwatch = new ColorSwatch((Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215) | (((int) (clamp01(this.alpha) * 255.0f)) << 24), false);
        colorSwatch.clickable().clickCallback(() -> {
            selectSwatch(colorSwatch);
        });
        colorSwatch.clickable().rightClickCallback(() -> {
            removeSwatch(colorSwatch);
        });
        this.swatches.add(colorSwatch);
        setSize(width(), computeHeight(this.swatches.size() + 1));
    }

    public void emitColorAndAlpha() {
        emitColor();
        emitAlpha();
    }

    static float clamp01(float f) {
        if (f < 0.0f) {
            return 0.0f;
        }
        return Math.min(f, 1.0f);
    }

    static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public void togglePicker() {
        if (this.openAnimator.isOne() || this.openAnimator.state()) {
            closePicker();
        } else {
            openPicker();
        }
    }

    public void openPicker() {
        this.openAnimator.state(true);
    }

    public void closePicker() {
        if (this.applyAction != null) {
            this.applyAction.run();
        }
        this.draggingHue = false;
        this.draggingSV = false;
        this.draggingAlpha = false;
        this.openAnimator.state(false);
    }

    public boolean opened() {
        return this.openAnimator.isOne() || this.openAnimator.state();
    }

    public ColorPickerWindow colorConsumer(Consumer<Integer> consumer) {
        this.colorCallback = consumer;
        return this;
    }

    public ColorPickerWindow alphaConsumer(Consumer<Float> consumer) {
        this.alphaCallback = consumer;
        return this;
    }

    public ColorPickerWindow applyCallback(Runnable runnable) {
        this.applyAction = runnable;
        return this;
    }
}
