package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.events.api.WindowVisibilityListener;

public abstract class AbstractWindow extends WidgetContainer {
    public WindowVisibilityListener windowVisibilityListener;

    public void visibilityListener(WindowVisibilityListener windowVisibilityListener685) {
        this.windowVisibilityListener = windowVisibilityListener685;
    }

    public void onVisibilityChanged(boolean z) {
        if (this.windowVisibilityListener != null) {
            this.windowVisibilityListener.onWindowVisibilityChanged(this, z);
        }
    }

    public void open() {
        visible(true);
        onVisibilityChanged(true);
    }

    public void close() {
        visible(false);
        onVisibilityChanged(false);
        handleClose();
    }

    public void toggle() {
        if (this.visible) {
            close();
        } else {
            open();
        }
    }

    public AbstractWindow(float f, float f2) {
        setSize(f, f2);
    }

    public void center(LayoutScaleContext layoutScaleContext) {
        setPosition(layoutScaleContext.alignToScale((layoutScaleContext.logicalWidth() - width()) / 2.0f), layoutScaleContext.alignToScale((layoutScaleContext.logicalHeight() - height()) / 2.0f));
    }
}
