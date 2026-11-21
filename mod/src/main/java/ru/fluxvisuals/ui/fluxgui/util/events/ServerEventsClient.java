package ru.fluxvisuals.ui.fluxgui.util.events;

import java.util.List;

/** Заглушка событий сервера для GUI FluxVisuals. */
public final class ServerEventsClient {
    private ServerEventsClient() {
    }

    public static void refreshAsync(Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }

    public static List<ServerEventsClient$ServerEvent> getEvents() {
        return List.of();
    }

    public static boolean isLoading() {
        return false;
    }

    public static String getLastError() {
        return "";
    }
}
