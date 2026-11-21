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
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.type.SoundModeOption;
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

public class SoundModeSettingElement<T extends Enum<T>> extends ModuleFrame {
    public static final int titleFontSize = 14;
    public static final int descFontSize = 13;
    public static final float M = 3.0f;
    public static final float N = 12.0f;
    public static final float O = 25.0f;
    public final Translation name;
    public final Translation description;
    public boolean expanded;
    public Supplier<T> valueSupplier;
    public Consumer<T> changeConsumer;
    public Supplier<Boolean> visibleCondition;
    public static final float P = 23.0f;
    public static final float Q = 6.0f;
    public static final float R = 2.0f;
    public static final float S = 200.0f;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont valueFont = Fonts.INTER_BOLD.get();
    public final MsdfFont descFont = Fonts.INTER_MEDIUM.get();
    public final ToggleAnimator dropdownAnimator = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final GlTexture arrowIcon = new GlTexture(new ClasspathResource("/icons/menu/new/arrow.png"));
    public final GlTexture checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
    public final GlTexture audioIcon = new GlTexture(new ClasspathResource("/icons/menu/new/audio.png"));
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final List<SoundModeOption<T>> options = new ArrayList();
    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float lastLayoutWidth = -1.0f;
    public String lastDescription = null;

    public float descriptionHeight = 0.0f;

