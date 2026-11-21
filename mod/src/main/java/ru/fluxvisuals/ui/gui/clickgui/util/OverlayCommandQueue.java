package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.WidgetParent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class OverlayCommandQueue {
    public final List<Consumer<DrawCtx>> commands = new ArrayList();

    public void record(Consumer<DrawCtx> consumer) {
        if (consumer == null) {
            return;
        }
        this.commands.add(consumer);
    }

    public void recordElement(WidgetParent widgetParent) {
        if (widgetParent == null) {
            return;
        }
        Objects.requireNonNull(widgetParent);
        record(widgetParent::render);
    }

    public void renderRecorded(DrawCtx drawCtx) {
        Iterator<Consumer<DrawCtx>> it = this.commands.iterator();
        while (it.hasNext()) {
            it.next().accept(drawCtx);
        }
        this.commands.clear();
    }

    public void clear() {
        this.commands.clear();
    }

    public boolean isEmpty() {
        return this.commands.isEmpty();
    }
}
