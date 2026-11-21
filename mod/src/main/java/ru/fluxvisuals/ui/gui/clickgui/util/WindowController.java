package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.AbstractWindow;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimationStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.BloomEffect;
import ru.fluxvisuals.ui.gui.clickgui.util.render.BlurEffect;
import ru.fluxvisuals.ui.gui.clickgui.util.math.DeltaTimeTracker;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.net.InputInterceptor;
import ru.fluxvisuals.ui.gui.clickgui.util.type.InputType;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.MenuWindow;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PixelPoint;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScaledRenderTarget;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Theme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

public class WindowController {
    public int windowVersion;
    public int backgroundAlphaValue;
    public int savedCursorMode;

    public ScreenResolution resolution;
    public static final long frameIntervalNanos = 16666667;
    public boolean dpiInitialized;
    public float dpiScaleFactor = 1.0f;
    public boolean autoDpiScale = true;
    public final List<AbstractWindow> windows = new ArrayList();
    public final InputInterceptor window = new InputInterceptor();
    public final DeltaTimeTracker deltaTimeTracker = new DeltaTimeTracker();
    public final ToggleAnimationStack animationStack = new ToggleAnimationStack();
    public final BlurEffect headerBlur = new BlurEffect();
    public final BlurEffect searchBlur = new BlurEffect();

    public final BloomEffect bloom = new BloomEffect();

    public final ScaledRenderTarget bloomBuffer = new ScaledRenderTarget(2);
    public int warmupFrames = 2;

    public PixelPoint mousePosition = new PixelPoint(0, 0);
    public long lastFrameTime = 0;
    public final AnimatedFloat dpiScaleAnimation = new AnimatedFloat(180, Easings.EASE_IN_OUT_CUBIC);

    public void init() {
        this.headerBlur.init();
        this.searchBlur.init();
        this.bloom.init();
    }

    public void draw(long j) {
        MenuWindow menuWindow776;
        if (this.warmupFrames > 0) {
            this.warmupFrames--;
            this.deltaTimeTracker.clear();
        }
        if (this.resolution == null) {
            this.resolution = ScreenResolution.resolution();
            if (this.autoDpiScale) {
                updateAutoDpiScale();
            }
        }
        long jNanoTime = System.nanoTime();
        boolean z = jNanoTime - this.lastFrameTime >= frameIntervalNanos;
        if (z) {
            this.lastFrameTime = jNanoTime;
        }
        WeightedEngine weightedEngine = new WeightedEngine(this.deltaTimeTracker.elapsedUnit(), this.animationStack);
        this.animationStack.begin();
        if (!this.dpiInitialized) {
            this.dpiScaleAnimation.set(this.dpiScaleFactor);
            this.dpiScaleAnimation.destination(this.dpiScaleFactor);
            this.dpiInitialized = true;
        }
        this.dpiScaleAnimation.animate(weightedEngine);
        float f = this.dpiScaleFactor;
        this.dpiScaleFactor = this.dpiScaleAnimation.animatedValue();
        if (Math.abs(f - this.dpiScaleFactor) > 1.0E-4f && (menuWindow776 = FluxVisuals.INSTANCE.menuWindow()) != null) {
            menuWindow776.requestCentering();
        }
        Iterator<AbstractWindow> it = this.windows.iterator();
        while (it.hasNext()) {
            it.next().animation(weightedEngine);
        }
        this.animationStack.end();
        this.window.begin(j);
        MatrixStack matrixStack = new MatrixStack();
        DrawEngine drawEngine154 = FluxVisuals.INSTANCE.drawEngine();
        Theme theme760 = FluxVisuals.INSTANCE.theme();
        RenderCommandQueue renderCommandQueue = new RenderCommandQueue();
        OverlayCommandQueue overlayCommandQueue = new OverlayCommandQueue();
        LayoutScaleContext layoutScaleContext = new LayoutScaleContext(this.resolution, this.dpiScaleFactor);
        DrawCtx drawCtx = new DrawCtx(this.window, this.resolution, matrixStack, this.window.determineMousePosition(), drawEngine154, drawEngine154.colorStack(), this.mousePosition, theme760, layoutScaleContext);
        boolean z2 = false;
        for (AbstractWindow abstractWindow : this.windows) {
            if (abstractWindow.visible()) {
                abstractWindow.layout(layoutScaleContext);
                abstractWindow.collectBlurElements(overlayCommandQueue);
                abstractWindow.collectBloomElements(renderCommandQueue);
                z2 = true;
            }
        }
        boolean z3 = !overlayCommandQueue.isEmpty();
        boolean z4 = !renderCommandQueue.isEmpty();
        boolean z5 = z3 && z2;
        if (z3 && z2) {
            drawEngine154.begin();
            overlayCommandQueue.renderRecorded(drawCtx);
            drawEngine154.end();
        }
        if (z4 && z2) {
            FramebufferUtil.clearTransparent(this.bloomBuffer.getFramebuffer());
            this.bloomBuffer.add(() -> {
                drawEngine154.begin();
                renderCommandQueue.renderRecorded(drawCtx);
                drawEngine154.end();
            });
            this.bloomBuffer.renderToFramebuffer();
        }
        if (z5 && z) {
            this.headerBlur.apply(32);
            this.searchBlur.apply(16, 2.0f);
            MinecraftClient.getInstance().getFramebuffer().resize(MinecraftClient.getInstance().getFramebuffer().textureWidth, MinecraftClient.getInstance().getFramebuffer().textureHeight);
        }
        if (z4 && z2) {
            this.bloom.apply(this.bloomBuffer.getFramebuffer(), 16);
        }
        drawEngine154.begin();
        if (this.backgroundAlphaValue != 0) {
            drawEngine154.rectangle(matrixStack.peek().getPositionMatrix(), 0.0f, 0.0f, this.resolution.screenWidth(), this.resolution.screenHeight(), drawEngine154.colorStack().darkAlpha(this.backgroundAlphaValue));
        }
        for (AbstractWindow class684Var2 : this.windows) {
            if (class684Var2.visible()) {
                class684Var2.render(drawCtx);
            }
        }
        drawEngine154.end();
        this.window.tickRefreshKeysIfNeeded();
    }

