package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.utils.BetterF3;
import ru.fluxvisuals.util.color.ColorUtil;

/**
 * DebugHudMixin — отменяет ванильный F3 и рисует кастомный оверлей при включённом BetterF3.
 */
@Environment(EnvType.CLIENT)
@Mixin(DebugHud.class)
public class DebugHudMixin {

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void onRender(DrawContext context, CallbackInfo ci) {
      BetterF3 module = null;
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         module = FluxVisualsClient.get.manager.get(BetterF3.class);
      }
      if (module == null || !module.enable) return;

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.world == null) return;

      // Show custom overlay ONLY when F3 key is held (like vanilla F3)
      if (!BetterF3.isF3Pressed()) return;

      ci.cancel(); // Cancel vanilla debug

      float guiScale = (float) mc.getWindow().getScaleFactor();
      if (guiScale <= 0.0F) guiScale = 1.0F;

      int leftOrRight = module.position.is("Right")
         ? mc.getWindow().getScaledWidth() - 180 : 5;
      int x = leftOrRight;
      int y = 5;
      int lineH = 11;
      int bgAlpha = 160;
      int textColor = 0xFFFFFFFF;
      int labelColor = 0xFFAAAAAA;
      int accentColor = 0xFF13FFAE;

      // Background panel
      int lines = 0;
      if (module.showFps.get()) lines += 1;
      if (module.showCoords.get()) lines += 3;
      if (module.showDirection.get()) lines += 1;
      if (module.showPing.get()) lines += 1;
      if (module.showEntities.get()) lines += 2;
      if (module.showMemory.get()) lines += 1;
      lines = Math.max(lines, 1);

      int panelH = lines * lineH + 12;
      int panelW = 175;
      context.fill(x - 2, y - 2, x + panelW, y + panelH, (bgAlpha << 24));

      int cursorY = y + 4;

      if (module.showFps.get()) {
         int fps = mc.getCurrentFps();
         drawDebugLine(context, x, cursorY, "FPS: ", String.valueOf(fps),
            fps >= 55 ? 0xFF55FF55 : fps >= 30 ? 0xFFFFFF55 : 0xFFFF5555, labelColor);
         cursorY += lineH;
      }

      if (module.showCoords.get()) {
         double px = mc.player.getX();
         double py = mc.player.getY();
         double pz = mc.player.getZ();
         drawDebugLine(context, x, cursorY, "XYZ: ",
            String.format("%.1f / %.1f / %.1f", px, py, pz), textColor, labelColor);
         cursorY += lineH;

         int bx = (int) Math.floor(px);
         int by = (int) Math.floor(py);
         int bz = (int) Math.floor(pz);
         drawDebugLine(context, x, cursorY, "Block: ",
            bx + " " + by + " " + bz, textColor, labelColor);
         cursorY += lineH;

         int chunkX = bx >> 4;
         int chunkZ = bz >> 4;
         drawDebugLine(context, x, cursorY, "Chunk: ",
            chunkX + " " + chunkZ, textColor, labelColor);
         cursorY += lineH;
      }

      if (module.showDirection.get()) {
         String facing = mc.player.getHorizontalFacing().asString().toUpperCase();
         float yaw = mc.player.getYaw() % 360;
         if (yaw < 0) yaw += 360;
         drawDebugLine(context, x, cursorY, "Facing: ", facing + String.format(" (%.1f°)", yaw), textColor, labelColor);
         cursorY += lineH;
      }

      if (module.showPing.get()) {
         PlayerListEntry entry = mc.getNetworkHandler() != null
            ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
         int ping = entry != null ? entry.getLatency() : 0;
         int pingColor = ping < 50 ? 0xFF55FF55 : ping < 150 ? 0xFFFFFF55 : 0xFFFF5555;
         drawDebugLine(context, x, cursorY, "Ping: ", ping + " ms", pingColor, labelColor);
         cursorY += lineH;
      }

      if (module.showEntities.get()) {
         int entityCount = 0;
         for (net.minecraft.entity.Entity ignored : mc.world.getEntities()) entityCount++;
         drawDebugLine(context, x, cursorY, "Entities: ", String.valueOf(entityCount), textColor, labelColor);
         cursorY += lineH;

         long chunkCount = mc.world.getChunkManager().getLoadedChunkCount();
         drawDebugLine(context, x, cursorY, "Chunks: ", String.valueOf(chunkCount), textColor, labelColor);
         cursorY += lineH;
      }

      if (module.showMemory.get()) {
         Runtime rt = Runtime.getRuntime();
         long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
         long max = rt.maxMemory() / (1024 * 1024);
         drawDebugLine(context, x, cursorY, "Memory: ", used + " / " + max + " MB", textColor, labelColor);
         cursorY += lineH;
      }
   }

   private void drawDebugLine(DrawContext ctx, int x, int y, String label, String value, int valueColor, int labelColor) {
      // Use vanilla text renderer for debug lines (simpler than Renderer2D here)
      var tr = MinecraftClient.getInstance().textRenderer;
      ctx.drawText(tr, label, x, y, labelColor, false);
      ctx.drawText(tr, value, x + tr.getWidth(label), y, valueColor, false);
   }
}
