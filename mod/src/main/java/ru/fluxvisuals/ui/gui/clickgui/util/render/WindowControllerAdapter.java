package ru.fluxvisuals.ui.gui.clickgui.util.render;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PixelPoint;
import ru.fluxvisuals.ui.gui.clickgui.util.WindowController;

public class WindowControllerAdapter {
    public final WindowController controller;

    public PixelPoint mousePosition = new PixelPoint(0, 0);

    public void preBlitFramebufferToBackbuffer(long j) throws MatchException {
        this.controller.draw(j);
        handleInput(new CursorMoveInput(this.controller.window().determineMousePosition()));
    }

    public void handleResize(int i, int i2) {
        this.controller.handleResize(i, i2);
    }

    public boolean handleInput(InputEvent inputEvent) throws MatchException {
        if (inputEvent instanceof CursorMoveInput) {
            try {
                this.mousePosition = ((CursorMoveInput) inputEvent).mousePosition();
            } catch (Throwable th) {
                throw new MatchException(th.toString(), th);
            }
        }
        return this.controller.handleInput(new InputEventContext(inputEvent, this.mousePosition, this.controller.dpiScaleFactor()));
    }

    public boolean interceptKeyboard() {
        return this.controller.interceptKeyboardIfScreenPresent() || this.controller.interceptKeyboard();
    }

    public boolean interceptMouse() {
        return this.controller.interceptCursorIfScreenNotPresent();
    }

    public WindowControllerAdapter(WindowController windowController) {
        this.controller = windowController;
    }
}
