package ru.fluxvisuals.module.hud;

import net.minecraft.world.World;
import ru.fluxvisuals.config.Setting;

/** Координаты игрока, с опцией конвертации в Незер. */
public class CoordsModule extends SimpleHudModule {
    private final Setting nether = Setting.bool("Nether coords", "Показывать координаты Незера", false);

    public CoordsModule() {
        super("Coords", "Координаты игрока");
        addSetting(nether);
    }

    @Override
    protected String getText() {
        var p = mc().player;
        if (p == null) return null;
        String xyz = String.format("XYZ %.1f %.1f %.1f", p.getX(), p.getY(), p.getZ());
        if (nether.getBoolean() && mc().world != null) {
            if (mc().world.getRegistryKey() == World.NETHER) {
                xyz += String.format(" §7→§f %.1f %.1f %.1f", p.getX() * 8, p.getY(), p.getZ() * 8);
            } else {
                xyz += String.format(" §7→§f %.1f %.1f %.1f", p.getX() / 8, p.getY(), p.getZ() / 8);
            }
        }
        return xyz;
    }
}
