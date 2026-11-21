package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ConfigFilter;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ConfigOrigin;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MultiSelectOption;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.math.MathHelper;

public class ThemeTabLayout extends AbstractTabLayout {
    static final GlTexture importIcon = new GlTexture(new ClasspathResource("/icons/menu/new/click.png"));
    static final float ax = 30.0f;
    static final float ay = 12.0f;
    static final float az = 97.0f;
    static final float aA = 20.0f;
    static final float aB = 0.0f;

    public ScrollbarWidget scrollbar;
    public final GlTexture themesIcon = new GlTexture(new ClasspathResource("/icons/menu/new/tabs/themes.png"));
    public final GlTexture brushIcon = new GlTexture(new ClasspathResource("/icons/menu/new/brush.png"));
    public final GlTexture filterIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    public final ToggleAnimator panelAnimator = ToggleAnimator.times(2, 100);
    public final WidgetBounds headerBounds = new WidgetBounds(aB, aB, aB, 71.0f);
    public final IconLabelBadge titleBadge = new IconLabelBadge(this.brushIcon, Translation.clearText("Ð¦Ð²ÐµÑ‚Ð¾Ð²Ñ‹Ðµ Ñ‚ÐµÐ¼Ñ‹"));
    public final List<ThemeFilterIndicator> filterIndicators = List.of(new ThemeFilterIndicator(ConfigFilter.FAVORITE, "Ð˜Ð·Ð±Ñ€Ð°Ð½Ð½Ñ‹Ðµ Ñ‚ÐµÐ¼Ñ‹"), new ThemeFilterIndicator(ConfigFilter.OFFICIAL, "ÐžÑ„Ð¸Ñ†Ð¸Ð°Ð»ÑŒÐ½Ñ‹Ðµ Ñ‚ÐµÐ¼Ñ‹"), new ThemeFilterIndicator(ConfigFilter.USER, "ÐŸÐ¾Ð»ÑŒÐ·Ð¾Ð²Ð°Ñ‚ÐµÐ»ÑŒÑÐºÐ¸Ðµ Ñ‚ÐµÐ¼Ñ‹"));
    public final ThemeCreatorPanel creatorPanel = new ThemeCreatorPanel();
    public final List<LabeledIconButton> actionButtons = new ArrayList();

    public final MultiSelectDropdown<ConfigFilter> filterDropdown = new MultiSelectDropdown<>(this.font, this.filterIcon, 7.0f, 8.0f, 5.5f, 12, 244.0f);
    public final Set<ConfigFilter> activeFilters = EnumSet.allOf(ConfigFilter.class);
    public boolean filtersDirty = true;
    public int lastFrameCount = -1;

    public ThemeTabLayout() {
        this.filterDropdown.setOptions(List.of(new MultiSelectOption(ConfigFilter.FAVORITE, "Ð˜Ð·Ð±Ñ€Ð°Ð½Ð½Ñ‹Ðµ"), new MultiSelectOption(ConfigFilter.OFFICIAL, "ÐžÑ„Ð¸Ñ†Ð¸Ð°Ð»ÑŒÐ½Ñ‹Ðµ"), new MultiSelectOption(ConfigFilter.USER, "ÐŸÐ¾Ð»ÑŒÐ·Ð¾Ð²Ð°Ñ‚ÐµÐ»ÑŒÑÐºÐ¸Ðµ")), this.activeFilters);
        this.actionButtons.addAll(List.of(new LabeledIconButton(Translation.clearText("Ð¡Ð¾Ð·Ð´Ð°Ñ‚ÑŒ Ñ‚ÐµÐ¼Ñƒ"), () -> {
            if (this.creatorPanel.isOpened()) {
                this.creatorPanel.close();
            } else {
                this.creatorPanel.open();
            }
        }, this.font, this.themesIcon, 7.0f, 8.0f, 5.5f, 12), new LabeledIconButton(Lang.CONFIG_BUTTON_IMPORT, () -> {
        }, this.font, importIcon, 7.0f, 8.0f, 5.5f, 12)));
        this.filterDropdown.onSelect(set -> {
            this.activeFilters.clear();
            this.activeFilters.addAll(set);
            this.filtersDirty = true;
        });
    }

