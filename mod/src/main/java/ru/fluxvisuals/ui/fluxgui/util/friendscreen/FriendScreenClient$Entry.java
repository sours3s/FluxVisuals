package ru.fluxvisuals.ui.fluxgui.util.friendscreen;

/** Запись списка друзей для строк настроек GUI. */
public final class FriendScreenClient$Entry {
    public final String login;
    public String name;
    public boolean online;

    public FriendScreenClient$Entry(String login, String name, boolean online) {
        this.login = login;
        this.name = name != null && !name.isBlank() ? name : login;
        this.online = online;
    }
}
