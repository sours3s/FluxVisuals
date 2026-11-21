package ru.fluxvisuals.ui.gui.clickgui.util.net;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PixelPoint;

import java.nio.DoubleBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

public class InputInterceptor {
    public boolean interceptCursorfScreenNotPresent;
    public boolean interceptKeyboardIfScreenPresent;
    public boolean interceptKeyboard;
    public boolean interceptSetCursorModeAnyway;
    public boolean keysNeedRefresh = false;

    public void begin(long j) {
        this.interceptCursorfScreenNotPresent = false;
        this.interceptKeyboardIfScreenPresent = false;
        this.interceptSetCursorModeAnyway = false;
        this.interceptKeyboard = false;
    }

    private long getWindowHandle() {
        return MinecraftClient.getInstance().getWindow().getHandle();
    }

    public void interceptSetCursorModeAnyway(boolean z) {
        this.interceptSetCursorModeAnyway = z;
    }

    public void interceptCursorfScreenNotPresent(boolean z) {
        this.interceptCursorfScreenNotPresent = z;
    }

    public void interceptKeyboardIfScreenPresent(boolean z) {
        this.interceptKeyboardIfScreenPresent = z;
    }

    public void interceptKeyboard(boolean z) {
        this.interceptKeyboard = z;
        net.minecraft.client.option.GameOptions gameOptions = Mc.INSTANCE.getGameOptions();
        for (KeyBinding keyBinding : new KeyBinding[]{gameOptions.forwardKey, gameOptions.backKey, gameOptions.leftKey, gameOptions.rightKey, gameOptions.jumpKey, gameOptions.sprintKey}) {
            keyBinding.setPressed(false);
            this.keysNeedRefresh = true;
        }
    }

    public void tickRefreshKeysIfNeeded() {
        if (this.keysNeedRefresh && !this.interceptKeyboard) {
            KeyBinding.updatePressedStates();
            this.keysNeedRefresh = false;
        }
    }

    public boolean cursorVisible() {
        return GLFW.glfwGetInputMode(getWindowHandle(), 208897) == 212993;
    }

    public void mousePosition(PixelPoint pixelPoint) {
        GLFW.glfwSetCursorPos(getWindowHandle(), pixelPoint.x(), pixelPoint.y());
    }

    public PixelPoint determineMousePosition() {
        MemoryStack memoryStackStackPush = MemoryStack.stackPush();
        try {
            DoubleBuffer doubleBufferMallocDouble = memoryStackStackPush.mallocDouble(1);
            DoubleBuffer doubleBufferMallocDouble2 = memoryStackStackPush.mallocDouble(1);
            GLFW.glfwGetCursorPos(getWindowHandle(), doubleBufferMallocDouble, doubleBufferMallocDouble2);
            PixelPoint pixelPoint = new PixelPoint(doubleBufferMallocDouble.get(), doubleBufferMallocDouble2.get());
            if (memoryStackStackPush != null) {
                memoryStackStackPush.close();
            }
            return pixelPoint;
        } catch (Throwable th) {
            if (memoryStackStackPush != null) {
                try {
                    memoryStackStackPush.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public void showCursor() {
        cursorInputMode(212993);
    }

    public void cursorInputMode(int i) {
        if (cursorInputMode() != i) {
            GLFW.glfwSetInputMode(getWindowHandle(), 208897, i);
        }
    }

    public int cursorInputMode() {
        return GLFW.glfwGetInputMode(getWindowHandle(), 208897);
    }

    public boolean interceptCursorfScreenNotPresent() {
        return this.interceptCursorfScreenNotPresent;
    }

    public boolean interceptKeyboardIfScreenPresent() {
        return this.interceptKeyboardIfScreenPresent;
    }

    public boolean interceptKeyboard() {
        return this.interceptKeyboard;
    }

    public boolean interceptSetCursorModeAnyway() {
        return this.interceptSetCursorModeAnyway;
    }
}
