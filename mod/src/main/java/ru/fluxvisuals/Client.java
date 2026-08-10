package ru.fluxvisuals;

import ru.fluxvisuals.api.render.ClientRenderer;
import ru.fluxvisuals.api.render.msdf.Fonts;
import ru.fluxvisuals.api.render.system.TextureUse;

public class Client {
    public static ClientRenderer RENDERER;
    public static Fonts FONTS;
    public static final String NAME = "FluxVisuals";
    public static final String MOD_ID = "fluxvisuals";
    
    public static void init() {
        RENDERER = new ClientRenderer();
        FONTS = new Fonts();
    }
}
