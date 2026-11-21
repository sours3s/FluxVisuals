package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.ActionDialogBuilder;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AvatarCache;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.config.LocalPreferencesFile;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.model.StaffProfile;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;
import ru.fluxvisuals.ui.gui.clickgui.setting.TextFieldSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConfigDetailPanel extends WidgetContainer {
    public static final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    public static final GlTexture folderIcon = new GlTexture(new ClasspathResource("/icons/menu/new/folder_fill.png"));
    public static final GlTexture calendarIcon = new GlTexture(new ClasspathResource("/icons/menu/new/calendar.png"));
    public static final GlTexture copyIcon = new GlTexture(new ClasspathResource("/icons/menu/new/copy.png"));
    public static final GlTexture resetIcon = new GlTexture(new ClasspathResource("/icons/menu/new/arrow_rotation.png"));
    public static final GlTexture checkmarkIcon = new GlTexture(new ClasspathResource("/assets/fluxvisuals/icons/menu/new/checkmark.png"));
    public static final float padding = 14.0f;
    public static final float gap = 6.0f;
    public volatile GlTexture avatarTexture;

    public CloudConfigCard currentContext;
    public Runnable requestCloseAnimated;
    public Runnable requestRefreshList;

    public Consumer<String> requestApplyById;

    public Consumer<CloudConfigCard> requestSaveCurrent;
    public Consumer<CloudConfigCard> requestResetToDefaults;
    public volatile boolean avatarLoading = false;
    public final GlTexture defaultAvatar = new GlTexture(new ClasspathResource("assets/fluxvisuals/textures/avatar.png"));
    public final IconLabelBadge headerBadge = new IconLabelBadge(folderIcon, Lang.CONFIG_INFO_HEADER);
    public final List<AbstractWidget> actionButtons = new ArrayList();
    public final ClickableBehavior copyClickable = new ClickableBehavior();
    public final ToggleAnimator copiedAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public boolean copied = false;
    public long copiedTime = 0;
    public boolean opened = false;
    public final ToggleTextButton autoSaveButton = new ToggleTextButton(this::toggleAutoSave, font, new GlTexture(new ClasspathResource("/icons/menu/new/save.png")), 7.0f, 8.0f, 5.5f, 12);

    public ConfigDetailPanel() {
        this.autoSaveButton.label(Lang.CONFIG_BUTTON_AUTOSAVE, 12);
        this.actionButtons.addAll(List.of(new LabeledIconButton(Lang.CONFIG_BUTTON_SAVE, this::openSaveDialog, font, new GlTexture(new ClasspathResource("/icons/menu/new/save.png")), 7.0f, 8.0f, 5.5f, 12), new LabeledIconButton(Lang.CONFIG_BUTTON_LOAD, this::loadConfig, font, new GlTexture(new ClasspathResource("/icons/menu/new/display.png")), 7.0f, 8.0f, 5.5f, 12), new LabeledIconButton(Lang.CONFIG_BUTTON_DELETE, this::openDeleteDialog, font, new GlTexture(new ClasspathResource("/icons/menu/new/trash.png")), 7.0f, 8.0f, 5.5f, 12), new LabeledIconButton(Lang.CONFIG_BUTTON_RENAME, this::openRenameDialog, font, new GlTexture(new ClasspathResource("/icons/menu/new/pencil.png")), 7.0f, 8.0f, 5.5f, 12), new LabeledIconButton(Lang.CONFIG_BUTTON_RESET, this::openResetDialog, font, resetIcon, 7.0f, 8.0f, 5.5f, 12), this.autoSaveButton));
        this.actionButtons.forEach((v1) -> {
            addChild(v1);
        });
        addChild(this.headerBadge);
    }

    public GlTexture resolveAvatar() {
        if (!this.avatarLoading) {
            this.avatarLoading = true;
            AvatarCache.load(this.currentContext.author().avatarUrl(), this.defaultAvatar, FluxVisuals.INSTANCE.executor()).thenAccept(glTexture -> {
                this.avatarTexture = glTexture;
            });
        }
        return this.avatarTexture != null ? this.avatarTexture : this.defaultAvatar;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.currentContext == null) {
            return;
        }
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        super.render(drawCtx);
        float fY = this.headerBadge.y() + this.headerBadge.height() + 8.0f;
        float fX = ((x() + width()) - padding) - (x() + padding);
        if (drawCtx.textWidthPhysical(font, this.currentContext.name(), 16) > fX) {
            int iComputeColor = colorStack115.computeColor(themePalette.text().tone(100).argb()) & 16777215;
            String str = "";
            for (int length = this.currentContext.name().length(); length > 0; length--) {
                String strSubstring = this.currentContext.name().substring(0, length);
                if (drawCtx.textWidthPhysical(font, strSubstring, 16) <= fX) {
                    str = strSubstring;
                    break;
                }
            }
            drawCtx.textWithHorizontalGradient(font, str, 16, x() + padding, fY, colorStack115.computeColor(themePalette.text().tone(100).argb()), iComputeColor);
        } else {
            drawCtx.text(font, this.currentContext.name(), 16, x() + padding, fY, colorStack115.computeColor(themePalette.text().tone(100).argb()));
        }
        float fFloatValue = ((Float) this.actionButtons.stream().map((v0) -> {
            return v0.y();
        }).max((v0, v1) -> {
            return Float.compare(v0, v1);
        }).orElse(Float.valueOf(0.0f))).floatValue() + ((Float) this.actionButtons.stream().map((v0) -> {
            return v0.height();
        }).max((v0, v1) -> {
            return Float.compare(v0, v1);
        }).orElse(Float.valueOf(0.0f))).floatValue() + 16.0f;
        if (this.currentContext != null) {
            StaffProfile staffProfile = this.currentContext.author();
            float f = fFloatValue + 9.0f;
            drawCtx.text(font, Lang.CONFIG_AUTHOR_LABEL.effective(), 12, x() + padding, f - (font.getHeight(12.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(600).argb()));
            float fX2 = ((x() + width()) - padding) - drawCtx.textWidthPhysical(font, staffProfile.name(), 13);
            drawCtx.text(font, staffProfile.name(), 13, fX2, f - (font.getHeight(13.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(200).argb()));
            drawCtx.roundedTexture(resolveAvatar(), (fX2 - gap) - 18.0f, fFloatValue, 18.0f, 18.0f, gap, colorStack115.white());
            float f2 = fFloatValue + 18.0f + 12.0f;
            float f3 = f2 + 8.0f;
            drawCtx.text(font, Lang.CONFIG_CREATED_LABEL.effective(), 12, x() + padding, f3 - (font.getHeight(12.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(600).argb()));
            float fX3 = ((x() + width()) - padding) - drawCtx.textWidthPhysical(font, this.currentContext.date(), 13);
            drawCtx.text(font, this.currentContext.date(), 13, fX3, f3 - (font.getHeight(13.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(200).argb()));
            drawCtx.texture(calendarIcon, (fX3 - 4.0f) - calendarIcon.width(), f3 - (calendarIcon.height() / 2.0f), calendarIcon.width(), calendarIcon.height(), colorStack115.computeColor(themePalette.text().tone(300).argb()));
            float f4 = f2 + 18.0f + 12.0f;
            float f5 = f4 + 8.0f;
            drawCtx.text(font, Lang.CONFIG_UPDATED_LABEL.effective(), 12, x() + padding, f5 - (font.getHeight(12.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(600).argb()));
            float fX4 = ((x() + width()) - padding) - drawCtx.textWidthPhysical(font, this.currentContext.formatDate(this.currentContext.lastModified()), 13);
            drawCtx.text(font, this.currentContext.formatDate(this.currentContext.lastModified()), 13, fX4, f5 - (font.getHeight(13.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(200).argb()));
            drawCtx.texture(calendarIcon, (fX4 - 4.0f) - calendarIcon.width(), f5 - (calendarIcon.height() / 2.0f), calendarIcon.width(), calendarIcon.height(), colorStack115.computeColor(themePalette.text().tone(300).argb()));
            float f6 = f4 + 18.0f + 12.0f + 8.0f;
            float fX5 = ((x() + width()) - padding) - copyIcon.width();
            float fHeight = f6 - (copyIcon.height() / 2.0f);
            drawCtx.text(font, Lang.CONFIG_CLOUD_CODE.effective(), 12, x() + padding, f6 - (font.getHeight(12.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(600).argb()));
            String strCloudId = this.currentContext.cloudId();
            drawCtx.text(font, strCloudId, 13, (fX5 - 4.0f) - drawCtx.textWidthPhysical(font, strCloudId, 13), f6 - (font.getHeight(13.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(200).argb()));
            float fSmoothAnimation = this.copiedAnimator.smoothAnimation();
            colorStack115.push();
            colorStack115.alpha(1.0f - fSmoothAnimation);
            drawCtx.texture(copyIcon, fX5, fHeight, copyIcon.width(), copyIcon.height(), colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(300).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), this.copyClickable.hoverAnimation()));
            colorStack115.pop();
            float f7 = 0.85f + (0.15f * fSmoothAnimation);
            float fWidth = checkmarkIcon.width() * f7;
            float fHeight2 = checkmarkIcon.height() * f7;
            float fWidth2 = fX5 + ((copyIcon.width() - fWidth) / 2.0f);
            float fHeight3 = fHeight + ((copyIcon.height() - fHeight2) / 2.0f);
            colorStack115.push();
            colorStack115.alpha(fSmoothAnimation);
            drawCtx.texture(checkmarkIcon, fWidth2, fHeight3, fWidth, fHeight2, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(300).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), this.copyClickable.hoverAnimation()));
            colorStack115.pop();
            this.copyClickable.setDimensions(fX5, fHeight, copyIcon.width(), copyIcon.height());
            this.copyClickable.clickCallback(() -> {
                StringUtil.copyToClipboard(this.currentContext.cloudId());
                this.copied = true;
                this.copiedTime = System.currentTimeMillis();
            });
            drawCtx.fillRect(x(), y(), 1.0f, height(), colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()));
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        calendarIcon.setDimensions(13, 13);
        copyIcon.setDimensions(13, 13);
        this.headerBadge.setPosition(x() + padding, y() + 16.0f);
        checkmarkIcon.setDimensions(13, 13);
        float fX = x() + padding;
        float fY = y() + 74.0f;
        float fWidth = width() - 28.0f;
        float fMax = 0.0f;
        for (AbstractWidget abstractWidget : this.actionButtons) {
            if (fX + abstractWidget.width() > x() + padding + fWidth && fX > x() + padding) {
                fX = x() + padding;
                fY += fMax + gap;
                fMax = 0.0f;
            }
            abstractWidget.setPosition(fX, fY);
            fX += abstractWidget.width() + gap;
            fMax = Math.max(fMax, abstractWidget.height());
        }
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (!z) {
            InputEvent inputEvent691 = inputEventContext.inputEvent();
            if (inputEvent691 instanceof KeyInput) {
                KeyInput keyInput = (KeyInput) inputEvent691;
                if (isOpened() && keyInput.keyAction().press() && keyInput.keyCode() == 256) {
                    this.requestCloseAnimated.run();
                    return true;
                }
            }
        }
        if (this.copyClickable.handleInput(inputEventContext, z)) {
            return true;
        }
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.copied && System.currentTimeMillis() - this.copiedTime > 600) {
            this.copied = false;
        }
        this.copiedAnimator.state(this.copied);
        this.copiedAnimator.animate(weightedEngine);
        this.copyClickable.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void toggle() {
        this.opened = !this.opened;
    }

    public void open(CloudConfigCard cloudConfigCard) {
        this.opened = true;
        this.currentContext = cloudConfigCard;
        this.avatarTexture = null;
        this.avatarLoading = false;
        this.copied = false;
        this.copiedAnimator.force(false);
        this.autoSaveButton.setActive(!FluxVisuals.INSTANCE.configManager().menuStateConfig().isAutoSaveDisabled(cloudConfigCard.cloudId()));
    }

    public void close() {
        if (this.opened) {
            this.opened = false;
        }
    }

    public void toggleAutoSave() {
        if (this.currentContext == null) {
            return;
        }
        LocalPreferencesFile localPreferencesFile = FluxVisuals.INSTANCE.configManager().menuStateConfig();
        localPreferencesFile.setAutoSaveDisabled(this.currentContext.cloudId(), !localPreferencesFile.isAutoSaveDisabled(this.currentContext.cloudId()));
        try {
            FluxVisuals.INSTANCE.configManager().saveMenuState();
        } catch (IOException e) {
            FluxVisuals.LOGGER.error("Failed to save menu scale state", e);
        }
    }

    public void openDeleteDialog() {
        ActionConfirmDialog actionConfirmDialog = FluxVisuals.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        actionConfirmDialog.open(new ActionDialogBuilder().title(Lang.CONFIG_DELETE_TITLE).description(Lang.CONFIG_DELETE_DESCRIPTION).placeholderText(Lang.CONFIG_DELETE_PLACEHOLDER).icon(new GlTexture(new ClasspathResource("/icons/menu/new/trash_fill.png"))).confirmLabel(Lang.CONFIG_DELETE_CONFIRM).placeholderColor(ThemePalette.red.argb()).onConfirm(() -> {
            if (this.currentContext != null) {
                actionConfirmDialog.setLoading(true, Lang.CONFIG_DELETE_LOADING.effective());
                FluxVisuals.INSTANCE.cloudConfigService().deleteConfig(this.currentContext.cloudId()).thenRun(() -> {
                    actionConfirmDialog.setLoading(false, "");
                    if (this.requestCloseAnimated != null) {
                        this.requestCloseAnimated.run();
                    }
                    this.currentContext = null;
                    if (this.requestRefreshList != null) {
                        this.requestRefreshList.run();
                    }
                    TabLayout tabLayout = FluxVisuals.INSTANCE.tabsController().current().renderStrategy();
                    if (tabLayout instanceof ConfigTabLayout) {
                        ((ConfigTabLayout) tabLayout).updateConfigList();
                    }
                    actionConfirmDialog.close();
                }).exceptionally(th -> {
                    actionConfirmDialog.setLoading(false, "");
                    FluxVisuals.LOGGER.error("Failed to delete config", th);
                    actionConfirmDialog.showError(FluxVisuals.INSTANCE.cloudConfigService().errorMessage(th).effective());
                    return null;
                });
            }
        }).build());
    }

    public void openRenameDialog() {
        String[] strArr = new String[1];
        strArr[0] = this.currentContext != null ? this.currentContext.name() : "";
        ActionConfirmDialog actionConfirmDialog = FluxVisuals.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        actionConfirmDialog.open(new ActionDialogBuilder().title(Lang.CONFIG_RENAME_TITLE).placeholderText(Lang.CONFIG_ACTION_PLACEHOLDER).icon(new GlTexture(new ClasspathResource("/icons/menu/new/folder_fill.png"))).content(frameElementColumn -> {
            TextFieldSettingElement textFieldSettingElement = new TextFieldSettingElement(Lang.CONFIG_NAME_LABEL, Lang.CONFIG_NAME_DESCRIPTION, Lang.CONFIG_NAME_PLACEHOLDER, false, 50, false, () -> {
                return strArr[0];
            });
            textFieldSettingElement.onChange(str -> {
                strArr[0] = str.trim();
                actionConfirmDialog.hideError();
            });
            textFieldSettingElement.focusAtEnd();
            frameElementColumn.addFrameElement(textFieldSettingElement);
        }).confirmLabel(Lang.CONFIG_RENAME_CONFIRM).enableConfirmButton(() -> {
            String strTrim = strArr[0].trim();
            return !strTrim.isEmpty() && (this.currentContext == null || !strTrim.equals(this.currentContext.name()));
        }).onConfirm(() -> {
            String strTrim = strArr[0].trim();
            if (this.currentContext == null || strTrim.isEmpty()) {
                return;
            }
            actionConfirmDialog.setLoading(true, Lang.CONFIG_RENAME_LOADING.effective());
            FluxVisuals.INSTANCE.cloudConfigService().renameConfig(this.currentContext.cloudId(), strTrim).thenRun(() -> {
                actionConfirmDialog.setLoading(false, "");
                if (this.requestRefreshList != null) {
                    this.requestRefreshList.run();
                }
                this.currentContext.name(strTrim);
                actionConfirmDialog.close();
            }).exceptionally(th -> {
                actionConfirmDialog.setLoading(false, "");
                FluxVisuals.LOGGER.error("Failed to rename config", th);
                actionConfirmDialog.showError(FluxVisuals.INSTANCE.cloudConfigService().errorMessage(th).effective());
                return null;
            });
        }).build());
    }

    public void openSaveDialog() {
        if (this.currentContext == null || this.requestSaveCurrent == null) {
            return;
        }
        ActionConfirmDialog actionConfirmDialog = FluxVisuals.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        actionConfirmDialog.open(new ActionDialogBuilder().title(Lang.CONFIG_SAVE_TITLE).description(Lang.CONFIG_SAVE_DESCRIPTION).placeholderText(Lang.CONFIG_SAVE_PLACEHOLDER).icon(new GlTexture(new ClasspathResource("/icons/menu/new/save.png"))).confirmLabel(Lang.CONFIG_SAVE_CONFIRM).onConfirm(() -> {
            this.requestSaveCurrent.accept(this.currentContext);
            actionConfirmDialog.close();
        }).build());
    }

    public void loadConfig() {
        if (this.currentContext == null || this.requestApplyById == null) {
            return;
        }
        this.requestApplyById.accept(this.currentContext.cloudId());
    }

    public void openResetDialog() {
        if (this.currentContext == null) {
            return;
        }
        ActionConfirmDialog actionConfirmDialog = FluxVisuals.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        actionConfirmDialog.open(new ActionDialogBuilder().title(Lang.CONFIG_RESET_TITLE).description(Lang.CONFIG_RESET_DESCRIPTION).placeholderText(Lang.CONFIG_RESET_PLACEHOLDER).icon(resetIcon).placeholderColor(ThemePalette.red.argb()).confirmLabel(Lang.CONFIG_RESET_CONFIRM).onConfirm(() -> {
            if (this.currentContext == null || this.requestResetToDefaults == null) {
                return;
            }
            actionConfirmDialog.setLoading(true, Lang.CONFIG_RESET_LOADING.effective());
            try {
                this.requestResetToDefaults.accept(this.currentContext);
                actionConfirmDialog.setLoading(false, "");
                actionConfirmDialog.close();
            } catch (Exception e) {
                actionConfirmDialog.setLoading(false, "");
                FluxVisuals.LOGGER.error("Failed to reset config", e);
                actionConfirmDialog.showError(e.getMessage());
            }
        }).build());
    }

    public CloudConfigCard getCurrentContext() {
        return this.currentContext;
    }

    public void setCurrentContext(CloudConfigCard cloudConfigCard) {
        this.currentContext = cloudConfigCard;
    }

    public void setRequestCloseAnimated(Runnable runnable) {
        this.requestCloseAnimated = runnable;
    }

    public void setRequestRefreshList(Runnable runnable) {
        this.requestRefreshList = runnable;
    }

    public void setRequestApplyConfigById(Consumer<String> consumer) {
        this.requestApplyById = consumer;
    }

    public void setRequestSaveCurrent(Consumer<CloudConfigCard> consumer) {
        this.requestSaveCurrent = consumer;
    }

    public void setRequestResetToDefaults(Consumer<CloudConfigCard> consumer) {
        this.requestResetToDefaults = consumer;
    }

    public boolean isOpened() {
        return this.opened;
    }
}
