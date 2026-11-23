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
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.render.HandAnimationEvent;
import ru.fluxvisuals.event.render.RenderItemEvent;

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
      at = {@At("HEAD")}
   )
   private void onRenderFirstPersonItem(
      AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci
   ) {
      this.fluxvisuals$currentHand = hand;
      RenderItemEvent renderItemEvent = new RenderItemEvent(matrices, hand, stack);
      EventManager.call(renderItemEvent);
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

   @Redirect(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch(F)F"
      )
   )
   private float redirectGetPitch(ClientPlayerEntity instance, float tickProgress) {
      return MinecraftClient.getInstance().gameRenderer.getCamera().getPitch();
   }

   @Redirect(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw(F)F"
      )
   )
   private float redirectGetYaw(ClientPlayerEntity instance, float tickProgress) {
      return MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
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
