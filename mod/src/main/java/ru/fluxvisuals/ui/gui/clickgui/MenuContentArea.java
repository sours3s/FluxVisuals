package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Language;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.LanguageChangeEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.io.IOException;
import org.joml.Vector4f;

public class MenuContentArea extends WidgetContainer {
    public final ToggleAnimator languageAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public boolean pendingLanguageChange = false;
    public Language pendingLanguage = null;

    public void requestNextLanguage() {
        Language language = FluxVisuals.INSTANCE.languages().current();
        Language[] languages313 = Language.values();
        requestLanguage(languages313[(language.ordinal() + 1) % languages313.length]);
    }

    public void requestLanguage(Language language) {
        if (language == null || language == FluxVisuals.INSTANCE.languages().current()) {
            return;
        }
        this.pendingLanguage = language;
        this.pendingLanguageChange = true;
        this.languageAnimation.state(true);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        FluxVisuals.INSTANCE.tabsController().current().drawFrames(drawCtx);
        com.mojang.blaze3d.textures.GpuTexture colorAttachment = FluxVisuals.INSTANCE.windowController().headerBlur().getBlurFramebuffer().getColorAttachment();
        colorStack115.push();
        colorStack115.alpha(this.languageAnimation.smoothAnimation());
        drawCtx.roundedBlur(drawCtx.getColorAttachmentId(FluxVisuals.INSTANCE.windowController().headerBlur().getBlurFramebuffer()), x(), y(), width(), height(), new Vector4f(0.0f, 8.0f, 0.0f, 8.0f), colorStack115.white());
        drawCtx.fillRoundedRect(x(), y(), width(), height(), new Vector4f(0.0f, 8.0f, 0.0f, 8.0f), colorStack115.computeColor(drawCtx.theme().palette().surfaceBackground().tone(900).argb(), 0.8f));
        colorStack115.pop();
        super.render(drawCtx);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.languageAnimation.animate(weightedEngine);
        if (this.pendingLanguageChange && this.languageAnimation.isOne()) {
            this.pendingLanguageChange = false;
            FluxVisuals.INSTANCE.languages().language(this.pendingLanguage);
            try {
                FluxVisuals.INSTANCE.configManager().menuStateConfig().setLanguage(this.pendingLanguage);
                FluxVisuals.INSTANCE.configManager().saveMenuState();
            } catch (IOException e) {
                FluxVisuals.LOGGER.error("Failed to save menu language state", e);
            }
            FluxVisuals.INSTANCE.tabsController().current().markFramesDirty();
        }
        if (!this.pendingLanguageChange && this.languageAnimation.isOne()) {
            this.languageAnimation.state(false);
        }
        super.animation(weightedEngine);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT);
        FluxVisuals.INSTANCE.tabsController().current().layout(layoutScaleContext, x(), y());
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = z | super.handleInput(inputEventContext, z);
        return zHandleInput | FluxVisuals.INSTANCE.tabsController().current().handleInputFrames(inputEventContext, zHandleInput);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        FluxVisuals.INSTANCE.tabsController().current().collectBlurElements(overlayCommandQueue);
        super.collectBlurElements(overlayCommandQueue);
    }
}