    @Override
    public void initialize(MenuTabElement menuTabElement) {
        super.initialize(menuTabElement);
        ScrollArea scrollArea = menuTabElement.scrollingAreaComponent();
        Objects.requireNonNull(scrollArea);
        Supplier supplier = scrollArea::scrollY;
        Supplier supplier2 = this::getContentHeight;
        Supplier supplier3 = () -> {
            return Float.valueOf(((MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT) - this.headerBounds.height()) - 16.0f);
        };
        ScrollArea class789VarScrollingAreaComponent2 = menuTabElement.scrollingAreaComponent();
        Objects.requireNonNull(class789VarScrollingAreaComponent2);
        this.scrollbar = new ScrollbarWidget(supplier, supplier2, supplier3, (v1) -> {
            scrollArea.scrollTo(v1);
        }, 10.0f, 3.0f, 24.0f);
    }

    @Override
    public void render(DrawCtx drawCtx) throws MatchException {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        colorStack115.push();
        colorStack115.alphaAnimation(this.tab.currentTabAnimation());
        this.filtersDirty |= this.lastFrameCount != this.tab.frames().size();
        this.tab.scrollingAreaComponent().beginArea(drawCtx, this.tab.framesOriginX(), this.tab.framesOriginY() + az, width(), height() - az);
        colorStack115.push();
        colorStack115.alpha(1.0f - Math.max(this.panelAnimator.smoothAnimation() - 1.0f, aB));
        this.scrollbar.render(drawCtx);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame abstractFrame = this.tab.frames().get(size);
            if (abstractFrame.visible() && isFrameVisible(abstractFrame, ax, originY() + az)) {
                abstractFrame.render(drawCtx);
            }
        }
        this.tab.scrollingAreaComponent().endArea(drawCtx, getContentHeight());
        renderOverlays(drawCtx);
        applyFilters();
        this.titleBadge.render(drawCtx);
        drawCtx.text(this.font, Translation.clearText("Ð”Ð¾ÑÑ‚ÑƒÐ¿Ð½Ñ‹Ðµ Ñ†Ð²ÐµÑ‚Ð¾Ð²Ñ‹Ðµ Ñ‚ÐµÐ¼Ñ‹").effective(), 16, this.headerBounds.x() + 10.0f, this.headerBounds.y() + this.titleBadge.height() + 8.0f, colorStack115.computeColor(themePalette.text().tone(300).argb()));
        renderFilterIndicators(drawCtx);
        this.actionButtons.forEach(labeledIconButton -> {
            labeledIconButton.render(drawCtx);
        });
        this.filterDropdown.render(drawCtx);
        colorStack115.pop();
        colorStack115.push();
        colorStack115.alpha(Math.max(this.panelAnimator.smoothAnimation() - 1.0f, aB));
        this.creatorPanel.render(drawCtx);
        colorStack115.pop();
        colorStack115.pop();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.tab.frames().forEach(abstractFrame -> {
            abstractFrame.layout(layoutScaleContext);
        });
        this.creatorPanel.layout(layoutScaleContext);
        this.creatorPanel.setPosition(originX(), originY());
        this.headerBounds.withPosition(originX() + ay, originY() + 16.0f).withSize(((width() - 16.0f) - 3.0f) - 24.0f, 71.0f);
        this.titleBadge.layout(layoutScaleContext);
        this.titleBadge.setPosition(this.headerBounds.x() + 10.0f, this.headerBounds.y());
        layoutFilterDropdown(layoutScaleContext);
        layoutScrollbar(layoutScaleContext);
        this.actionButtons.forEach(labeledIconButton -> {
            labeledIconButton.layout(layoutScaleContext);
        });
        float fWidth = 0.0f;
        for (int size = this.actionButtons.size() - 1; size >= 0; size--) {
            LabeledIconButton class747Var2 = this.actionButtons.get(size);
            fWidth += class747Var2.width() + 4.0f;
            class747Var2.setPosition(this.filterDropdown.x() - fWidth, this.filterDropdown.y());
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.filterDropdown.animation(weightedEngine);
        this.filterIndicators.forEach(themeFilterIndicator -> {
            themeFilterIndicator.animation().state(this.activeFilters.contains(themeFilterIndicator.filter()));
            themeFilterIndicator.animation().animate(weightedEngine);
        });
        this.panelAnimator.state(this.creatorPanel.isOpened());
        this.panelAnimator.animate(weightedEngine);
        this.creatorPanel.animation(weightedEngine);
        this.actionButtons.forEach(labeledIconButton -> {
            labeledIconButton.animation(weightedEngine);
        });
        this.scrollbar.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = z;
        if (this.creatorPanel.isOpened()) {
            this.creatorPanel.handleInput(inputEventContext, z2);
            InputEvent inputEvent691 = inputEventContext.inputEvent();
            if (inputEvent691 instanceof KeyInput) {
                KeyInput keyInput = (KeyInput) inputEvent691;
                if (keyInput.keyCode() == 256 && keyInput.keyAction().press()) {
                    this.creatorPanel.close();
                    this.panelAnimator.state(false);
                    return true;
                }
            }
            z2 = true;
        }
        boolean zHandleInput = z2 | this.scrollbar.handleInput(inputEventContext, z);
        boolean zHandleInput2 = zHandleInput | this.filterDropdown.handleInput(inputEventContext, zHandleInput);
        boolean zAnyMatch = zHandleInput2 | this.actionButtons.stream().anyMatch(labeledIconButton -> {
            return labeledIconButton.handleInput(inputEventContext, zHandleInput2);
        });
        InputEventContext inputEventContext688 = inputEventContext.withMouseOffset(aB, this.tab.scrollingAreaComponent().scrollY());
        boolean zInArea = zAnyMatch | inputEventContext.inArea(this.headerBounds.x(), this.headerBounds.y(), this.headerBounds.width(), this.headerBounds.height());
        for (AbstractFrame abstractFrame : this.tab.frames()) {
            if (abstractFrame.visible() && isFrameVisible(abstractFrame, ax, originY() + az)) {
                zInArea |= abstractFrame.handleInput(inputEventContext688, zInArea);
            }
        }
        return zInArea | this.tab.scrollingAreaComponent().handleInput(inputEventContext, zInArea);
    }

