package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
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
import ru.fluxvisuals.ui.gui.clickgui.util.type.ModeOption;
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.lang.Enum;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class ModeSettingElement<T extends Enum<T>> extends ModuleFrame {
    static final int titleTextSize = 14;
    static final int descTextSize = 13;
    static final float descGap = 3.0f;
    static final float blockSpacing = 12.0f;
    static final float rowHeight = 25.0f;
    static final float a = 23.0f;
    static final float b = 6.0f;
    static final float c = 2.0f;
    public final Translation name;
    public final Translation description;
    public boolean expanded;
    public String wrappedDescription;
    public String cachedDescription;
    public float descriptionHeight;
    public Supplier<T> valueSupplier;
    public Consumer<T> changeCallback;
    public Supplier<Boolean> visibleCondition;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont valueFont = Fonts.INTER_BOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final GlTexture arrowIcon = new GlTexture(new ClasspathResource("/icons/menu/new/arrow.png"));
    public final GlTexture checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
    public final ToggleAnimator expandAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<ModeOption<T>> options = new ArrayList();
    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        invalidateLayout();
    };
    public final WidgetBounds boxBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public float cachedWidth = -1.0f;
    public boolean descriptionDirty = true;

    public ModeSettingElement(Translation translation, Translation class254Var2, T[] tArr) {
        this.name = translation;
        this.description = class254Var2;
        for (T t : tArr) {
            this.options.add(new ModeOption<>(t));
        }
        setupOptionCallbacks();
        this.clickBehavior.clickCallback(this::toggleExpanded);
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
        float fMethod011 = drawDescription(drawCtx, drawTitle(drawCtx, y())) + blockSpacing;
        this.boxBounds.withPosition(x(), fMethod011).withSize(f, rowHeight);
        drawBox(drawCtx, fMethod011, f);
        drawBoxContent(drawCtx, fMethod011, f);
        updateBounds(fMethod011, f);
    }

    @Override
    public void drawOverlay(DrawCtx drawCtx) {
        if (this.options.isEmpty()) {
            return;
        }
        if (!this.expandAnimation.isZero() || this.expanded) {
            ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
            colorStack115.push();
            colorStack115.alpha(this.expandAnimation.smoothAnimation());
            drawDropdownBackground(drawCtx);
            drawDropdownOptions(drawCtx);
            colorStack115.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.enumIcon.setDimensions(12, 12);
        this.arrowIcon.setDimensions(12, 12);
        this.checkmarkIcon.setDimensions(titleTextSize, titleTextSize);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibleCondition != null) {
            visible(this.visibleCondition.get().booleanValue());
        }
        this.expandAnimation.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        for (ModeOption<T> modeOption : this.options) {
            modeOption.clickableBehavior().animate(weightedEngine);
            if (this.valueSupplier != null) {
                modeOption.currentOptionAnimation.state(modeOption.option == this.valueSupplier.get());
                modeOption.currentOptionAnimation.animate(weightedEngine);
            }
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zMethod004 = false;
        if (this.expanded) {
            zMethod004 = handleDropdownInput(inputEventContext, z);
        }
        if (this.clickBehavior.handleInput(inputEventContext, z || zMethod004)) {
            zMethod004 = true;
        }
        return zMethod004;
    }

    @Override
    public void handleClose() {
        closeDropdown();
        super.handleClose();
    }

    @Override
    public void handleViewportVisibility(float f, float f2, float f3) {
        if (this.expanded || !this.expandAnimation.isZero()) {
            float fY = y() - f3;
            if (fY + height() < f || fY > f2) {
                closeDropdown();
            }
        }
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescription(this.parent.width());
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += descGap + this.descriptionHeight;
        }
        return height + blockSpacing + rowHeight;
    }

    public void setupOptionCallbacks() {
        for (ModeOption<T> modeOption : this.options) {
            modeOption.clickableBehavior().clickCallback(() -> {
                if (this.valueSupplier != null && modeOption.option != this.valueSupplier.get()) {
                    toggleExpanded();
                }
                if (this.changeCallback != null) {
                    this.changeCallback.accept((T) ((Enum) modeOption.option));
                }
            });
        }
    }

    public float drawTitle(DrawCtx drawCtx, float f) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        drawCtx.text(this.titleFont, this.name.effective(), titleTextSize, x(), f, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), this.highlightAnimation.value()));
        return f + this.titleFont.getHeight(14.0f);
    }

    public float drawDescription(DrawCtx drawCtx, float f) {
        if (this.wrappedDescription == null) {
            return f;
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), this.highlightAnimation.value());
        float f2 = f + descGap;
        drawCtx.text(this.descriptionFont, this.wrappedDescription, descTextSize, x(), f2, iInterpolate);
        return f2 + this.descriptionHeight;
    }

    public void drawBox(DrawCtx drawCtx, float f, float f2) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        drawCtx.fillOutlinedRoundedRect(x(), f, f2, rowHeight, b, 2.5f, colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.clickBehavior.hoverAnimation()), colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.clickBehavior.hoverAnimation()));
    }

    public void drawBoxContent(DrawCtx drawCtx, float f, float f2) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        float fX = x() + 10.0f;
        drawCtx.texture(this.enumIcon, fX, (f + 12.5f) - Math.round(this.enumIcon.height() / c), this.enumIcon.width(), this.enumIcon.height(), colorStack115.computeColor(themePalette.text().tone(500).argb()));
        drawArrow(drawCtx, f, f2);
        drawValueText(drawCtx, f, f2, fX + this.enumIcon.width() + b);
    }

    public void drawArrow(DrawCtx drawCtx, float f, float f2) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        float fX = ((x() + f2) - 10.0f) - this.arrowIcon.width();
        float fRound = (f + 12.5f) - Math.round(this.arrowIcon.height() / c);
        float fWidth = fX + (this.arrowIcon.width() / c);
        float fHeight = fRound + (this.arrowIcon.height() / c);
        float fSmoothAnimation = (float) (3.141592653589793d * ((double) this.expandAnimation.smoothAnimation()));
        float physical = drawCtx.layoutContext().toPhysical(fWidth);
        float physical2 = drawCtx.layoutContext().toPhysical(fHeight);
        drawCtx.matrixStack().push();
        drawCtx.matrixStack().translate(physical, physical2, 0.0f);
        drawCtx.matrixStack().multiply(new Quaternionf().rotateZ(fSmoothAnimation));
        drawCtx.matrixStack().translate(-physical, -physical2, 0.0f);
        drawCtx.texture(this.arrowIcon, fX, fRound, this.arrowIcon.width(), this.arrowIcon.height(), colorStack115.computeColor(themePalette.text().tone(600).argb()));
        drawCtx.matrixStack().pop();
    }

    public void drawValueText(DrawCtx drawCtx, float f, float f2, float f3) {
        if (this.valueSupplier == null) {
            return;
        }
        T t = this.valueSupplier.get();
        if (t instanceof DisplayNamed) {
            DisplayNamed displayNamed = (DisplayNamed) t;
            int iComputeColor = drawCtx.drawEngine().colorStack().computeColor(drawCtx.theme().palette().text().tone(300).argb());
            String strEffective = displayNamed.getDisplayName().effective();
            float fX = ((((x() + f2) - 10.0f) - this.arrowIcon.width()) - 10.0f) - f3;
            float fRound = (f + 12.5f) - Math.round(this.valueFont.getHeight(blockSpacing) / c);
            if (drawCtx.textWidthPhysical(this.valueFont, strEffective, 12) > fX) {
                drawTruncatedText(drawCtx, strEffective, f3, fRound, fX, iComputeColor);
            } else {
                drawCtx.text(this.valueFont, strEffective, 12, f3, fRound, iComputeColor);
            }
        }
    }

    public void drawTruncatedText(DrawCtx drawCtx, String str, float f, float f2, float f3, int i) {
        int i2 = i & 16777215;
        String str2 = "";
        for (int length = str.length(); length > 0; length--) {
            String strSubstring = str.substring(0, length);
            if (drawCtx.textWidthPhysical(this.valueFont, strSubstring, 12) <= f3) {
                str2 = strSubstring;
                break;
            }
        }
        drawCtx.textWithHorizontalGradient(this.valueFont, str2, 12, f, f2, i, i2);
    }

    public void drawDropdownBackground(DrawCtx drawCtx) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        drawCtx.fillOutlinedRoundedRect(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height(), 7.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()));
    }

    public void drawDropdownOptions(DrawCtx drawCtx) {
        T t = this.valueSupplier == null ? null : this.valueSupplier.get();
        for (int i = 0; i < this.options.size(); i++) {
            ModeOption<T> modeOption = this.options.get(i);
            float fY = this.dropdownBounds.y() + b + (i * rowHeight);
            if (fY + a > this.dropdownBounds.y() + this.dropdownBounds.height()) {
                modeOption.clickableBehavior.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
                return;
            } else {
                modeOption.clickableBehavior.setDimensions(this.dropdownBounds.x(), fY, this.dropdownBounds.width(), a);
                drawOption(drawCtx, modeOption, fY, t != null && modeOption.option == t);
            }
        }
    }

    public void drawOption(DrawCtx drawCtx, ModeOption<T> modeOption, float f, boolean z) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(400).argb(), 0), colorStack115.computeColor(themePalette.surfaceBackground().tone(400).argb(), 100), modeOption.clickableBehavior().hoverAnimation()), colorStack115.computeColor(themePalette.surfaceBackground().tone(400).argb()), modeOption.currentOptionAnimation());
        float fX = this.dropdownBounds.x() + 5.0f;
        float fWidth = this.dropdownBounds.width() - 10.0f;
        drawCtx.fillRoundedRect(fX, f, fWidth, a, new Vector4f(b, b, b, b), iInterpolate);
        if (z) {
            drawCtx.textureVerticalC(this.checkmarkIcon, ((fX + fWidth) - 5.0f) - this.checkmarkIcon.width(), f + 11.5f, this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack115.computeColor(themePalette.text().tone(100).argb()));
        }
        T t = modeOption.option;
        if (t instanceof DisplayNamed) {
            drawCtx.text(this.valueFont, ((DisplayNamed) t).getDisplayName().effective(), 12, fX + 5.0f, (f + 11.5f) - Math.round(this.valueFont.getHeight(blockSpacing) / c), colorStack115.computeColor(themePalette.text().tone(z ? 100 : 500).argb()));
        }
    }

    public void updateBounds(float f, float f2) {
        float f3 = f + rowHeight + descGap;
        float fMethod018 = computeDropdownHeight();
        float fX = x();
        if (this.options.isEmpty() || (!this.expanded && this.expandAnimation.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
        } else {
            this.dropdownBounds.withPosition(fX, f3).withSize(f2, fMethod018);
        }
        this.clickBehavior.setDimensions(x(), y(), f2, height());
    }

    public float computeDropdownHeight() {
        if (this.expandAnimation.isZero() && !this.expanded) {
            return 0.0f;
        }
        int size = this.options.size();
        return blockSpacing + (size * a) + (Math.max(0, size - 1) * c);
    }

    public boolean handleDropdownInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = false;
        for (int size = this.options.size() - 1; size >= 0; size--) {
            if (this.options.get(size).clickableBehavior().handleInput(inputEventContext, z || z2)) {
                z2 = true;
            }
        }
        if (!z && !z2 && (((inputEventContext.inputEvent() instanceof MouseButtonInput) || (inputEventContext.inputEvent() instanceof CursorMoveInput)) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height()))) {
            return true;
        }
        if (inputEventContext.inputEvent() instanceof ScrollInput) {
            closeDropdown();
            return true;
        }
        if (!z && (inputEventContext.inputEvent() instanceof MouseButtonInput)) {
            boolean zInArea = inputEventContext.inArea(this.clickBehavior.ownerX(), this.clickBehavior.ownerY(), this.clickBehavior.ownerW(), this.clickBehavior.ownerH());
            boolean zInArea2 = inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height());
            if (!zInArea && !zInArea2) {
                closeDropdown();
                return true;
            }
        }
        return z2;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
        this.expandAnimation.state(this.expanded);
    }

    public void closeDropdown() {
        if (this.expanded || !this.expandAnimation.isZero()) {
            this.expanded = false;
            this.expandAnimation.state(false);
        }
    }

    public void invalidateLayout() {
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescription = null;
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            clearDescription(f);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionCached(f, strEffective)) {
            return;
        }
        wrapDescription(f, strEffective == null ? "" : strEffective);
    }

    public void clearDescription(float f) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.cachedWidth = f;
        this.cachedDescription = null;
    }

    public boolean isDescriptionCached(float f, String str) {
        return !this.descriptionDirty && f == this.cachedWidth && str != null && str.equals(this.cachedDescription);
    }

    public void wrapDescription(float f, String str) {
        this.wrappedDescription = StringUtil.formatTextToFitWidth(str, f, this.descriptionFont, descTextSize);
        this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descTextSize);
        this.cachedWidth = f;
        this.cachedDescription = str;
        this.descriptionDirty = false;
    }

    public Translation name() {
        return this.name;
    }

    public ModeSettingElement<T> currentValueSupplier(Supplier<T> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public ModeSettingElement<T> onChangeCallback(Consumer<T> consumer) {
        this.changeCallback = consumer;
        return this;
    }

    public ModeSettingElement<T> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }
}
