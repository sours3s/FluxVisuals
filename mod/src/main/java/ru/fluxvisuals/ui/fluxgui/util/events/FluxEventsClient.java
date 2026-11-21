package ru.fluxvisuals.ui.fluxgui.util.events;

import java.util.List;

/** Заглушка внешних событий для страницы событий GUI FluxVisuals. */
public final class FluxEventsClient {
    private FluxEventsClient() {
    }

    public static void refreshAsync(Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }

    public static void refreshMinesAsync(Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }

    public static List<FluxEventsClient$FluxEvent> getEvents() {
        return List.of();
    }

    public static List<FluxEventsClient$FluxMine> getMines() {
        return List.of();
    }

    public static boolean isEventsLoading() {
        return false;
    }

    public static boolean isMinesLoading() {
        return false;
    }

    public static String getLastError() {
        return "";
    }

    public static String getLastMineError() {
        return "";
    }
}