    public boolean handleInput(InputEventContext inputEventContext) {
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        this.mousePosition = inputEventContext.logicalMousePosition();
        boolean z = false;
        boolean z2 = inputEvent691.type() == InputType.BUTTON;
        boolean z3 = inputEvent691.type() == InputType.CURSOR;
        int size = this.windows.size();
        int i = this.windowVersion;
        if (!this.windows.isEmpty()) {
            for (int size2 = this.windows.size() - 1; size2 >= 0; size2--) {
                AbstractWindow abstractWindow = this.windows.get(size2);
                if (abstractWindow.handleInput(inputEventContext, false)) {
                    z = true;
                    if (!z2 || this.windows.size() != size || this.windowVersion != i || size2 == this.windows.size() - 1) {
                        break;
                    }
                    this.windows.remove(size2);
                    this.windows.add(abstractWindow);
                    this.windowVersion++;
                    break;
                }
            }
        }
        return z;
    }

    public boolean interceptKeyboardIfScreenPresent() {
        return Mc.INSTANCE.getCurrentScreen() != null && this.window.interceptKeyboardIfScreenPresent();
    }

    public boolean interceptKeyboard() {
        return window().interceptKeyboard();
    }

    public boolean interceptCursorIfScreenNotPresent() {
        return Mc.INSTANCE.getCurrentScreen() == null && this.window.interceptCursorfScreenNotPresent();
    }

    public void handleResize(int i, int i2) {
        this.resolution = new ScreenResolution(i, i2);
        if (this.autoDpiScale) {
            updateAutoDpiScale();
        }
    }

    public void newWindow(AbstractWindow abstractWindow) {
        if (this.windows.contains(abstractWindow)) {
            return;
        }
        this.windows.add(abstractWindow);
        this.windowVersion++;
    }

    public void setManualDpiScaleFactor(float f) {
        this.autoDpiScale = false;
        float fMethod004 = sanitizeDpiScale(f);
        if (!this.dpiInitialized) {
            this.dpiScaleFactor = fMethod004;
            this.dpiScaleAnimation.set(fMethod004);
            this.dpiInitialized = true;
        }
        this.dpiScaleAnimation.destination(fMethod004);
        MenuWindow menuWindow776 = FluxVisuals.INSTANCE.menuWindow();
        if (menuWindow776 != null) {
            menuWindow776.requestCentering();
        }
    }

