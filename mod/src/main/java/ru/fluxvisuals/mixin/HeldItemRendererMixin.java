package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.render.HandAnimationEvent;
import ru.fluxvisuals.event.render.RenderItemEvent;
import net.minecraft.registry.tag.ItemTags;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraItemStackRenderStateExtension;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.math.vector.FiguraVec3;
import net.minecraft.client.render.model.json.Transformation;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeldItemRendererMixin.class);
    private static long fluxvisuals$lastItemErrorNanos;
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
   @Unique
   private boolean fluxvisuals$customPoseApplied;
   @Unique
   private boolean fluxvisuals$customPosePopped;
   @Unique
   private boolean fluxvisuals$hideMainArm;
   @Unique
   private boolean fluxvisuals$customWeaponRenderedLastFrame;
   @Unique
   private boolean fluxvisuals$customWeaponRenderedThisFrame;

   @Shadow
   protected abstract void renderFirstPersonItem(
      AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, OrderedRenderCommandQueue var9, int var10
   );

   @Shadow
   private void renderArmHoldingItem(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
         float equipProgress, float swingProgress, Arm arm) { }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderFirstPersonItem(
      AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci
   ) {
      // Off-hand must not inherit the main-hand swing pose.
      if (this.fluxvisuals$customPoseApplied && !this.fluxvisuals$customPosePopped && hand == Hand.OFF_HAND) {
         matrices.pop();
         this.fluxvisuals$customPosePopped = true;
      }
      this.fluxvisuals$currentHand = hand;
      RenderItemEvent renderItemEvent = new RenderItemEvent(matrices, hand, stack);
      EventManager.call(renderItemEvent);
   }


   /**
    * Apply the swing pose before Figura renders the avatar hand. Figura hooks the HEAD of
    * HeldItemRenderer.renderItem, so injecting into renderFirstPersonItem would be too late.
    */
   @Inject(
      method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
      at = {@At("HEAD")},
      cancellable = true,
      order = 500
   )
   private void fluxvisuals$applySwingBeforeAvatar(
      float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue,
      ClientPlayerEntity player, int light, CallbackInfo ci
   ) {
      this.fluxvisuals$hideMainArm = this.fluxvisuals$customWeaponRenderedLastFrame;
      this.fluxvisuals$customWeaponRenderedThisFrame = false;
      this.fluxvisuals$customPoseApplied = false;
      this.fluxvisuals$customPosePopped = false;
      if (player == null || player.getMainHandStack().isEmpty() || player.isUsingItem()) return;
      ItemStack held = player.getMainHandStack();
      if (held.getItem() instanceof net.minecraft.item.CrossbowItem) return;
      ru.fluxvisuals.module.impl.visuals.SwingAnimation swingAnim =
            ru.fluxvisuals.client.FluxVisualsClient.get.manager.get(ru.fluxvisuals.module.impl.visuals.SwingAnimation.class);
      if (swingAnim == null || !swingAnim.enable
            || !ru.fluxvisuals.module.impl.visuals.SwingAnimation.isCustomAnimationActive()) return;
      float swingProgress = player.getHandSwingProgress(tickProgress);
      if (swingProgress <= 0.0F) return;
      float equipProgress = 1.0F - MathHelper.lerp(
            tickProgress, this.lastEquipProgressMainHand, this.equipProgressMainHand);
      matrices.push();
      ru.fluxvisuals.module.impl.visuals.SwingAnimation.applyFirstPersonPose(
            matrices, Hand.MAIN_HAND, swingProgress, equipProgress);
      this.fluxvisuals$customPoseApplied = true;
   }

   @Inject(
      method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
      at = {@At("RETURN")}
   )
   private void fluxvisuals$restoreSwingAfterAvatar(
      float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue,
      ClientPlayerEntity player, int light, CallbackInfo ci
   ) {
      if (this.fluxvisuals$customPoseApplied && !this.fluxvisuals$customPosePopped) {
         matrices.pop();
         this.fluxvisuals$customPosePopped = true;
      }
      this.fluxvisuals$customWeaponRenderedLastFrame = this.fluxvisuals$customWeaponRenderedThisFrame;
   }

   @Inject(
      method = "applyEquipOffset",
      at = {@At("HEAD")},
      cancellable = true
   )
   private void fluxvisuals$cancelEquipOffset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
      if (this.fluxvisuals$customPoseApplied && !this.fluxvisuals$customPosePopped
            && this.fluxvisuals$currentHand == Hand.MAIN_HAND) {
         ci.cancel();
      }
   }

   /**
    * Figura отправляет аватар оружия с той позой MatrixStack, которая есть в момент
    * вызова. Ванильный слой применяет transform предмета позже, поэтому до вызова
    * itemRenderEvent применяем его сами на копии матрицы руки.
    */
   @Redirect(
      method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"
      )
   )
   private void fluxvisuals$renderFirstPersonFiguraItem(
      ItemRenderState renderState, MatrixStack matrices, OrderedRenderCommandQueue queue,
      int light, int overlay, int seed
   ) {
      if (!(renderState instanceof FiguraItemStackRenderStateExtension state)) {
         renderState.render(matrices, queue, light, overlay, seed);
         return;
      }
      ItemDisplayContext displayContext = state.figura$getDisplayContext();
      // Third-person Figura уже вызывает itemRenderEvent в своих слоях — здесь
      // обрабатываем только руки, чтобы не дублировать кастомную модель.
      if (displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
         && displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
         renderState.render(matrices, queue, light, overlay, seed);
         return;
      }
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null) {
         renderState.render(matrices, queue, light, overlay, seed);
         return;
      }
      try {
         Avatar avatar = AvatarManager.getAvatarForPlayer(client.player.getUuid());
         if (avatar == null || avatar.luaRuntime == null) {
            renderState.render(matrices, queue, light, overlay, seed);
            return;
         }
         Transformation transform = state.figura$getItemTransform();
         if (transform == null) {
            renderState.render(matrices, queue, light, overlay, seed);
            return;
         }

         boolean rendered = avatar.itemRenderEvent(
            ItemStackAPI.verify(state.figura$getItemStack()),
            displayContext.name(),
            FiguraVec3.fromVec3f(transform.translation()),
            FiguraVec3.of(transform.rotation().z(), transform.rotation().y(), transform.rotation().x()),
            FiguraVec3.fromVec3f(transform.scale()),
            state.figura$isLeftHanded(),
            matrices, queue, light, overlay
         );
         if (rendered && this.fluxvisuals$isMainHandContext(displayContext, client.player)) {
            this.fluxvisuals$customWeaponRenderedThisFrame = true;
         }
         if (!rendered) {
            renderState.render(matrices, queue, light, overlay, seed);
         }
      } catch (Throwable t) {
         long now = System.nanoTime();
         if (now - fluxvisuals$lastItemErrorNanos > 5_000_000_000L) {
            fluxvisuals$lastItemErrorNanos = now;
            LOGGER.warn("Figura first-person item render failed", t);
         }
         renderState.render(matrices, queue, light, overlay, seed);
      }
   }

   private boolean fluxvisuals$hasSwordInHands(ClientPlayerEntity player) {
      return player.getMainHandStack().isIn(ItemTags.SWORDS)
            || player.getOffHandStack().isIn(ItemTags.SWORDS);
   }

   private boolean fluxvisuals$isMainHandContext(ItemDisplayContext context, ClientPlayerEntity player) {
      return (player.getMainArm() == Arm.RIGHT && context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            || (player.getMainArm() == Arm.LEFT && context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
   }

   /**
    * Vanilla draws the first-person hand before the item model. If Figura returned a custom
    * weapon model last frame, skip only that arm — the item render below still runs.
    */
   @Redirect(
      method = "renderFirstPersonItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V"
      )
   )
   private void fluxvisuals$skipCustomWeaponArm(
      HeldItemRenderer renderer, MatrixStack matrices, OrderedRenderCommandQueue queue,
      int light, float equipProgress, float swingProgress, Arm arm
   ) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && fluxvisuals$hasSwordInHands(client.player)) return;
      ((HeldItemRendererMixin) (Object) renderer).renderArmHoldingItem(matrices, queue, light, equipProgress, swingProgress, arm);
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

}
