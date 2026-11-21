package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.net.ReallyWorldProcess;
import ru.fluxvisuals.ui.gui.clickgui.util.net.ReconnectTask;

import java.util.List;

public class ReallyWorldServerHandler implements ReconnectHandler {
    @Override
    public List<String> getServerIds() {
        return List.of("reallyworld");
    }

    @Override
    public ReconnectTask createProcess() {
        return new ReallyWorldProcess();
    }
}