    public void enableAutoDpiScale() {
        this.autoDpiScale = true;
        updateAutoDpiScale();
        MenuWindow menuWindow776 = FluxVisuals.INSTANCE.menuWindow();
        if (menuWindow776 != null) {
            menuWindow776.requestCentering();
        }
    }

    public void updateAutoDpiScale() {
        ScreenResolution screenResolution;
        if (this.resolution != null) {
            screenResolution = this.resolution;
        } else if (Mc.INSTANCE.getWindow() == null) {
            return;
        } else {
            screenResolution = ScreenResolution.resolution();
        }
        float fMethod003 = computeDpiScale(screenResolution);
        if (!this.dpiInitialized) {
            this.dpiScaleFactor = fMethod003;
            this.dpiScaleAnimation.set(fMethod003);
            this.dpiInitialized = true;
        }
        this.dpiScaleAnimation.destination(fMethod003);
    }

    public float computeDpiScale(ScreenResolution screenResolution) {
        int iScreenWidth = screenResolution.screenWidth();
        int iScreenHeight = screenResolution.screenHeight();
        if (iScreenWidth >= 5120 || iScreenHeight >= 2880) {
            return 3.0f;
        }
        if (iScreenWidth >= 3840 || iScreenHeight >= 2160) {
            return 2.0f;
        }
        if (iScreenWidth >= 2560 || iScreenHeight >= 1440) {
            return 1.5f;
        }
        if (iScreenWidth >= 1920 || iScreenHeight >= 1080) {
            return 1.0f;
        }
        return (iScreenWidth >= 1280 || iScreenHeight >= 720) ? 0.75f : 1.0f;
    }

    public float sanitizeDpiScale(float f) {
        if (f <= 0.0f) {
            return 1.0f;
        }
        return f;
    }

    public void removeWindow(AbstractWindow abstractWindow) {
        if (this.windows.remove(abstractWindow)) {
            this.windowVersion++;
        }
    }

    public void bringToFront(AbstractWindow abstractWindow) {
        if (!this.windows.remove(abstractWindow)) {
            newWindow(abstractWindow);
        } else {
            this.windows.add(abstractWindow);
            this.windowVersion++;
        }
    }

    public <T extends AbstractWindow> T getWindow(Class<T> cls) {
        for (AbstractWindow abstractWindow : this.windows) {
            if (cls.isInstance(abstractWindow)) {
                return cls.cast(abstractWindow);
            }
        }
        return null;
    }

    public void showCursor(InputInterceptor inputInterceptor) {
        Mc.INSTANCE.getMinecraft().onCursorEnterChanged();
        this.savedCursorMode = inputInterceptor.cursorInputMode();
        inputInterceptor.showCursor();
    }

    public void revertCursor(InputInterceptor inputInterceptor) {
        if (Mc.INSTANCE.getCurrentScreen() == null) {
            inputInterceptor.cursorInputMode(this.savedCursorMode);
        }
        Mc.INSTANCE.getMinecraft().onCursorEnterChanged();
    }

    public void recenterMouse() {
        if (this.resolution == null || Mc.INSTANCE.getCurrentScreen() != null) {
            return;
        }
        this.window.mousePosition(new PixelPoint(this.resolution.screenWidth() / 2, this.resolution.screenHeight() / 2));
    }

    public float dpiScaleFactor() {
        return this.dpiScaleFactor;
    }

    public boolean autoDpiScale() {
        return this.autoDpiScale;
    }

    public InputInterceptor window() {
        return this.window;
    }

    public BlurEffect headerBlur() {
        return this.headerBlur;
    }

    public BlurEffect searchBlur() {
        return this.searchBlur;
    }

    public BloomEffect bloom() {
        return this.bloom;
    }

    public ScaledRenderTarget bloomBuffer() {
        return this.bloomBuffer;
    }

    public WindowController backgroundAlpha(int i) {
        this.backgroundAlphaValue = i;
        return this;
    }

    public ScreenResolution resolution() {
        return this.resolution;
    }
}
