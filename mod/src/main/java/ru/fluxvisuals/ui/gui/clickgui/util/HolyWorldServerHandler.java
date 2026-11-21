package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.net.HolyWorldProcess;
import ru.fluxvisuals.ui.gui.clickgui.util.net.ReconnectTask;

import java.util.List;

public class HolyWorldServerHandler implements ReconnectHandler {
    @Override
    public List<String> getServerIds() {
        return List.of("holyworld");
    }

    @Override
    public ReconnectTask createProcess() {
        return new HolyWorldProcess();
    }
}
