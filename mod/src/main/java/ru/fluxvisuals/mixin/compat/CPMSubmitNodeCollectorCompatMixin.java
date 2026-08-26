package ru.fluxvisuals.mixin.compat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.function.TriFunction;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.NodeCollectorExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraSubmission;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;

@Mixin(targets = "com.tom.cpm.client.CPMOrderedSubmitNodeCollector$CPMSubmitNodeCollector", remap = false)
public abstract class CPMSubmitNodeCollectorCompatMixin implements NodeCollectorExtension {

	@Shadow
	@Final
	private OrderedRenderCommandQueue collector;

	@Unique
	private final List<FiguraSubmission> flux$figuraSubmissions = new ArrayList<>();

	@Override
	public <S extends EntityRenderState> void submitFiguraModel(Avatar avatar, S renderState,
			TriFunction<Avatar, S, VertexConsumerProvider, Void> renderer) {
		if (this.collector instanceof NodeCollectorExtension ext) {
			ext.submitFiguraModel(avatar, renderState, renderer);
			return;
		}
		this.flux$figuraSubmissions.add(this.flux$wrapSubmission(avatar, renderState, renderer));
	}

	@Override
	public List<FiguraSubmission> getFiguraSubmissions() {
		if (this.collector instanceof NodeCollectorExtension ext) {
			return ext.getFiguraSubmissions();
		}
		return this.flux$figuraSubmissions;
	}

	@Unique
	@SuppressWarnings({"unchecked", "rawtypes"})
	private FiguraSubmission flux$wrapSubmission(Avatar avatar, EntityRenderState renderState,
			TriFunction<Avatar, ? extends EntityRenderState, VertexConsumerProvider, Void> renderer) {
		TriFunction<Avatar, EntityRenderState, VertexConsumerProvider, Void> cast =
				(TriFunction) renderer;
		return new FiguraSubmission(avatar, renderState, cast);
	}
}
