package ru.fluxvisuals.ui.gui.clickgui.util.type;

import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;

public class UserSession {
    private final String username;
    private final GlTexture texture;

    public UserSession(String username) {
        this.username = username;
        this.texture = new GlTexture(new ClasspathResource("/icons/menu/new/client_logo.png"));
    }

    public String username() {
        return this.username;
    }

    public GlTexture texture() {
        return this.texture;
    }
}