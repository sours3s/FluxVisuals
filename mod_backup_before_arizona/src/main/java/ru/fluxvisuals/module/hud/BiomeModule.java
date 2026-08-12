package ru.fluxvisuals.module.hud;

/** Текущий биом. */
public class BiomeModule extends SimpleHudModule {
    public BiomeModule() {
        super("Biome", "Текущий биом");
    }

    @Override
    protected String getText() {
        var p = mc().player;
        if (p == null || mc().world == null) return null;
        String name = mc().world.getBiome(p.getBlockPos())
                .getKeyOrValue()
                .left()
                .map(k -> k.getValue().getPath())
                .orElse("unknown");
        return "Biome " + name.replace('_', ' ');
    }
}
