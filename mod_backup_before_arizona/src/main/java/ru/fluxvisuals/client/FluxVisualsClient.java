package ru.fluxvisuals.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import ru.fluxvisuals.config.ConfigManager;
import ru.fluxvisuals.hud.HudLayout;
import ru.fluxvisuals.module.ModuleManager;
import ru.fluxvisuals.module.hud.*;
import ru.fluxvisuals.module.misc.ClickGuiModule;
import ru.fluxvisuals.module.target.*;
import ru.fluxvisuals.module.visual.*;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.target.HitTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа FluxVisuals (клиент). Загружает конфиг, модули и события.
 * Чисто клиентская отрисовка — ни один пакет не отправляется и не модифицируется.
 */
public class FluxVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "fluxvisuals";
    public static final String NAME = "FluxVisuals";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("{} client initializing...", NAME);
        registerModules();
        ConfigManager.INSTANCE.load();
        registerEvents();
        LOGGER.info("{} client initialized ({} modules)", NAME, ModuleManager.INSTANCE.getAll().size());
    }

    private void registerModules() {
        var mm = ModuleManager.INSTANCE;
        // HUD
        mm.register(new Watermark());
        mm.register(new ArrayListModule());
        mm.register(new FpsModule());
        mm.register(new CoordsModule());
        mm.register(new DirectionModule());
        mm.register(new BiomeModule());
        mm.register(new SessionStatsModule());
        mm.register(new TpsModule());
        mm.register(new SpeedometerModule());
        mm.register(new CpsModule());
        mm.register(new HealthHudModule());
        mm.register(new ArmorHudModule());
        mm.register(new PotionHudModule());
        // TARGET
        mm.register(new TargetHudModule());
        mm.register(new TargetInfoModule());
        mm.register(new NametagsModule());
        mm.register(new HitEffectsModule());
        mm.register(new HitmarkersModule());
        mm.register(new EspModule());
        // VISUAL
        mm.register(new JumpCirclesModule());
        mm.register(new LandCirclesModule());
        mm.register(new TrailModule());
        mm.register(new CrosshairModule());
        mm.register(new FireOverlayModule());
        // MISC
        mm.register(new ClickGuiModule());
    }

    private boolean configApplied;

    private void registerEvents() {
        var mc = net.minecraft.client.MinecraftClient.getInstance();

        // Тик модулей
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // конфиг применяем на первом тике, когда mc() уже существует
            if (!configApplied) {
                configApplied = true;
                ConfigManager.INSTANCE.applyPending();
            }
            ModuleManager.INSTANCE.tick(client);
            HitTracker.tick(client);
        });

        // HUD-рендер (2D)
        HudRenderCallback.EVENT.register((g, tickCounter) -> {
            int sw = g.getScaledWindowWidth();
            int sh = g.getScaledWindowHeight();
            HudLayout.INSTANCE.reset(sw, sh);
            ModuleManager.INSTANCE.render2D(g, tickCounter.getTickProgress(false));
        });

        // Мир-рендер (3D): после отрисовки мира (оверлеи поверх всего)
        WorldRenderEvents.END_MAIN.register(context -> {
            Camera cam = mc.gameRenderer.getCamera();
            if (cam == null || !cam.isReady()) return;
            ModuleManager.INSTANCE.render3D(
                    context.matrices(),
                    context.consumers(),
                    cam,
                    mc.getRenderTickCounter().getTickProgress(false));
        });

        // Сохранение конфига при выходе из игры
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ConfigManager.INSTANCE.save()));
    }
}
