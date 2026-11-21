package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.ActionDialogBuilder;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AvatarCache;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.io.IOException;
import java.util.function.Consumer;

public class ConfigCard extends AbstractFrame {
    static final GlTexture saveIcon = loadTexture("/icons/menu/new/save.png");
    static final GlTexture loadIcon = loadTexture("/icons/menu/new/display.png");
    static final GlTexture infoIcon = loadTexture("/icons/menu/new/text_scroll.png");
    public final GlTexture folderIcon;
    public final GlTexture calendarIcon;
    public final GlTexture defaultAvatar;
    public final AnimatedFloat animX;
    public final AnimatedFloat animY;
    public final ToggleAnimator selectAnimator;
    public final ToggleAnimator favoriteAnimator;
    public final ToggleAnimator hoverAnimator;
    public final ClickableBehavior favoriteClickable;
    public final WidgetBounds iconBounds;
    public final IconButtonWidget loadButton;
    public final IconButtonWidget saveButton;
    public final IconButtonWidget infoButton;

    public final CloudConfigCard configContext;
    public boolean favorite;
    public boolean selected;
    public boolean positioned;
    public boolean dragging;
    public volatile GlTexture avatarTexture;
    public volatile boolean avatarLoading;

    static GlTexture loadTexture(String str) {
        return new GlTexture(new ClasspathResource(str));
    }

    public ConfigCard(CloudConfigCard cloudConfigCard, Consumer<CloudConfigCard> consumer, Consumer<CloudConfigCard> consumer2) {
        super(new FrameElementColumn(0.0f, 0.0f));
        this.folderIcon = loadTexture("/icons/menu/new/folder_fill.png");
        this.calendarIcon = loadTexture("/icons/menu/new/calendar.png");
        this.defaultAvatar = loadTexture("assets/fluxvisuals/textures/avatar.png");
        this.animX = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.animY = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.selectAnimator = new ToggleAnimator(100, Easings.LINEAR);
        this.favoriteAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
        this.hoverAnimator = new ToggleAnimator(100, Easings.LINEAR);
        this.favoriteClickable = new ClickableBehavior();
        this.iconBounds = new WidgetBounds(0.0f, 0.0f, 36.0f, 36.0f);
        this.configContext = cloudConfigCard;
        this.favorite = FluxVisuals.INSTANCE.configManager().menuStateConfig().isConfigFavorite(cloudConfigCard.cloudId());
        this.favoriteAnimator.state(this.favorite);
        this.favoriteClickable.clickCallback(this::invertFavorite);
        this.loadButton = new IconButtonWidget(loadIcon, () -> {
            FluxVisuals.INSTANCE.cloudConfigService().downloadConfig(cloudConfigCard.cloudId()).thenAccept(loadedConfig -> {
                if (!this.selected) {
                    FluxVisuals.INSTANCE.cloudConfigService().applyConfig(loadedConfig);
                }
                FluxVisuals.INSTANCE.configManager().saveLastConfigID();
                consumer2.accept(cloudConfigCard);
            }).exceptionally(th -> {
                FluxVisuals.LOGGER.error("Load failed", th);
                return null;
            });
        }, 14.0f, 14.0f).tooltip(Lang.CONFIG_BUTTON_LOAD_TOOLTIP, loadIcon);
        this.saveButton = new IconButtonWidget(saveIcon, this::openSaveDialog, 14.0f, 14.0f).tooltip(Lang.CONFIG_BUTTON_SAVE_TOOLTIP, saveIcon);
        this.infoButton = new IconButtonWidget(infoIcon, () -> {
            consumer.accept(cloudConfigCard);
        }, 14.0f, 14.0f).tooltip(Lang.CONFIG_BUTTON_INFO_TOOLTIP, infoIcon);
        addChild(this.infoButton);
        addChild(this.loadButton);
        addChild(this.saveButton);
    }

