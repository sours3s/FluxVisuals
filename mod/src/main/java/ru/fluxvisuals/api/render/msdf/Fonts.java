package ru.fluxvisuals.api.render.msdf;

import lombok.Getter;
import ru.fluxvisuals.api.render.system.TextureUse;

import java.util.HashMap;
import java.util.Map;

public class Fonts {
    @Getter
    private final Map<String, MsdfFont> fonts = new HashMap<>();
    public Fonts() {}
    public MsdfFont get(TextureUse textureUse) {
        return fonts.computeIfAbsent(textureUse.name(), name -> MsdfFont.builder()
                .atlas(name.toLowerCase())
                .data(name.toLowerCase())
                .build(textureUse));
    }
    public void clearFonts() {
        fonts.clear();
    }
}
