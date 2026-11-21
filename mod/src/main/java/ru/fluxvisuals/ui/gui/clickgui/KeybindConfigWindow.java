package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BindMode;
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
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSettingElement;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeybindConfigWindow extends OverlayWidget {
    public final FrameElementColumn contentColumn;
    public final Translation title;
    public final String labelText;
    public final float headerHeight;
    public final KeybindSettingElement keybindElement;

    public final DualToggleElement bindModeToggle;
    public Supplier<BindMode> bindModeSupplier;
    public final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    public final GlTexture propertyIcon = new GlTexture(new ClasspathResource("/icons/menu/new/property.png"));
    public final GlTexture keyboardIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png"));
    public final ToggleAnimator openAnimation = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);
    public final WidgetBounds labelBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final ToggleAnimator labelHoverAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final IconLabelBadge keybindBadge = new IconLabelBadge(this.propertyIcon, Translation.clearText("KEYBIND"));

    public KeybindConfigWindow(Translation translation, String str, Supplier<List<Integer>> supplier) {
        setSize(241.0f, 48.0f);
        this.title = translation;
        this.labelText = str;
        this.contentColumn = new FrameElementColumn(width() - 28.0f, 14.0f);
        this.keybindElement = new KeybindSettingElement(Lang.KEY, null, supplier);
        this.keybindElement.visibleSupplier(() -> {
            return true;
        });
        this.bindModeToggle = new DualToggleElement(Lang.BIND_MODE, Lang.HOLD, Lang.TOGGLE);
        this.contentColumn.addFrameElement(this.bindModeToggle);
        this.contentColumn.addFrameElement(this.keybindElement);
        this.headerHeight = height();
        addChild(this.contentColumn);
    }

    public void bindType(Supplier<BindMode> supplier, Consumer<BindMode> consumer) {
        this.bindModeSupplier = supplier;
        if (supplier != null) {
            this.bindModeToggle.setState(supplier.get() == BindMode.HOLD);
        }
        this.bindModeToggle.setOnToggleCallback(bool -> {
            if (consumer != null) {
                consumer.accept(bool.booleanValue() ? BindMode.HOLD : BindMode.TOGGLE);
            }
        });
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.keybindElement.onChange(consumer);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.openAnimation.isZero()) {
            return;
        }
        float f = totalHeight();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        colorStack115.push();
        colorStack115.alpha(this.openAnimation.smoothAnimation());
        drawCtx.bloom(x(), y(), width(), f, 20.0f, drawCtx.getColorAttachmentId(FluxVisuals.INSTANCE.windowController().bloom().getBloomFramebuffer()));
        drawCtx.fillOutlinedRoundedRect(x(), y(), width(), f, 12.0f, 2.5f, colorStack115.computeColor(ThemePalette.white.argb(), 0.03f), colorStack115.computeColor(themePalette.frameBackground().argb()));
        drawCtx.texture(this.keyboardIcon, x() + 14.0f, (y() + (this.headerHeight / 2.0f)) - (this.keyboardIcon.height() / 2.0f), this.keyboardIcon.width(), this.keyboardIcon.height(), colorStack115.computeColor(themePalette.text().tone(400).argb()));
        float fX = x() + 14.0f + this.keyboardIcon.width() + 8.0f;
        float fY = (y() + (this.headerHeight / 2.0f)) - (this.font.getHeight(15.0f) / 2.0f);
        String str = this.labelText;
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.font, str, 15);
        this.labelBounds.withPosition(fX, fY).withSize(fTextWidthPhysical, this.font.getHeight(15.0f));
        float fX2 = this.keybindBadge.x() - 30.0f;
        boolean z = fX + fTextWidthPhysical > fX2;
        String str2 = str;
        float fSmoothAnimation = z ? 1.0f - this.labelHoverAnimation.smoothAnimation() : 1.0f;
        if (fSmoothAnimation > 0.1f && z) {
            int iMax = Math.max(0, str.length() - ((int) Math.ceil(((fX + fTextWidthPhysical) - fX2) / (fTextWidthPhysical / str.length()))));
            String strSubstring = str.substring(0, iMax);
            if (iMax < str.length()) {
                strSubstring = strSubstring + "...";
            }
            str2 = strSubstring;
        }
        drawCtx.text(this.font, str2, 15, fX, fY, colorStack115.computeColor(themePalette.text().tone(100).argb()));
        colorStack115.push();
        colorStack115.alpha(fSmoothAnimation);
        this.keybindBadge.render(drawCtx);
        colorStack115.pop();
        this.contentColumn.render(drawCtx);
        this.contentColumn.renderOverlays(drawCtx);
        colorStack115.pop();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.keybindBadge.layout(layoutScaleContext);
        this.keybindBadge.setPosition(((x() + width()) - 14.0f) - this.keybindBadge.width(), (y() + (this.headerHeight / 2.0f)) - (this.keybindBadge.height() / 2.0f));
        this.contentColumn.setPosition(x() + 14.0f, this.labelBounds.y() + this.labelBounds.height() + 14.0f);
        this.keyboardIcon.setDimensions(14, 14);
        this.propertyIcon.setDimensions(10, 10);
        setSize(width(), this.headerHeight);
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.openAnimation.isZero()) {
            return false;
        }
        boolean zInArea = inputEventContext.inArea(this.labelBounds.x(), this.labelBounds.y(), this.labelBounds.width(), this.labelBounds.height());
        if (this.labelBounds.x() + this.labelBounds.width() < (x() + width()) - 14.0f) {
            this.labelHoverAnimation.state(zInArea && !z);
        }
        if (super.handleInput(inputEventContext, z)) {
            return true;
        }
        if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && !inputEventContext.inArea(x(), y(), width(), height() + contentHeight())) {
            return true;
        }
        if (inputEventContext.inputEvent() instanceof ScrollInput) {
            if (inputEventContext.inArea(x(), y(), width(), height() + contentHeight())) {
                return true;
            }
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
        if (!(class691VarInputEvent2 instanceof MouseButtonInput) || !((MouseButtonInput) class691VarInputEvent2).action().press()) {
            return false;
        }
        if (inputEventContext.inArea(x(), y(), width(), height() + contentHeight())) {
            return true;
        }
        closeWindow();
        return true;
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        renderCommandQueue.record(drawCtx -> {
            ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
            ThemePalette themePalette = drawCtx.theme().palette();
            colorStack115.push();
            colorStack115.alpha(this.openAnimation.smoothAnimation());
            drawCtx.fillRoundedRect(x(), y(), width(), totalHeight(), 12.0f, colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb()));
            colorStack115.pop();
        });
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.bindModeSupplier != null) {
            boolean z = this.bindModeSupplier.get() == BindMode.HOLD;
            if (this.bindModeToggle.isLeftActive() != z) {
                this.bindModeToggle.setState(z);
            }
        }
        this.openAnimation.animate(weightedEngine);
        this.labelHoverAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean isOpen() {
        return this.openAnimation.state() || !this.openAnimation.isZero();
    }

    public void openWindow() {
        if (isOpen()) {
            return;
        }
        this.openAnimation.state(true);
    }

    public void closeWindow() {
        if (isOpen()) {
            this.openAnimation.state(false);
            super.handleClose();
        }
    }

    public float totalHeight() {
        return this.headerHeight + Math.min(contentHeight(), 250.0f);
    }

    public float contentHeight() {
        if (this.contentColumn.height() <= 0.0f) {
            return 0.0f;
        }
        return 14.0f + this.contentColumn.height();
    }
}
