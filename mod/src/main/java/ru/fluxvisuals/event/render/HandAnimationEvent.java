package ru.fluxvisuals.event.render;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.client.util.math.MatrixStack;
import ru.fluxvisuals.event.Event;

@Environment(EnvType.CLIENT)
public class HandAnimationEvent extends Event {
   private MatrixStack matrices;
   private Hand hand;
   private float swingProgress;
   private float equipProgress;

   @Generated
   public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress) {
      this(matrices, hand, swingProgress, 0.0F);
   }

   @Generated
   public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress, float equipProgress) {
      this.matrices = matrices;
      this.hand = hand;
      this.swingProgress = swingProgress;
      this.equipProgress = equipProgress;
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }

   @Generated
   public Hand getHand() {
      return this.hand;
   }

   @Generated
   public float getSwingProgress() {
      return this.swingProgress;
   }

   @Generated
   public float getEquipProgress() {
      return this.equipProgress;
   }

   @Generated
   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
   }

   @Generated
   public void setHand(Hand hand) {
      this.hand = hand;
   }

   @Generated
   public void setSwingProgress(float swingProgress) {
      this.swingProgress = swingProgress;
   }

   @Generated
   public void setEquipProgress(float equipProgress) {
      this.equipProgress = equipProgress;
   }
}
