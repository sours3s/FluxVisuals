package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.net.ReconnectTask;

import java.util.List;

public interface ReconnectHandler {
    List<String> getServerIds();

    ReconnectTask createProcess();
}
