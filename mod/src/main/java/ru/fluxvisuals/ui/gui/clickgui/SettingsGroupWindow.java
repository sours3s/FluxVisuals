package ru.fluxvisuals.ui.gui.clickgui;
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
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.setting.EnumSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ExpandableSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.setting.GroupSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.setting.ModeSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ModeSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.setting.MultiSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.MultiSelectSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.NumberSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.OrderedEnumSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.OrderedListSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
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

import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingsGroupWindow extends OverlayWidget {
    public final MsdfFont font;
    public final GlTexture propertyIcon;
    public final GlTexture groupIcon;
    public final IconLabelBadge settingsBadge;
    public final ToggleAnimator openAnimator;
    public final Translation title;

    public final FrameElementColumn settingsColumn;

    public final ScrollArea scrollArea;

    public final ScrollbarWidget scrollbar;
    public final WidgetBounds titleBounds;
    public final float headerHeight;
    public final ToggleAnimator hoverAnimator;

    public SettingsGroupWindow(Translation translation, List<Setting> list) {
        this(translation, list, true);
    }

    public SettingsGroupWindow(Translation translation, List<Setting> list, boolean z) {
        this.font = Fonts.INTER_SEMIBOLD.get();
        this.propertyIcon = new GlTexture(new ClasspathResource("/icons/menu/new/property.png"));
        this.groupIcon = new GlTexture(new ClasspathResource("/icons/menu/new/group.png"));
        this.settingsBadge = new IconLabelBadge(this.propertyIcon, Translation.clearText("SETTINGS GROUP"));
        this.openAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);
        this.scrollArea = new ScrollArea();
        this.titleBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.hoverAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
        setSize(290.0f, 48.0f);
        ScrollArea scrollArea789 = this.scrollArea;
        Objects.requireNonNull(scrollArea789);
        Supplier supplier = scrollArea789::scrollY;
        Supplier supplier2 = this::contentHeight;
        ScrollArea class789Var2 = this.scrollArea;
        Objects.requireNonNull(class789Var2);
        Supplier supplier3 = class789Var2::height;
        ScrollArea class789Var3 = this.scrollArea;
        Objects.requireNonNull(class789Var3);
        this.scrollbar = new ScrollbarWidget(supplier, supplier2, supplier3, (v1) -> {
            scrollArea789.scrollTo(v1);
        }, 8.0f, 4.0f, 30.0f);
        this.settingsColumn = new FrameElementColumn(computeColumnWidth(), 14.0f);
        this.settingsColumn.clearElements();
        this.headerHeight = height();
        this.title = translation;
        list.forEach(setting -> {
            if (setting instanceof BooleanSetting) {
                BooleanSetting booleanSetting = (BooleanSetting) setting;
                Translation name = booleanSetting.getName();
                Translation description = booleanSetting.getDescription();
                Objects.requireNonNull(booleanSetting);
                CheckboxSettingElement checkboxSettingElement = new CheckboxSettingElement(name, description, booleanSetting::getKeyBind, z);
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.toggledSupplier(booleanSetting::isValue);
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.changeRunnable(wrapWithAutoSave(booleanSetting::switchValue));
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.onChange(wrapConsumerWithAutoSave(booleanSetting::setKey));
                Objects.requireNonNull(booleanSetting);
                Supplier<BindMode> supplier4 = booleanSetting::getType;
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.setupBindType(supplier4, booleanSetting::setType);
                checkboxSettingElement.visibleSupplier(booleanSetting.getVisible());
                this.settingsColumn.addFrameElement(checkboxSettingElement);
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
                this.settingsColumn.addFrameElement(sliderSettingElement);
                return;
            }
            if (setting instanceof ModeSetting) {
                addModeSetting((ModeSetting) setting);
                return;
            }
            if (setting instanceof EnumSetting) {
                addEnumSetting((EnumSetting) setting);
                return;
            }
            if (setting instanceof MultiSelectSetting) {
                addMultiSelectSetting((MultiSelectSetting) setting);
                return;
            }
            if (setting instanceof TextFieldSetting) {
                TextFieldSetting textFieldSetting = (TextFieldSetting) setting;
                Translation name2 = textFieldSetting.getName();
                Translation description2 = textFieldSetting.getDescription();
                Translation placeholder = textFieldSetting.getPlaceholder();
                boolean zIsOnlyDigits = textFieldSetting.isOnlyDigits();
                int max = textFieldSetting.getMax();
                boolean zIsPassword = textFieldSetting.isPassword();
                Objects.requireNonNull(textFieldSetting);
                TextFieldSettingElement textFieldSettingElement = new TextFieldSettingElement(name2, description2, placeholder, zIsOnlyDigits, max, zIsPassword, textFieldSetting::getText);
                Objects.requireNonNull(textFieldSetting);
                textFieldSettingElement.onChange(wrapConsumerWithAutoSave(textFieldSetting::setText));
                textFieldSettingElement.visibleSupplier(textFieldSetting.getVisible());
                this.settingsColumn.addFrameElement(textFieldSettingElement);
                return;
            }
            if (setting instanceof ButtonSetting) {
                ButtonSetting buttonSetting = (ButtonSetting) setting;
                ButtonSettingElement buttonSettingElement = new ButtonSettingElement(buttonSetting.getName(), buttonSetting.getDescription(), buttonSetting.getButtonName(), wrapWithAutoSave(buttonSetting.getRunnable()));
                buttonSettingElement.visibleSupplier(buttonSetting.getVisible());
                this.settingsColumn.addFrameElement(buttonSettingElement);
                return;
            }
            if (setting instanceof ColorSetting) {
                ColorSetting colorSetting = (ColorSetting) setting;
                Translation name3 = colorSetting.getName();
                Translation description3 = colorSetting.getDescription();
                Objects.requireNonNull(colorSetting);
                Supplier supplier5 = colorSetting::getColorWithoutAlpha;
                Objects.requireNonNull(colorSetting);
                ColorSettingElement colorSettingElement = new ColorSettingElement(name3, description3, supplier5, colorSetting::getAlpha);
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
                this.settingsColumn.addFrameElement(colorSettingElement);
                return;
            }
            if (setting instanceof OrderedEnumSetting) {
                addOrderedEnumSetting((OrderedEnumSetting) setting);
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
                this.settingsColumn.addFrameElement(groupSettingElement);
                return;
            }
            if (!(setting instanceof KeybindSetting)) {
                if (setting instanceof SeparatorSetting) {
                    this.settingsColumn.addFrameElement(new SeparatorElement());
                    return;
                }
                return;
            }
            KeybindSetting keybindSetting = (KeybindSetting) setting;
            Translation name4 = keybindSetting.getName();
            Translation description4 = keybindSetting.getDescription();
            Objects.requireNonNull(keybindSetting);
            KeybindSettingElement keybindSettingElement = new KeybindSettingElement(name4, description4, keybindSetting::getKeyBind);
            keybindSettingElement.visibleSupplier(keybindSetting.getVisible());
            Objects.requireNonNull(keybindSetting);
            keybindSettingElement.onChange(wrapConsumerWithAutoSave(keybindSetting::setKey));
            this.settingsColumn.addFrameElement(keybindSettingElement);
        });
        addChild(this.settingsColumn);
    }

    public <E extends Enum<E>> void addModeSetting(ModeSetting<E> modeSetting) {
        ModeSettingElement modeSettingElement = new ModeSettingElement(modeSetting.getName(), modeSetting.getDescription(), modeSetting.options());
        Objects.requireNonNull(modeSetting);
        modeSettingElement.onChangeCallback(wrapConsumerWithAutoSave(modeSetting::setCurrentValue));
        Objects.requireNonNull(modeSetting);
        modeSettingElement.currentValueSupplier(modeSetting::currentValue);
        modeSettingElement.visibleSupplier(modeSetting.getVisible());
        this.settingsColumn.addFrameElement(modeSettingElement);
    }

    public <E extends Enum<E>> void addMultiSelectSetting(MultiSelectSetting<E> multiSelectSetting) {
        Translation name = multiSelectSetting.getName();
        Translation description = multiSelectSetting.getDescription();
        Enum[] enumArrOptions = multiSelectSetting.options();
        Objects.requireNonNull(multiSelectSetting);
        Supplier supplier = multiSelectSetting::selectedValues;
        Objects.requireNonNull(multiSelectSetting);
        MultiSelectSettingElement multiSelectSettingElement = new MultiSelectSettingElement(name, description, enumArrOptions, supplier, wrapConsumerWithAutoSave(multiSelectSetting::toggle));
        multiSelectSettingElement.visibleSupplier(multiSelectSetting.getVisible());
        this.settingsColumn.addFrameElement(multiSelectSettingElement);
    }

    public <E extends Enum<E>> void addOrderedEnumSetting(OrderedEnumSetting<E> orderedEnumSetting) {
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
        this.settingsColumn.addFrameElement(orderedListSettingElement);
    }

    public <E extends Enum<E>> void addEnumSetting(EnumSetting<E> enumSetting) {
        SoundModeSettingElement soundModeSettingElement = new SoundModeSettingElement(enumSetting.getName(), enumSetting.getDescription(), enumSetting.options(), enumSetting.soundAction());
        Objects.requireNonNull(enumSetting);
        soundModeSettingElement.onChangeCallback(wrapConsumerWithAutoSave(enumSetting::setCurrentValue));
        Objects.requireNonNull(enumSetting);
        soundModeSettingElement.currentValueSupplier(enumSetting::currentValue);
        soundModeSettingElement.visibleSupplier(enumSetting.getVisible());
        this.settingsColumn.addFrameElement(soundModeSettingElement);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.openAnimator.isZero()) {
            return;
        }
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        colorStack115.push();
        colorStack115.alpha(this.openAnimator.smoothAnimation());
        float f = totalHeight();
        drawCtx.bloom(x(), y(), width(), f, 20.0f, drawCtx.getColorAttachmentId(FluxVisuals.INSTANCE.windowController().bloom().getBloomFramebuffer()));
        drawCtx.fillOutlinedRoundedRect(x(), y(), width(), f, 12.0f, 2.5f, colorStack115.computeColor(ThemePalette.white.argb(), 0.03f), colorStack115.computeColor(themePalette.frameBackground().argb()));
        drawCtx.texture(this.groupIcon, x() + 14.0f, (y() + (this.headerHeight / 2.0f)) - (this.groupIcon.height() / 2.0f), this.groupIcon.width(), this.groupIcon.height(), colorStack115.computeColor(themePalette.text().tone(400).argb()));
        float fX = x() + 14.0f + this.groupIcon.width() + 8.0f;
        float fY = (y() + (this.headerHeight / 2.0f)) - (this.font.getHeight(15.0f) / 2.0f);
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.font, this.title.effective(), 15);
        this.titleBounds.withPosition(fX, fY).withSize(fTextWidthPhysical, this.font.getHeight(15.0f));
        float fX2 = this.settingsBadge.x() - 30.0f;
        boolean z = fX + fTextWidthPhysical > fX2;
        String strEffective = this.title.effective();
        float fSmoothAnimation = z ? 1.0f - this.hoverAnimator.smoothAnimation() : 1.0f;
        if (fSmoothAnimation > 0.1f && z) {
            int iMax = Math.max(0, this.title.effective().length() - ((int) Math.ceil(((fX + fTextWidthPhysical) - fX2) / (fTextWidthPhysical / this.title.effective().length()))));
            String strSubstring = this.title.effective().substring(0, iMax);
            if (iMax < this.title.effective().length()) {
                strSubstring = strSubstring + "...";
            }
            strEffective = strSubstring;
        }
        drawCtx.text(this.font, strEffective, 15, fX, fY, colorStack115.computeColor(themePalette.text().tone(100).argb()));
        colorStack115.push();
        colorStack115.alpha(fSmoothAnimation);
        this.settingsBadge.render(drawCtx);
        colorStack115.pop();
        this.scrollArea.beginArea(drawCtx, x(), (y() + this.headerHeight) - 1.0f, width(), (totalHeight() - this.headerHeight) + 2.0f);
        this.settingsColumn.render(drawCtx);
        this.scrollArea.endArea(drawCtx, contentHeight());
        drawCtx.matrixStack().push();
        drawCtx.matrixStack().translate(0.0f, -drawCtx.layoutContext().toPhysical(this.scrollArea.scrollY()), 0.0f);
        this.settingsColumn.renderOverlays(drawCtx);
        drawCtx.matrixStack().pop();
        if (needsScrollbar()) {
            this.scrollbar.render(drawCtx);
        }
        colorStack115.pop();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.settingsColumn.setPosition(x() + 14.0f, this.titleBounds.y() + this.titleBounds.height() + 14.0f);
        this.settingsColumn.setWidth(computeColumnWidth());
        this.settingsBadge.layout(layoutScaleContext);
        this.settingsBadge.setPosition(((x() + width()) - 14.0f) - this.settingsBadge.width(), (y() + (this.headerHeight / 2.0f)) - (this.settingsBadge.height() / 2.0f));
        this.groupIcon.setDimensions(14, 14);
        this.propertyIcon.setDimensions(10, 10);
        setSize(width(), this.headerHeight);
        this.scrollArea.layout(layoutScaleContext);
        this.scrollbar.layout(layoutScaleContext);
        this.scrollbar.setSize(4.0f, height() + contentHeight());
        this.scrollbar.setPosition(((x() + width()) - 10.0f) - this.scrollbar.width(), this.titleBounds.y() + this.titleBounds.height() + 6.0f);
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.openAnimator.isZero()) {
            return false;
        }
        float fScrollY = this.scrollArea.scrollY();
        boolean zInArea = inputEventContext.inArea(this.titleBounds.x(), this.titleBounds.y(), this.titleBounds.width(), this.titleBounds.height());
        if (this.titleBounds.x() + this.titleBounds.width() < (x() + width()) - 14.0f) {
            this.hoverAnimator.state(zInArea && !z);
        }
        if (super.handleInput(inputEventContext.withMouseOffset(0.0f, fScrollY), z)) {
            return true;
        }
        if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && !inputEventContext.inArea(x(), y(), width(), totalHeight())) {
            return true;
        }
        if ((inputEventContext.inputEvent() instanceof ScrollInput) && !inputEventContext.inArea(x(), y(), width(), totalHeight())) {
            closeWindow();
            return false;
        }
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (inputEvent691 instanceof KeyInput) {
            KeyInput keyInput = (KeyInput) inputEvent691;
            if (keyInput.keyAction().press() && keyInput.keyCode() == 256) {
                closeWindow();
                return true;
            }
        }
        InputEvent class691VarInputEvent2 = inputEventContext.inputEvent();
        if ((class691VarInputEvent2 instanceof MouseButtonInput) && ((MouseButtonInput) class691VarInputEvent2).action().press()) {
            if (inputEventContext.inArea(x(), y(), width(), totalHeight())) {
                return true;
            }
            closeWindow();
            return true;
        }
        if (needsScrollbar() && this.scrollbar.handleInput(inputEventContext, z)) {
            return true;
        }
        return this.scrollArea.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimator.animate(weightedEngine);
        this.scrollbar.animation(weightedEngine);
        this.hoverAnimator.animate(weightedEngine);
        this.scrollArea.animation(weightedEngine);
        this.settingsColumn.handleViewportVisibility(this.scrollArea.visibleTop(), this.scrollArea.visibleBottom(), y());
        super.animation(weightedEngine);
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        renderCommandQueue.record(drawCtx -> {
            ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
            ThemePalette themePalette = drawCtx.theme().palette();
            colorStack115.push();
            colorStack115.alpha(this.openAnimator.smoothAnimation());
            drawCtx.fillRoundedRect(x(), y(), width(), totalHeight(), 12.0f, colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb()));
            colorStack115.pop();
        });
        super.collectBloomElements(renderCommandQueue);
    }

    public void openWindow() {
        if (isOpen()) {
            return;
        }
        this.openAnimator.state(true);
    }

    public void closeWindow() {
        if (isOpen()) {
            this.openAnimator.state(false);
            super.handleClose();
        }
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

    public boolean needsScrollbar() {
        return contentHeight() > (totalHeight() - this.headerHeight) + 0.5f;
    }

    @Override
    public boolean isOpen() {
        return this.openAnimator.state() || !this.openAnimator.isZero();
    }

    public float totalHeight() {
        return this.headerHeight + Math.min(contentHeight(), 400.0f);
    }

    public float computeColumnWidth() {
        return width() - (needsScrollbar() ? 38.0f : 28.0f);
    }

    public float contentHeight() {
        if (this.settingsColumn != null && this.settingsColumn.height() > 0.0f) {
            return 14.0f + this.settingsColumn.height();
        }
        return 0.0f;
    }
}