    @Override
    public float getContentHeight() {
        float f = 0.0f;
        for (AbstractFrame abstractFrame : this.tab.frames()) {
            if (abstractFrame.visible()) {
                float fY = ((abstractFrame.y() - this.tab.framesOriginY()) - az) + abstractFrame.height() + abstractFrame.contentHeight();
                if (fY > f) {
                    f = fY;
                }
            }
        }
        return f + 10.0f;
    }

    @Override
    public void positionFrames() {
        List<AbstractFrame> list = this.tab.frames().stream().filter((v0) -> {
            return v0.visible();
        }).sorted((abstractFrame, class757Var2) -> {
            if (!(abstractFrame instanceof ThemeCardFrame)) {
                return 0;
            }
            ThemeCardFrame themeCardFrame = (ThemeCardFrame) abstractFrame;
            if (!(class757Var2 instanceof ThemeCardFrame)) {
                return 0;
            }
            ThemeCardFrame class839Var2 = (ThemeCardFrame) class757Var2;
            if (themeCardFrame.favorite() != class839Var2.favorite()) {
                return themeCardFrame.favorite() ? -1 : 1;
            }
            ConfigOrigin configOrigin = themeCardFrame.themeContext().kind();
            ConfigOrigin class763VarKind2 = class839Var2.themeContext().kind();
            if (configOrigin == ConfigOrigin.OFFICIAL && class763VarKind2 == ConfigOrigin.USER) {
                return -1;
            }
            return (configOrigin == ConfigOrigin.USER && class763VarKind2 == ConfigOrigin.OFFICIAL) ? 1 : 0;
        }).toList();
        float fFloatValue = ((Float) list.stream().map((v0) -> {
            return v0.width();
        }).max((v0, v1) -> {
            return Float.compare(v0, v1);
        }).orElse(Float.valueOf(aB))).floatValue();
        int iMax = fFloatValue > aB ? Math.max(1, (int) Math.floor((((((width() - ay) - this.scrollbar.width()) - 24.0f) + 3.0f) + aA) / (fFloatValue + aA))) : 1;
        float[] fArr = new float[iMax];
        float[] fArr2 = new float[iMax];
        for (int i = 0; i < iMax; i++) {
            fArr2[i] = i * (fFloatValue + aA);
        }
        for (AbstractFrame class757Var3 : list) {
            int iIndexOfMin = indexOfMin(fArr);
            float f = fArr2[iIndexOfMin];
            float f2 = fArr[iIndexOfMin];
            float fHeight = class757Var3.height() + class757Var3.contentHeight();
            if (class757Var3 instanceof ThemeCardFrame) {
                ((ThemeCardFrame) class757Var3).targetPosition(originX() + ay + f, originY() + az + f2, !this.tab.forceImmediateFramePositioning());
            } else {
                class757Var3.setPosition(originX() + ay + f, originY() + az + f2);
            }
            fArr[iIndexOfMin] = fArr[iIndexOfMin] + fHeight + aB;
        }
    }

