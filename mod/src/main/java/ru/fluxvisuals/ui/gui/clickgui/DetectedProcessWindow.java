package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BlacklistedProcess;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ProcessWindowInfo;

public final class DetectedProcessWindow {
    public final BlacklistedProcess target;
    public final ProcessWindowInfo window;

    public DetectedProcessWindow(BlacklistedProcess blacklistedProcess, ProcessWindowInfo processWindowInfo) {
        this.target = blacklistedProcess;
        this.window = processWindowInfo;
    }
}
