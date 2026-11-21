package ru.fluxvisuals.ui.fluxgui.setting;

import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil.Type;
import org.lwjgl.glfw.GLFW;

public class BindSetting extends Setting<Integer> {
    public static final int MOUSE_CODE_OFFSET = 1000;
    private boolean listening = false;

    public BindSetting(String name, int defaultKey) {
        super(name, defaultKey);
    }

    public BindSetting(String name, int defaultKey, Supplier<Boolean> visibility) {
        super(name, defaultKey, visibility);
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public boolean isListening() {
        return this.listening;
    }

    public static boolean shouldBlockKeyInput() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc == null ? true : mc.currentScreen != null;
    }

    public static int encodeMouseButton(int button) {
        return 1000 + button;
    }

    public static boolean isMouseButtonCode(int code) {
        return code >= 1000;
    }

    public static int decodeMouseButton(int code) {
        return isMouseButtonCode(code) ? code - 1000 : -1;
    }

    public static boolean isPressed(long windowHandle, int code) {
        if (code != -1 && code != 0 && code != -1) {
            return isMouseButtonCode(code) ? GLFW.glfwGetMouseButton(windowHandle, decodeMouseButton(code)) == 1 : GLFW.glfwGetKey(windowHandle, code) == 1;
        } else {
            return false;
        }
    }

    public static String getBindName(int code) {
        if (code == -1) {
            return "NONE";
        }

        if (code == 0 || code == -1) {
            return "NONE";
        }

        if (isMouseButtonCode(code)) {
            return switch (decodeMouseButton(code)) {
                case 0 -> "MOUSE1";
                case 1 -> "MOUSE2";
                case 2 -> "MOUSE3";
                case 3 -> "MOUSE4";
                case 4 -> "MOUSE5";
                default -> "MOUSE" + Math.max(1, decodeMouseButton(code) + 1);
            };
        } else {
            String key = Type.KEYSYM.createFromCode(code).getTranslationKey();
            return key.substring(key.lastIndexOf(46) + 1).toUpperCase();
        }
    }
}