    @Override
    public float width() {
        return MenuWindow.MENU_WIDTH;
    }

    public void layoutScrollbar(LayoutScaleContext layoutScaleContext) {
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT;
        this.scrollbar.setPosition(((originX() + width()) - ay) - this.scrollbar.width(), this.headerBounds.y() + this.headerBounds.height());
        this.scrollbar.setSize(this.scrollbar.width(), f);
        this.scrollbar.layout(layoutScaleContext);
    }

    public void layoutFilterDropdown(LayoutScaleContext layoutScaleContext) {
        this.filterDropdown.setPosition((this.headerBounds.x() + this.headerBounds.width()) - this.filterDropdown.width(), (this.headerBounds.y() + (this.headerBounds.height() / 2.0f)) - (this.filterDropdown.height() / 2.0f));
        this.filterDropdown.layout(layoutScaleContext);
    }

    public void renderFilterIndicators(DrawCtx drawCtx) throws MatchException {
        int iComputeColor;
        float fCeil = MathHelper.ceil(this.font.getHeight(11.0f));
        float fY = this.headerBounds.y() + this.titleBadge.height() + 8.0f + this.font.getHeight(16.0f) + ay;
        float fX = this.headerBounds.x() + 10.0f + 6.0f;
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        for (ThemeFilterIndicator themeFilterIndicator : this.filterIndicators) {
            float fSmoothAnimation = themeFilterIndicator.animation().smoothAnimation();
            float fCeil2 = 11.0f + MathHelper.ceil(drawCtx.textWidthPhysical(this.font, themeFilterIndicator.label(), 11)) + 16.0f;
            if (fSmoothAnimation > 0.001f) {
                switch (themeFilterIndicator.filter().ordinal()) {
                    case 0:
                        iComputeColor = colorStack115.computeColor(themePalette.favorite().argb());
                        break;
                    case 1:
                        iComputeColor = colorStack115.computeColor(themePalette.accentBright().argb());
                        break;
                    case 2:
                        iComputeColor = colorStack115.computeColor(themePalette.text().tone(600).argb());
                        break;
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
                int i = iComputeColor;
                colorStack115.push();
                colorStack115.alpha(fSmoothAnimation);
                drawCtx.outlineCircle(MathHelper.ceil(fX), MathHelper.ceil(fY + (fCeil / 2.0f)) - 1.0f, 5.0f, 2.5f, colorStack115.computeColor(i), colorStack115.computeColor(i, 0.2f));
                drawCtx.text(this.font, themeFilterIndicator.label(), 11, fX + 5.0f + 6.0f, fY, colorStack115.computeColor(themePalette.text().tone(500).argb()));
                colorStack115.pop();
            }
            fX += fCeil2 * fSmoothAnimation;
        }
    }

    public void applyFilters() {
        if (this.filtersDirty) {
            for (AbstractFrame abstractFrame : this.tab.frames()) {
                if (abstractFrame instanceof ThemeCardFrame) {
                    ThemeCardFrame themeCardFrame = (ThemeCardFrame) abstractFrame;
                    themeCardFrame.updateFilterVisibility((this.activeFilters.contains(ConfigFilter.FAVORITE) && themeCardFrame.favorite()) || (this.activeFilters.contains(ConfigFilter.OFFICIAL) && themeCardFrame.themeContext().kind() == ConfigOrigin.OFFICIAL) || (this.activeFilters.contains(ConfigFilter.USER) && themeCardFrame.themeContext().kind() == ConfigOrigin.USER));
                }
            }
            this.tab.markFramesDirty();
            this.filtersDirty = false;
            this.lastFrameCount = this.tab.frames().size();
        }
    }
}
