package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.config.StyleConfig;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.BrandLogo;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.IconRender;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import ru.fluxvisuals.mixin.accessor.BossBarHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class Watermark implements HudElement {
    public static final Watermark INSTANCE = new Watermark();
    private static final float X = 776.0F;
    private static final float Y = 32.0F;
    private static final float HEIGHT = 39.0F;
    private static final float BASELINE = 24.583F;
    private static final float SEPARATOR_Y = 13.5F;
    private static final float SEPARATOR_HEIGHT = 12.0F;
    private static final float ICON_Y = 13.5F;
    private static final float TILE_X = 12.0F;
    private static final float TILE_Y = 10.0F;
    private static final float TILE_WIDTH = 20.0F;
    private static final float TILE_HEIGHT = 19.0F;
    private static final float TILE_RADIUS = 4.0F;
    private static final float LOGO_SIZE = 12.0F;
    private static final float COMPACT_LOGO_SIZE = 15.75F;
    private static final float BOSS_BAR_WATERMARK_TOP = 54.0F;
    private static final Identifier GLOW_TEXTURE = Identifier.of("fluxvisuals", "textures/glow.png");
    private static final String CLIENT_NAME = "FluxVisuals";
    private static final String FPS_UNIT = "fps";
    private static final String PING_UNIT = "ping";
    private static float currentWidth = 368.308F;
    private static float animatedFps = 0.0F;
    private static float animatedPing = 0.0F;
    private static long lastAnimTime = 0L;
    private static float bossBarOffset;
    private static long lastBossAnimationNanos;
    private static boolean manuallyMovedDuringBossBar;

    private Watermark() {
    }

    @Override
    public String id() {
        return "watermark";
    }

    @Override
    public float x() {
        return Hud.DESIGN_WIDTH / 2.0F - currentWidth / 2.0F;
    }

    @Override
    public float y() {
        return 32.0F;
    }

    @Override
    public float width() {
        return currentWidth;
    }

    @Override
    public float height() {
        return 39.0F;
    }

    @Override
    public boolean hasContent() {
        return true;
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        updateAnimations();
        String playerName = Hud.playerName();
        String fpsText = String.valueOf(Math.max(0, Math.round(animatedFps)));
        String pingText = String.valueOf(Math.max(0, Math.round(animatedPing)));
        float nameWidth = TextRender.width(CLIENT_NAME, 14.241F);
        float userWidth = TextRender.width(playerName, 13.224F);
        float fpsWidth = TextRender.width(fpsText, 13.224F);
        float fpsUnitWidth = TextRender.width("fps", 13.224F);
        float pingWidth = TextRender.width(pingText, 13.224F);
        float pingUnitWidth = TextRender.width("ping", 13.224F);
        float curX = 776.0F;
        float logoTileX = curX + 12.0F;
        float nameX = logoTileX + 20.0F + 9.877F;
        float userSepX = nameX + nameWidth + 12.0F;
        float userIconX = userSepX + 2.0F + 9.0F;
        float userTextX = userIconX + IconRender.getDesignSize("person") + 8.772F;
        float fpsSepX = userTextX + userWidth + 12.0F;
        float fpsIconX = fpsSepX + 2.0F + 10.0F;
        float fpsValX = fpsIconX + IconRender.getDesignSize("monitor") + 9.576F;
        float fpsUnitX = fpsValX + fpsWidth + 6.0F;
        float pingSepX = fpsUnitX + fpsUnitWidth + 12.0F;
        float pingIconX = pingSepX + 2.0F + 8.905F;
        float pingValX = pingIconX + IconRender.getDesignSize("ping") + 9.099F;
        float pingUnitX = pingValX + pingWidth + 6.0F;
        float rightEdge = pingUnitX + pingUnitWidth + 12.0F;
        float calculatedWidth = Math.max(368.308F, rightEdge - curX);
        currentWidth = MathHelper.lerp(0.15F, currentWidth, calculatedWidth);
        float finalX = Hud.DESIGN_WIDTH / 2.0F - currentWidth / 2.0F;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(finalX - curX, 0.0F);
        hud.panel(context, curX, 32.0F, currentWidth, 39.0F, 10.0F);
        String primaryIcon = Hud.primaryIcon("logo");
        if (StyleConfig.watermarkLogo) {
            if (!primaryIcon.equals("logo") || !drawCompactLogo(context, logoTileX, 42.0F)) {
                RoundedRenderer.drawRoundedRect(context, logoTileX, 42.0F, 20.0F, 19.0F, 4.0F, Hud.animatedColor(Hud.accent()));
                float styledIconSize = IconRender.getDesignSize(primaryIcon) * Hud.primaryIconScale();
                IconRender.draw(
                    context,
                    primaryIcon,
                    logoTileX + (20.0F - styledIconSize) / 2.0F,
                    42.0F + (19.0F - styledIconSize) / 2.0F,
                    styledIconSize,
                    Hud.animatedColor(-1)
                );
            }
        }

        TextRender.draw(context, CLIENT_NAME, nameX, 56.583F, 14.241F, Hud.animatedColor(-2434342));
        Hud.separator(context, userSepX, 45.5F, 12.0F, 452984831);
        IconRender.draw(context, "person", userIconX, 45.5F, IconRender.getDesignSize("person"), Hud.animatedColor(Hud.accent()));
        TextRender.draw(context, playerName, userTextX, 56.583F, 13.224F, Hud.animatedColor(-2434342));
        Hud.separator(context, fpsSepX, 45.5F, 12.0F, 452984831);
        IconRender.draw(context, "monitor", fpsIconX, 45.5F, IconRender.getDesignSize("monitor"), Hud.animatedColor(Hud.accent()));
        TextRender.draw(context, fpsText, fpsValX, 56.583F, 13.224F, Hud.animatedColor(-2434342));
        TextRender.draw(context, "fps", fpsUnitX, 56.583F, 13.224F, Hud.animatedColor(-1076176166));
        Hud.separator(context, pingSepX, 45.5F, 12.0F, 452984831);
        IconRender.draw(context, "ping", pingIconX, 44.75F, IconRender.getDesignSize("ping"), Hud.animatedColor(Hud.accent()));
        TextRender.draw(context, pingText, pingValX, 56.583F, 13.224F, Hud.animatedColor(-2434342));
        TextRender.draw(context, "ping", pingUnitX, 56.583F, 13.224F, Hud.animatedColor(-1076176166));
        context.getMatrices().popMatrix();
    }

    private static boolean drawCompactLogo(DrawContext context, float tileX, float tileY) {
        int accent = Hud.animatedColor(Hud.accent());
        return drawCompactLogo(context, tileX + 10.0F, tileY + 9.5F, 15.75F, accent);
    }

    public static boolean drawCompactLogo(DrawContext context, float centerX, float centerY, float size, int color) {
        float iconX = centerX - size / 2.0F;
        float iconY = centerY - size / 2.0F;
        drawCompactGlow(context, iconX, iconY, size, color);
        BrandLogo.draw(context, iconX, iconY, size);
        return true;
    }

    private static void drawCompactGlow(DrawContext context, float x, float y, float size, int color) {
        float scale = size / 15.75F;
        float pad = 9.0F * scale;
        drawGlowLayer(context, x - pad, y - pad, size + pad * 2.0F, size + pad * 2.0F, Hud.fade(color, 0.2F));
        float widePad = 13.0F * scale;
        drawGlowLayer(context, x - widePad, y - widePad, size + widePad * 2.0F, size + widePad * 2.0F, Hud.fade(color, 0.095F));
    }

    private static void drawGlowLayer(DrawContext context, float x, float y, float width, float height, int color) {
        Gui.tex(context, GLOW_TEXTURE)
            .vertex(x, y + height, 0.0F, 1.0F, color)
            .vertex(x + width, y + height, 1.0F, 1.0F, color)
            .vertex(x + width, y, 1.0F, 0.0F, color)
            .vertex(x, y, 0.0F, 0.0F, color)
            .submit(context);
    }

    private static void updateAnimations() {
        long now = System.currentTimeMillis();
        if (lastAnimTime == 0L) {
            lastAnimTime = now;
            animatedFps = rawFps();
            animatedPing = rawPing();
        } else {
            float delta = Math.min(1.0F, (float)(now - lastAnimTime) / 1000.0F);
            lastAnimTime = now;
            float targetFps = rawFps();
            float targetPing = rawPing();
            float factor = 1.0F - (float)Math.exp(-8.0 * delta);
            animatedFps = MathHelper.lerp(factor, animatedFps, targetFps);
            animatedPing = MathHelper.lerp(factor, animatedPing, targetPing);
        }
    }

    private static int rawFps() {
        return MinecraftClient.getInstance().getCurrentFps();
    }

    private static int rawPing() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (player != null && handler != null) {
            PlayerListEntry entry = handler.getPlayerListEntry(player.getUuid());
            return entry == null ? 0 : entry.getLatency();
        } else {
            return 0;
        }
    }

    static float automaticYOffset() {
        int bossBarCount = bossBarCount();
        boolean bossBarVisible = bossBarCount > 0;
        if (!bossBarVisible) {
            manuallyMovedDuringBossBar = false;
        }

        float manualTop = 32.0F + HudEditor.offset(INSTANCE.id())[1];
        boolean aboveBossSafePosition = manualTop < 54.0F;
        float target = bossBarVisible && aboveBossSafePosition && !manuallyMovedDuringBossBar ? 54.0F - manualTop : 0.0F;
        long now = System.nanoTime();
        if (lastBossAnimationNanos == 0L) {
            lastBossAnimationNanos = now;
        }

        float delta = Math.min(0.1F, (float)(now - lastBossAnimationNanos) / 1.0E9F);
        lastBossAnimationNanos = now;
        float factor = 1.0F - (float)Math.exp(-8.0F * delta);
        bossBarOffset = MathHelper.lerp(factor, bossBarOffset, target);
        if (Math.abs(bossBarOffset - target) < 0.01F) {
            bossBarOffset = target;
        }

        return bossBarOffset;
    }

    static float beginManualMove() {
        if (bossBarCount() <= 0) {
            return 0.0F;
        }

        manuallyMovedDuringBossBar = true;
        float bakedOffset = bossBarOffset;
        bossBarOffset = 0.0F;
        return bakedOffset;
    }

    private static int bossBarCount() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null && client.inGameHud.getBossBarHud() != null) {
            try {
                return ((BossBarHudAccessor)client.inGameHud.getBossBarHud()).getBossBars().size();
            } catch (RuntimeException var2) {
                return 0;
            }
        } else {
            return 0;
        }
    }
}