    public final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        this.descriptionDirty = true;
        this.lastLayoutWidth = -1.0f;
        this.lastDescription = null;
    };

    public SoundModeSettingElement(Translation translation, Translation class254Var2, T[] tArr, Consumer<T> consumer) {
        this.name = translation;
        this.description = class254Var2;
        for (T t : tArr) {
            this.options.add(new SoundModeOption<>(t));
        }
        for (SoundModeOption<T> soundModeOption : this.options) {
            soundModeOption.clickableBehavior().clickCallback(() -> {
                if (this.valueSupplier != null && soundModeOption.option != this.valueSupplier.get()) {
                    invert();
                }
                if (this.changeConsumer != null) {
                    this.changeConsumer.accept((T) ((Enum) soundModeOption.option));
                }
            });
            soundModeOption.soundClickableBehavior.clickCallback(() -> {
                if (consumer != null) {
                    consumer.accept(soundModeOption.option);
                }
            });
        }
        this.clickable.clickCallback(this::invert);
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
        float fValue = this.highlightAnimation.value();
        drawCtx.text(this.titleFont, this.name.effective(), titleFontSize, x(), fY, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), fValue));
        float f3 = 0.0f;
        if (this.wrappedDescription != null) {
            fY += this.titleFont.getHeight(14.0f) + M;
            drawCtx.text(this.descFont, this.wrappedDescription, descFontSize, x(), fY, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), fValue));
            f3 = this.descriptionHeight;
        }
        float height = fY + (this.description != null ? f3 : this.titleFont.getHeight(14.0f)) + N;
        drawCtx.fillOutlinedRoundedRect(x(), height, f, O, Q, 2.5f, colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.clickable.hoverAnimation()), colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.clickable.hoverAnimation()));
        float fX = x() + 10.0f;
        drawCtx.texture(this.enumIcon, fX, (height + 12.5f) - Math.round(this.enumIcon.height() / R), this.enumIcon.width(), this.enumIcon.height(), colorStack115.computeColor(themePalette.text().tone(500).argb()));
        float fX2 = ((x() + f) - 10.0f) - this.arrowIcon.width();
        float fRound = (height + 12.5f) - Math.round(this.arrowIcon.height() / R);
        float fWidth = fX2 + (this.arrowIcon.width() / R);
        float fHeight = fRound + (this.arrowIcon.height() / R);
        float fSmoothAnimation = (float) (3.141592653589793d * ((double) this.dropdownAnimator.smoothAnimation()));
        float physical = drawCtx.layoutContext().toPhysical(fWidth);
        float physical2 = drawCtx.layoutContext().toPhysical(fHeight);
        drawCtx.matrixStack().push();
        drawCtx.matrixStack().translate(physical, physical2, 0.0f);
        drawCtx.matrixStack().multiply(new Quaternionf().rotateZ(fSmoothAnimation));
        drawCtx.matrixStack().translate(-physical, -physical2, 0.0f);
        drawCtx.texture(this.arrowIcon, fX2, fRound, this.arrowIcon.width(), this.arrowIcon.height(), colorStack115.computeColor(themePalette.text().tone(600).argb()));
        drawCtx.matrixStack().pop();
        if (this.valueSupplier == null) {
            return;
        }
        T t = this.valueSupplier.get();
        drawCtx.texture(this.audioIcon, (x() + f) - 15.0f, y(), 15.0f, 15.0f, colorStack115.computeColor(themePalette.text().tone(400).argb()));
        if (t instanceof DisplayNamed) {
            DisplayNamed displayNamed = (DisplayNamed) t;
            float fWidth2 = fX + this.enumIcon.width() + Q;
            int iComputeColor = colorStack115.computeColor(themePalette.text().tone(300).argb());
            String strEffective = displayNamed.getDisplayName().effective();
            float f4 = (fX2 - 10.0f) - fWidth2;
            if (drawCtx.textWidthPhysical(this.valueFont, strEffective, 12) > f4) {
                int i = iComputeColor & 16777215;
                String str = "";
                for (int length = strEffective.length(); length > 0; length--) {
                    String strSubstring = strEffective.substring(0, length);
                    if (drawCtx.textWidthPhysical(this.valueFont, strSubstring, 12) <= f4) {
                        str = strSubstring;
                        break;
                    }
                }
                drawCtx.textWithHorizontalGradient(this.valueFont, str, 12, fWidth2, (height + 12.5f) - Math.round(this.valueFont.getHeight(N) / R), iComputeColor, i);
            } else {
                drawCtx.text(this.valueFont, strEffective, 12, fWidth2, (height + 12.5f) - Math.round(this.valueFont.getHeight(N) / R), iComputeColor);
            }
        }
        float f5 = height + O + M;
        float fMethod006 = computeDropdownHeight();
        if (this.options.isEmpty() || (!this.expanded && this.dropdownAnimator.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
        } else {
            this.dropdownBounds.withPosition(x(), f5).withSize(f, fMethod006);
        }
        this.clickable.setDimensions(x(), y(), f, height());
    }

    @Override
    public void drawOverlay(DrawCtx drawCtx) {
        if (this.options.isEmpty()) {
            return;
        }
        if (!this.dropdownAnimator.isZero() || this.expanded) {
            ThemePalette themePalette = drawCtx.theme().palette();
            ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
            colorStack115.push();
            colorStack115.alpha(this.dropdownAnimator.smoothAnimation());
            drawCtx.matrixStack().push();
            drawCtx.matrixStack().translate(0.0f, 0.0f, S);
            float fHeight = this.dropdownBounds.height();
            float fY = this.dropdownBounds.y();
            float fWidth = this.dropdownBounds.width();
            drawCtx.fillOutlinedRoundedRect(this.dropdownBounds.x(), fY, fWidth, fHeight, 7.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()));
            T t = this.valueSupplier == null ? null : this.valueSupplier.get();
            for (int i = 0; i < this.options.size(); i++) {
                SoundModeOption<T> soundModeOption = this.options.get(i);
                float f = fY + Q + (i * O);
                if (f + P > fY + fHeight) {
                    soundModeOption.clickableBehavior.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
                    break;
                }
                soundModeOption.clickableBehavior.setDimensions(this.dropdownBounds.x(), f, fWidth, P);
                boolean z = t != null && soundModeOption.option == t;
                int iInterpolate = colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(400).argb(), 0), colorStack115.computeColor(themePalette.surfaceBackground().tone(400).argb(), 100), soundModeOption.clickableBehavior().hoverAnimation()), colorStack115.computeColor(themePalette.surfaceBackground().tone(400).argb()), soundModeOption.currentOptionAnimation());
                float fX = this.dropdownBounds.x() + 5.0f;
                float f2 = fWidth - 10.0f;
                drawCtx.fillRoundedRect(fX, f, f2, P, new Vector4f(Q, Q, Q, Q), iInterpolate);
                float f3 = ((fX + f2) - 5.0f) - 14.0f;
                int iComputeColor = colorStack115.computeColor(themePalette.text().tone(600).argb());
                if (z) {
                    float fWidth2 = ((fX + f2) - 5.0f) - this.checkmarkIcon.width();
                    drawCtx.textureVerticalC(this.checkmarkIcon, fWidth2, f + 11.5f, this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack115.computeColor(themePalette.text().tone(100).argb()));
                    f3 = (fWidth2 - Q) - 14.0f;
                    iComputeColor = colorStack115.computeColor(themePalette.text().tone(100).argb());
                }
                soundModeOption.soundClickableBehavior.setDimensions(f3 - 4.0f, ((f + 11.5f) - (14.0f / R)) - 4.0f, 14.0f + (4.0f * R), 14.0f + (4.0f * R));
                drawCtx.textureVerticalC(this.audioIcon, f3, f + 11.5f, this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack115.interpolate(iComputeColor, colorStack115.computeColor(themePalette.text().tone(!z ? 300 : 50).argb()), soundModeOption.soundClickableBehavior.hoverAnimation()));
                T t2 = soundModeOption.option;
                if (t2 instanceof DisplayNamed) {
                    drawCtx.text(this.valueFont, ((DisplayNamed) t2).getDisplayName().effective(), 12, this.dropdownBounds.x() + 5.0f + 5.0f, (f + 11.5f) - Math.round(this.valueFont.getHeight(N) / R), colorStack115.computeColor(!z ? themePalette.text().tone(500).argb() : themePalette.text().tone(100).argb()));
                }
            }
            drawCtx.matrixStack().pop();
            colorStack115.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.enumIcon.setDimensions(12, 12);
        this.arrowIcon.setDimensions(12, 12);
        this.checkmarkIcon.setDimensions(titleFontSize, titleFontSize);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibleCondition != null) {
            visible(this.visibleCondition.get().booleanValue());
        }
        this.dropdownAnimator.animate(weightedEngine);
        this.clickable.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        for (SoundModeOption<T> soundModeOption : this.options) {
            soundModeOption.clickableBehavior().animate(weightedEngine);
            soundModeOption.soundClickableBehavior.animate(weightedEngine);
            if (this.valueSupplier != null) {
                soundModeOption.currentOptionAnimation.state(soundModeOption.option == this.valueSupplier.get());
                soundModeOption.currentOptionAnimation.animate(weightedEngine);
            }
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = false;
        if (this.expanded || !this.dropdownAnimator.isZero()) {
            for (int size = this.options.size() - 1; size >= 0; size--) {
                SoundModeOption<T> soundModeOption = this.options.get(size);
                boolean zHandleInput2 = zHandleInput | soundModeOption.soundClickableBehavior.handleInput(inputEventContext, z || zHandleInput);
                zHandleInput = zHandleInput2 | soundModeOption.clickableBehavior.handleInput(inputEventContext, z || zHandleInput2);
            }
            if (!z && !zHandleInput) {
                if ((inputEventContext.inputEvent() instanceof MouseButtonInput) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
                if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
            }
            if (inputEventContext.inputEvent() instanceof ScrollInput) {
                if (inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
                closeDropdown();
                return true;
            }
            if (!z && (inputEventContext.inputEvent() instanceof MouseButtonInput)) {
                boolean zInArea = inputEventContext.inArea(this.clickable.ownerX(), this.clickable.ownerY(), this.clickable.ownerW(), this.clickable.ownerH());
                boolean zInArea2 = inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height());
                if (!zInArea && !zInArea2) {
                    closeDropdown();
                    return true;
                }
            }
        }
        if (this.clickable.handleInput(inputEventContext, z || zHandleInput)) {
            zHandleInput = true;
        }
        return zHandleInput;
    }

    @Override
    public void handleClose() {
        closeDropdown();
        super.handleClose();
    }

    @Override
    public void handleViewportVisibility(float f, float f2, float f3) {
        if (this.expanded || !this.dropdownAnimator.isZero()) {
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
            height += M + this.descriptionHeight;
        }
        return height + 37.0f;
    }

    public float computeDropdownHeight() {
        if (this.dropdownAnimator.isZero() && !this.expanded) {
            return 0.0f;
        }
        int size = this.options.size();
        return N + (size * P) + (Math.max(0, size - 1) * R);
    }

    public void closeDropdown() {
        if (this.expanded || !this.dropdownAnimator.isZero()) {
            this.expanded = false;
            this.dropdownAnimator.state(false);
        }
    }

    public void invert() {
        this.expanded = !this.expanded;
        this.dropdownAnimator.state(this.expanded);
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

    public Translation name() {
        return this.name;
    }

    public SoundModeSettingElement<T> currentValueSupplier(Supplier<T> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public SoundModeSettingElement<T> onChangeCallback(Consumer<T> consumer) {
        this.changeConsumer = consumer;
        return this;
    }

    public SoundModeSettingElement<T> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }
}
