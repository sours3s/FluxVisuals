package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MultiSelectOption;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MultiSelectOptionItem;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.joml.Quaternionf;

public class MultiSelectDropdown<T> extends AbstractWidget {
    public boolean initialized;
    public final float dropdownWidth;
    public final MsdfFont font;
    public final GlTexture icon;
    public final float cornerRadius;
    public final float paddingX;
    public final float paddingY;
    public final int iconSize;
    public Consumer<Set<T>> selectCallback;
    public boolean open;
    public static final float optionHeight = 23.0f;
    public static final float optionPadding = 6.0f;
    public static final float rowSpacing = 2.0f;
    public final AnimatedFloat widthAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final AnimatedFloat heightAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<MultiSelectOptionItem<T>> optionItems = new ArrayList();
    public final MsdfFont optionFont = Fonts.INTER_BOLD.get();
    public final GlTexture arrowIcon = new GlTexture(new ClasspathResource("/icons/menu/new/arrow.png")).setDimensions(8, 5);

    public final WidgetBounds headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public String text = "";
    public int textSize = 12;

    public final Set<T> selectedValues = new HashSet();
    public final ToggleAnimator openAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);

    public MultiSelectDropdown(MsdfFont msdfFont, GlTexture glTexture, float f, float f2, float f3, int i, float f4) {
        this.font = msdfFont;
        this.icon = glTexture;
        this.cornerRadius = f;
        this.paddingX = f2;
        this.paddingY = f3;
        this.iconSize = i;
        this.dropdownWidth = f4;
        this.clickBehavior.clickCallback(this::toggle);
    }

    public void onSelect(Consumer<Set<T>> consumer) {
        this.selectCallback = consumer;
    }

    public void setOptions(List<MultiSelectOption<T>> list, Set<T> set) {
        this.optionItems.clear();
        this.selectedValues.clear();
        for (MultiSelectOption<T> multiSelectOption : list) {
            MultiSelectOptionItem<T> multiSelectOptionItem = new MultiSelectOptionItem<>(multiSelectOption, () -> {
                return Boolean.valueOf(this.selectedValues.contains(multiSelectOption.value()));
            });
            multiSelectOptionItem.clickableBehavior().clickCallback(() -> {
                toggleSelection(multiSelectOptionItem.option().value(), true);
            });
            multiSelectOptionItem.toggleSwitch().runnable(() -> {
                toggleSelection(multiSelectOptionItem.option().value(), true);
            });
            this.optionItems.add(multiSelectOptionItem);
        }
        if (set == null || set.isEmpty()) {
            this.selectedValues.addAll(list.stream().limit(1L).map((v0) -> {
                return v0.value();
            }).toList());
        } else {
            this.selectedValues.addAll(set);
        }
        ensureSelection();
        updateText();
    }

    public void label(String str, int i) {
        this.text = str;
        this.textSize = i;
    }

    public void toggle() {
        this.open = !this.open;
    }

    public void toggleSelection(T t, boolean z) {
        if (this.optionItems.isEmpty()) {
            return;
        }
        if (!this.selectedValues.contains(t)) {
            this.selectedValues.add(t);
        } else if (this.selectedValues.size() > 1) {
            this.selectedValues.remove(t);
        }
        ensureSelection();
        updateText();
        if (!z || this.selectCallback == null) {
            return;
        }
        this.selectCallback.accept(Set.copyOf(this.selectedValues));
    }

    public void ensureSelection() {
        if (!this.selectedValues.isEmpty() || this.optionItems.isEmpty()) {
            return;
        }
        this.selectedValues.add(((MultiSelectOptionItem<T>) this.optionItems.getFirst()).option().value());
    }

    public void updateText() {
        this.text = (String) this.optionItems.stream().filter(multiSelectOptionItem -> {
            return this.selectedValues.contains(multiSelectOptionItem.option().value());
        }).map(class751Var2 -> {
            return class751Var2.option().label();
        }).collect(Collectors.joining(", "));
        if (!this.text.isBlank() || this.optionItems.isEmpty()) {
            return;
        }
        this.text = ((MultiSelectOptionItem) this.optionItems.getFirst()).option().label();
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fWidth = width();
        float fHeight = height();
        float fSmoothAnimation = this.openAnimation.smoothAnimation();
        float size = 12.0f + (optionHeight * this.optionItems.size()) + (rowSpacing * (this.optionItems.size() - 1));
        ToggleAnimator toggleAnimator323 = this.clickBehavior.hoverAnimation();
        ToggleAnimator toggleAnimator = this.openAnimation;
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb(), 0.8f), colorStack115.computeColor(themePalette.surfaceBackground().tone(800).argb(), 70), toggleAnimator323);
        int iComputeColor = colorStack115.computeColor(themePalette.text().tone(500).argb());
        int iComputeColor2 = colorStack115.computeColor(themePalette.text().tone(400).argb());
        int iInterpolate2 = colorStack115.interpolate(colorStack115.interpolate(iComputeColor, iComputeColor2, toggleAnimator, toggleAnimator), iComputeColor2, toggleAnimator323, toggleAnimator323);
        drawCtx.fillOutlinedRoundedRect(x(), y(), fWidth, fHeight, this.cornerRadius, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), iInterpolate);
        float fX = x() + this.paddingX;
        float fY = y() + (fHeight / rowSpacing);
        drawCtx.texture(this.icon, fX, fY - (this.iconSize / 2), this.iconSize, this.iconSize, colorStack115.computeColor(iInterpolate2));
        float fX2 = x() + this.paddingX + this.iconSize + 5.0f;
        float fY2 = (y() + (fHeight / rowSpacing)) - (this.font.getHeight(this.textSize) / rowSpacing);
        float fX3 = ((x() + fWidth) - this.paddingX) - 10.0f;
        float f = fY - (10.0f / rowSpacing);
        drawCtx.text(this.font, truncateText(drawCtx, this.font, this.text, this.textSize, Math.max(0.0f, (fX3 - optionPadding) - fX2)), this.textSize, fX2, fY2, iInterpolate2);
        float f2 = fX3 + (10.0f / rowSpacing);
        float f3 = f + (10.0f / rowSpacing);
        float fSmoothAnimation2 = (float) (3.141592653589793d * ((double) this.openAnimation.smoothAnimation()));
        drawCtx.matrixStack().push();
        drawCtx.matrixStack().translate(f2, f3, 0.0f);
        drawCtx.matrixStack().multiply(new Quaternionf().rotateZ(fSmoothAnimation2));
        drawCtx.matrixStack().translate(-f2, -f3, 0.0f);
        drawCtx.texture(this.arrowIcon, fX3, f, 10.0f, 10.0f, colorStack115.computeColor(themePalette.text().tone(600).argb()));
        drawCtx.matrixStack().pop();
        if (this.optionItems.isEmpty() || (!this.open && this.openAnimation.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
            return;
        }
        float fY3 = y() + fHeight + 3.0f + fSmoothAnimation;
        this.dropdownBounds.withPosition(x(), fY3).withSize(width(), size);
        colorStack115.push();
        colorStack115.alpha(fSmoothAnimation);
        drawCtx.fillOutlinedRoundedRect(x(), fY3, fWidth, size, 8.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb()));
        for (int i = 0; i < this.optionItems.size(); i++) {
            MultiSelectOptionItem<T> multiSelectOptionItem = this.optionItems.get(i);
            float f4 = fY3 + optionPadding + (i * 25.0f);
            int iInterpolate3 = colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(600).argb()), colorStack115.computeColor(themePalette.text().tone(500).argb()), multiSelectOptionItem.clickableBehavior().hoverAnimation()), colorStack115.computeColor(themePalette.text().tone(300).argb()), multiSelectOptionItem.currentOptionAnimation());
            float fX4 = x() + 5.0f;
            multiSelectOptionItem.toggleSwitch().render(drawCtx);
            drawCtx.text(this.optionFont, multiSelectOptionItem.option().label(), this.textSize, fX4 + 5.0f, (f4 + 11.5f) - (this.optionFont.getHeight(this.textSize) / rowSpacing), iInterpolate3);
        }
        colorStack115.pop();
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = false;
        if (this.open && !z && (inputEventContext.inputEvent() instanceof MouseButtonInput)) {
            boolean zInArea = inputEventContext.inArea(this.headerBounds.x(), this.headerBounds.y(), this.headerBounds.width(), this.headerBounds.height());
            boolean zInArea2 = inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height());
            if (!zInArea && !zInArea2) {
                close();
                return true;
            }
        }
        if (this.open) {
            for (int size = this.optionItems.size() - 1; size >= 0; size--) {
                MultiSelectOptionItem<T> multiSelectOptionItem = this.optionItems.get(size);
                if (multiSelectOptionItem.toggleSwitch().handleInput(inputEventContext, z || z2)) {
                    z2 = true;
                }
                if (multiSelectOptionItem.clickableBehavior().handleInput(inputEventContext, z || z2)) {
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
        float fMax = Math.max(this.iconSize, Math.max(this.font.metrics().lineHeight(), 10.0f)) + (this.paddingY * rowSpacing);
        float f = this.dropdownWidth;
        if (!this.initialized) {
            this.widthAnimation.set(f);
            this.heightAnimation.set(fMax);
            this.initialized = true;
        }
        this.widthAnimation.destination(f);
        this.heightAnimation.destination(fMax);
        setSize(this.widthAnimation.animatedValue(), this.heightAnimation.animatedValue());
        this.headerBounds.withPosition(x(), y()).withSize(width(), height());
        this.clickBehavior.setDimensions(x(), y(), width(), height());
        if (this.optionItems.isEmpty()) {
            return;
        }
        float fY = y() + height() + 3.0f;
        for (int i = 0; i < this.optionItems.size(); i++) {
            MultiSelectOptionItem<T> multiSelectOptionItem = this.optionItems.get(i);
            float f2 = fY + optionPadding + (i * 25.0f);
            multiSelectOptionItem.clickableBehavior().setDimensions(x(), f2, width(), optionHeight);
            multiSelectOptionItem.toggleSwitch().layout(layoutScaleContext);
            multiSelectOptionItem.toggleSwitch().setPosition(((x() + width()) - 9.0f) - multiSelectOptionItem.toggleSwitch().width(), ((f2 + 11.5f) - (multiSelectOptionItem.toggleSwitch().height() / rowSpacing)) + 1.0f);
            multiSelectOptionItem.toggleSwitch().layout(layoutScaleContext);
        }
    }

    public String truncateText(DrawCtx drawCtx, MsdfFont msdfFont, String str, int i, float f) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        if (drawCtx.textWidthPhysical(msdfFont, str, i) <= f) {
            return str;
        }
        if (drawCtx.textWidthPhysical(msdfFont, ".", i) > f) {
            return "";
        }
        int i2 = 0;
        int length = str.length();
        while (i2 < length) {
            int i3 = ((i2 + length) + 1) >>> 1;
            if (drawCtx.textWidthPhysical(msdfFont, str.substring(0, i3) + ".", i) <= f) {
                i2 = i3;
            } else {
                length = i3 - 1;
            }
        }
        return str.substring(0, i2) + ".";
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
        for (MultiSelectOptionItem<T> multiSelectOptionItem : this.optionItems) {
            multiSelectOptionItem.clickableBehavior().animate(weightedEngine);
            multiSelectOptionItem.toggleSwitch().animation(weightedEngine);
            multiSelectOptionItem.currentOptionAnimation.state(this.selectedValues.contains(multiSelectOptionItem.option().value()));
            multiSelectOptionItem.currentOptionAnimation.animate(weightedEngine);
        }
    }

    public List<MultiSelectOption<T>> options() {
        return this.optionItems.stream().map((v0) -> {
            return v0.option();
        }).toList();
    }

    public boolean isOpen() {
        return this.open;
    }
}
