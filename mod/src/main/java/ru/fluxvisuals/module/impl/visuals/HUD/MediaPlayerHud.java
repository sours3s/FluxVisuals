package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.media.MediaSessionReader;
import ru.fluxvisuals.util.render.TextCache;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Компактная HUD-панель текущей медиа-сессии Windows.
 *
 * <p>Показывает обложку, название, исполнителя, состояние воспроизведения,
 * тайминг и прогресс. Чтение сессии выполняет {@link MediaSessionReader},
 * который запускается лениво при первом обращении к панели.
 */
@Environment(EnvType.CLIENT)
public final class MediaPlayerHud {
   private static final float WIDTH = 180.0F;
   private static final float HEIGHT = 66.0F;

   private static final float COVER_X = 8.0F;
   private static final float COVER_Y = 9.0F;
   private static final float COVER_SIZE = 46.0F;

   private static final float TEXT_X = 62.0F;
   private static final float ICON_Y = 45.0F;
   private static final float PREV_X = 62.0F;
   private static final float PLAY_X = 80.0F;
   private static final float NEXT_X = 98.0F;

   private static final Identifier COVER_ID = Identifier.of("fluxvisuals", "media_player_cover");

   private static NativeImageBackedTexture coverTexture;
   private static volatile boolean coverReady;
   private static boolean prevPressed;

   private MediaPlayerHud() {
   }

   /** True, когда есть хотя бы одна активная медиа-сессия. */
   public static boolean hasContent() {
      MediaSessionReader reader = MediaSessionReader.getInstance();
      reader.start();
      return reader.hasMedia();
   }

   public static void render(Renderer2D r2, DrawContext ctx) {
      MediaSessionReader reader = MediaSessionReader.getInstance();
      reader.start();

      if (!reader.hasMedia()) {
         clearCover();
         return;
      }
      if (r2 == null || ctx == null || FontRegistry.INTER_MEDIUM == null) {
         return;
      }

      DraggableManager.DragSession session = DraggableManager.getInstance()
         .beginDrag("mediaPlayerHud", 20.0F, 100.0F, WIDTH, HEIGHT);
      float x = session.positionX();
      float y = session.positionY();

      Hud.drawClientRect(r2, x, y, WIDTH, HEIGHT, 8.0F, 1.0F, 1.0F);

      float coverX = x + COVER_X;
      float coverY = y + COVER_Y;
      Hud.drawClientRectLight(r2, coverX, coverY, COVER_SIZE, COVER_SIZE, 5.0F, 1.0F, 0.0F);

      if (reader.consumeThumbnailUpdate()) {
         Path thumbnail = reader.getThumbnailPath();
         if (thumbnail != null && Files.isRegularFile(thumbnail)) {
            scheduleCoverLoad(thumbnail);
         } else {
            clearCover();
         }
      }

      if (coverReady) {
         int ix = Math.round(coverX);
         int iy = Math.round(coverY);
         int size = Math.round(COVER_SIZE);
         ctx.drawTexture(RenderPipelines.GUI_TEXTURED, COVER_ID,
            ix, iy, 0.0F, 0.0F, size, size, size, size, 0xFFFFFFFF);
      } else {
         int muted = ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 0.32F);
         r2.circle(coverX + COVER_SIZE / 2.0F, coverY + COVER_SIZE / 2.0F, 7.0F, 0.0F, 1.0F, muted);
      }

      int textColor = Renderer2D.ColorUtil.getTextColor(1, 1);
      int mainColor = Renderer2D.ColorUtil.getMainColor(1, 1);

      String title = fitText(r2, safe(reader.getTitle()), 108.0F, 18.0F);
      String artist = fitText(r2, safe(reader.getArtist()), 108.0F, 15.0F);
      if (title.isEmpty()) {
         title = "—";
      }
      if (artist.isEmpty()) {
         artist = "—";
      }

      r2.text(FontRegistry.INTER_MEDIUM, x + TEXT_X, y + 19.0F, 18.0F, title, textColor);
      r2.text(FontRegistry.INTER_MEDIUM, x + TEXT_X, y + 35.0F, 15.0F,
         artist, ColorUtil.replAlpha(textColor, 0.62F));

      drawPrevious(r2, x + PREV_X, y + ICON_Y, mainColor);
      if (reader.isPlaying()) {
         drawPause(r2, x + PLAY_X, y + ICON_Y, mainColor);
      } else {
         drawTriangle(r2, x + PLAY_X, y + ICON_Y, mainColor, true);
      }
      drawNext(r2, x + NEXT_X, y + ICON_Y, mainColor);

      long position = reader.getPositionMs();
      long duration = reader.getDurationMs();
      String timing = formatTime(position) + " / " + formatTime(duration);
      float timingSize = 13.0F;
      float timingWidth = TextCache.width(r2, FontRegistry.INTER_MEDIUM, timing, timingSize);
      r2.text(FontRegistry.INTER_MEDIUM, x + WIDTH - 8.0F - timingWidth, y + 49.0F,
         timingSize, timing, ColorUtil.replAlpha(textColor, 0.75F));

