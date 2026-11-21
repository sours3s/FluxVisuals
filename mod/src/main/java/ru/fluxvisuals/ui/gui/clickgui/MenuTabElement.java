package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.setting.ButtonSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.CheckboxSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.setting.ColorSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.FrameDimmingController;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.setting.GroupSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.setting.ModeSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ModuleCategory;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.setting.MultiSelectSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.setting.SliderSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.SoundModeSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.TextFieldSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.net.TooltipService;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.joml.Vector4f;

public final class MenuTabElement extends Widget {
    public final MsdfFont SEMI_BOLD;
    public final ModuleTab moduleTab;
    public GlTexture icon;
    public final String iconName;
    public final Translation name;
    public final ClickableBehavior clickableBehavior;
    public final ToggleAnimator currentTabAnimation;
    public final List<AbstractFrame> frames;

    public final ScrollArea scrollingAreaComponent;
    public float framesOriginX;
    public float framesOriginY;
    public boolean framesLayoutDirty;
    public final Map<AbstractFrame, Float> frameHeights;
    public final FrameDimmingController dimmingManager;
    public AbstractFrame pendingScrollTarget;
    public int pendingScrollDelay;
    public boolean forceImmediateFramePositioning;
    public Set<ModuleCategory> activeCategories;
    public boolean active;
    public boolean inactiveTooltipShown;

    public TabLayout renderStrategy;
    public volatile boolean pendingFramesUpdate;
    public List<AbstractFrame> pendingNewFrames;
    public static final float inactiveAlpha = 0.7f;
    public static final Translation comingSoonText = Translation.clearText("Ð¡ÐºÐ¾Ñ€Ð¾ Ð¿Ð¾ÑÐ²Ð¸Ñ‚ÑÑ");

    public MenuTabElement(ModuleTab moduleTab847, Translation translation, String str, TabLayout tabLayout) {
        this.SEMI_BOLD = Fonts.INTER_SEMIBOLD.get();
        this.clickableBehavior = new ClickableBehavior();
        this.currentTabAnimation = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
        this.frames = new ArrayList();
        this.scrollingAreaComponent = new ScrollArea();
        this.framesLayoutDirty = true;
        this.frameHeights = new HashMap();
        this.dimmingManager = new FrameDimmingController();
        this.pendingScrollDelay = 0;
        this.activeCategories = Set.of();
        this.active = true;
        this.inactiveTooltipShown = false;
        this.pendingFramesUpdate = false;
        this.pendingNewFrames = new ArrayList();
        this.moduleTab = moduleTab847;
        this.name = translation;
        this.iconName = str;
        this.renderStrategy = tabLayout;
        tabLayout.initialize(this);
        this.clickableBehavior.clickCallback(() -> {
            FluxVisuals.INSTANCE.tabsController().open(this);
        });
    }

    public MenuTabElement(ModuleTab moduleTab, Translation translation, String str) {
        this(moduleTab, translation, str, new ModuleGridLayout());
    }

    public void setRenderStrategy(TabLayout tabLayout) {
        this.renderStrategy = tabLayout;
        tabLayout.initialize(this);
        this.framesLayoutDirty = true;
    }

    public void setActive(boolean z) {
        this.active = z;
        if (z && this.inactiveTooltipShown) {
            TooltipService tooltipService736 = tooltipService();
            if (tooltipService736 != null) {
                tooltipService736.hide(this);
            }
            this.inactiveTooltipShown = false;
        }
    }

    public String title() {
        return this.name.effective();
    }

