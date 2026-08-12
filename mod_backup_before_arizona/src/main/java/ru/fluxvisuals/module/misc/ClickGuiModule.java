package ru.fluxvisuals.module.misc;

import org.lwjgl.glfw.GLFW;
import ru.fluxvisuals.gui.ClickGuiScreen;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;

/**
 * Открывает ClickGUI (настоящий Screen). Правый Shift — открыть/закрыть.
 * Screen сам обрабатывает курсор, мышь и клавиши; при закрытии отключает этот модуль.
 */
public class ClickGuiModule extends Module {
    public static ClickGuiModule INSTANCE;

    public ClickGuiModule() {
        super("ClickGUI", "Панель управления модулями", Category.MISC);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc() != null && !(mc().currentScreen instanceof ClickGuiScreen)) {
            mc().setScreen(new ClickGuiScreen());
        }
    }

    @Override
    public void onDisable() {
        if (mc() != null && mc().currentScreen instanceof ClickGuiScreen) {
            mc().setScreen(null);
        }
    }

    @Override
    public boolean shouldList() { return false; }
}
