package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.fog.FogRenderer;

public interface IGameRenderer {
    GuiRenderer flux$getGuiRenderer();
    FogRenderer flux$getFogRenderer();
    GuiRenderState flux$getGuiState();
    void flux$flushGui();
}

