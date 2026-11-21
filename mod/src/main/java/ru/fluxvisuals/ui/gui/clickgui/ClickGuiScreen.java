package ru.fluxvisuals.ui.gui.clickgui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.WindowController;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyModifiers;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseModifiers;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PixelPoint;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.type.KeyInputAction;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MouseButtonAction;

/**
 * Vanilla Screen that drives the source ClickGui's WindowController.
 *
 * In 1.21.11 input dispatch moved to net.minecraft.client.Mouse / Keyboard, which call the current
 * screen with the new event objects (Click, KeyInput, CharInput). We translate them into the mod's
 * own input model and forward them to WindowController.handleInput.
 */
public class ClickGuiScreen extends Screen {
    private final WindowController controller;

    public ClickGuiScreen() {
        super(Text.empty());
        this.controller = FluxVisuals.INSTANCE.windowController();
    }

    @Override
    protected void init() {
        // В исходныйе ClientInitEvent диспатчился из MinecraftClientMixin и создавал
        // DrawEngine (фрагментный шейдер) + windowController.init(). Здесь этого вызова
        // не было — дублируем его, чтобы рендер кликгуи был готов до отрисовки.
        FluxVisuals.INSTANCE.eventDispatcher().dispatch(new ru.fluxvisuals.ui.gui.clickgui.events.impl.ClientInitEvent());
        MenuWindow menuWindow = FluxVisuals.INSTANCE.menuWindow();
        if (menuWindow != null && !menuWindow.menuOpen) {
            menuWindow.menuOpen = true;
            menuWindow.openAnimation.state(true);
            menuWindow.openAnimation.force(true);  // сразу показывать без анимации при открытии экрана
            menuWindow.open();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        WindowController windowController = this.controller;
        windowController.window().begin(0L);
        windowController.mousePosition = new PixelPoint(mouseX, mouseY);
        windowController.draw(0L);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        // RShift открывает экран в KeyboardMixin. Повторы этого же нажатия не должны
        // попадать в MenuWindow, иначе он переключит menuOpen обратно в false.
        if (keyInput.comp_4795() == 344) {
            return true;
        }
        InputEvent inputEvent = new ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput(
            keyInput.getKeycode(), keyInput.comp_4796(), new KeyInputAction(1), new KeyModifiers(keyInput.comp_4797()));
        if (this.controller.handleInput(this.eventContext(inputEvent))) {
            return true;
        }
        // Первый ESC закрывает меню (MenuWindow.handleInput), второй — экран полностью
        // (shouldCloseOnEsc=false, поэтому vanilla сам не закроет).
        if (keyInput.getKeycode() == 256) {
            net.minecraft.client.MinecraftClient.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        InputEvent inputEvent = new ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput(
            keyInput.getKeycode(), keyInput.comp_4796(), new KeyInputAction(0), new KeyModifiers(keyInput.comp_4797()));
        if (this.controller.handleInput(this.eventContext(inputEvent))) {
            return true;
        }
        return super.keyReleased(keyInput);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        MouseButtonInput input = new MouseButtonInput(click.button(), new MouseButtonAction(1), new MouseModifiers(click.comp_4797()));
        if (this.controller.handleInput(this.eventContext(input))) {
            return true;
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        MouseButtonInput input = new MouseButtonInput(click.button(), new MouseButtonAction(0), new MouseModifiers(click.comp_4797()));
        if (this.controller.handleInput(this.eventContext(input))) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        ScrollInput input = new ScrollInput(amountY);
        if (this.controller.handleInput(this.eventContext(input))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        InputEvent input = new ru.fluxvisuals.ui.gui.clickgui.util.model.CharInput(charInput.comp_4793(), charInput.comp_4794());
        if (this.controller.handleInput(this.eventContext(input))) {
            return true;
        }
        return super.charTyped(charInput);
    }

    @Override
    public boolean mouseDragged(Click click, double d, double e) {
        MouseButtonInput input = new MouseButtonInput(click.button(), new MouseButtonAction(1), new MouseModifiers(click.comp_4797()));
        if (this.controller.handleInput(this.eventContext(input))) {
            return true;
        }
        return super.mouseDragged(click, d, e);
    }

    private InputEventContext eventContext(InputEvent inputEvent) {
        PixelPoint mouse = this.controller.window().determineMousePosition();
        return new InputEventContext(inputEvent, mouse, this.controller.dpiScaleFactor());
    }

    @Override
    public void close() {
        if (FluxVisuals.INSTANCE.menuWindow() != null) {
            FluxVisuals.INSTANCE.menuWindow().close();
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Let the MenuWindow handle ESC internally
        return false;
    }
}