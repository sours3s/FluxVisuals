package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.net.FunTimeProcess;
import ru.fluxvisuals.ui.gui.clickgui.util.net.ReconnectTask;

import java.util.List;

public class FunTimeServerHandler implements ReconnectHandler {
    public static final String funtimeId = "funtime";

    @Override
    public List<String> getServerIds() {
        return List.of(funtimeId, "spookytime");
    }

    @Override
    public ReconnectTask createProcess() {
        return new FunTimeProcess();
    }
}
