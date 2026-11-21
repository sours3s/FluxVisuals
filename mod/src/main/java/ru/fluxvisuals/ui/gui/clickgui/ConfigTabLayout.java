package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.ActionDialogBuilder;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.net.CloudConfigDto;
import ru.fluxvisuals.ui.gui.clickgui.util.net.CloudConfigMetadata;
import ru.fluxvisuals.ui.gui.clickgui.util.net.CloudConfigService;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.config.ConfigFile;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.type.DropdownOption;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.config.LoadedConfig;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.type.SortOrder;
import ru.fluxvisuals.ui.gui.clickgui.setting.TextFieldSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigTabLayout extends AbstractTabLayout {
    static final float an = 30.0f;
    static final float ao = 12.0f;
    static final float ap = 72.0f;
    static final float aq = 12.0f;
    static final float ar = 0.0f;
    public final Consumer<CloudConfigCard> openDetailCallback;
    public final Consumer<CloudConfigCard> afterLoadCallback;

    public ScrollbarWidget scrollbar;
    public final DropdownWidget<SortOrder> sortDropdown;

    public SortOrder currentSortType;
    public final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    public final GlTexture folderIcon = new GlTexture(new ClasspathResource("/icons/menu/new/folder_fill.png"));
    public final GlTexture sortIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final List<LabeledIconButton> actionButtons = new ArrayList();
    public final IconLabelBadge headerBadge = new IconLabelBadge(this.folderIcon, Lang.CONFIG_TAB_PLACEHOLDER);
    public final WidgetBounds headerBounds = new WidgetBounds(ar, ar, ar, ar);
    public final ToggleAnimator detailAnimator = ToggleAnimator.times(3, 150);
    public ConfigCard selectedFrame = null;

    public final ConfigDetailPanel detailPanel = new ConfigDetailPanel();

    public ConfigTabLayout() {
        this.detailPanel.setRequestCloseAnimated(this::closeDetail);
        this.detailPanel.setRequestRefreshList(this::updateConfigList);
        this.detailPanel.setRequestApplyConfigById(this::applyConfigById);
        this.detailPanel.setRequestSaveCurrent(this::saveConfig);
        this.detailPanel.setRequestResetToDefaults(this::resetToDefaults);
        this.currentSortType = SortOrder.NEWEST_FIRST;
        this.openDetailCallback = cloudConfigCard -> {
            CloudConfigCard currentContext;
            if (this.detailPanel.isOpened() && (currentContext = this.detailPanel.getCurrentContext()) != null && currentContext.cloudId().equals(cloudConfigCard.cloudId())) {
                this.detailPanel.close();
            } else {
                this.detailPanel.open(cloudConfigCard);
            }
            this.detailAnimator.state(this.detailPanel.isOpened());
        };
        this.afterLoadCallback = class768Var2 -> {
            if (this.detailPanel.isOpened()) {
                this.openDetailCallback.accept(class768Var2);
            }
        };
        this.actionButtons.addAll(List.of(new LabeledIconButton(Lang.CONFIG_BUTTON_IMPORT, this::openImportDialog, this.font, new GlTexture(new ClasspathResource("/icons/menu/new/binary.png")), 7.0f, 8.0f, 5.5f, 12), new LabeledIconButton(Lang.CONFIG_BUTTON_CREATE, this::openCreateDialog, this.font, new GlTexture(new ClasspathResource("/icons/menu/new/click.png")), 7.0f, 8.0f, 5.5f, 12)));
        this.sortDropdown = new DropdownWidget<>(this.font, this.sortIcon, 8.0f, 8.0f, 5.5f, 12);
        this.sortDropdown.setOptions(List.of(new DropdownOption(SortOrder.NEWEST_FIRST, Lang.CONFIG_SORT_NEWEST), new DropdownOption(SortOrder.OLDEST_FIRST, Lang.CONFIG_SORT_OLDEST), new DropdownOption(SortOrder.ALPHABETICAL_AZ, Lang.CONFIG_SORT_ALPHABETICAL_AZ), new DropdownOption(SortOrder.ALPHABETICAL_ZA, Lang.CONFIG_SORT_ALPHABETICAL_ZA), new DropdownOption(SortOrder.AUTHORSHIP, Lang.CONFIG_SORT_AUTHOR)), SortOrder.NEWEST_FIRST);
        this.sortDropdown.label(Lang.CONFIG_SORT_NEWEST, 12);
        this.sortDropdown.onSelect(it -> {
            this.currentSortType = it;
            FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
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
    public void render(DrawCtx drawCtx) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        syncActiveSelection(this.tab);
        colorStack115.push();
        colorStack115.alphaAnimation(this.tab.currentTabAnimation());
        this.tab.scrollingAreaComponent().beginArea(drawCtx, this.tab.framesOriginX(), this.tab.framesOriginY() + ap, width(), height() - ap);
        float fMethod008 = framesAlpha();
        colorStack115.push();
        colorStack115.alpha(fMethod008);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame abstractFrame = this.tab.frames().get(size);
            if (abstractFrame.visible() && isFrameVisible(abstractFrame, an, originY() + ap)) {
                abstractFrame.render(drawCtx);
            }
        }
        this.tab.scrollingAreaComponent().endArea(drawCtx, getContentHeight());
        if (needsScrollbar()) {
            this.scrollbar.render(drawCtx);
        }
        this.headerBadge.render(drawCtx);
        drawCtx.text(this.font, Lang.CONFIG_AVAILABLE.effective(), 16, this.headerBounds.x() + 10.0f, this.headerBounds.y() + this.headerBadge.height() + 8.0f, colorStack115.computeColor(themePalette.text().tone(300).argb()));
        this.sortDropdown.render(drawCtx);
        this.actionButtons.forEach(labeledIconButton -> {
            labeledIconButton.render(drawCtx);
        });
        colorStack115.pop();
        renderOverlays(drawCtx);
        if (!this.detailAnimator.isZero()) {
            colorStack115.push();
            colorStack115.alpha(Math.max(this.detailAnimator.smoothAnimation() - 2.0f, ar));
            this.detailPanel.render(drawCtx);
            colorStack115.pop();
        }
        colorStack115.pop();
    }

    @Override
    public void renderOverlays(DrawCtx drawCtx) {
        float physical = drawCtx.layoutContext().toPhysical(this.tab.scrollingAreaComponent().scrollY());
        float fMethod008 = framesAlpha();
        drawCtx.matrixStack().push();
        drawCtx.matrixStack().translate(ar, -physical, ar);
        drawCtx.drawEngine().colorStack().push();
        drawCtx.drawEngine().colorStack().alpha(fMethod008);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame abstractFrame = this.tab.frames().get(size);
            if (abstractFrame.visible()) {
                abstractFrame.renderOverlays(drawCtx);
            }
        }
        drawCtx.drawEngine().colorStack().pop();
        drawCtx.matrixStack().pop();
    }

    public float framesAlpha() {
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        if (fSmoothAnimation < 1.0f || fSmoothAnimation > 2.0f) {
            return fSmoothAnimation < 1.0f ? 1.0f - fSmoothAnimation : fSmoothAnimation - 2.0f;
        }
        return ar;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.tab.frames().forEach(abstractFrame -> {
            abstractFrame.layout(layoutScaleContext);
        });
        this.headerBounds.withPosition(originX() + 12.0f, originY() + 16.0f).withSize(((width() - 12.0f) - (needsScrollbar() ? this.scrollbar.width() : ar)) - 24.0f, 46.0f);
        this.headerBadge.layout(layoutScaleContext);
        this.headerBadge.setPosition(this.headerBounds.x() + 10.0f, this.headerBounds.y());
        this.sortDropdown.layout(layoutScaleContext);
        this.sortDropdown.setPosition(((this.headerBounds.x() + this.headerBounds.width()) - 10.0f) - this.sortDropdown.width(), (this.headerBounds.y() + this.headerBounds.height()) - this.sortDropdown.height());
        float fX = this.sortDropdown.x() - 4.0f;
        for (LabeledIconButton labeledIconButton : this.actionButtons) {
            labeledIconButton.layout(layoutScaleContext);
            float fWidth = fX - labeledIconButton.width();
            labeledIconButton.setPosition(fWidth, this.sortDropdown.y());
            fX = fWidth - 4.0f;
        }
        this.detailPanel.layout(layoutScaleContext);
        this.detailPanel.setSize(310.0f, height());
        this.detailPanel.setPosition(this.tab.framesOriginX() + width(), originY());
        layoutScrollbar(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        this.detailAnimator.animate(weightedEngine);
        if (fSmoothAnimation != this.detailAnimator.smoothAnimation()) {
            FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
        }
        this.detailPanel.animation(weightedEngine);
        this.sortDropdown.animation(weightedEngine);
        this.actionButtons.forEach(labeledIconButton -> {
            labeledIconButton.animation(weightedEngine);
        });
        this.scrollbar.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zAnyMatch = z | this.actionButtons.stream().anyMatch(labeledIconButton -> {
            return labeledIconButton.handleInput(inputEventContext, z);
        });
        boolean zHandleInput = zAnyMatch | this.sortDropdown.handleInput(inputEventContext, zAnyMatch);
        if (needsScrollbar()) {
            zHandleInput |= this.scrollbar.handleInput(inputEventContext, z);
        }
        if (!this.detailAnimator.isZero()) {
            zHandleInput |= this.detailPanel.handleInput(inputEventContext, zHandleInput);
        }
        InputEventContext inputEventContext688 = inputEventContext.withMouseOffset(ar, this.tab.scrollingAreaComponent().scrollY());
        for (AbstractFrame abstractFrame : this.tab.frames()) {
            if (abstractFrame.visible() && isFrameVisible(abstractFrame, an, originY() + ap)) {
                zHandleInput |= abstractFrame.handleInput(inputEventContext688, zHandleInput);
            }
        }
        return zHandleInput | this.tab.scrollingAreaComponent().handleInput(inputEventContext, zHandleInput);
    }

    public void resetToDefaults(CloudConfigCard cloudConfigCard) {
        if (cloudConfigCard == null) {
            return;
        }
        ConfigFile configFile = FluxVisuals.INSTANCE.defaultConfigFile();
        if (configFile == null) {
            FluxVisuals.LOGGER.warn("Default config is not available");
            return;
        }
        FluxVisuals.INSTANCE.cloudConfigService().applyConfig(new LoadedConfig(CloudConfigDto.of(cloudConfigCard.cloudId(), cloudConfigCard.name(), cloudConfigCard.author().name()), configFile));
        FluxVisuals.INSTANCE.cloudConfigService().saveConfig(cloudConfigCard.cloudId(), FluxVisuals.INSTANCE.moduleRepository(), FluxVisuals.INSTANCE.widgetStack()).thenRun(() -> {
            updateConfigList();
            FluxVisuals.INSTANCE.configManager().saveLastConfigID();
        }).exceptionally(th -> {
            FluxVisuals.LOGGER.error("Failed to overwrite config with defaults", th);
            return null;
        });
    }

    public void updateConfigList() {
        FluxVisuals.INSTANCE.cloudConfigService().listConfigs().thenAccept(list -> {
            ArrayList arrayList = new ArrayList();
            Iterator it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(new ConfigCard(new CloudConfigCard((CloudConfigMetadata) it.next(), FluxVisuals.INSTANCE.userSession().texture()), this.openDetailCallback, this.afterLoadCallback));
            }
            this.tab.updateFramesSafe(arrayList);
            FluxVisuals.LOGGER.info("Loaded {} cloud configs", Integer.valueOf(list.size()));
        }).exceptionally(th -> {
            FluxVisuals.LOGGER.error("Failed to load cloud configs", th);
            return null;
        });
    }

    public void selectFrame(ConfigCard configCard) {
        if (this.selectedFrame != null && this.selectedFrame != configCard) {
            this.selectedFrame.selected(false);
        }
        this.selectedFrame = configCard;
        if (this.selectedFrame == null || this.selectedFrame.selected()) {
            return;
        }
        this.selectedFrame.selected(true);
    }

    public void clearSelection() {
        if (this.selectedFrame != null) {
            this.selectedFrame.selected(false);
            this.selectedFrame = null;
        }
    }

    public void syncActiveSelection(MenuTabElement menuTabElement) {
        CloudConfigService cloudConfigService357 = FluxVisuals.INSTANCE.cloudConfigService();
        ConfigCard configCard = null;
        for (AbstractFrame abstractFrame : menuTabElement.frames()) {
            if (abstractFrame instanceof ConfigCard) {
                ConfigCard class818Var2 = (ConfigCard) abstractFrame;
                if (cloudConfigService357.configActive(class818Var2.configContext().cloudId())) {
                    configCard = class818Var2;
                    break;
                }
            }
        }
        if (configCard == null) {
            clearSelection();
        } else {
            selectFrame(configCard);
        }
    }

    @Override
    public float getContentHeight() {
        float f = 0.0f;
        for (AbstractFrame abstractFrame : this.tab.frames()) {
            if (abstractFrame.visible()) {
                float fY = ((abstractFrame.y() - this.tab.framesOriginY()) - ap) + abstractFrame.height() + abstractFrame.contentHeight();
                if (fY > f) {
                    f = fY;
                }
            }
        }
        return f + 10.0f;
    }

    @Override
    public void positionFrames() throws MatchException {
        List<AbstractFrame> listFrames = this.tab.frames();
        sortFrames(listFrames);
        float fFloatValue = ((Float) this.tab.frames().stream().filter((v0) -> {
            return v0.visible();
        }).map((v0) -> {
            return v0.width();
        }).max((v0, v1) -> {
            return Float.compare(v0, v1);
        }).orElse(Float.valueOf(ar))).floatValue();
        int iMax = fFloatValue > ar ? Math.max(1, (int) Math.floor((((((width() - 12.0f) - (needsScrollbar() ? this.scrollbar.width() : ar)) - 24.0f) + 3.0f) + 12.0f) / (fFloatValue + 12.0f))) : 1;
        float[] fArr = new float[iMax];
        float[] fArr2 = new float[iMax];
        for (int i = 0; i < iMax; i++) {
            fArr2[i] = i * (fFloatValue + 12.0f);
        }
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        boolean z = fSmoothAnimation > ar && fSmoothAnimation < 3.0f;
        for (AbstractFrame abstractFrame : listFrames) {
            if (abstractFrame.visible()) {
                int iIndexOfMin = indexOfMin(fArr);
                float f = fArr2[iIndexOfMin];
                float f2 = fArr[iIndexOfMin];
                float fHeight = abstractFrame.height() + abstractFrame.contentHeight();
                if (abstractFrame instanceof ConfigCard) {
                    ((ConfigCard) abstractFrame).targetPosition(originX() + 12.0f + f, originY() + ap + f2, (z || this.tab.forceImmediateFramePositioning()) ? false : true);
                } else {
                    abstractFrame.setPosition(originX() + 12.0f + f, originY() + ap + f2);
                }
                fArr[iIndexOfMin] = fArr[iIndexOfMin] + fHeight + ar;
            }
        }
    }

    public void layoutScrollbar(LayoutScaleContext layoutScaleContext) {
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT;
        this.scrollbar.setPosition(((originX() + width()) - 12.0f) - this.scrollbar.width(), this.headerBounds.y() + this.headerBounds.height());
        this.scrollbar.setSize(this.scrollbar.width(), f);
        this.scrollbar.layout(layoutScaleContext);
    }

    public void sortFrames(List<AbstractFrame> list) throws MatchException {
        Comparator<? super AbstractFrame> comparator;
        switch (this.currentSortType.ordinal()) {
            case 0:
                comparator = (abstractFrame, class757Var2) -> {
                    if (!(abstractFrame instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard configCard818___ = (ConfigCard) abstractFrame;
                    if (!(class757Var2 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var2;
                    boolean zFavorite = configCard818___.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    return Long.compare(class818Var2.configContext().lastModified(), configCard818___.configContext().lastModified());
                };
                break;
            case 1:
                comparator = (class757Var3, class757Var4) -> {
                    if (!(class757Var3 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard configCard818___ = (ConfigCard) class757Var3;
                    if (!(class757Var4 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var4;
                    boolean zFavorite = configCard818___.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    int iCompare = Long.compare(configCard818___.configContext().lastModified(), class818Var2.configContext().lastModified());
                    return iCompare != 0 ? iCompare : configCard818___.configContext().cloudId().compareToIgnoreCase(class818Var2.configContext().cloudId());
                };
                break;
            case 2:
                comparator = (class757Var5, class757Var6) -> {
                    if (!(class757Var5 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard configCard818___ = (ConfigCard) class757Var5;
                    if (!(class757Var6 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var6;
                    boolean zFavorite = configCard818___.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    int iCompareToIgnoreCase = configCard818___.configContext().name().compareToIgnoreCase(class818Var2.configContext().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : configCard818___.configContext().cloudId().compareToIgnoreCase(class818Var2.configContext().cloudId());
                };
                break;
            case 3:
                comparator = (class757Var7, class757Var8) -> {
                    if (!(class757Var7 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard configCard818___ = (ConfigCard) class757Var7;
                    if (!(class757Var8 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var8;
                    boolean zFavorite = configCard818___.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    int iCompareToIgnoreCase = class818Var2.configContext().name().compareToIgnoreCase(configCard818___.configContext().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : configCard818___.configContext().cloudId().compareToIgnoreCase(class818Var2.configContext().cloudId());
                };
                break;
            case 4:
                comparator = (class757Var9, class757Var10) -> {
                    if (!(class757Var9 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard configCard818___ = (ConfigCard) class757Var9;
                    if (!(class757Var10 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var10;
                    boolean zFavorite = configCard818___.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    int iCompareToIgnoreCase = configCard818___.configContext().author().name().compareToIgnoreCase(class818Var2.configContext().author().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : configCard818___.configContext().name().compareToIgnoreCase(class818Var2.configContext().name());
                };
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        list.sort(comparator);
    }

    public void openImportDialog() {
        String[] strArr = {""};
        ActionConfirmDialog actionConfirmDialog = FluxVisuals.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        actionConfirmDialog.open(new ActionDialogBuilder().title(Lang.CONFIG_IMPORT_TITLE).placeholderText(Lang.CONFIG_ACTION_PLACEHOLDER).icon(this.folderIcon).content(frameElementColumn -> {
            TextFieldSettingElement textFieldSettingElement = new TextFieldSettingElement(Lang.CONFIG_IMPORT_ID_LABEL, Lang.CONFIG_IMPORT_ID_DESCRIPTION, Lang.CONFIG_IMPORT_ID_PLACEHOLDER, false, 50, false, () -> {
                return "";
            });
            textFieldSettingElement.onChange(str -> {
                strArr[0] = str.trim();
                actionConfirmDialog.hideError();
            });
            textFieldSettingElement.focusAtEnd();
            frameElementColumn.addFrameElement(textFieldSettingElement);
        }).confirmLabel(Lang.CONFIG_IMPORT_CONFIRM).enableConfirmButton(() -> {
            return strArr[0].trim().length() == 16;
        }).onConfirm(() -> {
            String strTrim = strArr[0].trim();
            actionConfirmDialog.setLoading(true, Lang.CONFIG_IMPORT_LOADING.effective());
            FluxVisuals.INSTANCE.cloudConfigService().importConfig(strTrim).thenCompose(r4 -> {
                return FluxVisuals.INSTANCE.cloudConfigService().downloadConfig(strTrim);
            }).thenAccept(loadedConfig -> {
                FluxVisuals.INSTANCE.cloudConfigService().applyConfig(loadedConfig);
                actionConfirmDialog.setLoading(false, "");
                updateConfigList();
                FluxVisuals.LOGGER.info("Config imported & applied: {} by {}", loadedConfig.details().name(), loadedConfig.details().author());
                actionConfirmDialog.close();
                FluxVisuals.INSTANCE.configManager().saveLastConfigID();
            }).exceptionally(th -> {
                actionConfirmDialog.setLoading(false, "");
                FluxVisuals.LOGGER.error("Failed to import/apply config", th);
                actionConfirmDialog.showError(FluxVisuals.INSTANCE.cloudConfigService().errorMessage(th).effective());
                return null;
            });
        }).build());
    }

    public void openCreateDialog() {
        String[] strArr = {""};
        ActionConfirmDialog actionConfirmDialog = FluxVisuals.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        actionConfirmDialog.open(new ActionDialogBuilder().title(Lang.CONFIG_CREATE_TITLE).placeholderText(Lang.CONFIG_ACTION_PLACEHOLDER).icon(this.folderIcon).content(frameElementColumn -> {
            TextFieldSettingElement textFieldSettingElement = new TextFieldSettingElement(Lang.CONFIG_NAME_LABEL, Lang.CONFIG_NAME_DESCRIPTION, Lang.CONFIG_NAME_PLACEHOLDER, false, 30, false, () -> {
                return "";
            });
            textFieldSettingElement.onChange(str -> {
                strArr[0] = str.trim();
                actionConfirmDialog.hideError();
            });
            textFieldSettingElement.focusAtEnd();
            frameElementColumn.addFrameElement(textFieldSettingElement);
        }).confirmLabel(Lang.CONFIG_CREATE_CONFIRM).enableConfirmButton(() -> {
            return !strArr[0].trim().isEmpty();
        }).onConfirm(() -> {
            String strTrim = strArr[0].trim();
            actionConfirmDialog.setLoading(true, Lang.CONFIG_CREATE_LOADING.effective());
            FluxVisuals.INSTANCE.cloudConfigService().createConfig(strTrim, FluxVisuals.INSTANCE.moduleRepository(), FluxVisuals.INSTANCE.widgetStack()).thenAccept(str -> {
                actionConfirmDialog.setLoading(false, "");
                updateConfigList();
                FluxVisuals.LOGGER.info("Config uploaded: {} (id: {})", strTrim, str);
                actionConfirmDialog.close();
            }).exceptionally(th -> {
                actionConfirmDialog.setLoading(false, "");
                FluxVisuals.LOGGER.error("Failed to upload config", th);
                actionConfirmDialog.showError(FluxVisuals.INSTANCE.cloudConfigService().errorMessage(th).effective());
                return null;
            });
        }).build());
    }

    public void closeDetail() {
        if (this.detailPanel.isOpened()) {
            this.detailPanel.close();
            this.detailAnimator.state(false);
            FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
        }
    }

    public void applyConfigById(String str) {
        FluxVisuals.INSTANCE.cloudConfigService().downloadConfig(str).thenAccept(loadedConfig -> {
            FluxVisuals.INSTANCE.cloudConfigService().applyConfig(loadedConfig);
            FluxVisuals.LOGGER.info("Config applied: {} by {}", loadedConfig.details().name(), loadedConfig.details().author());
            updateConfigList();
            FluxVisuals.INSTANCE.configManager().saveLastConfigID();
        }).exceptionally(th -> {
            FluxVisuals.LOGGER.error("Failed to load/apply config", th);
            return null;
        });
    }

    public void saveConfig(CloudConfigCard cloudConfigCard) {
        if (cloudConfigCard == null) {
            return;
        }
        FluxVisuals.INSTANCE.cloudConfigService().saveConfig(cloudConfigCard.cloudId(), FluxVisuals.INSTANCE.moduleRepository(), FluxVisuals.INSTANCE.widgetStack()).thenRun(() -> {
            FluxVisuals.LOGGER.info("Config saved: {}", cloudConfigCard.cloudId());
            updateConfigList();
        }).exceptionally(th -> {
            FluxVisuals.LOGGER.error("Failed to save config", th);
            return null;
        });
    }

    public boolean needsScrollbar() {
        return getContentHeight() > (((MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT) - this.headerBounds.height()) - 16.0f) + 0.5f;
    }

    @Override
    public float width() {
        return MenuWindow.MENU_WIDTH - (this.detailPanel.width() * ((Math.max(ar, Math.min(1.0f, this.detailAnimator.smoothAnimation() - 1.0f)) / 2.0f) * 2.0f));
    }

    public ConfigCard getSelectedFrame() {
        return this.selectedFrame;
    }

    public SortOrder getCurrentSortType() {
        return this.currentSortType;
    }
}
