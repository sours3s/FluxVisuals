package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.font.icon.IconTtfBridge;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.util.media.MediaSessionReader;
import ru.fluxvisuals.ui.fluxgui.util.render.RenderUtil;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class MediaPlayer implements HudElement {
    public static final MediaPlayer INSTANCE = new MediaPlayer();
    private static final float X = 40.0F;
    private static final float Y = 90.0F;
    private static final float WIDTH = 180.0F;
    private static final float HEIGHT = 66.0F;
    private static final float ART_X = 8.0F;
    private static final float ART_Y = 7.0F;
    private static final float ART_SIZE = 34.0F;
    private static final float TEXT_X = 50.0F;
    private static final float TEXT_RIGHT = 8.0F;
    private static final float TITLE_SIZE = 13.2F;
    private static final float ARTIST_SIZE = 11.2F;
    private static final float TIME_SIZE = 9.2F;
    private static final float PROGRESS_X = 8.0F;
    private static final float PROGRESS_Y = 59.0F;
    private static final float PROGRESS_WIDTH = 164.0F;
    private static final float PROGRESS_HEIGHT = 2.0F;
    private static final Identifier THUMBNAIL = Identifier.of("fluxvisuals", "dynamic/media_thumb_hud");
    private final MediaSessionReader reader = MediaSessionReader.getInstance();
    private NativeImageBackedTexture thumbnail;
    private int thumbnailWidth;
    private int thumbnailHeight;
    private boolean thumbnailRegistered;
    private boolean readerStarted;
    private long lastThumbnailAttempt;

    private MediaPlayer() {
    }

    @Override
    public String id() {
        return "media_player";
    }

    @Override
    public float x() {
        return 40.0F;
    }

    @Override
    public float y() {
        return 90.0F;
    }

    @Override
    public float width() {
        return 180.0F;
    }

    @Override
    public float height() {
        return 66.0F;
    }

    @Override
    public boolean hasContent() {
        this.updateLifecycle();
        return ru.fluxvisuals.ui.fluxgui.module.render.Hud.mediaPlayer.getValue() && this.reader.hasMedia();
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        this.updateLifecycle();
        this.updateThumbnail();
        hud.panel(context, 40.0F, 90.0F, 180.0F, 66.0F, 10.0F);
        if (!this.reader.hasMedia()) {
            String emptyText = ru.fluxvisuals.ui.fluxgui.module.render.Hud.text("Нет медиа", "No media");
            float textX = 40.0F + (180.0F - TextRender.width(emptyText, 13.2F)) / 2.0F;
            TextRender.draw(context, emptyText, textX, 127.224F, 13.2F, Hud.animatedColor(-2434342));
        } else {
            this.drawArtwork(context);
            float textWidth = 122.0F;
            String title = fit(this.reader.getTitle(), textWidth, 13.2F);
            String artist = fit(this.reader.getArtist(), textWidth, 11.2F);
            TextRender.draw(context, title, 90.0F, 109.0F, 13.2F, Hud.animatedColor(-2434342));
            TextRender.draw(context, artist, 90.0F, 123.5F, 11.2F, Hud.animatedColor(-2829100));
            long duration = this.reader.getDurationMs();
            long position = this.reader.getPositionMs();
            String current = formatTime(position);
            String total = formatTime(duration);
            TextRender.draw(context, current, 48.0F, 144.5F, 9.2F, Hud.animatedColor(-1076176166));
            TextRender.draw(context, total, 212.0F - TextRender.width(total, 9.2F), 144.5F, 9.2F, Hud.animatedColor(-1076176166));
            this.drawControls(context);
            RoundedRenderer.drawRoundedRect(context, 48.0F, 149.0F, 164.0F, 2.0F, 1.0F, Hud.animatedColor(452984831));
            if (duration > 0L) {
                float progress = MathHelper.clamp((float)position / (float)duration, 0.0F, 1.0F);
                RoundedRenderer.drawRoundedRect(context, 48.0F, 149.0F, Math.max(1.0F, 164.0F * progress), 2.0F, 1.0F, Hud.animatedColor(Hud.accent()));
            }
        }
    }

    private void drawControls(DrawContext context) {
        float size = 7.6F;
        float gap = 8.0F;
        float totalWidth = size * 3.0F + gap * 2.0F;
        float x = 130.0F - totalWidth / 2.0F;
        float y = 134.0F;
        int color = Hud.animatedColor(-2434342);
        IconTtfBridge.tryDrawIconsFontChar(context, 'v', x, y, size, color);
        IconTtfBridge.tryDrawIconsFontChar(context, (char)(this.reader.isPlaying() ? 'y' : 'x'), x + size + gap, y, size, color);
        IconTtfBridge.tryDrawIconsFontChar(context, 'Z', x + (size + gap) * 2.0F, y, size, color);
    }

    private void drawArtwork(DrawContext context) {
        this.drawArtwork(context, 48.0F, 97.0F);
    }

    private void drawArtwork(DrawContext context, float x, float y) {
        this.drawArtwork(context, x, y, 34.0F);
    }

    private void drawArtwork(DrawContext context, float x, float y, float size) {
        if (this.thumbnailRegistered && this.thumbnail != null) {
            RenderUtil.drawRoundedTexture(
                context,
                THUMBNAIL,
                x,
                y,
                size,
                size,
                5.0F,
                true,
                true,
                true,
                true,
                0.0F,
                0.0F,
                this.thumbnailWidth,
                this.thumbnailHeight,
                this.thumbnailWidth,
                this.thumbnailHeight,
                (Hud.animatedColor(-1) >>> 24 & 0xFF) / 255.0F
            );
        } else {
            RoundedRenderer.drawRoundedRect(context, x, y, size, size, 5.0F, Hud.animatedColor(1526726655));
            int color = Hud.accent();
            float factor = size / 34.0F;
            RoundedRenderer.drawRoundedRect(context, x + 12.0F * factor, y + 20.0F * factor, 7.0F * factor, 5.0F * factor, 2.5F * factor, color);
            RoundedRenderer.drawRoundedRect(context, x + 21.0F * factor, y + 17.0F * factor, 7.0F * factor, 5.0F * factor, 2.5F * factor, color);
            RoundedRenderer.drawRoundedRect(context, x + 17.0F * factor, y + 9.0F * factor, 2.0F * factor, 13.0F * factor, 1.0F * factor, color);
            RoundedRenderer.drawRoundedRect(context, x + 26.0F * factor, y + 7.0F * factor, 2.0F * factor, 12.0F * factor, 1.0F * factor, color);
            RoundedRenderer.drawRoundedRect(context, x + 17.0F * factor, y + 7.0F * factor, 11.0F * factor, 2.0F * factor, 1.0F * factor, color);
        }
    }

    public void prepareWorldArtwork() {
        this.reader.start();
        this.updateThumbnail();
    }

    public boolean hasWorldArtwork() {
        return this.thumbnailRegistered && this.thumbnail != null;
    }

    public Identifier getWorldArtworkTexture() {
        return THUMBNAIL;
    }

    public int getWorldArtworkWidth() {
        return this.thumbnailWidth;
    }

    public int getWorldArtworkHeight() {
        return this.thumbnailHeight;
    }

    private void updateLifecycle() {
        boolean enabled = ru.fluxvisuals.ui.fluxgui.module.render.Hud.mediaPlayer.getValue();
        if (enabled && !this.readerStarted) {
            this.reader.start();
            this.readerStarted = true;
        } else if (!enabled && this.readerStarted) {
            this.reader.stop();
            this.cleanupThumbnail();
            this.readerStarted = false;
        }
    }

    private void updateThumbnail() {
        if (this.reader.consumeThumbnailUpdate()
            || !this.thumbnailRegistered && this.reader.hasMedia() && System.currentTimeMillis() - this.lastThumbnailAttempt > 2000L) {
            this.loadThumbnail();
        }
    }

    private void loadThumbnail() {
        this.lastThumbnailAttempt = System.currentTimeMillis();

        try {
            Path path = this.reader.getThumbnailPath();
            if (path != null && Files.exists(path) && Files.size(path) >= 100L) {
                try (InputStream input = Files.newInputStream(path)) {
                    NativeImage image = NativeImage.read(input);
                    if (image != null) {
                        this.cleanupThumbnail();
                        this.thumbnailWidth = image.getWidth();
                        this.thumbnailHeight = image.getHeight();
                        this.thumbnail = new NativeImageBackedTexture(() -> "fluxvisuals/media", image);
                        MinecraftClient.getInstance().getTextureManager().registerTexture(THUMBNAIL, this.thumbnail);
                        this.thumbnailRegistered = true;
                        return;
                    }

                    return;
                }
            } else {
                this.cleanupThumbnail();
            }
        } catch (LinkageError | Exception var7) {
            this.thumbnailRegistered = false;
        }
    }

    private void cleanupThumbnail() {
        if (this.thumbnail != null) {
            try {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(THUMBNAIL);
            } catch (Exception var2) {
            }
        }

        this.thumbnail = null;
        this.thumbnailRegistered = false;
        this.thumbnailWidth = 0;
        this.thumbnailHeight = 0;
    }

    private static String fit(String text, float maxWidth, float size) {
        if (text != null && !text.isEmpty()) {
            if (TextRender.width(text, size) <= maxWidth) {
                return text;
            }

            String suffix = "...";

            for (int end = text.length(); end > 0; end--) {
                String value = text.substring(0, end).trim() + suffix;
                if (TextRender.width(value, size) <= maxWidth) {
                    return value;
                }
            }

            return suffix;
        } else {
            return "";
        }
    }

    private static String formatTime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }
}
