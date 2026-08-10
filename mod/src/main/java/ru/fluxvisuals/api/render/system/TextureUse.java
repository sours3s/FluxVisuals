package ru.fluxvisuals.api.render.system;

public enum TextureUse {
    SFMEDIUM(0), ICONS(1), EMOJIS(2), ICONS_NURIK(3);
    public final int id;
    TextureUse(int id) {
        this.id = id;
    }
}
