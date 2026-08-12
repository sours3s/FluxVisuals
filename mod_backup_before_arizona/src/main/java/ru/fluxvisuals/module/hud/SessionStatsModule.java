package ru.fluxvisuals.module.hud;

/** Статистика сессии: время в игре и адрес сервера. */
public class SessionStatsModule extends SimpleHudModule {
    private long sessionStart = -1;

    public SessionStatsModule() {
        super("SessionStats", "Время в игре и сервер");
    }

    @Override
    protected String getText() {
        var mc = mc();
        if (mc.world == null) {
            sessionStart = -1;
            return null;
        }
        if (sessionStart < 0) sessionStart = System.currentTimeMillis();
        long uptimeSec = (System.currentTimeMillis() - sessionStart) / 1000;
        String server = "Singleplayer";
        if (mc.getCurrentServerEntry() != null) server = mc.getCurrentServerEntry().address;
        return "Session " + fmt(uptimeSec) + " §7|§f " + server;
    }

    private String fmt(long s) {
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
