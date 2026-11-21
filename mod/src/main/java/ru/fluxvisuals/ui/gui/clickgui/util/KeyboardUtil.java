package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyboardUtil {
    public static final Map<Integer, String> modifierKeyNames = Map.ofEntries(Map.entry(340, "SHIFT"), Map.entry(344, "RSHIFT"), Map.entry(341, "CTRL"), Map.entry(345, "RCTRL"), Map.entry(342, "ALT"), Map.entry(346, "RALT"), Map.entry(343, "SUPER"), Map.entry(347, "RSUPER"));
    public static final Map<Integer, String> specialKeyNames = Map.ofEntries(Map.entry(96, "`"), Map.entry(39, "'"), Map.entry(44, ","), Map.entry(46, "."), Map.entry(47, "/"), Map.entry(59, ";"), Map.entry(61, "="), Map.entry(91, "["), Map.entry(92, "\\"), Map.entry(93, "]"), Map.entry(45, "-"), Map.entry(256, "ESC"), Map.entry(259, "BACK"), Map.entry(280, "CAPS"), Map.entry(161, "WORLD_1"), Map.entry(162, "WORLD_2"));

    public static String formatCombination(List<Integer> list) {
        return (String) list.stream().map((v0) -> {
            return keyToString(v0);
        }).collect(Collectors.joining(" + "));
    }

    public static String keyToString(int i) {
        String str = modifierKeyNames.get(Integer.valueOf(i));
        if (str != null) {
            return str;
        }
        InputUtil.Key keyCreateFromCode = i < 8 ? InputUtil.Type.MOUSE.createFromCode(i) : InputUtil.Type.KEYSYM.createFromCode(i);
        String str2 = specialKeyNames.get(Integer.valueOf(i));
        if (str2 != null) {
            return str2;
        }
        return i == -1 ? "NONE" : keyCreateFromCode.getTranslationKey().replace("key.keyboard.", "").replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
    }

    public static int normalizeModifier(int i) {
        switch (i) {
            case 340:
            case 344:
                return 340;
            case 341:
            case 345:
                return 341;
            case 342:
            case 346:
                return 342;
            case 343:
            case 347:
                return 343;
            default:
                return i;
        }
    }

    public static boolean isKeyPressed(int i) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), i);
    }

    public static boolean isKeyPressed(KeybindSetting keybindSetting) {
        int key = keybindSetting.getKey();
        return Mc.INSTANCE.getMinecraft().currentScreen == null && keybindSetting.getVisible().get().booleanValue() && isKeyPressed(getKeyType(key), key);
    }

    public static InputUtil.Type getKeyType(int i) {
        return i < 8 ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
    }

    public static boolean isKeyPressed(InputUtil.Key key) {
        return isKeyPressed(key.getCategory(), key.getCode());
    }

    public static boolean isKeyPressed(InputUtil.Type type, int i) {
        if (i == -1) {
            return false;
        }
        switch (type) {
            case KEYSYM:
                return GLFW.glfwGetKey(Mc.INSTANCE.getWindow().getHandle(), i) == 1;
            case MOUSE:
                return GLFW.glfwGetMouseButton(Mc.INSTANCE.getWindow().getHandle(), i) == 1;
            default:
                return false;
        }
    }

    public static boolean isMouseKeyPressed(int i) {
        return GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), i) == 1;
    }

    public static String keysToString(List<Integer> list) {
        return (list == null || list.isEmpty()) ? keyToString(-1) : (String) list.stream().map((v0) -> {
            return keyToString(v0);
        }).collect(Collectors.joining(" + "));
    }

    public KeyboardUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
