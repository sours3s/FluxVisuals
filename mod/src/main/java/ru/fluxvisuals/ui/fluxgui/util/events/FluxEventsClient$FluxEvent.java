package ru.fluxvisuals.ui.fluxgui.util.events;

/** Данные события для страницы событий GUI FluxVisuals. */
public final class FluxEventsClient$FluxEvent {
    public String id = "";
    public String phase = "";
    public String name = "";
    public String rarity = "";
    public int anarchyId;
    public String server = "";

    public boolean shouldDisplay() {
        return true;
    }

    public boolean isActiveEvent() {
        return false;
    }

    public String getTimerLabel() {
        return "—";
    }

    public String getCoordinatesDisplay() {
        return "—";
    }

    public boolean hasLocation() {
        return false;
    }
}
