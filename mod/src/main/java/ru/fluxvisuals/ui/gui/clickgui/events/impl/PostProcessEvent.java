package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.Event;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;

import java.util.function.Consumer;

public class PostProcessEvent implements Event {
    public final OverlayCommandQueue blurQueue;

    public final RenderCommandQueue bloomQueue;

    public final DrawCtx context;

    public void addBlur(Consumer<DrawCtx> consumer) {
        this.blurQueue.record(consumer);
    }

    public void addBloom(Consumer<DrawCtx> consumer) {
        this.bloomQueue.record(consumer);
    }

    public DrawCtx context() {
        return this.context;
    }

    public PostProcessEvent(OverlayCommandQueue overlayCommandQueue, RenderCommandQueue renderCommandQueue, DrawCtx drawCtx) {
        this.blurQueue = overlayCommandQueue;
        this.bloomQueue = renderCommandQueue;
        this.context = drawCtx;
    }
}
