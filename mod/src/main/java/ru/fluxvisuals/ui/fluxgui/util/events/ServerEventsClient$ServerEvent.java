package ru.fluxvisuals.ui.fluxgui.util.events;

/** Событие сервера для GUI FluxVisuals. */
public record ServerEventsClient$ServerEvent(String id, String serverName, String name, String rarity) {
}
