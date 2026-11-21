package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BindMode;
import ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ButtonSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ButtonSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.CheckboxSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.setting.ColorSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ColorSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.ConfigAutoSaveScheduler;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.setting.EnumSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ExpandableSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.setting.GroupSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.render.HighlightAnimation;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.setting.ModeSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ModeSettingElement;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModule;
import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModuleAdapter;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.setting.MultiSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.MultiSelectSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.NumberSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.OrderedEnumSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.OrderedListSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;
import ru.fluxvisuals.ui.gui.clickgui.setting.SeparatorSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.setting.SliderSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.SoundModeSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.TextFieldSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.TextFieldSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModuleCard extends AbstractFrame {
    public final GlTexture frameIcon;
    public final GlTexture starIcon;
    public final GlTexture starFilledIcon;
    public final MsdfFont font;
    static final float height = 48.0f;
    public final ClickGuiModule clickGuiModule;
    public final Module module;
    public final String[] aliases;
    public final String name;
    public final ToggleSwitch toggleSwitch;

    public final KeybindField keybindField;
    static final float columnWidth = 252.0f;
    static final float padding = 14.0f;
    public boolean favorite;
    public final TextureToggleWidget favoriteToggle;
    public final ToggleAnimator nameHoverAnimation;
    public float nameX;
    public float nameY;
    public float nameWidth;
    public float nameHeight;
    public final AnimatedFloat xAnimation;
    public final AnimatedFloat yAnimation;

    public final KeybindConfigWindow keybindWindow;
    public final WidgetBounds keybindBounds;
    public float scrollOffset;

    public final HighlightAnimation highlightAnimation;

    public ScreenResolution resolution;
    public boolean positionInitialized;
    public static final float gap = 8.0f;
    public static final float screenMargin = 5.0f;
    public boolean dragging;

    public ModuleCard(Module module605, String[] strArr) {
        this(new ClickGuiModuleAdapter(module605), module605, strArr);
    }

    public ModuleCard(ClickGuiModule clickGuiModule, Module module605, String[] strArr) {
        super(new FrameElementColumn(columnWidth, padding));
        this.frameIcon = new GlTexture(new ClasspathResource("/icons/menu/new/frame.png"));
        this.starIcon = new GlTexture(new ClasspathResource("/icons/menu/new/star.png"));
        this.starFilledIcon = new GlTexture(new ClasspathResource("/icons/menu/new/star_fill.png"));
        this.font = Fonts.INTER_SEMIBOLD.get();
        this.nameHoverAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
        this.xAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.yAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.keybindBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
        this.clickGuiModule = clickGuiModule;
        this.module = module605;
        this.aliases = strArr;
        this.name = clickGuiModule.getName();
        this.favorite = FluxVisuals.INSTANCE.configManager().menuStateConfig().isModuleFavorite(this.name);
        Objects.requireNonNull(clickGuiModule);
        this.toggleSwitch = new ToggleSwitch(24.0f, 17.0f, clickGuiModule::isState);
        ToggleSwitch toggleSwitch754 = this.toggleSwitch;
        Objects.requireNonNull(clickGuiModule);
        toggleSwitch754.runnable(wrapWithAutoSave(clickGuiModule::switchState));
        Translation translation = Lang.BINDING_MODULE;
        String name = clickGuiModule.getName();
        Objects.requireNonNull(clickGuiModule);
        this.keybindWindow = new KeybindConfigWindow(translation, name, clickGuiModule::getKeyBind);
        KeybindConfigWindow keybindConfigWindow = this.keybindWindow;
        Objects.requireNonNull(clickGuiModule);
        Supplier<BindMode> supplier = clickGuiModule::getType;
        Objects.requireNonNull(clickGuiModule);
        keybindConfigWindow.bindType(supplier, clickGuiModule::setType);
        KeybindConfigWindow class755Var2 = this.keybindWindow;
        Objects.requireNonNull(clickGuiModule);
        class755Var2.onChange(wrapConsumerWithAutoSave(clickGuiModule::setKey));
        FluxVisuals.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.keybindWindow);
        this.favoriteToggle = new TextureToggleWidget(this.starIcon, this.starFilledIcon, () -> {
            return Boolean.valueOf(this.favorite);
        }, padding, padding);
        this.favoriteToggle.runnable(this::invertFavorite);
        this.keybindField = new KeybindField(Fonts.INTER_SEMIBOLD.get(), () -> {
            toggleKeybindWindow();
        }, gap, 4.0f, 6.0f, 11).addMutableKeys(clickGuiModule.getKeyBind());
        KeybindField keybindField743 = this.keybindField;
        Objects.requireNonNull(clickGuiModule);
        keybindField743.onChange(wrapConsumerWithAutoSave(clickGuiModule::setKey));
        clickGuiModule.getSettings().forEach(setting -> {
            if (setting instanceof BooleanSetting) {
                BooleanSetting booleanSetting = (BooleanSetting) setting;
                Translation name2 = booleanSetting.getName();
                Translation description = booleanSetting.getDescription();
                Objects.requireNonNull(booleanSetting);
                CheckboxSettingElement checkboxSettingElement = new CheckboxSettingElement(name2, description, booleanSetting::getKeyBind);
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.toggledSupplier(booleanSetting::isValue);
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.changeRunnable(wrapWithAutoSave(booleanSetting::switchValue));
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.onChange(wrapConsumerWithAutoSave(booleanSetting::setKey));
                Objects.requireNonNull(booleanSetting);
                Supplier<BindMode> supplier2 = booleanSetting::getType;
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.setupBindType(supplier2, booleanSetting::setType);
                checkboxSettingElement.visibleSupplier(booleanSetting.getVisible());
                this.frameElementsContainer.addFrameElement(checkboxSettingElement);
                return;
            }
            if (setting instanceof NumberSetting) {
                NumberSetting numberSetting = (NumberSetting) setting;
                SliderSettingElement sliderSettingElement = new SliderSettingElement(numberSetting.getName(), numberSetting.getDescription(), numberSetting.valueUnit(), numberSetting.max(), numberSetting.min(), numberSetting.step());
                Objects.requireNonNull(numberSetting);
                sliderSettingElement.currentValueSupplier(numberSetting::currentValue);
                Objects.requireNonNull(numberSetting);
                sliderSettingElement.onValueChanged(wrapConsumerWithAutoSave((v1) -> {
                    numberSetting.setCurrentValue(v1);
                }));
                sliderSettingElement.visibleSupplier(numberSetting.getVisible());
                this.frameElementsContainer.addFrameElement(sliderSettingElement);
                return;
            }
            if (setting instanceof ModeSetting) {
                addModeElement((ModeSetting) setting);
                return;
            }
            if (setting instanceof EnumSetting) {
                addSoundModeElement((EnumSetting) setting);
                return;
            }
            if (setting instanceof MultiSelectSetting) {
                addMultiSelectElement((MultiSelectSetting) setting);
                return;
            }
            if (setting instanceof TextFieldSetting) {
                TextFieldSetting textFieldSetting = (TextFieldSetting) setting;
                Translation name3 = textFieldSetting.getName();
                Translation description2 = textFieldSetting.getDescription();
                Translation placeholder = textFieldSetting.getPlaceholder();
                boolean zIsOnlyDigits = textFieldSetting.isOnlyDigits();
                int max = textFieldSetting.getMax();
                boolean zIsPassword = textFieldSetting.isPassword();
                Objects.requireNonNull(textFieldSetting);
                TextFieldSettingElement textFieldSettingElement = new TextFieldSettingElement(name3, description2, placeholder, zIsOnlyDigits, max, zIsPassword, textFieldSetting::getText);
                Objects.requireNonNull(textFieldSetting);
                textFieldSettingElement.onChange(wrapConsumerWithAutoSave(textFieldSetting::setText));
                textFieldSettingElement.visibleSupplier(textFieldSetting.getVisible());
                this.frameElementsContainer.addFrameElement(textFieldSettingElement);
                return;
            }
            if (setting instanceof ButtonSetting) {
                ButtonSetting buttonSetting = (ButtonSetting) setting;
                ButtonSettingElement buttonSettingElement = new ButtonSettingElement(buttonSetting.getName(), buttonSetting.getDescription(), buttonSetting.getButtonName(), wrapWithAutoSave(buttonSetting.getRunnable()));
                buttonSettingElement.visibleSupplier(buttonSetting.getVisible());
                this.frameElementsContainer.addFrameElement(buttonSettingElement);
                return;
            }
            if (setting instanceof OrderedEnumSetting) {
                addOrderedListElement((OrderedEnumSetting) setting);
                return;
            }
            if (setting instanceof ColorSetting) {
                ColorSetting colorSetting = (ColorSetting) setting;
                Translation name4 = colorSetting.getName();
                Translation description3 = colorSetting.getDescription();
                Objects.requireNonNull(colorSetting);
                Supplier supplier3 = colorSetting::getColorWithoutAlpha;
                Objects.requireNonNull(colorSetting);
                ColorSettingElement colorSettingElement = new ColorSettingElement(name4, description3, supplier3, colorSetting::getAlpha);
                colorSettingElement.visibleSupplier(colorSetting.getVisible());
                Objects.requireNonNull(colorSetting);
                colorSettingElement.colorConsumer((v1) -> {
                    colorSetting.setColor(v1);
                });
                Objects.requireNonNull(colorSetting);
                colorSettingElement.alphaConsumer((v1) -> {
                    colorSetting.setAlpha(v1);
                });
                colorSettingElement.commitRunnable(this::scheduleAutoSave);
                this.frameElementsContainer.addFrameElement(colorSettingElement);
                return;
            }
            if (setting instanceof ExpandableSetting) {
                ExpandableSetting expandableSetting = (ExpandableSetting) setting;
                GroupSettingElement groupSettingElement = new GroupSettingElement(expandableSetting.getName(), expandableSetting.getDescription(), expandableSetting.getSubSettings(), expandableSetting.getKeybinds());
                Objects.requireNonNull(expandableSetting);
                groupSettingElement.toggledSupplier(expandableSetting::isValue);
                Objects.requireNonNull(expandableSetting);
                groupSettingElement.changeRunnable(wrapWithAutoSave(expandableSetting::switchValue));
                groupSettingElement.visibleSupplier(expandableSetting.getVisible());
                Objects.requireNonNull(expandableSetting);
                groupSettingElement.onChange(wrapConsumerWithAutoSave(expandableSetting::setKey));
                this.frameElementsContainer.addFrameElement(groupSettingElement);
                return;
            }
            if (!(setting instanceof KeybindSetting)) {
                if (setting instanceof SeparatorSetting) {
                    this.frameElementsContainer.addFrameElement(new SeparatorElement());
                    return;
                }
                return;
            }
            KeybindSetting keybindSetting = (KeybindSetting) setting;
            Translation name5 = keybindSetting.getName();
            Translation description4 = keybindSetting.getDescription();
            Objects.requireNonNull(keybindSetting);
            KeybindSettingElement keybindSettingElement = new KeybindSettingElement(name5, description4, keybindSetting::getKeyBind);
            keybindSettingElement.visibleSupplier(keybindSetting.getVisible());
            Objects.requireNonNull(keybindSetting);
            keybindSettingElement.onChange(wrapConsumerWithAutoSave(keybindSetting::setKey));
            this.frameElementsContainer.addFrameElement(keybindSettingElement);
        });
        addChild(this.toggleSwitch);
        addChild(this.favoriteToggle);
        addChild(this.keybindField);
    }

    public <E extends Enum<E>> void addOrderedListElement(OrderedEnumSetting<E> orderedEnumSetting) {
        Objects.requireNonNull(orderedEnumSetting);
        Supplier<List<E>> supplier = orderedEnumSetting::orderedValues;
        Objects.requireNonNull(orderedEnumSetting);
        Supplier<Set<E>> supplier2 = orderedEnumSetting::getSelected;
        Objects.requireNonNull(orderedEnumSetting);
        OrderedListSettingElement<E> orderedListSettingElement = new OrderedListSettingElement<>(supplier, supplier2, orderedEnumSetting::optionName, wrapConsumerWithAutoSave(e -> {
            if (orderedEnumSetting.isSelected(e)) {
                orderedEnumSetting.deselect(e);
            } else {
                orderedEnumSetting.select(e);
            }
        }), (num, num2) -> {
            orderedEnumSetting.move(num.intValue(), num2.intValue());
            scheduleAutoSave();
        });
        orderedListSettingElement.visibleSupplier(orderedEnumSetting.getVisible());
        this.frameElementsContainer.addFrameElement(orderedListSettingElement);
    }

    public void toggleKeybindWindow() {
        float fDpiScaleFactor = FluxVisuals.INSTANCE.windowController().dpiScaleFactor();
        if (this.keybindWindow.isOpen() || this.resolution == null) {
            this.keybindWindow.closeWindow();
            return;
        }
        float fScreenWidth = this.resolution.screenWidth() / fDpiScaleFactor;
        float fScreenHeight = this.resolution.screenHeight() / fDpiScaleFactor;
        float fWidth = this.keybindWindow.width();
        float f = this.keybindWindow.totalHeight();
        float fX = this.keybindBounds.x();
        float fY = this.keybindBounds.y() - this.scrollOffset;
        float fWidth2 = this.keybindBounds.width();
        float fHeight = this.keybindBounds.height();
        float f2 = fX + fWidth2 + gap;
        float f3 = (fX - gap) - fWidth;
        if (f2 + fWidth > fScreenWidth - screenMargin && f3 >= screenMargin) {
            f2 = f3;
        }
        float fMax = Math.max(screenMargin, Math.min(f2, (fScreenWidth - fWidth) - screenMargin));
        float f4 = (fY + (fHeight / 2.0f)) - (f / 2.0f);
        if (f4 + f > fScreenHeight - screenMargin) {
            f4 = (fScreenHeight - f) - screenMargin;
        }
        if (f4 < screenMargin) {
            f4 = 5.0f;
        }
        this.keybindWindow.openWindow();
        this.keybindWindow.setPosition(fMax, f4);
    }

    public <E extends Enum<E>> void addModeElement(ModeSetting<E> modeSetting) {
        ModeSettingElement modeSettingElement = new ModeSettingElement(modeSetting.getName(), modeSetting.getDescription(), modeSetting.options());
        Objects.requireNonNull(modeSetting);
        modeSettingElement.onChangeCallback(wrapConsumerWithAutoSave(modeSetting::setCurrentValue));
        Objects.requireNonNull(modeSetting);
        modeSettingElement.currentValueSupplier(modeSetting::currentValue);
        modeSettingElement.visibleSupplier(modeSetting.getVisible());
        this.frameElementsContainer.addFrameElement(modeSettingElement);
    }

    public <E extends Enum<E>> void addMultiSelectElement(MultiSelectSetting<E> multiSelectSetting) {
        Translation name = multiSelectSetting.getName();
        Translation description = multiSelectSetting.getDescription();
        Enum[] enumArrOptions = multiSelectSetting.options();
        Objects.requireNonNull(multiSelectSetting);
        Supplier supplier = multiSelectSetting::selectedValues;
        Objects.requireNonNull(multiSelectSetting);
        MultiSelectSettingElement multiSelectSettingElement = new MultiSelectSettingElement(name, description, enumArrOptions, supplier, wrapConsumerWithAutoSave(multiSelectSetting::toggle));
        multiSelectSettingElement.visibleSupplier(multiSelectSetting.getVisible());
        this.frameElementsContainer.addFrameElement(multiSelectSettingElement);
    }

    public <E extends Enum<E>> void addSoundModeElement(EnumSetting<E> enumSetting) {
        SoundModeSettingElement soundModeSettingElement = new SoundModeSettingElement(enumSetting.getName(), enumSetting.getDescription(), enumSetting.options(), enumSetting.soundAction());
        Objects.requireNonNull(enumSetting);
        soundModeSettingElement.onChangeCallback(wrapConsumerWithAutoSave(enumSetting::setCurrentValue));
        Objects.requireNonNull(enumSetting);
        soundModeSettingElement.currentValueSupplier(enumSetting::currentValue);
        soundModeSettingElement.visibleSupplier(enumSetting.getVisible());
        this.frameElementsContainer.addFrameElement(soundModeSettingElement);
    }

    public void scheduleAutoSave() {
        ConfigAutoSaveScheduler.scheduleAutoSave();
    }

    public Runnable wrapWithAutoSave(Runnable runnable) {
        return () -> {
            if (runnable != null) {
                runnable.run();
            }
            scheduleAutoSave();
        };
    }

    public <T> Consumer<T> wrapConsumerWithAutoSave(Consumer<T> consumer) {
        return obj -> {
            consumer.accept(obj);
            scheduleAutoSave();
        };
    }

    public List<Setting> settings() {
        return this.clickGuiModule.getSettings();
    }

    @Override
    public void updateViewportVisibility(float f, float f2, float f3) {
        this.scrollOffset = f;
        super.updateViewportVisibility(f, f2, f3);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        this.resolution = drawCtx.resolution();
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        this.favoriteToggle.inactiveColor(themePalette.text().tone(600));
        this.favoriteToggle.activeColor(ThemePalette.golden);
        drawCtx.fillOutlinedRoundedRect(x(), y(), width(), height() + contentHeight(), 12.0f, 2.5f, colorStack115.computeColor(ThemePalette.white.argb(), 0.03f), colorStack115.computeColor(themePalette.frameBackground().argb()));
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.accent().argb()), this.toggleSwitch.toggleSwitchAnimation());
        int iComputeColor = colorStack115.computeColor(themePalette.text().tone(100).argb());
        drawCtx.texture(this.frameIcon, x() + padding, (y() + (height() / 2.0f)) - (this.frameIcon.height() / 2.0f), this.frameIcon.width(), this.frameIcon.height(), iInterpolate);
        this.nameX = x() + padding + this.frameIcon.width() + gap;
        this.nameY = (y() + (height() / 2.0f)) - (this.font.getHeight(15.0f) / 2.0f);
        this.nameWidth = drawCtx.textWidthPhysical(this.font, this.name, 15);
        this.nameHeight = this.font.getHeight(15.0f);
        float fX = this.favoriteToggle.x() - 20.0f;
        String str = this.name;
        boolean z = this.nameX + this.nameWidth > fX;
        float fSmoothAnimation = z ? 1.0f - this.nameHoverAnimation.smoothAnimation() : 1.0f;
        if (fSmoothAnimation > 0.1f && z) {
            int iMax = Math.max(0, this.name.length() - ((int) Math.ceil(((this.nameX + this.nameWidth) - fX) / (this.nameWidth / this.name.length()))));
            String strSubstring = this.name.substring(0, iMax);
            if (iMax < this.name.length()) {
                strSubstring = strSubstring + "...";
            }
            str = strSubstring;
        }
        drawCtx.text(this.font, str, 15, this.nameX, this.nameY, iComputeColor);
        renderFrameElements(drawCtx);
        colorStack115.push();
        colorStack115.alpha(fSmoothAnimation);
        this.favoriteToggle.render(drawCtx);
        this.keybindField.render(drawCtx);
        this.keybindBounds.withPosition(this.keybindField.x(), this.keybindField.y()).withSize(this.keybindField.width(), this.keybindField.height());
        this.toggleSwitch.render(drawCtx);
        this.toggleSwitch.setClickableArea(x(), y(), width(), height());
        colorStack115.pop();
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.dragging = z;
        if (z && this.positionInitialized) {
            this.xAnimation.set(x());
            this.yAnimation.set(y());
            this.xAnimation.destination(x());
            this.yAnimation.destination(y());
        }
    }

    @Override
    public void renderOverlays(DrawCtx drawCtx) {
        renderFrameOverlays(drawCtx);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.keybindWindow.layout(layoutScaleContext);
        this.frameIcon.setDimensions(14, 14);
        this.starIcon.setDimensions(14, 14);
        this.starFilledIcon.setDimensions(14, 14);
        this.toggleSwitch.setPosition(((x() + width()) - padding) - this.toggleSwitch.width(), (y() + (height() / 2.0f)) - (this.toggleSwitch.height() / 2.0f));
        this.keybindField.rawX(() -> {
            return Float.valueOf(((((x() + width()) - padding) - this.toggleSwitch.width()) - gap) - this.keybindField.computeDesiredWidth(layoutScaleContext));
        });
        float fX = ((((x() + width()) - padding) - this.toggleSwitch.width()) - gap) - this.keybindField.width();
        this.keybindField.setPosition(fX, (y() + (height() / 2.0f)) - (this.keybindField.height() / 2.0f));
        this.favoriteToggle.setPosition((fX - gap) - this.favoriteToggle.width(), (y() + (height() / 2.0f)) - (this.favoriteToggle.height() / 2.0f));
        this.favoriteToggle.hitboxPadding(4.0f);
        this.frameElementsContainer.setPosition(x() + padding, Math.max(Math.max(this.toggleSwitch.y() + this.toggleSwitch.height(), this.keybindField.y() + this.keybindField.height()), this.favoriteToggle.y() + this.favoriteToggle.height()) + padding);
        super.layout(layoutScaleContext);
    }

    public void invertFavorite() {
        this.favorite = !this.favorite;
        try {
            FluxVisuals.INSTANCE.configManager().menuStateConfig().setModuleFavorite(this.name, this.favorite);
            FluxVisuals.INSTANCE.configManager().saveMenuState();
        } catch (IOException e) {
            FluxVisuals.LOGGER.error("Failed to save module favorite state", e);
        }
        FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
    }

    public void targetPosition(float f, float f2, boolean z) {
        if (!this.positionInitialized || !z) {
            this.positionInitialized = true;
            snapTo(f, f2);
        } else if (this.dragging) {
            snapTo(f, f2);
        } else {
            this.xAnimation.destination(f);
            this.yAnimation.destination(f2);
        }
    }

    public void snapTo(float f, float f2) {
        setPosition(f, f2);
        this.xAnimation.set(f);
        this.yAnimation.set(f2);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        this.nameHoverAnimation.state(inputEventContext.inArea(this.nameX, this.nameY, this.nameWidth, this.nameHeight) && !z);
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        super.collectBlurElements(overlayCommandQueue);
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        this.keybindWindow.collectBloomElements(renderCommandQueue);
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.nameHoverAnimation.animate(weightedEngine);
        this.keybindWindow.animation(weightedEngine);
        this.keybindField.syncKeys(this.clickGuiModule.getKeyBind());
        if (this.positionInitialized) {
            this.xAnimation.animate(weightedEngine);
            this.yAnimation.animate(weightedEngine);
            setPosition(this.xAnimation.animatedValue(), this.yAnimation.animatedValue());
        }
        this.highlightAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public float width() {
        return 280.0f;
    }

    @Override
    public float height() {
        return height;
    }

    @Override
    public float contentHeight() {
        if (this.frameElementsContainer.height() <= 0.0f) {
            return 0.0f;
        }
        return padding + this.frameElementsContainer.height();
    }

    public Module module() {
        return this.module;
    }

    public String[] aliases() {
        return this.aliases;
    }

    public String name() {
        return this.name;
    }

    public boolean favorite() {
        return this.favorite;
    }

    public HighlightAnimation highlightAnimation() {
        return this.highlightAnimation;
    }
}