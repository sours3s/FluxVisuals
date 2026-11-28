package ru.fluxvisuals.mixin;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.MaceHelper;

@Environment(EnvType.CLIENT)
@Mixin(DrawContext.class)
public abstract class MaceHelperDrawContextMixin {
   @Shadow
   public abstract void fill(int x1, int y1, int x2, int y2, int color);

   @Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;III)V", at = @At("HEAD"))
   private void fluxvisuals$maceItemHighlight(LivingEntity entity, ItemStack stack, int x, int y, int size, CallbackInfo ci) {
      MaceHelper maceHelper = FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null
         ? FluxVisualsClient.get.manager.get(MaceHelper.class)
         : null;
      if (maceHelper != null && maceHelper.enable && maceHelper.hotbarHighlight.get() && MaceHelper.isMace(stack)) {
         Color color = MaceHelper.getChargeColor();
         int rgb = new Color(color.getRed(), color.getGreen(), color.getBlue(), 120).getRGB();
         Matrix3x2fStack matrices = ((DrawContext) (Object) this).getMatrices();
         matrices.pushMatrix();
         matrices.translate(0.0F, 0.0F);
         this.fill(x, y, x + 16, y + 16, rgb);
         matrices.popMatrix();
      }
   }
}
