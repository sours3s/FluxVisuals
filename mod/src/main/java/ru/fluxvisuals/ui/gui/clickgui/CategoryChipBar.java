package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ModuleCategory;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PixelPoint;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CategoryChipBar extends WidgetContainer {
    public static final float chipHeight = 27.0f;
    public static final float chipCornerRadius = 14.0f;
    public static final int labelFontSize = 12;
    public static final float iconSize = 12.0f;
    public static final float rowSpacing = 8.0f;
    public static final int iconTextGap = 12;
    public static final int columnCount = 2;
    public final MsdfFont labelFont = Fonts.INTER_SEMIBOLD.get();
    public final Map<ModuleCategory, CategoryChip> chipCache = new EnumMap(ModuleCategory.class);
    public final List<CategoryChip> chips = new ArrayList();

    @Override
    public void render(DrawCtx drawCtx) {
        ModuleTab moduleTab847 = FluxVisuals.INSTANCE.tabsController().current().moduleTab();
        if (ModuleCategory.categoriesForTab(moduleTab847).isEmpty()) {
            return;
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.colorStack();
        Set<ModuleCategory> setSelectedCategories = FluxVisuals.INSTANCE.tabsController().selectedCategories(moduleTab847);
        for (CategoryChip categoryChip : this.chips) {
            categoryChip.selected(setSelectedCategories.contains(categoryChip.category()));
            categoryChip.render(drawCtx, themePalette, colorStack115);
        }
        super.render(drawCtx);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) throws MatchException {
        if (super.handleInput(inputEventContext, z)) {
            return true;
        }
        ModuleTab moduleTab847 = FluxVisuals.INSTANCE.tabsController().current().moduleTab();
        if (ModuleCategory.categoriesForTab(moduleTab847).isEmpty()) {
            return z;
        }
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (inputEvent691 instanceof CursorMoveInput) {
            try {
                PixelPoint pixelPoint = ((CursorMoveInput) inputEvent691).mousePosition();
                float fX = pixelPoint.x();
                float fY = pixelPoint.y();
                for (CategoryChip categoryChip : this.chips) {
                    categoryChip.hoverAnimator.state(categoryChip.contains(fX, fY));
                }
            } catch (Throwable th) {
                throw new MatchException(th.toString(), th);
            }
        }
        if (inputEvent691 instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
            if (mouseButtonInput.action().press() && mouseButtonInput.button() == 0) {
                float fX2 = inputEventContext.logicalMousePosition().x();
                float fY2 = inputEventContext.logicalMousePosition().y();
                for (CategoryChip class788Var2 : this.chips) {
                    if (class788Var2.contains(fX2, fY2)) {
                        FluxVisuals.INSTANCE.tabsController().toggleCategory(moduleTab847, class788Var2.category());
                        return true;
                    }
                }
            }
        }
        return z;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        List<ModuleCategory> listCategoriesForTab = ModuleCategory.categoriesForTab(FluxVisuals.INSTANCE.tabsController().current().moduleTab());
        this.chips.clear();
        if (listCategoriesForTab.isEmpty()) {
            visible(false);
            return;
        }
        visible(true);
        float fX = x();
        float fY = y();
        float fWidth = width();
        int i = 0;
        for (int i2 = 0; i2 < listCategoriesForTab.size(); i2 += 2) {
            ModuleCategory moduleCategory = listCategoriesForTab.get(i2);
            CategoryChip categoryChip = this.chipCache.computeIfAbsent(moduleCategory, class672Var2 -> {
                return new CategoryChip(this, class672Var2);
            });
            float fTextWidthPhysical = 42.0f + layoutScaleContext.textWidthPhysical(this.labelFont, moduleCategory.displayName(), 12);
            float f = fY + (i * 35.0f);
            categoryChip.bounds(fX, f, fTextWidthPhysical, chipHeight);
            this.chips.add(categoryChip);
            if (i2 + 1 < listCategoriesForTab.size()) {
                ModuleCategory class672Var3 = listCategoriesForTab.get(i2 + 1);
                CategoryChip class788VarComputeIfAbsent2 = this.chipCache.computeIfAbsent(class672Var3, class672Var4 -> {
                    return new CategoryChip(this, class672Var4);
                });
                float fTextWidthPhysical2 = 42.0f + layoutScaleContext.textWidthPhysical(this.labelFont, class672Var3.displayName(), 12);
                float f2 = fX + fTextWidthPhysical + 5.0f;
                float f3 = fWidth - (f2 - fX);
                class788VarComputeIfAbsent2.bounds(f2, f, fTextWidthPhysical2, chipHeight);
                this.chips.add(class788VarComputeIfAbsent2);
            }
            i++;
        }
        setSize(width(), (i * chipHeight) + ((i - 1) * rowSpacing));
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        for (CategoryChip categoryChip : this.chips) {
            categoryChip.hoverAnimator.animate(weightedEngine);
            categoryChip.selectedAnimator.animate(weightedEngine);
        }
        super.animation(weightedEngine);
    }

    public boolean hasElements() {
        return !ModuleCategory.categoriesForTab(FluxVisuals.INSTANCE.tabsController().current().moduleTab()).isEmpty();
    }
}
