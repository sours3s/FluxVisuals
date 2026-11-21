package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BlacklistedProcess;
import ru.fluxvisuals.ui.gui.clickgui.DetectedProcessWindow;

import java.util.ArrayList;
import java.util.List;

public final class ActivityLogger {
    public ActivityLogger() {}
    public void start() { }
    public static String getWindow() { return ""; }
    public static String getCPU() { return ""; }
    public static String getGPU() { return ""; }
    public static List<DetectedProcessWindow> findMatchesForTargets(BlacklistedProcess[] targets) { return new ArrayList<>(); }
}
