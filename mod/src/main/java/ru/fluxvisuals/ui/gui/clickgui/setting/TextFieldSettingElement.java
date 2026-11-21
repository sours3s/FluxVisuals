package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
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
import ru.fluxvisuals.ui.gui.clickgui.TextInputField;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextFieldSettingElement extends ModuleFrame {
    public static final int nameFontSize = 14;
    public static final int descriptionFontSize = 13;
    public static final int inputFontSize = 13;
    public static final float T = 3.0f;
    public static final float U = 10.0f;
    public static final float V = 25.0f;
    public static final float W = 110.0f;
    public final Translation name;
    public final Translation placeholder;
    public final Translation description;
    public final boolean masked;

    public Consumer<String> changeCallback;
    public Supplier<Boolean> visibilitySupplier;
    public final MsdfFont nameFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont textFont = Fonts.INTER_MEDIUM.get();
    public final GlTexture pencilTexture = new GlTexture(new ClasspathResource("/icons/menu/new/pencil.png"));
    public final TextInputField inputField = new TextInputField(this.textFont, 13);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final WidgetBounds inputBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public float scaleFactor = 1.0f;

    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float lastWrapWidth = -1.0f;
    public String lastDescription = null;
    public float descriptionHeight = 0.0f;
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        this.descriptionDirty = true;
        this.lastWrapWidth = -1.0f;
        this.lastDescription = null;
    };

    public TextFieldSettingElement(Translation translation, Translation class254Var2, Translation class254Var3, boolean z, int i, boolean z2, Supplier<String> supplier) {
        this.name = translation;
        this.placeholder = class254Var3;
        this.description = class254Var2;
        this.masked = z2;
        if (supplier != null) {
            this.inputField.append(supplier.get());
        }
        this.inputField.numbersOnly(z);
        if (i != -1) {
            this.inputField.maxLength(i);
        }
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
        String strEffective;
        updateDescription(f);
        ThemePalette themePalette = drawCtx.theme().palette();
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        ColorStack colorStack115 = drawEngine154.colorStack();
        float fY = y();
        float fValue = this.highlightAnimation.value();
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), fValue);
        int iComputeColor = colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb());
        int iComputeColor2 = colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb());
        int iComputeColor3 = colorStack115.computeColor(themePalette.text().tone(300).argb());
        int iComputeColor4 = colorStack115.computeColor(themePalette.text().tone(500).argb());
        int iInterpolate2 = colorStack115.interpolate(iComputeColor, colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.inputField.hoverAnimation());
        int iInterpolate3 = colorStack115.interpolate(iComputeColor2, colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.inputField.hoverAnimation());
        String strText = (this.inputField.focused() || !this.masked) ? this.inputField.text() : "*".repeat(this.inputField.text().length());
        if (this.placeholder != null && strText.isEmpty() && !this.inputField.focused()) {
            iComputeColor3 = colorStack115.computeColor(themePalette.text().tone(600).argb());
        }
        float fX = this.inputBounds.x() + 8.0f;
        if (!strText.isEmpty() || this.inputField.focused()) {
            strEffective = strText;
        } else {
            strEffective = this.placeholder != null ? this.placeholder.effective() : "";
        }
        String str = strEffective;
        if (this.inputField.focused()) {
            drawCtx.window().interceptKeyboard(true);
        }
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.nameFont, this.name.effective(), nameFontSize);
        float f3 = (f - W) - U;
        if (this.wrappedDescription != null || fTextWidthPhysical >= f3) {
            drawCtx.text(this.nameFont, this.name.effective(), nameFontSize, x(), fY, iInterpolate);
            if (this.wrappedDescription != null) {
                fY += this.nameFont.getHeight(14.0f) + T;
                drawCtx.text(this.textFont, this.wrappedDescription, 13, x(), fY, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), fValue));
            }
            this.inputBounds.withPosition(x(), fY + (this.description != null ? this.descriptionHeight : this.nameFont.getHeight(14.0f)) + U).withSize(f, V);
        } else {
            this.inputBounds.withPosition((x() + f) - W, y()).withSize(W, V);
            drawCtx.text(this.nameFont, this.name.effective(), nameFontSize, x(), (this.inputBounds.y() + (this.inputBounds.height() / 2.0f)) - (this.nameFont.getHeight(14.0f) / 2.0f), iInterpolate);
        }
        float fY2 = this.inputBounds.y() + (this.inputBounds.height() / 2.0f);
        drawCtx.fillOutlinedRoundedRect(this.inputBounds.x(), this.inputBounds.y(), this.inputBounds.width(), this.inputBounds.height(), 6.0f, 2.5f, iInterpolate3, iInterpolate2);
        drawCtx.textureVerticalC(this.pencilTexture, fX, fY2, this.pencilTexture.width(), this.pencilTexture.height(), iComputeColor4);
        float fWidth = (fX + (this.pencilTexture.width() + 6.0f)) - this.inputField.viewportOffset();
        drawEngine154.beginStencil();
        drawCtx.fillOutlinedRoundedRect(this.inputBounds.x() + 8.0f + this.pencilTexture.width() + 6.0f, this.inputBounds.y(), this.inputBounds.width() - 40.0f, this.inputBounds.height(), 6.0f, 2.5f, iInterpolate3, iInterpolate2);
        drawEngine154.prepareStencil(1);
        float height = this.textFont.getHeight(13.0f);
        if (this.inputField.hasSelection()) {
            int iSelMin = this.inputField.selMin();
            int iSelMax = this.inputField.selMax();
            float fTextWidthPhysical2 = fWidth + drawCtx.textWidthPhysical(this.textFont, str.substring(0, iSelMin), 13);
            drawCtx.fillRoundedRect(fTextWidthPhysical2, (fY2 - (height / 2.0f)) - 1.0f, ((fWidth + drawCtx.textWidthPhysical(this.textFont, str.substring(0, iSelMax), 13)) + 1.0f) - fTextWidthPhysical2, height + 2.0f, 4.0f, colorStack115.computeColor(themePalette.accent().argb(), 128));
        }
        drawCtx.text(this.textFont, str, 13, fWidth, fY2 - (height / 2.0f), iComputeColor3);
        if (this.inputField.isCursorVisible() && !this.inputField.hasSelection()) {
            drawCtx.fillRect(fWidth + drawCtx.textWidthPhysical(this.textFont, str.substring(0, this.inputField.cursorIndex()), 13), fY2 - (height / 2.0f), 1.0f, height, colorStack115.computeColor(16777215));
        }
        drawEngine154.endStencil();
        this.inputField.changeText(str2 -> {
            if (this.changeCallback == null) {
                return;
            }
            this.changeCallback.accept(str2);
        });
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.pencilTexture.setDimensions(12, 12);
        this.scaleFactor = layoutScaleContext.scaleFactor();
        this.inputField.listen(this.inputBounds.x(), this.inputBounds.y(), this.inputBounds.width(), this.inputBounds.height(), this.inputBounds.width() - 47.0f, this.inputBounds.x() + 8.0f + this.pencilTexture.width() + 6.0f, this.scaleFactor);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.inputField.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        this.inputField.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public float height() {
        float fMax;
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescription(this.parent.width() - 18.0f);
        }
        float fMethod003 = measureWidth(this.name.effective(), nameFontSize);
        float fWidth = (this.parent.width() - W) - U;
        if ((this.description == null || this.wrappedDescription == null) && fMethod003 < fWidth) {
            fMax = Math.max(this.nameFont.getHeight(14.0f), V);
        } else {
            float height = 0.0f + this.nameFont.getHeight(14.0f);
            if (this.description != null && this.wrappedDescription != null) {
                height += T + this.descriptionHeight;
            }
            fMax = height + 35.0f;
        }
        return fMax;
    }

    public float measureWidth(String str, int i) {
        if (this.scaleFactor <= 0.0f || this.scaleFactor == 1.0f) {
            return this.nameFont.getWidth(str, i);
        }
        return this.nameFont.getWidth(str, Math.max(1, Math.round(i * this.scaleFactor))) / this.scaleFactor;
    }

    @Override
    public void handleClose() {
        this.inputField.focused(false);
        super.handleClose();
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.lastWrapWidth = f;
            this.lastDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.lastWrapWidth || strEffective == null || !strEffective.equals(this.lastDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.textFont, 13);
            this.descriptionHeight = this.textFont.getHeightWithLineBreaks(this.wrappedDescription, 13);
            this.lastWrapWidth = f;
            this.lastDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    public void focusAtEnd() {
        this.inputField.focusAtEnd();
    }

    public Translation name() {
        return this.name;
    }

    public TextFieldSettingElement onChange(Consumer<String> consumer) {
        this.changeCallback = consumer;
        return this;
    }

    public TextFieldSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }
}