    public void drawCollapsed(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        colorStack115.push();
        colorStack115.alpha(this.active ? 1.0f : inactiveAlpha);
        int iInterpolate = colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(400).argb()), this.clickableBehavior.hoverAnimation()), colorStack115.computeColor(themePalette.accent().argb()), this.currentTabAnimation);
        GlTexture class073VarMethod003 = getIcon();
        drawCtx.texture(class073VarMethod003, x(), y(), class073VarMethod003.width(), class073VarMethod003.height(), iInterpolate);
        this.clickableBehavior.setDimensions((x() + (class073VarMethod003.width() / 2.0f)) - (30.0f / 2.0f), (y() + (class073VarMethod003.height() / 2.0f)) - (30.0f / 2.0f), 30.0f, 30.0f);
        updateTooltipAnchor(drawCtx);
        colorStack115.pop();
    }

    public void drawExpanded(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        colorStack115.push();
        colorStack115.alpha(this.active ? 1.0f : inactiveAlpha);
        int iInterpolate = colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb(), 0), colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb(), 150), this.clickableBehavior.hoverAnimation()), colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb()), this.currentTabAnimation);
        int iInterpolate2 = colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(400).argb()), this.clickableBehavior.hoverAnimation()), colorStack115.computeColor(themePalette.accent().argb()), this.currentTabAnimation);
        drawCtx.fillRoundedRect(x(), y(), expandedWidth(), expandedHeight(), new Vector4f(6.0f, 6.0f, 6.0f, 6.0f), iInterpolate);
        GlTexture class073VarMethod003 = getIcon();
        drawCtx.texture(class073VarMethod003, x() + 12.0f, (y() + (expandedHeight() / 2.0f)) - (class073VarMethod003.height() / 2), class073VarMethod003.width(), class073VarMethod003.height(), iInterpolate2);
        drawCtx.text(this.SEMI_BOLD, this.name.effective(), 12, x() + 12.0f + class073VarMethod003.width() + 8.0f, (y() + (expandedHeight() / 2.0f)) - (this.SEMI_BOLD.getHeight(12.0f) / 2.0f), iInterpolate2);
        this.clickableBehavior.setDimensions(x(), y(), expandedWidth(), expandedHeight());
        updateTooltipAnchor(drawCtx);
        colorStack115.pop();
    }

    public void drawFrames(DrawCtx drawCtx) {
        if (this.renderStrategy != null) {
            this.renderStrategy.render(drawCtx);
        }
    }

    public void renderFrameOverlays(DrawCtx drawCtx) {
        if (this.renderStrategy != null) {
            this.renderStrategy.renderOverlays(drawCtx);
        }
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        overlayCommandQueue.record(this::drawFrames);
        Iterator<AbstractFrame> it = this.frames.iterator();
        while (it.hasNext()) {
            it.next().collectBlurElements(overlayCommandQueue);
        }
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        Iterator<AbstractFrame> it = this.frames.iterator();
        while (it.hasNext()) {
            it.next().collectBloomElements(renderCommandQueue);
        }
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.frames.forEach(abstractFrame -> {
            abstractFrame.onMenuDrag(z);
        });
        super.onMenuDrag(z);
    }

    static int indexOfMin(float[] fArr) {
        int i = 0;
        float f = fArr[0];
        for (int i2 = 1; i2 < fArr.length; i2++) {
            if (fArr[i2] < f) {
                f = fArr[i2];
                i = i2;
            }
        }
        return i;
    }

    public void layout(LayoutScaleContext layoutScaleContext, float f, float f2) {
        if (this.framesOriginX != f || this.framesOriginY != f2) {
            this.framesOriginX = f;
            this.framesOriginY = f2;
            this.framesLayoutDirty = true;
            this.forceImmediateFramePositioning = true;
        }
        layoutFrames(layoutScaleContext);
    }

    public void layoutFrames(LayoutScaleContext layoutScaleContext) {
        if (this.renderStrategy != null) {
            this.renderStrategy.layout(layoutScaleContext);
        } else {
            this.frames.forEach(abstractFrame -> {
                abstractFrame.layout(layoutScaleContext);
            });
        }
        for (AbstractFrame class757Var2 : this.frames) {
            float fHeight = class757Var2.height() + class757Var2.contentHeight();
            Float f = this.frameHeights.get(class757Var2);
            if (f == null || Math.abs(f.floatValue() - fHeight) > 0.01f) {
                this.frameHeights.put(class757Var2, Float.valueOf(fHeight));
                this.framesLayoutDirty = true;
            }
        }
        if (this.framesLayoutDirty) {
            positionFrames();
            this.framesLayoutDirty = false;
            this.forceImmediateFramePositioning = false;
        }
        getIcon().setDimensions(14, 14);
    }

    public void positionFrames() {
        if (this.renderStrategy != null) {
            this.renderStrategy.positionFrames();
        }
    }

    public boolean handleInputFrames(InputEventContext inputEventContext, boolean z) {
        return this.renderStrategy != null ? this.renderStrategy.handleInput(inputEventContext, z) : z;
    }

    public float contentHeight() {
        if (this.renderStrategy != null) {
            return this.renderStrategy.getContentHeight();
        }
        return 0.0f;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        layoutFrames(layoutScaleContext);
    }

    public void markFramesDirty() {
        this.framesLayoutDirty = true;
    }

    public void focusFrame(AbstractFrame abstractFrame) {
        if (abstractFrame == null || !this.frames.contains(abstractFrame)) {
            return;
        }
        this.framesLayoutDirty = true;
        this.forceImmediateFramePositioning = true;
        this.pendingScrollTarget = abstractFrame;
        this.pendingScrollDelay = 2;
        if (abstractFrame instanceof ModuleCard) {
            ((ModuleCard) abstractFrame).highlight();
        }
        this.dimmingManager.focusFrame(abstractFrame);
    }

    public void focusSetting(AbstractFrame abstractFrame, Setting setting) {
        if (abstractFrame == null || !this.frames.contains(abstractFrame)) {
            return;
        }
        this.framesLayoutDirty = true;
        this.forceImmediateFramePositioning = true;
        this.pendingScrollTarget = abstractFrame;
        this.pendingScrollDelay = 2;
        this.dimmingManager.focusFrame(abstractFrame);
        FrameElementColumn frameElementColumn = abstractFrame.frameElementsContainer();
        if (frameElementColumn != null) {
            for (Widget widget : frameElementColumn.children()) {
                if (widget instanceof ModuleFrame) {
                    ModuleFrame moduleFrame = (ModuleFrame) widget;
                    if (matchesSetting(moduleFrame, setting)) {
                        abstractFrame.focusElement(moduleFrame);
                        return;
                    }
                }
            }
        }
    }

    static int typeSwitch00732(Object o) {
        if (o == null) return -1;
        if (o instanceof CheckboxSettingElement) return 0;
        if (o instanceof SliderSettingElement) return 1;
        if (o instanceof ModeSettingElement) return 2;
        if (o instanceof SoundModeSettingElement) return 3;
        if (o instanceof KeybindSettingElement) return 4;
        if (o instanceof MultiSelectSettingElement) return 5;
        if (o instanceof ColorSettingElement) return 6;
        if (o instanceof GroupSettingElement) return 7;
        if (o instanceof TextFieldSettingElement) return 8;
        if (o instanceof ButtonSettingElement) return 9;
        return 10;
    }

    public boolean matchesSetting(ModuleFrame moduleFrame, Setting setting) {
        String strEffective;
        Objects.requireNonNull(moduleFrame);
        switch (typeSwitch00732(moduleFrame)) {
            case 0:
                strEffective = ((CheckboxSettingElement) moduleFrame).name().effective();
                break;
            case 1:
                strEffective = ((SliderSettingElement) moduleFrame).name().effective();
                break;
            case 2:
                strEffective = ((ModeSettingElement) moduleFrame).name().effective();
                break;
            case 3:
                strEffective = ((SoundModeSettingElement) moduleFrame).name().effective();
                break;
            case 4:
                strEffective = ((KeybindSettingElement) moduleFrame).name().effective();
                break;
            case 5:
                strEffective = ((MultiSelectSettingElement) moduleFrame).name().effective();
                break;
            case 6:
                strEffective = ((ColorSettingElement) moduleFrame).name().effective();
                break;
            case 7:
                strEffective = ((GroupSettingElement) moduleFrame).name().effective();
                break;
            case 8:
                strEffective = ((TextFieldSettingElement) moduleFrame).name().effective();
                break;
            case 9:
                strEffective = ((ButtonSettingElement) moduleFrame).name().effective();
                break;
            default:
                strEffective = null;
                break;
        }
        String str = strEffective;
        return str != null && str.equals(setting.getName().effective());
    }

    public void newFrame(AbstractFrame abstractFrame) {
        this.frames.add(abstractFrame);
        updateFrameVisibility(abstractFrame);
        this.framesLayoutDirty = true;
    }

    public void applyCategoryFilter(Set<ModuleCategory> set) {
        this.activeCategories = set == null ? Set.of() : Set.copyOf(set);
        this.frames.forEach(this::updateFrameVisibility);
        this.framesLayoutDirty = true;
    }

    public void updateFrameVisibility(AbstractFrame abstractFrame) {
        if (!(abstractFrame instanceof ModuleCard)) {
            abstractFrame.visible(true);
        } else {
            ModuleCard moduleCard = (ModuleCard) abstractFrame;
            ModuleTab tab = moduleCard.clickGuiModule.getModuleTab();
            abstractFrame.visible(this.activeCategories.isEmpty() || (tab != null && this.activeCategories.contains(tab)));
        }
    }

    public void scrollToPendingTarget() {
        if (this.pendingScrollTarget == null) {
            return;
        }
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT;
        float fY = this.pendingScrollTarget.y();
        if (fY < 0.1f) {
            this.pendingScrollDelay = 1;
            return;
        }
        this.scrollingAreaComponent.scrollTo(MathUtil.clamp(Math.max(0.0f, (fY - this.framesOriginY) - 10.0f), 0.0f, Math.max(0.0f, contentHeight() - f)));
        this.pendingScrollTarget = null;
        this.pendingScrollDelay = 0;
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        AbstractFrame abstractFrame;
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        boolean zHandleInput2 = zHandleInput | this.clickableBehavior.handleInput(inputEventContext, zHandleInput);
        updateInactiveTooltip(inputEventContext);
        if (this.dimmingManager.isDimming() && (abstractFrame = this.dimmingManager.focusedFrame()) != null) {
            InputEvent inputEvent691 = inputEventContext.inputEvent();
            if ((inputEvent691 instanceof MouseButtonInput) && ((MouseButtonInput) inputEvent691).action().press() && !inputEventContext.inArea(abstractFrame.x(), abstractFrame.y(), abstractFrame.width(), abstractFrame.height() + abstractFrame.contentHeight())) {
                clearHighlight();
            }
        }
        return zHandleInput2;
    }

    public void clearHighlight() {
        this.dimmingManager.clearFocus();
        for (AbstractFrame abstractFrame : this.frames) {
            if (abstractFrame instanceof ModuleCard) {
                ((ModuleCard) abstractFrame).highlightAnimation().clearHighlight();
            }
            abstractFrame.clearElementFocus();
        }
    }

    @Override
    public void handleClose() {
        this.frames.forEach((v0) -> {
            v0.handleClose();
        });
        clearHighlight();
        super.handleClose();
    }

    public void updateInactiveTooltip(InputEventContext inputEventContext) {
        TooltipService tooltipService736 = tooltipService();
        if (tooltipService736 == null) {
            return;
        }
        if (this.active) {
            if (this.inactiveTooltipShown) {
                this.inactiveTooltipShown = false;
                tooltipService736.hide(this);
                return;
            }
            return;
        }
        boolean zState = this.clickableBehavior.hoverAnimation().state();
        if (inputEventContext.inputEvent() instanceof CursorMoveInput) {
            if (zState && !this.inactiveTooltipShown) {
                this.inactiveTooltipShown = true;
                tooltipService736.show(this, tooltipBounds(), comingSoonText, null);
            } else {
                if (zState || !this.inactiveTooltipShown) {
                    return;
                }
                this.inactiveTooltipShown = false;
                tooltipService736.hide(this);
            }
        }
    }

    public void updateTooltipAnchor(DrawCtx drawCtx) {
        TooltipService tooltipService736 = tooltipService();
        if (!this.inactiveTooltipShown || this.active || tooltipService736 == null || !tooltipService736.isOwnedBy(this)) {
            return;
        }
        tooltipService736.updateAnchor(this, tooltipBounds(), drawCtx);
    }

    public TooltipService tooltipService() {
        MenuWindow menuWindow776 = FluxVisuals.INSTANCE.menuWindow();
        if (menuWindow776 == null) {
            return null;
        }
        return menuWindow776.tooltipService();
    }

    public WidgetBounds tooltipBounds() {
        return new WidgetBounds(this.clickableBehavior.ownerX(), this.clickableBehavior.ownerY() + 5.0f, this.clickableBehavior.ownerW(), this.clickableBehavior.ownerH());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.pendingFramesUpdate) {
            this.frames.clear();
            this.frames.addAll(this.pendingNewFrames);
            this.pendingNewFrames.clear();
            this.pendingFramesUpdate = false;
            this.framesLayoutDirty = true;
        }
        this.clickableBehavior.animate(weightedEngine);
        this.currentTabAnimation.state(this == FluxVisuals.INSTANCE.tabsController().current());
        this.currentTabAnimation.animate(weightedEngine);
        this.scrollingAreaComponent.animation(weightedEngine);
        float fVisibleTop = this.scrollingAreaComponent.visibleTop();
        float fVisibleBottom = this.scrollingAreaComponent.visibleBottom();
        for (AbstractFrame abstractFrame : this.frames) {
            if (abstractFrame.visible()) {
                abstractFrame.updateViewportVisibility(fVisibleTop, fVisibleBottom, this.framesOriginY);
            }
        }
        if (this.renderStrategy != null) {
            this.renderStrategy.animation(weightedEngine);
        }
        this.frames.forEach(class757Var2 -> {
            class757Var2.animation(weightedEngine);
        });
        this.dimmingManager.animate(weightedEngine);
        if (this.pendingScrollTarget != null && this.pendingScrollDelay > 0) {
            this.pendingScrollDelay--;
            if (this.pendingScrollDelay == 0) {
                scrollToPendingTarget();
            }
        }
        super.animation(weightedEngine);
    }

    public void updateFramesSafe(List<AbstractFrame> list) {
        this.pendingNewFrames.clear();
        this.pendingNewFrames.addAll(list);
        this.pendingFramesUpdate = true;
    }

    public float collapsedWidth() {
        return getIcon().width();
    }

    public float collapsedHeight() {
        return getIcon().height();
    }

    public float expandedHeight() {
        return 35.0f;
    }

    public float expandedWidth() {
        return 110.0f;
    }

    public GlTexture getIcon() {
        if (this.icon == null) {
            this.icon = new GlTexture(new ClasspathResource("/icons/menu/new/tabs/%s.png".formatted(this.iconName))).setDimensions(14, 14);
        }
        return this.icon;
    }

    @Override
    public float width() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float height() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void open() {
    }

    public MsdfFont SEMI_BOLD() {
        return this.SEMI_BOLD;
    }

    public ModuleTab moduleTab() {
        return this.moduleTab;
    }

    public GlTexture icon() {
        return this.icon;
    }

    public String iconName() {
        return this.iconName;
    }

    public Translation name() {
        return this.name;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator currentTabAnimation() {
        return this.currentTabAnimation;
    }

    public List<AbstractFrame> frames() {
        return this.frames;
    }

    public ScrollArea scrollingAreaComponent() {
        return this.scrollingAreaComponent;
    }

    public float framesOriginX() {
        return this.framesOriginX;
    }

    public float framesOriginY() {
        return this.framesOriginY;
    }

    public boolean framesLayoutDirty() {
        return this.framesLayoutDirty;
    }

    public Map<AbstractFrame, Float> frameHeights() {
        return this.frameHeights;
    }

    public FrameDimmingController dimmingManager() {
        return this.dimmingManager;
    }

    public AbstractFrame pendingScrollTarget() {
        return this.pendingScrollTarget;
    }

    public int pendingScrollDelay() {
        return this.pendingScrollDelay;
    }

    public boolean forceImmediateFramePositioning() {
        return this.forceImmediateFramePositioning;
    }

    public Set<ModuleCategory> activeCategories() {
        return this.activeCategories;
    }

    public boolean active() {
        return this.active;
    }

    public boolean inactiveTooltipShown() {
        return this.inactiveTooltipShown;
    }

    public boolean pendingFramesUpdate() {
        return this.pendingFramesUpdate;
    }

    public List<AbstractFrame> pendingNewFrames() {
        return this.pendingNewFrames;
    }

    public TabLayout renderStrategy() {
        return this.renderStrategy;
    }
}
