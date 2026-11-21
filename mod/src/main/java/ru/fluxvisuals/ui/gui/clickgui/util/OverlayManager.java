package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.OverlayWidget;

import java.util.LinkedHashSet;
import java.util.Set;

public class OverlayManager {
    public final Set<OverlayWidget> popups = new LinkedHashSet();
    public final Set<OverlayWidget> modals = new LinkedHashSet();

    public void registerModal(OverlayWidget overlayWidget) {
        if (overlayWidget != null) {
            this.modals.add(overlayWidget);
        }
    }

    public void registerPopup(OverlayWidget overlayWidget) {
        if (overlayWidget != null) {
            this.popups.add(overlayWidget);
        }
    }

    public void renderPopup(DrawCtx drawCtx) {
        this.popups.stream().filter((v0) -> {
            return v0.isOpen();
        }).forEach(overlayWidget -> {
            overlayWidget.render(drawCtx);
        });
    }

    public void renderModal(DrawCtx drawCtx) {
        this.modals.stream().filter((v0) -> {
            return v0.isOpen();
        }).forEach(overlayWidget -> {
            overlayWidget.render(drawCtx);
        });
    }

    public boolean handlePopupInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = z;
        for (OverlayWidget overlayWidget : this.popups) {
            if (overlayWidget.isOpen()) {
                zHandleInput |= overlayWidget.handleInput(inputEventContext, zHandleInput);
            }
        }
        return zHandleInput;
    }

    public boolean handleModalInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = z;
        for (OverlayWidget overlayWidget : this.modals) {
            if (overlayWidget.isOpen()) {
                zHandleInput |= overlayWidget.handleInput(inputEventContext, zHandleInput);
            }
        }
        return zHandleInput;
    }
}
