package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.type.DropdownOption;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DropdownOptionItem;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class DropdownWidget<T> extends AbstractWidget {
    public boolean initialized;
    public final MsdfFont labelFont;
    public final GlTexture iconTexture;
    public final float cornerRadius;
    public final float padding;
    public final float verticalPadding;
    public final int iconSize;
    public float panelWidth;
    public Consumer<T> onSelectCallback;
    public T selectedValue;
    public boolean open;
    public static final float OPTION_HEIGHT = 23.0f;
    public static final float OPTION_PADDING = 6.0f;
    public static final float TWO = 2.0f;
    public final AnimatedFloat widthAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final AnimatedFloat heightAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<DropdownOptionItem<T>> optionItems = new ArrayList();
    public final MsdfFont optionFont = Fonts.INTER_BOLD.get();
    public final GlTexture arrowTexture = new GlTexture(new ClasspathResource("/icons/menu/new/arrow.png")).setDimensions(8, 5);

    public final WidgetBounds headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds panelBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public Translation currentLabel = Translation.clearText("");
    public int labelSize = 12;
    public final ToggleAnimator openAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final GlTexture checkmarkTexture = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));

    public DropdownWidget(MsdfFont msdfFont, GlTexture glTexture, float f, float f2, float f3, int i) {
        this.labelFont = msdfFont;
        this.iconTexture = glTexture;
        this.cornerRadius = f;
        this.padding = f2;
        this.verticalPadding = f3;
        this.iconSize = i;
        this.clickBehavior.clickCallback(this::toggle);
    }

    public void onSelect(Consumer<T> consumer) {
        this.onSelectCallback = consumer;
    }

    public void setOptions(List<DropdownOption<T>> list, T t) {
        this.optionItems.clear();
        for (DropdownOption<T> dropdownOption : list) {
            DropdownOptionItem<T> dropdownOptionItem = new DropdownOptionItem<>(dropdownOption);
            dropdownOptionItem.clickableBehavior().clickCallback(() -> {
                applySelection(dropdownOption.value(), true);
            });
            this.optionItems.add(dropdownOptionItem);
        }
        applySelection(t, false);
    }

    public void select(T t) {
        applySelection(t, false);
    }

    public void label(Translation translation, int i) {
        this.currentLabel = translation;
        this.labelSize = i;
    }

    public void toggle() {
        this.open = !this.open;
    }

    public void applySelection(T t, boolean z) {
        if (this.optionItems.isEmpty()) {
            return;
        }
        DropdownOptionItem<T> dropdownOptionItem740 = this.optionItems.stream().filter(dropdownOptionItem -> {
            return Objects.equals(dropdownOptionItem.option().value(), t);
        }).findFirst().orElse((DropdownOptionItem) this.optionItems.getFirst());
        this.selectedValue = dropdownOptionItem740.option().value();
        this.currentLabel = dropdownOptionItem740.option().label();
        if (!z || this.onSelectCallback == null) {
            return;
        }
        this.onSelectCallback.accept(dropdownOptionItem740.option().value());
        this.open = false;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fWidth = width();
        float fHeight = height();
        float fSmoothAnimation = this.openAnimation.smoothAnimation();
        float size = 12.0f + (OPTION_HEIGHT * this.optionItems.size()) + (TWO * (this.optionItems.size() - 1));
        ToggleAnimator toggleAnimator323 = this.clickBehavior.hoverAnimation();
        ToggleAnimator toggleAnimator = this.openAnimation;
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb(), 0.8f), colorStack115.computeColor(themePalette.surfaceBackground().tone(800).argb(), 70), toggleAnimator323);
        int iComputeColor = colorStack115.computeColor(themePalette.text().tone(500).argb());
        int iComputeColor2 = colorStack115.computeColor(themePalette.text().tone(400).argb());
        int iInterpolate2 = colorStack115.interpolate(colorStack115.interpolate(iComputeColor, iComputeColor2, toggleAnimator, toggleAnimator), iComputeColor2, toggleAnimator323, toggleAnimator323);
        drawCtx.fillOutlinedRoundedRect(x(), y(), fWidth, fHeight, this.cornerRadius, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), iInterpolate);
        float fX = x() + this.padding;
        float fY = y() + (fHeight / TWO);
        drawCtx.texture(this.iconTexture, fX, fY - (this.iconSize / 2), this.iconSize, this.iconSize, colorStack115.computeColor(iInterpolate2));
        float fX2 = x() + this.padding + this.iconSize + 5.0f;
        drawCtx.text(this.labelFont, this.currentLabel.effective(), this.labelSize, fX2, (y() + (fHeight / TWO)) - (this.labelFont.getHeight(this.labelSize) / TWO), iInterpolate2);
        float fTextWidthPhysical = fX2 + drawCtx.textWidthPhysical(this.labelFont, this.currentLabel.effective(), this.labelSize) + OPTION_PADDING;
        float f = fY - (10.0f / TWO);
        float f2 = fTextWidthPhysical + (10.0f / TWO);
        float f3 = f + (10.0f / TWO);
        float fSmoothAnimation2 = (float) (3.141592653589793d * ((double) this.openAnimation.smoothAnimation()));
        float physical = drawCtx.layoutContext().toPhysical(f2);
        float physical2 = drawCtx.layoutContext().toPhysical(f3);
        drawCtx.matrixStack().push();
        drawCtx.matrixStack().translate(physical, physical2, 0.0f);
        drawCtx.matrixStack().multiply(new Quaternionf().rotateZ(fSmoothAnimation2));
        drawCtx.matrixStack().translate(-physical, -physical2, 0.0f);
        drawCtx.texture(this.arrowTexture, fTextWidthPhysical, f, 10.0f, 10.0f, colorStack115.computeColor(themePalette.text().tone(600).argb()));
        drawCtx.matrixStack().pop();
        if (this.optionItems.isEmpty() || (!this.open && this.openAnimation.isZero())) {
            this.panelBounds.withSize(0.0f, 0.0f);
            return;
        }
        float fY2 = y() + fHeight + 3.0f + fSmoothAnimation;
        this.panelBounds.withPosition(x(), fY2).withSize(this.panelWidth, size);
        colorStack115.push();
        colorStack115.alpha(fSmoothAnimation);
        drawCtx.fillOutlinedRoundedRect(x(), fY2, this.panelWidth, size, 8.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb()));
        for (int i = 0; i < this.optionItems.size(); i++) {
            DropdownOptionItem<T> dropdownOptionItem = this.optionItems.get(i);
            float f4 = fY2 + OPTION_PADDING + (i * 25.0f);
            int iInterpolate3 = colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(600).argb()), colorStack115.computeColor(themePalette.text().tone(500).argb()), dropdownOptionItem.clickableBehavior().hoverAnimation()), colorStack115.computeColor(themePalette.text().tone(300).argb()), dropdownOptionItem.currentOptionAnimation());
            float fX3 = x() + 5.0f;
            float f5 = this.panelWidth - 10.0f;
            drawCtx.fillRoundedRect(fX3, f4, f5, OPTION_HEIGHT, new Vector4f(OPTION_PADDING, OPTION_PADDING, OPTION_PADDING, OPTION_PADDING), colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb(), 0), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb(), 100), dropdownOptionItem.clickableBehavior().hoverAnimation()), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), dropdownOptionItem.currentOptionAnimation()));
            if (Objects.equals(dropdownOptionItem.option().value(), this.selectedValue)) {
                drawCtx.textureVerticalC(this.checkmarkTexture, ((fX3 + f5) - 5.0f) - this.checkmarkTexture.width(), f4 + 11.5f, this.checkmarkTexture.width(), this.checkmarkTexture.height(), colorStack115.computeColor(themePalette.text().tone(100).argb()));
            }
            drawCtx.text(this.optionFont, dropdownOptionItem.option().label().effective(), this.labelSize, fX3 + 5.0f, (f4 + 11.5f) - (this.optionFont.getHeight(this.labelSize) / TWO), iInterpolate3);
        }
        colorStack115.pop();
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = false;
        if (this.open && !z && (inputEventContext.inputEvent() instanceof MouseButtonInput)) {
            boolean zInArea = inputEventContext.inArea(this.headerBounds.x(), this.headerBounds.y(), this.headerBounds.width(), this.headerBounds.height());
            boolean zInArea2 = inputEventContext.inArea(this.panelBounds.x(), this.panelBounds.y(), this.panelBounds.width(), this.panelBounds.height());
            if (!zInArea && !zInArea2) {
                close();
                return true;
            }
        }
        if (this.open) {
            for (int size = this.optionItems.size() - 1; size >= 0; size--) {
                if (this.optionItems.get(size).clickableBehavior().handleInput(inputEventContext, z || z2)) {
                    z2 = true;
                }
            }
        }
        if (this.clickBehavior.handleInput(inputEventContext, z || z2)) {
            z2 = true;
        }
        return z2;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        float fTextWidthPhysical = layoutScaleContext.textWidthPhysical(this.labelFont, this.currentLabel.effective(), this.labelSize);
        float fMax = Math.max(this.iconSize, Math.max(this.labelFont.metrics().lineHeight(), 10.0f)) + (this.verticalPadding * TWO);
        float f = this.iconSize + 5.0f + fTextWidthPhysical + OPTION_PADDING + 10.0f + (this.padding * TWO);
        if (!this.initialized) {
            this.widthAnimation.set(f);
            this.heightAnimation.set(fMax);
            this.initialized = true;
        }
        this.widthAnimation.destination(f);
        this.heightAnimation.destination(fMax);
        setSize(this.widthAnimation.animatedValue(), this.heightAnimation.animatedValue());
        this.panelWidth = computePanelWidth(layoutScaleContext);
        this.headerBounds.withPosition(x(), y()).withSize(width(), height());
        this.clickBehavior.setDimensions(x(), y(), width(), height());
        this.checkmarkTexture.setDimensions(14, 14);
        if (this.optionItems.isEmpty()) {
            return;
        }
        float fY = y() + height() + 3.0f;
        for (int i = 0; i < this.optionItems.size(); i++) {
            this.optionItems.get(i).clickableBehavior().setDimensions(x(), fY + OPTION_PADDING + (i * 25.0f), this.panelWidth, OPTION_HEIGHT);
        }
    }

    @Override
    public void handleClose() {
        close();
        super.handleClose();
    }

    public void close() {
        if (this.open || !this.openAnimation.isZero()) {
            this.open = false;
            this.openAnimation.state(false);
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickBehavior.animate(weightedEngine);
        this.openAnimation.state(this.open).animate(weightedEngine);
        this.widthAnimation.animate(weightedEngine);
        this.heightAnimation.animate(weightedEngine);
        for (DropdownOptionItem<T> dropdownOptionItem : this.optionItems) {
            dropdownOptionItem.clickableBehavior().animate(weightedEngine);
            if (this.selectedValue != null) {
                dropdownOptionItem.currentOptionAnimation.state(Objects.equals(dropdownOptionItem.option().value(), this.selectedValue));
                dropdownOptionItem.currentOptionAnimation.animate(weightedEngine);
            }
        }
    }

    public float computePanelWidth(LayoutScaleContext layoutScaleContext) {
        float fMax = 0.0f;
        Iterator<DropdownOptionItem<T>> it = this.optionItems.iterator();
        while (it.hasNext()) {
            fMax = Math.max(fMax, layoutScaleContext.textWidthPhysical(this.optionFont, it.next().option().label().effective(), this.labelSize));
        }
        return Math.max(width(), fMax + this.checkmarkTexture.width() + 25.0f);
    }

    public List<DropdownOption<T>> options() {
        return this.optionItems.stream().map((v0) -> {
            return v0.option();
        }).toList();
    }

    public boolean isOpen() {
        return this.open;
    }
}
