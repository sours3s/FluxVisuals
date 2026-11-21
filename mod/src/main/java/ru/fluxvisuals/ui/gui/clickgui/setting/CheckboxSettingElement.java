package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BindMode;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.events.api.EventCallback;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.render.HighlightAnimation;
import ru.fluxvisuals.ui.gui.clickgui.IconButtonWidget;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.KeybindConfigWindow;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.LanguageChangeEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CheckboxSettingElement extends ModuleFrame {
    public static final float descriptionGap = 3.0f;
    public static final int nameTextSize = 14;
    public static final int descriptionTextSize = 13;
    public final MsdfFont titleFont;
    public final MsdfFont descriptionFont;
    public final GlTexture checkmarkIcon;
    public final GlTexture keybindIcon;
    public final Translation name;
    public final Translation description;
    public final ToggleAnimator checkAnimator;
    public final ClickableBehavior clickBehavior;
    public final HighlightAnimation highlightAnimation;
    public Supplier<Boolean> toggledStateSupplier;
    public Supplier<Boolean> visibilitySupplier;
    public Runnable changeAction;

    public final IconButtonWidget keybindButton;
    public boolean showKeybindButton;
    public boolean lastToggledState;
    public String wrappedDescription;
    public boolean descriptionDirty;
    public float cachedWidth;
    public String cachedDescriptionText;
    public float descriptionHeight;
    public final WidgetBounds keybindButtonBounds;
    public final EventCallback<LanguageChangeEvent> languageChangeCallback;
    public final KeybindConfigWindow keybindWindow;
    public ScreenResolution resolution;
    public static final float windowGap = 8.0f;
    public static final float margin = 5.0f;

    public CheckboxSettingElement(Translation translation, Translation class254Var2, Supplier<List<Integer>> supplier) {
        this(translation, class254Var2, supplier, true);
    }

    public CheckboxSettingElement(Translation translation, Translation class254Var2, Supplier<List<Integer>> supplier, boolean z) {
        this.titleFont = Fonts.INTER_SEMIBOLD.get();
        this.descriptionFont = Fonts.INTER_MEDIUM.get();
        this.checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
        this.keybindIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png"));
        this.checkAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);
        this.clickBehavior = new ClickableBehavior();
        this.highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
        this.lastToggledState = false;
        this.wrappedDescription = null;
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescriptionText = null;
        this.descriptionHeight = 0.0f;
        this.keybindButtonBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.languageChangeCallback = event -> {
            this.descriptionDirty = true;
            this.cachedWidth = -1.0f;
            this.cachedDescriptionText = null;
        };
        this.name = translation;
        this.description = class254Var2;
        this.showKeybindButton = z;
        this.keybindWindow = new KeybindConfigWindow(Lang.BINDING_MODULE, translation.effective(), supplier);
        FluxVisuals.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.keybindWindow);
        this.clickBehavior.clickCallback(this::invert);
        this.keybindButton = new IconButtonWidget(this.keybindIcon, this::toggleKeybindWindow, 16.0f, 16.0f);
    }

    public void setupBindType(Supplier<BindMode> supplier, Consumer<BindMode> consumer) {
        if (this.keybindWindow != null) {
            this.keybindWindow.bindType(supplier, consumer);
        }
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.keybindWindow.onChange(consumer);
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
        float fX = this.keybindButtonBounds.x();
        float fY = this.keybindButtonBounds.y() - this.viewportTop;
        float fWidth2 = this.keybindButtonBounds.width();
        float fHeight = this.keybindButtonBounds.height();
        float f2 = fX + fWidth2 + windowGap;
        float f3 = (fX - windowGap) - fWidth;
        if (f2 + fWidth > fScreenWidth - margin && f3 >= margin) {
            f2 = f3;
        }
        float fMax = Math.max(margin, Math.min(f2, (fScreenWidth - fWidth) - margin));
        float f4 = (fY + (fHeight / 2.0f)) - (f / 2.0f);
        if (f4 + f > fScreenHeight - margin) {
            f4 = (fScreenHeight - f) - margin;
        }
        if (f4 < margin) {
            f4 = 5.0f;
        }
        this.keybindWindow.openWindow();
        this.keybindWindow.setPosition(fMax, f4);
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
        this.resolution = drawCtx.resolution();
        updateWrappedDescription((((f - 18.0f) - (this.showKeybindButton ? this.keybindButton.width() : 0.0f)) - 10.0f) - margin);
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        float fX = (x() + f) - 18.0f;
        float f3 = 18.0f / 2.0f;
        float fY = (y() + (height() / 2.0f)) - f3;
        float fY2 = y();
        float fValue = this.highlightAnimation.value();
        drawCtx.text(this.titleFont, this.name.effective(), nameTextSize, x(), fY2, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), fValue));
        if (this.wrappedDescription != null) {
            drawCtx.text(this.descriptionFont, this.wrappedDescription, descriptionTextSize, x(), fY2 + this.titleFont.getHeight(14.0f) + descriptionGap, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), fValue));
        }
        if (this.showKeybindButton) {
            float fX2 = (((x() + f) - 18.0f) - 10.0f) - this.keybindButton.width();
            float fHeight = (fY + f3) - (this.keybindButton.height() / 2.0f);
            this.keybindButton.setPosition(fX2, fHeight);
            this.keybindButton.setColor(themePalette.text().tone(800).argb());
            this.keybindButton.render(drawCtx);
            this.keybindButtonBounds.withPosition(fX2, fHeight).withSize(this.keybindButton.width(), this.keybindButton.height());
        } else {
            this.keybindButtonBounds.withSize(0.0f, 0.0f);
        }
        int iComputeColor = colorStack115.computeColor(themePalette.surfaceOutline().tone(500).argb());
        int iComputeColor2 = colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb());
        int iComputeColor3 = colorStack115.computeColor(themePalette.accentBright().argb());
        int iInterpolate = colorStack115.interpolate(colorStack115.interpolate(iComputeColor, iComputeColor2, this.clickBehavior.hoverAnimation()), colorStack115.interpolate(iComputeColor3, colorStack115.autoBrightenDarken(iComputeColor3, 0.1f), this.clickBehavior.hoverAnimation()), this.checkAnimator);
        int iComputeColor4 = colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb());
        int iComputeColor5 = colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb());
        int iComputeColor6 = colorStack115.computeColor(themePalette.accent().argb());
        drawCtx.fillOutlinedRoundedRect(fX, fY, 18.0f, 18.0f, margin, 2.5f, iInterpolate, colorStack115.interpolate(colorStack115.interpolate(iComputeColor4, iComputeColor5, this.clickBehavior.hoverAnimation()), colorStack115.interpolate(iComputeColor6, colorStack115.autoBrightenDarken(iComputeColor6, 0.1f), this.clickBehavior.hoverAnimation()), this.checkAnimator));
        drawCtx.texture(this.checkmarkIcon, (fX + (18.0f / 2.0f)) - Math.round(this.checkmarkIcon.width() / 2.0f), (fY + (18.0f / 2.0f)) - Math.round(this.checkmarkIcon.height() / 2.0f), this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack115.computeColor(16777215, colorStack115.alphaMultiplier() * this.checkAnimator.smoothAnimation()));
        this.clickBehavior.setDimensions(x(), y(), f, height());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.keybindWindow.layout(layoutScaleContext);
        if (this.showKeybindButton) {
            this.keybindButton.layout(layoutScaleContext);
        }
        this.checkmarkIcon.setDimensions(nameTextSize, nameTextSize);
    }

    public float textHeight(String str, int i, int i2) {
        float height = this.titleFont.getHeight(i);
        if (this.description != null) {
            height = height + descriptionGap + this.descriptionFont.getHeightWithLineBreaks(str, i2);
        }
        return height;
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.showKeybindButton && this.keybindButton.handleInput(inputEventContext, z)) {
            return true;
        }
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.resolution != null) {
            updateWrappedDescription((((this.resolution.width() - 18.0f) - (this.showKeybindButton ? this.keybindButton.width() : 0.0f)) - 10.0f) - margin);
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += descriptionGap + this.descriptionHeight;
        }
        return Math.max(height, 18.0f);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        boolean zBooleanValue;
        if (this.toggledStateSupplier != null && (zBooleanValue = this.toggledStateSupplier.get().booleanValue()) != this.lastToggledState) {
            this.checkAnimator.state(zBooleanValue);
            this.lastToggledState = zBooleanValue;
        }
        if (this.showKeybindButton) {
            this.keybindButton.animation(weightedEngine);
        }
        this.keybindWindow.animation(weightedEngine);
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        this.highlightAnimation.animate(weightedEngine);
        this.checkAnimator.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        this.keybindWindow.closeWindow();
        super.handleClose();
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        this.keybindWindow.collectBloomElements(renderCommandQueue);
        super.collectBloomElements(renderCommandQueue);
    }

    public void updateWrappedDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.cachedWidth = f;
            this.cachedDescriptionText = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.cachedWidth || strEffective == null || !strEffective.equals(this.cachedDescriptionText)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descriptionFont, descriptionTextSize);
            this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionTextSize);
            this.cachedWidth = f;
            this.cachedDescriptionText = strEffective;
            this.descriptionDirty = false;
        }
    }

    public void invert() {
        if (this.changeAction != null) {
            this.changeAction.run();
        }
    }

    public Translation name() {
        return this.name;
    }

    public CheckboxSettingElement toggledSupplier(Supplier<Boolean> supplier) {
        this.toggledStateSupplier = supplier;
        return this;
    }

    public CheckboxSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }

    public CheckboxSettingElement changeRunnable(Runnable runnable) {
        this.changeAction = runnable;
        return this;
    }
}
