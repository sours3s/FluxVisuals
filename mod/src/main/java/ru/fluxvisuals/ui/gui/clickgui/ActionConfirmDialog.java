package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ActionDialogData;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.Objects;

public class ActionConfirmDialog extends AbstractFrame {
    public final MsdfFont semiBoldFont;
    public final MsdfFont mediumFont;
    public final GlTexture errorIcon;
    public final GlTexture progressIcon;
    public static final Translation confirmLabelText = Lang.ACTION_CONFIRM;
    public static final String clickIconPath = "/icons/menu/new/click.png";
    public static final float defaultWidth = 320.0f;
    public static final float contentPadding = 12.0f;
    public static final float boxCornerRadius = 10.0f;
    public static final float elementSpacing = 8.0f;
    public static final float boxOutlineThickness = 2.5f;
    public static final float windowCornerRadius = 8.0f;
    public static final float sectionSpacing = 16.0f;
    public static final int animationDurationMs = 220;
    public static final float backgroundAlpha = 0.8f;
    public String errorMessage;
    public final ToggleAnimator errorAnimator;

    public final WidgetBounds errorBounds;
    public final WidgetBounds dialogBounds;
    public final ToggleAnimator openAnimator;
    public final LoadingButtonWidget confirmButton;
    public IconLabelBadge iconBadge;

    public ActionDialogData dialogData;
    int layoutFramesRemaining;
    public boolean opened;

