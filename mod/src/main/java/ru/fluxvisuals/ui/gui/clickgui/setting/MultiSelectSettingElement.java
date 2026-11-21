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
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.LanguageChangeEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MultiSelectOptionButton;
import ru.fluxvisuals.ui.gui.clickgui.ScrollArea;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.ScrollbarWidget;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.lang.Enum;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.joml.Quaternionf;

public class MultiSelectSettingElement<T extends Enum<T>> extends ModuleFrame {
    public static final float X = 140.0f;
    public static final int titleTextSize = 14;
    public static final int descTextSize = 13;
    public static final float Y = 3.0f;
    public static final float Z = 12.0f;
    public static final float aa = 25.0f;
    public static final float ab = 10.0f;
    public static final float ac = 15.0f;
    public static final float ad = 10.0f;
    public static final float ae = 22.0f;
    public static final float af = 15.0f;
    public final Translation name;
    public final Translation description;
    public final Supplier<Set<T>> selectedSupplier;
    public Supplier<Boolean> visibleCondition;
    public boolean expanded;
    public final ScrollbarWidget scrollbar;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final MsdfFont valueFont = Fonts.INTER_BOLD.get();
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator expandAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final GlTexture multiEnumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/multienum.png"));
    public final GlTexture arrowIcon = new GlTexture(new ClasspathResource("/icons/menu/new/arrow.png"));
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<MultiSelectOptionButton<T>> options = new ArrayList();
    public final WidgetBounds boxBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float cachedWidth = -1.0f;

