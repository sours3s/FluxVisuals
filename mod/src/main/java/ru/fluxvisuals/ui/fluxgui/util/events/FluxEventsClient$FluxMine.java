package ru.fluxvisuals.ui.fluxgui.util.events;

/** Данные авто-шахты для страницы событий GUI FluxVisuals. */
public final class FluxEventsClient$FluxMine {
    public int anarchyId;
    public String server = "";
    public FluxEventsClient$MineRarityKind nextMineRarity = FluxEventsClient$MineRarityKind.COMMON;

    public String getDisplayServer() {
        return anarchyId > 0 ? "Анархия " + anarchyId : server;
    }

    public String getDisplayTime() {
        return "—";
    }
}