    public ActionConfirmDialog() {
        super(new FrameElementColumn(296.0f, contentPadding));
        this.semiBoldFont = Fonts.INTER_SEMIBOLD.get();
        this.mediumFont = Fonts.INTER_MEDIUM.get();
        this.errorIcon = new GlTexture(new ClasspathResource("/icons/menu/new/error.png"));
        this.progressIcon = new GlTexture(new ClasspathResource("/icons/menu/new/progress.png"));
        this.errorMessage = null;
        this.errorAnimator = new ToggleAnimator(animationDurationMs, Easings.EASE_IN_OUT_CUBIC);
        this.errorBounds = new WidgetBounds(0.0f, 0.0f, defaultWidth, 0.0f);
        this.dialogBounds = new WidgetBounds(0.0f, 0.0f, defaultWidth, 0.0f);
        this.openAnimator = new ToggleAnimator(animationDurationMs, Easings.EASE_IN_OUT_CUBIC);
        this.layoutFramesRemaining = 2;
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.MENU_HEIGHT);
        this.confirmButton = new LoadingButtonWidget(new GlTexture(new ClasspathResource(clickIconPath)), 12, confirmLabelText, 296, 25, this::confirm, 8.0f, true);
        this.confirmButton.loadingVisuals(this.progressIcon, Lang.ACTION_LOADING);
        addChild(this.confirmButton);
    }

    public void setLoading(boolean z, String str) {
        if (str != null && !str.isBlank()) {
            this.confirmButton.loadingVisuals(this.progressIcon, Translation.clearText(str));
        }
        this.confirmButton.loading(z);
    }

    public void showError(String str) {
        this.errorMessage = str;
        this.errorAnimator.state(true);
    }

    public void hideError() {
        this.errorAnimator.state(false);
        if (this.errorAnimator.isZero()) {
            this.errorMessage = null;
        }
    }

    public void open(ActionDialogData actionDialogData) {
        Objects.requireNonNull(actionDialogData, "ActionData cannot be null");
        this.dialogData = actionDialogData;
        this.iconBadge = new IconLabelBadge(actionDialogData.icon(), actionDialogData.placeholderText());
        if (actionDialogData.needsChangePlaceholderColor()) {
            this.iconBadge.setBaseColor(Integer.valueOf(actionDialogData.placeHolderColor()));
        }
        this.confirmButton.primary(new GlTexture(new ClasspathResource(clickIconPath)), 12, actionDialogData.confirmLabel() != null ? actionDialogData.confirmLabel() : confirmLabelText);
        this.confirmButton.loading(false);
        this.confirmButton.enabled(actionDialogData.enableConfirmButton() != null ? actionDialogData.enableConfirmButton() : () -> {
            return true;
        });
        this.frameElementsContainer.clearElements();
        if (actionDialogData.contentConfigurator() != null) {
            actionDialogData.contentConfigurator().accept(this.frameElementsContainer);
        }
        this.layoutFramesRemaining = 2;
        this.opened = !this.opened;
        this.openAnimator.state(this.opened);
    }

    public void close() {
        this.opened = false;
        this.openAnimator.state(false);
        hideError();
        setLoading(false, "");
    }

    public boolean isOpen() {
        return this.opened || !this.openAnimator.isZero();
    }

    public float computeContentHeight() {
        float fHeight = 16.0f;
        if (this.iconBadge != null) {
            fHeight = sectionSpacing + this.iconBadge.height() + 8.0f;
        }
        float height = fHeight + this.semiBoldFont.getHeight(sectionSpacing);
        if (this.dialogData.description() != null) {
            height += 8.0f + this.mediumFont.getHeightWithLineBreaks(this.dialogData.description().effective(), 12);
        }
        float fHeight2 = height + sectionSpacing;
        if (this.frameElementsContainer.height() > 0.0f) {
            fHeight2 = fHeight2 + this.frameElementsContainer.height() + sectionSpacing;
        }
        return fHeight2 + this.confirmButton.height() + sectionSpacing;
    }

    public float computeErrorHeight() {
        float heightWithLineBreaks = 24.0f;
        if (this.errorMessage != null) {
            heightWithLineBreaks = 24.0f + this.semiBoldFont.getHeightWithLineBreaks(this.errorMessage, 12);
        }
        return heightWithLineBreaks;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.openAnimator.isZero() || this.dialogData == null) {
            return;
        }
        if (this.layoutFramesRemaining > 0) {
            this.layoutFramesRemaining--;
            this.dialogBounds.withSize(this.dialogData.width() > 0.0f ? this.dialogData.width() : defaultWidth, computeContentHeight());
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        colorStack115.push();
        try {
            colorStack115.alpha(this.openAnimator.smoothAnimation());
            drawCtx.roundedBlur(drawCtx.getColorAttachmentId(FluxVisuals.INSTANCE.windowController().searchBlur().getBlurFramebuffer()), x(), y(), width(), height(), 8.0f, colorStack115.white());
            drawCtx.fillRoundedRect(x() - 1.0f, y(), width() + 2.0f, height() + 1.0f, 8.0f, colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb(), backgroundAlpha));
            colorStack115.push();
            colorStack115.alpha(this.errorAnimator.smoothAnimation());
            drawCtx.fillOutlinedRoundedRect(this.errorBounds.x(), this.errorBounds.y(), this.errorBounds.width(), this.errorBounds.height(), boxCornerRadius, boxOutlineThickness, colorStack115.computeColor(15620209, 0.3f), colorStack115.computeColor(15620209, 0.07f));
            drawCtx.textureVerticalC(this.errorIcon, this.errorBounds.x() + contentPadding, this.errorBounds.y() + (this.errorBounds.height() / 2.0f), this.errorIcon.width(), this.errorIcon.height(), colorStack115.computeColor(themePalette.error().tone(500).argb()));
            if (this.errorMessage != null) {
                drawCtx.text(this.semiBoldFont, this.errorMessage, 12, this.errorBounds.x() + contentPadding + this.errorIcon.width() + 8.0f, (this.errorBounds.y() + (this.errorBounds.height() / 2.0f)) - (this.semiBoldFont.getHeightWithLineBreaks(this.errorMessage, 12) / 2.0f), colorStack115.computeColor(themePalette.error().tone(500).argb()));
            }
            colorStack115.pop();
            drawCtx.fillOutlinedRoundedRect(this.dialogBounds.x(), this.dialogBounds.y(), this.dialogBounds.width(), this.dialogBounds.height(), boxCornerRadius, boxOutlineThickness, colorStack115.computeColor(themePalette.surfaceOutline().tone(500).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(800).argb()));
            float fY = this.dialogBounds.y() + sectionSpacing;
            if (this.iconBadge != null) {
                this.iconBadge.render(drawCtx);
                fY = this.iconBadge.y() + this.iconBadge.height() + 8.0f;
            }
            drawCtx.text(this.semiBoldFont, this.dialogData.title().effective(), 16, this.dialogBounds.x() + contentPadding, fY, colorStack115.computeColor(themePalette.text().tone(100).argb()));
            float height = fY + this.semiBoldFont.getHeight(sectionSpacing) + 8.0f;
            if (this.dialogData.description() != null) {
                drawCtx.text(this.mediumFont, this.dialogData.description().effective(), 12, this.dialogBounds.x() + contentPadding, height, colorStack115.computeColor(themePalette.text().tone(500).argb()));
            }
            if (this.frameElementsContainer.height() > 0.0f) {
                renderFrameElements(drawCtx);
            }
            this.confirmButton.baseColor(colorStack115.computeColor(themePalette.accent().argb()));
            this.confirmButton.textColor(colorStack115.computeColor(ThemePalette.white.argb()));
            this.confirmButton.outlineColor(colorStack115.computeColor(themePalette.accentBright().argb()));
            this.confirmButton.render(drawCtx);
        } finally {
            colorStack115.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        if (this.dialogData == null) {
            return;
        }
        this.errorIcon.setDimensions(15, 15);
        this.errorBounds.withSize(this.dialogData.width() > 0.0f ? this.dialogData.width() : defaultWidth, computeErrorHeight());
        float fX = x() + (width() / 2.0f);
        float fY = y() + (height() / 2.0f);
        float fHeight = this.errorBounds.height() * this.errorAnimator.smoothAnimation();
        float fSmoothAnimation = 6.0f * this.errorAnimator.smoothAnimation();
        float fHeight2 = fY - (((fHeight + fSmoothAnimation) + this.dialogBounds.height()) / 2.0f);
        if (this.errorMessage != null && !this.errorAnimator.isZero()) {
            this.errorBounds.withPosition(fX - (this.errorBounds.width() / 2.0f), fHeight2);
        }
        this.dialogBounds.withPosition(fX - (this.dialogBounds.width() / 2.0f), fHeight2 + fHeight + fSmoothAnimation);
        float fY2 = this.dialogBounds.y() + sectionSpacing;
        if (this.iconBadge != null) {
            this.iconBadge.setPosition(this.dialogBounds.x() + contentPadding, fY2);
            this.iconBadge.layout(layoutScaleContext);
            fY2 = this.iconBadge.y() + this.iconBadge.height() + 8.0f;
        }
        float height = fY2 + this.semiBoldFont.getHeight(sectionSpacing);
        if (this.dialogData.description() != null) {
            height += 8.0f + this.mediumFont.getHeightWithLineBreaks(this.dialogData.description().effective(), 12);
        }
        float fHeight3 = height + sectionSpacing;
        if (this.frameElementsContainer.height() > 0.0f) {
            this.frameElementsContainer.setPosition(this.dialogBounds.x() + contentPadding, fHeight3);
            fHeight3 += this.frameElementsContainer.height() + sectionSpacing;
        }
        this.confirmButton.setPosition(this.dialogBounds.x() + contentPadding, fHeight3);
        this.confirmButton.layout(layoutScaleContext);
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimator.animate(weightedEngine);
        this.confirmButton.animation(weightedEngine);
        this.errorAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.openAnimator.isZero() || this.dialogData == null) {
            return false;
        }
        if (!z) {
            InputEvent inputEvent691 = inputEventContext.inputEvent();
            if (inputEvent691 instanceof KeyInput) {
                KeyInput keyInput = (KeyInput) inputEvent691;
                if (keyInput.keyAction().press() && (keyInput.keyCode() == 257 || keyInput.keyCode() == 335)) {
                    if ((this.dialogData.enableConfirmButton() == null || this.dialogData.enableConfirmButton().getAsBoolean()) && !this.confirmButton.loading() && this.dialogData.onConfirm() != null) {
                        confirm();
                        return true;
                    }
                }
            }
        }
        if (((inputEventContext.inputEvent() instanceof ScrollInput) && !z) || this.confirmButton.handleInput(inputEventContext, z) || super.handleInput(inputEventContext, z)) {
            return true;
        }
        if (z) {
            return false;
        }
        InputEvent class691VarInputEvent2 = inputEventContext.inputEvent();
        if (class691VarInputEvent2 instanceof KeyInput) {
            KeyInput class696Var2 = (KeyInput) class691VarInputEvent2;
            if ((class696Var2.keyAction().press() || class696Var2.keyAction().repeat()) && class696Var2.keyCode() == 256) {
                close();
                return true;
            }
        }
        InputEvent class691VarInputEvent3 = inputEventContext.inputEvent();
        if (class691VarInputEvent3 instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) class691VarInputEvent3;
            if (mouseButtonInput.action().press() && mouseButtonInput.button() == 0) {
                if (inputEventContext.inArea(this.dialogBounds.x(), this.dialogBounds.y(), this.dialogBounds.width(), this.dialogBounds.height())) {
                    return true;
                }
                close();
                return true;
            }
        }
        return inputEventContext.inputEvent() instanceof CursorMoveInput;
    }

    public void confirm() {
        if (this.dialogData == null || this.dialogData.onConfirm() == null) {
            return;
        }
        this.dialogData.onConfirm().run();
    }

    @Override
    public float contentHeight() {
        return this.dialogBounds.height();
    }

    public boolean isOpened() {
        return this.opened;
    }
}
