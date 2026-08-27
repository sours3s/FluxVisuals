package ru.fluxvisuals.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Отключен: проблемы с маппингами в 1.21.11 (Invalid descriptor на VertexConsumerProvider)
// @Mixin(EntityRenderer.class)
// public abstract class EntityRendererMixin<T extends net.minecraft.entity.Entity, S extends EntityRenderState> {
//     @Inject(method = "render", at = @At("HEAD"))
//     private void onRender(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
//         // В 1.21+ первый параметр - это EntityRenderState, а не Entity
//         // state содержит все данные для рендеринга (позиция, поворот, анимации и т.д.)
//
//         // Здесь можно добавить вашу логику рендеринга сущностей
//         // Например, рендеринг кастомных эффектов вокруг сущностей
//     }
//
//     // @Inject(method = "tick", at = @At("HEAD"))
//     // private void onTick(CallbackInfo ci) {
//     //     // Отключено: метод tick отсутствует в EntityRenderer 1.21.11
//     //     // Получаем экземпляр EntityRenderer
//     //     EntityRenderer<T, S> renderer = (EntityRenderer<T, S>)(Object)this;
//     //
//     //     // Здесь можно добавить логику обновления сущностей каждый тик
//     //     // Например, обновление кастомных эффектов вокруг сущностей
//     // }
// }