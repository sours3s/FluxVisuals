package ru.fluxvisuals.ui.fluxgui.util.events;

/** Редкость шахты для страницы событий GUI FluxVisuals. */
public enum FluxEventsClient$MineRarityKind {
    LEGENDARY("Легендарная"),
    MYTHICAL("Мифическая"),
    COMMON("Обычная");

    private final String label;

    FluxEventsClient$MineRarityKind(String label) {
        this.label = label;
    }

    public String displayLabel() {
        return label;
    }
}