    public String cachedDescription = null;
    public float descriptionHeight = 0.0f;
    public final ScrollArea scrollArea = new ScrollArea();
    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescription = null;
    };

    public MultiSelectSettingElement(Translation translation, Translation class254Var2, T[] tArr, Supplier<Set<T>> supplier, Consumer<T> consumer) {
        ScrollArea scrollArea789 = this.scrollArea;
        Objects.requireNonNull(scrollArea789);
        Supplier supplier2 = scrollArea789::scrollY;
        Supplier supplier3 = this::computeContentHeight;
        ScrollArea class789Var2 = this.scrollArea;
        Objects.requireNonNull(class789Var2);
        Supplier supplier4 = class789Var2::height;
        ScrollArea class789Var3 = this.scrollArea;
        Objects.requireNonNull(class789Var3);
        this.scrollbar = new ScrollbarWidget(supplier2, supplier3, supplier4, (v1) -> {
            scrollArea789.scrollTo(v1);
        }, 4.0f, 4.0f, 30.0f);
        this.name = translation;
        this.description = class254Var2;
        this.selectedSupplier = supplier;
        for (T t : tArr) {
            this.options.add(new MultiSelectOptionButton<>(t));
        }
        Set<T> set = supplier.get();
        for (MultiSelectOptionButton<T> multiSelectOptionButton : this.options) {
            multiSelectOptionButton.toggleAnimation().force(set.contains(multiSelectOptionButton.option()));
            multiSelectOptionButton.clickableBehavior().clickCallback(() -> {
                boolean z = !((Set) supplier.get()).contains(multiSelectOptionButton.option());
                consumer.accept(multiSelectOptionButton.option());
                multiSelectOptionButton.toggleAnimation().state(z);
            });
        }
        this.clickBehavior.clickCallback(this::invert);
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
        drawCtx.text(this.titleFont, this.name.effective(), titleTextSize, x(), fY, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), fValue));
        float f3 = 0.0f;
        if (this.wrappedDescription != null) {
            fY += this.titleFont.getHeight(14.0f) + Y;
            drawCtx.text(this.descriptionFont, this.wrappedDescription, descTextSize, x(), fY, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), fValue));
            f3 = this.descriptionHeight;
        }
        float height = fY + (this.description != null ? f3 : this.titleFont.getHeight(14.0f)) + Z;
        float fMethod007 = computeContentHeight();
        this.boxBounds.withPosition(x(), height).withSize(f, aa);
        drawCtx.fillOutlinedRoundedRect(x(), height, f, aa, 6.0f, 2.5f, colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.clickBehavior.hoverAnimation()), colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.clickBehavior.hoverAnimation()));
        float fX = x() + 10.0f;
        drawCtx.texture(this.enumIcon, fX, (height + 12.5f) - Math.round(this.enumIcon.height() / 2.0f), this.enumIcon.width(), this.enumIcon.height(), colorStack115.computeColor(themePalette.text().tone(500).argb()));
        float fX2 = ((x() + f) - 10.0f) - this.arrowIcon.width();
        float fRound = (height + 12.5f) - Math.round(this.arrowIcon.height() / 2.0f);
        float fWidth = fX2 + (this.arrowIcon.width() / 2.0f);
        float fHeight = fRound + (this.arrowIcon.height() / 2.0f);
        float fSmoothAnimation = (float) (3.141592653589793d * ((double) this.expandAnimation.smoothAnimation()));
        float physical = drawCtx.layoutContext().toPhysical(fWidth);
        float physical2 = drawCtx.layoutContext().toPhysical(fHeight);
        drawCtx.matrixStack().push();
        drawCtx.matrixStack().translate(physical, physical2, 0.0f);
        drawCtx.matrixStack().multiply(new Quaternionf().rotateZ(fSmoothAnimation));
        drawCtx.matrixStack().translate(-physical, -physical2, 0.0f);
        drawCtx.texture(this.arrowIcon, fX2, fRound, this.arrowIcon.width(), this.arrowIcon.height(), colorStack115.computeColor(themePalette.text().tone(600).argb()));
        drawCtx.matrixStack().pop();
        if (this.selectedSupplier == null) {
            return;
        }
        Set<T> set = this.selectedSupplier.get();
        drawCtx.texture(this.multiEnumIcon, (x() + f) - this.multiEnumIcon.width(), y(), this.multiEnumIcon.width(), this.multiEnumIcon.height(), colorStack115.computeColor(themePalette.text().tone(set.isEmpty() ? 700 : 400).argb()));
        String strEffective = set.isEmpty() ? Lang.NOTHING_SELECTED.effective() : (String) set.stream().map(r2 -> {
            return r2 instanceof DisplayNamed ? ((DisplayNamed) r2).getDisplayName().effective() : r2.name();
        }).collect(Collectors.joining(", "));
        float fWidth2 = fX + this.enumIcon.width() + 6.0f;
        int iComputeColor = colorStack115.computeColor(set.isEmpty() ? themePalette.text().tone(500).argb() : themePalette.text().tone(300).argb());
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
            drawCtx.textWithHorizontalGradient(this.valueFont, str, 12, fWidth2, (height + 12.5f) - Math.round(this.valueFont.getHeight(Z) / 2.0f), iComputeColor, i);
        } else {
            drawCtx.text(this.valueFont, strEffective, 12, fWidth2, (height + 12.5f) - Math.round(this.valueFont.getHeight(Z) / 2.0f), iComputeColor);
        }
        float f5 = height + aa + Y;
        if (this.options.isEmpty() || (!this.expanded && this.expandAnimation.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
        } else {
            this.dropdownBounds.withPosition(x(), f5).withSize(f, Math.min(fMethod007, X));
        }
        this.clickBehavior.setDimensions(x(), y(), f, height());
    }

    @Override
    public void drawOverlay(DrawCtx drawCtx) {
        if (this.options.isEmpty()) {
            return;
        }
        if (!this.expandAnimation.isZero() || this.expanded) {
            ThemePalette themePalette = drawCtx.theme().palette();
            ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
            int iComputeColor = colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb());
            int iComputeColor2 = colorStack115.computeColor(themePalette.accent().argb());
            int iComputeColor3 = colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb());
            int iComputeColor4 = colorStack115.computeColor(themePalette.accentBright().argb());
            int iComputeColor5 = colorStack115.computeColor(themePalette.surfaceBackground().tone(200).argb());
            int iWhite = colorStack115.white();
            colorStack115.push();
            colorStack115.alpha(this.expandAnimation.smoothAnimation());
            float fY = this.dropdownBounds.y();
            float fHeight = this.dropdownBounds.height();
            float fWidth = this.dropdownBounds.width();
            drawCtx.fillOutlinedRoundedRect(this.dropdownBounds.x(), fY, fWidth, fHeight, 7.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()));
            this.scrollArea.beginArea(drawCtx, this.dropdownBounds.x(), this.dropdownBounds.y() + 10.0f, this.dropdownBounds.width(), this.dropdownBounds.height() - 10.0f);
            float f = fY + 10.0f;
            float fVisibleTop = f + this.scrollArea.visibleTop();
            float fVisibleBottom = f + this.scrollArea.visibleBottom();
            int i = 0;
            while (i < this.options.size()) {
                MultiSelectOptionButton<T> multiSelectOptionButton = this.options.get(i);
                ClickableBehavior clickableBehavior766 = ((MultiSelectOptionButton) multiSelectOptionButton).clickableBehavior;
                float f2 = fY + 10.0f + (i * aa);
                if (f2 + 15.0f < fVisibleTop || f2 > fVisibleBottom) {
                    ((MultiSelectOptionButton) multiSelectOptionButton).clickableBehavior.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
                } else {
                    float fX = ((this.dropdownBounds.x() + fWidth) - (needsScrollbar() ? 20.0f : 10.0f)) - ae;
                    ((MultiSelectOptionButton) multiSelectOptionButton).clickableBehavior.setDimensions(this.dropdownBounds.x(), f2, fWidth, 15.0f + (i == this.options.size() - 1 ? 0.0f : 10.0f));
                    ToggleAnimator toggleAnimator = multiSelectOptionButton.toggleAnimation();
                    drawCtx.fillOutlinedRoundedRect(fX, f2, ae, 15.0f, 8.0f, 2.5f, colorStack115.toggleDarkenedInterpolateColor(iComputeColor3, iComputeColor4, 0.05f, clickableBehavior766.hoverAnimation(), toggleAnimator), colorStack115.toggleDarkenedInterpolateColor(iComputeColor, iComputeColor2, 0.05f, clickableBehavior766.hoverAnimation(), toggleAnimator));
                    float f3 = (15.0f - (2.0f * Y)) / 2.0f;
                    drawCtx.circle(fX + Y + f3 + (((ae - (2.0f * Y)) - (2.0f * f3)) * toggleAnimator.smoothAnimation()), f2 + 7.5f, f3, colorStack115.interpolate(iComputeColor5, iWhite, toggleAnimator));
                    int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(100).argb()), toggleAnimator);
                    T t = (T) (((MultiSelectOptionButton) multiSelectOptionButton).option);
                    if (t instanceof DisplayNamed) {
                        drawCtx.text(this.valueFont, ((DisplayNamed) t).getDisplayName().effective(), 12, this.dropdownBounds.x() + 5.0f + 5.0f, (f2 + 7.5f) - Math.round(this.valueFont.getHeight(Z) / 2.0f), iInterpolate);
                    }
                }
                i++;
            }
            this.scrollArea.endArea(drawCtx, computeContentHeight() - 10.0f);
            if (needsScrollbar()) {
                this.scrollbar.render(drawCtx);
            }
            colorStack115.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.enumIcon.setDimensions(12, 12);
        this.arrowIcon.setDimensions(12, 12);
        this.multiEnumIcon.setDimensions(15, 15);
        this.scrollArea.layout(layoutScaleContext);
        if (needsScrollbar()) {
            this.scrollbar.layout(layoutScaleContext);
            this.scrollbar.setSize(4.0f, Math.min(computeContentHeight(), X));
            this.scrollbar.setPosition(((this.dropdownBounds.x() + this.dropdownBounds.width()) - 10.0f) - this.scrollbar.width(), (this.dropdownBounds.y() + 10.0f) - 4.0f);
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibleCondition != null) {
            visible(this.visibleCondition.get().booleanValue());
        }
        this.expandAnimation.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        if (needsScrollbar()) {
            this.scrollbar.animation(weightedEngine);
        }
        this.highlightAnimation.animate(weightedEngine);
        for (MultiSelectOptionButton<T> multiSelectOptionButton : this.options) {
            multiSelectOptionButton.clickableBehavior().animate(weightedEngine);
            multiSelectOptionButton.toggleAnimation().animate(weightedEngine);
        }
        Set<T> setOf = this.selectedSupplier != null ? this.selectedSupplier.get() : Set.of();
        for (MultiSelectOptionButton<T> class837Var2 : this.options) {
            class837Var2.toggleAnimation().state(setOf.contains(class837Var2.option()));
            class837Var2.toggleAnimation().animate(weightedEngine);
            class837Var2.clickableBehavior().animate(weightedEngine);
        }
        this.scrollArea.animation(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = false;
        if (this.expanded) {
            z2 = this.scrollArea.handleInput(inputEventContext, z);
            if (needsScrollbar() && this.scrollbar.handleInput(inputEventContext, z)) {
                z2 = true;
            }
            InputEventContext inputEventContext688 = inputEventContext.withMouseOffset(0.0f, this.scrollArea.scrollY());
            for (int size = this.options.size() - 1; size >= 0; size--) {
                if (this.options.get(size).clickableBehavior().handleInput(inputEventContext688, z || z2)) {
                    z2 = true;
                }
            }
            if (!z && !z2) {
                if ((inputEventContext.inputEvent() instanceof MouseButtonInput) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
                if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
            }
            if ((inputEventContext.inputEvent() instanceof ScrollInput) && !inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
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
        }
        if (this.clickBehavior.handleInput(inputEventContext, z || z2)) {
            z2 = true;
        }
        return z2;
    }

    @Override
    public void handleClose() {
        this.scrollArea.scrollTo(0.0f);
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
            height += Y + this.descriptionHeight;
        }
        return height + 37.0f;
    }

    public float computeContentHeight() {
        if (this.expandAnimation.isZero() && !this.expanded) {
            return 0.0f;
        }
        int size = this.options.size();
        return 20.0f + (size * 15.0f) + (Math.max(0, size - 1) * 10.0f);
    }

    public void closeDropdown() {
        if (this.expanded || !this.expandAnimation.isZero()) {
            this.expanded = false;
            this.expandAnimation.state(false);
            this.scrollArea.scrollTo(0.0f);
        }
    }

    public boolean needsScrollbar() {
        return computeContentHeight() > X;
    }

    public void invert() {
        this.expanded = !this.expanded;
        this.expandAnimation.state(this.expanded);
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.cachedWidth = f;
            this.cachedDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.cachedWidth || strEffective == null || !strEffective.equals(this.cachedDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descriptionFont, descTextSize);
            this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descTextSize);
            this.cachedWidth = f;
            this.cachedDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    public Translation name() {
        return this.name;
    }

    public MultiSelectSettingElement<T> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }
}