    public GlTexture resolveAvatar() {
        if (!this.avatarLoading) {
            this.avatarLoading = true;
            AvatarCache.load(this.configContext.author().avatarUrl(), this.defaultAvatar, FluxVisuals.INSTANCE.executor()).thenAccept(glTexture -> {
                this.avatarTexture = glTexture;
            });
        }
        return this.avatarTexture != null ? this.avatarTexture : this.defaultAvatar;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        MsdfFont msdfFont = Fonts.INTER_BOLD.get();
        MsdfFont class161Var2 = Fonts.INTER_SEMIBOLD.get();
        float fMax = this.selected ? Math.max(this.selectAnimator.smoothAnimation(), 0.5f) : !this.selectAnimator.isZero() ? this.selectAnimator.smoothAnimation() : Math.min(this.hoverAnimator.smoothAnimation(), 0.5f);
        if (fMax > 0.0f) {
            colorStack115.push();
            colorStack115.alpha(fMax);
            drawCtx.fillOutlinedRoundedRect(x(), y(), width(), height(), 10.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(800).argb()));
            colorStack115.pop();
        }
        drawCtx.fillOutlinedRoundedRect(this.iconBounds.x(), this.iconBounds.y(), this.iconBounds.width(), this.iconBounds.height(), 10.0f, 2.5f, colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb(), 0), colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), this.selectAnimator.smoothAnimation()), colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.favoriteClickable.hoverAnimation().smoothAnimation()));
        drawCtx.textureVerticalC(this.folderIcon, (this.iconBounds.x() + (this.iconBounds.width() / 2.0f)) - 7.0f, this.iconBounds.y() + (this.iconBounds.height() / 2.0f), 14, 14, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(600).argb()), colorStack115.computeColor(ThemePalette.golden.argb()), this.favoriteAnimator.smoothAnimation()));
        float fX = ((x() + width()) - 10.0f) - drawCtx.textWidthPhysical(class161Var2, this.configContext.author().name(), 12);
        colorStack115.push();
        colorStack115.alpha(this.hoverAnimator.state() ? 0.0f : 1.0f - this.hoverAnimator.smoothAnimation());
        drawCtx.text(class161Var2, this.configContext.author().name(), 12, fX, (this.iconBounds.y() + (this.iconBounds.height() / 2.0f)) - (class161Var2.getHeight(12.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(300).argb()));
        drawCtx.roundedTexture(resolveAvatar(), fX - 26.0f, (this.iconBounds.y() + (this.iconBounds.height() / 2.0f)) - 10.0f, 20.0f, 20.0f, 6.0f, colorStack115.white());
        colorStack115.pop();
        colorStack115.push();
        colorStack115.alpha(this.hoverAnimator.smoothAnimation());
        this.loadButton.render(drawCtx);
        this.saveButton.render(drawCtx);
        this.infoButton.render(drawCtx);
        colorStack115.pop();
        float fX2 = this.iconBounds.x() + this.iconBounds.width() + 8.0f;
        drawCtx.texture(this.calendarIcon, fX2, this.iconBounds.y() + 2.0f, 11.0f, 11.0f, colorStack115.computeColor(themePalette.text().tone(700).argb()));
        drawCtx.text(msdfFont, this.configContext.date(), 10, fX2 + 15.0f, this.iconBounds.y() + 2.0f, colorStack115.computeColor(themePalette.text().tone(700).argb()));
        int iComputeColor = colorStack115.computeColor(themePalette.text().tone(300).argb());
        float f = (fX - 46.0f) - fX2;
        float fY = this.iconBounds.y() + 2.0f + msdfFont.getHeight(10.0f) + 4.0f;
        if (drawCtx.textWidthPhysical(msdfFont, this.configContext.name(), 13) <= f) {
            drawCtx.text(msdfFont, this.configContext.name(), 13, fX2, fY, iComputeColor);
            return;
        }
        String str = "";
        for (int length = this.configContext.name().length(); length > 0; length--) {
            String strSubstring = this.configContext.name().substring(0, length);
            if (drawCtx.textWidthPhysical(msdfFont, strSubstring, 13) <= f) {
                str = strSubstring;
                break;
            }
        }
        drawCtx.textWithHorizontalGradient(msdfFont, str, 13, fX2, fY, iComputeColor, iComputeColor & 16777215);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.folderIcon.setDimensions(14, 14);
        this.calendarIcon.setDimensions(11, 11);
        this.iconBounds.withPosition(x() + 10.0f, (y() + (height() / 2.0f)) - 18.0f);
        this.favoriteClickable.setDimensions(this.iconBounds.x(), this.iconBounds.y(), this.iconBounds.width(), this.iconBounds.height());
        float fX = ((x() + width()) - 16.0f) - this.saveButton.width();
        float fY = (this.iconBounds.y() + (this.iconBounds.height() / 2.0f)) - (this.saveButton.height() / 2.0f);
        this.infoButton.setPosition(fX, fY);
        this.saveButton.setPosition((fX - 10.0f) - this.saveButton.width(), fY);
        this.loadButton.setPosition(((fX - 20.0f) - this.saveButton.width()) - this.loadButton.width(), fY);
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.positioned) {
            this.animX.animate(weightedEngine);
            this.animY.animate(weightedEngine);
            setPosition(this.animX.animatedValue(), this.animY.animatedValue());
        }
        this.favoriteClickable.animate(weightedEngine);
        this.selectAnimator.state(this.selected);
        this.selectAnimator.animate(weightedEngine);
        this.favoriteAnimator.animate(weightedEngine);
        this.hoverAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = z;
        if (this.favoriteClickable.handleInput(inputEventContext, z)) {
            z2 = true;
        }
        if (inputEventContext.inputEvent() instanceof CursorMoveInput) {
            this.hoverAnimator.state(inputEventContext.inArea(x(), y() + 2.0f, width(), height() - 4.0f) && !z);
        }
        if (super.handleInput(inputEventContext, z2)) {
            z2 = true;
        }
        return z2;
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.dragging = z;
        if (z && this.positioned) {
            this.animX.set(x());
            this.animY.set(y());
            this.animX.destination(x());
            this.animY.destination(y());
        }
    }

    @Override
    public float contentHeight() {
        return 0.0f;
    }

    @Override
    public float width() {
        return 281.0f;
    }

    @Override
    public float height() {
        return 62.0f;
    }

    public void targetPosition(float f, float f2, boolean z) {
        if (this.positioned && z && !this.dragging) {
            this.animX.destination(f);
            this.animY.destination(f2);
        } else {
            this.positioned = true;
            snapTo(f, f2);
        }
    }

    public void snapTo(float f, float f2) {
        setPosition(f, f2);
        this.animX.set(f);
        this.animY.set(f2);
    }

    public void invertFavorite() {
        this.favorite = !this.favorite;
        this.favoriteAnimator.state(this.favorite);
        try {
            FluxVisuals.INSTANCE.configManager().menuStateConfig().setConfigFavorite(this.configContext.cloudId(), this.favorite);
            FluxVisuals.INSTANCE.configManager().saveMenuState();
        } catch (IOException e) {
            FluxVisuals.LOGGER.error("Failed to save favorite", e);
        }
        FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
    }

    public void openSaveDialog() {
        ActionConfirmDialog actionConfirmDialog = FluxVisuals.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        actionConfirmDialog.open(new ActionDialogBuilder().title(Lang.CONFIG_SAVE_TITLE).description(Lang.CONFIG_SAVE_DESCRIPTION).placeholderText(Lang.CONFIG_SAVE_PLACEHOLDER).icon(saveIcon).confirmLabel(Lang.CONFIG_SAVE_CONFIRM).onConfirm(() -> {
            actionConfirmDialog.setLoading(true, Lang.CONFIG_SAVE_LOADING.effective());
            FluxVisuals.INSTANCE.cloudConfigService().saveConfig(this.configContext.cloudId(), FluxVisuals.INSTANCE.moduleRepository(), FluxVisuals.INSTANCE.widgetStack()).thenRun(() -> {
                actionConfirmDialog.setLoading(false, "");
                actionConfirmDialog.close();
            }).exceptionally(th -> {
                actionConfirmDialog.setLoading(false, "");
                FluxVisuals.LOGGER.error("Save failed", th);
                actionConfirmDialog.showError(FluxVisuals.INSTANCE.cloudConfigService().errorMessage(th).effective());
                return null;
            });
        }).build());
    }

    public CloudConfigCard configContext() {
        return this.configContext;
    }

    public boolean favorite() {
        return this.favorite;
    }

    public boolean selected() {
        return this.selected;
    }

    public ConfigCard selected(boolean z) {
        this.selected = z;
        return this;
    }
}
