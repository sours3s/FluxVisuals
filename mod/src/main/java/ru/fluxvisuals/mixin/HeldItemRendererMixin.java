package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.render.HandAnimationEvent;
import ru.fluxvisuals.event.render.RenderItemEvent;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.math.vector.FiguraVec3;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
    @Shadow
    private ItemStack mainHand;
    @Shadow
    private ItemStack offHand;
    @Shadow
    private float equipProgressMainHand;
    @Shadow
    private float lastEquipProgressMainHand;
    @Shadow
    private float equipProgressOffHand;
    @Shadow
    private float lastEquipProgressOffHand;
    @Unique
    private float fluxvisuals$originalSwingProgress;
   @Unique
   private float fluxvisuals$currentEquipProgress;
   @Unique
   private Hand fluxvisuals$currentHand;

   @Shadow
   protected abstract void renderFirstPersonItem(
      AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, OrderedRenderCommandQueue var9, int var10
   );

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderFirstPersonItem(
      AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci
   ) {
      this.fluxvisuals$currentHand = hand;
      RenderItemEvent renderItemEvent = new RenderItemEvent(matrices, hand, stack);
      EventManager.call(renderItemEvent);

      // Figura 0.1.5 вызывает itemRenderEvent только в third-person слоях, из-за чего
      // аватары оружия от первого лица никогда не появляются. Добавляем событие здесь.
      if (!stack.isEmpty()) {
         try {
            Avatar avatar = AvatarManager.getAvatarForPlayer(player.getUuid());
            if (avatar != null && avatar.luaRuntime != null) {
               FiguraVec3 zero = FiguraVec3.of();
               FiguraVec3 one = FiguraVec3.of(1.0, 1.0, 1.0);
               String context = hand == Hand.OFF_HAND ? "FIRST_PERSON_LEFT_HAND" : "FIRST_PERSON_RIGHT_HAND";
               boolean rendered = avatar.itemRenderEvent(
                  ItemStackAPI.verify(stack), context, zero, zero, one,
                  player.getMainArm() == Arm.LEFT, matrices, queue, light, OverlayTexture.DEFAULT_UV
               );
               if (rendered) {
                  ci.cancel();
               }
            }
         } catch (Throwable ignored) {
            // Figura может отсутствовать или аватар может быть не загружен — ванильный рендер остаётся.
         }
      }
   }

   @ModifyVariable(
      method = "renderFirstPersonItem",
      at = @At("HEAD"),
      ordinal = 2,
      argsOnly = true
   )
   private float modifySwingProgress(float swingProgress, AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand) {
      if (hand == Hand.MAIN_HAND) {
         this.fluxvisuals$originalSwingProgress = swingProgress;
         ru.fluxvisuals.module.impl.visuals.SwingAnimation swingAnim = ru.fluxvisuals.client.FluxVisualsClient.get.manager.get(ru.fluxvisuals.module.impl.visuals.SwingAnimation.class);
         if (swingAnim != null && swingAnim.enable && ru.fluxvisuals.module.impl.visuals.SwingAnimation.isCustomAnimationActive()) {
            return 0.0F;
         }
      }
      return swingProgress;
   }

   @ModifyVariable(
      method = "renderFirstPersonItem",
      at = @At("HEAD"),
      ordinal = 3,
      argsOnly = true
   )
   private float modifyEquipProgress(float equipProgress, AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand) {
      if (hand == Hand.MAIN_HAND) {
         this.fluxvisuals$currentEquipProgress = equipProgress;
      }
      return equipProgress;
   }



   @Inject(
      method = {"applySwingOffset"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handAnimationHook(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null) {
         return;
      }

      Hand hand = arm == client.player.getMainArm() ? Hand.MAIN_HAND : Hand.OFF_HAND;
      HandAnimationEvent event = new HandAnimationEvent(matrices, hand, this.fluxvisuals$originalSwingProgress, this.fluxvisuals$currentEquipProgress);
      EventManager.call(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Unique
   private boolean fluxvisuals$isChargedCrossbow(ItemStack stack) {
      return stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
   }
}
