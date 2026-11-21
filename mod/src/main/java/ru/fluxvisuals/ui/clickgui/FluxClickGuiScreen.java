package ru.fluxvisuals.ui.clickgui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.fluxvisuals.ui.gui.clickgui.externall.MenuScreen;

public final class FluxClickGuiScreen extends Screen {
    private final MenuScreen menuScreen;

    public FluxClickGuiScreen() {
        super(Text.literal("FluxVisuals ClickGui"));
        this.menuScreen = new MenuScreen();
    }

    @Override
    protected void init() {
        super.init();
        // Minecraft вызывает init() только у этого Screen. Донорский MenuScreen — обычный
        // вложенный объект, поэтому его init() нужно вызвать вручную: без него globalAnim
        // остаётся на нуле и весь GUI рисуется прозрачным.
        this.menuScreen.initializeForWrapper();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Не вызываем super.render() (избегаем blur-краша)
        // Используем донорский рендер через MenuScreen
        menuScreen.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // Правый Shift открывает/закрывает GUI в KeyboardMixin. Если передавать его сюда,
        // autorepeat доходит до хендлеров донора и меню мгновенно закрывается.
        if (input.comp_4795() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return true;
        }
        return menuScreen.keyPressed(input) || super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        return menuScreen.keyReleased(input) || super.keyReleased(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return menuScreen.charTyped(input) || super.charTyped(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return menuScreen.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return menuScreen.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return menuScreen.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return menuScreen.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        menuScreen.close();
        super.close();
    }
}