      float barX = x + 8.0F;
      float barY = y + 60.0F;
      float barWidth = WIDTH - 16.0F;
      float barHeight = 2.5F;
      Hud.drawClientRectLight(r2, barX, barY, barWidth, barHeight, 1.25F, 1.0F, 0.0F);
      if (duration > 0L) {
         float progress = Math.max(0.0F, Math.min(1.0F, (float) position / (float) duration));
         if (progress > 0.0F) {
            r2.rect(barX, barY, barWidth * progress, barHeight, 1.25F, mainColor);
         }
      }

      handleClicks(x, y, reader);

      DraggableManager.getInstance().endDrag(session);
   }

   private static void scheduleCoverLoad(Path path) {
      MinecraftClient mc = MinecraftClient.getInstance();
      mc.execute(() -> {
         destroyCover();
         try (InputStream stream = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(stream);
            if (image == null) {
               return;
            }
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> COVER_ID.toString(), image);
            mc.getTextureManager().registerTexture(COVER_ID, texture);
            coverTexture = texture;
            coverReady = true;
         } catch (Exception ignored) {
            coverReady = false;
         }
      });
   }

   private static void clearCover() {
      if (!coverReady && coverTexture == null) {
         return;
      }
      coverReady = false;
      MinecraftClient mc = MinecraftClient.getInstance();
      mc.execute(MediaPlayerHud::destroyCover);
   }

   private static void destroyCover() {
      MinecraftClient mc = MinecraftClient.getInstance();
      mc.getTextureManager().destroyTexture(COVER_ID);
      coverTexture = null;
      coverReady = false;
   }

   private static void handleClicks(float x, float y, MediaSessionReader reader) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getWindow() == null) {
         prevPressed = false;
         return;
      }

      long window = mc.getWindow().getHandle();
      if (window == 0L) {
         prevPressed = false;
         return;
      }

      boolean pressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
      if (mc.currentScreen != null || !pressed || prevPressed) {
         prevPressed = pressed;
         return;
      }
      prevPressed = true;

      double[] cursorX = new double[1];
      double[] cursorY = new double[1];
      GLFW.glfwGetCursorPos(window, cursorX, cursorY);
      float mx = (float) cursorX[0];
      float my = (float) cursorY[0];

      float iconTop = y + ICON_Y - 7.0F;
      if (inside(mx, my, x + PREV_X - 2.0F, iconTop, 14.0F, 14.0F)) {
         reader.previous();
      } else if (inside(mx, my, x + PLAY_X - 2.0F, iconTop, 14.0F, 14.0F)) {
         reader.playPause();
      } else if (inside(mx, my, x + NEXT_X - 2.0F, iconTop, 14.0F, 14.0F)) {
         reader.next();
      }
   }

   private static boolean inside(float px, float py, float x, float y, float w, float h) {
      return px >= x && px <= x + w && py >= y && py <= y + h;
   }

   private static void drawPrevious(Renderer2D r2, float x, float y, int color) {
      r2.rect(x, y - 4.0F, 1.8F, 8.0F, 0.6F, color);
      r2.rect(x + 3.0F, y - 4.0F, 1.8F, 8.0F, 0.6F, color);
      drawTriangle(r2, x + 8.0F, y, color, false);
   }

   private static void drawNext(Renderer2D r2, float x, float y, int color) {
      drawTriangle(r2, x, y, color, true);
      r2.rect(x + 9.0F, y - 4.0F, 1.8F, 8.0F, 0.6F, color);
      r2.rect(x + 12.0F, y - 4.0F, 1.8F, 8.0F, 0.6F, color);
   }

   private static void drawPause(Renderer2D r2, float x, float y, int color) {
      r2.rect(x, y - 4.0F, 2.2F, 8.0F, 0.7F, color);
      r2.rect(x + 4.0F, y - 4.0F, 2.2F, 8.0F, 0.7F, color);
   }

   /** Простой треугольник из нескольких полос — без зависимости от глифов иконок. */
   private static void drawTriangle(Renderer2D r2, float x, float y, int color, boolean right) {
      float[] widths = {2.0F, 4.0F, 6.0F, 4.0F, 2.0F};
      for (int i = 0; i < widths.length; i++) {
         float w = widths[i];
         float startX = right ? x : x + (6.0F - w);
         r2.rect(startX, y - 4.0F + i * 1.6F, w, 1.4F, 0.5F, color);
      }
   }

   private static String fitText(Renderer2D r2, String text, float maxWidth, float size) {
      if (text.isEmpty()) {
         return text;
      }
      if (TextCache.width(r2, FontRegistry.INTER_MEDIUM, text, size) <= maxWidth) {
         return text;
      }

      String cut = text;
      while (!cut.isEmpty() && TextCache.width(r2, FontRegistry.INTER_MEDIUM, cut + "...", size) > maxWidth) {
         cut = cut.substring(0, cut.length() - 1);
      }
      return cut.isEmpty() ? "" : cut + "...";
   }

   private static String safe(String value) {
      return value == null ? "" : value.strip();
   }

   private static String formatTime(long millis) {
      long totalSeconds = Math.max(0L, millis) / 1000L;
      return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L);
   }
}
