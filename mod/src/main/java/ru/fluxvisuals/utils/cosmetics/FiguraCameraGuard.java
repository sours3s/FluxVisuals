package ru.fluxvisuals.utils.cosmetics;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.api.RendererAPI;

/** Clears Figura camera overrides that would put the camera inside an avatar. */
@Environment(EnvType.CLIENT)
public final class FiguraCameraGuard {
   private FiguraCameraGuard() {
   }

   public static void clearLocalOverrides() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.options == null || !mc.options.getPerspective().isFirstPerson()) {
         return;
      }
      Avatar avatar = AvatarManager.getAvatar(mc.player);
      if (avatar == null || avatar.luaRuntime == null || avatar.luaRuntime.renderer == null) {
         return;
      }
      clear(avatar.luaRuntime.renderer);
   }

   public static void clear(RendererAPI renderer) {
      if (renderer == null) {
         return;
      }
      renderer.cameraPos = null;
      renderer.cameraPivot = null;
      renderer.cameraOffsetPivot = null;
      renderer.cameraRot = null;
      renderer.cameraOffsetRot = null;
      renderer.eyeOffset = null;
      renderer.cameraMat = null;
      renderer.cameraNormal = null;
   }
}
