package ru.fluxvisuals.api.render.util;

import com.mojang.blaze3d.systems.RenderPass;

public interface IRenderPass {
    void apply(RenderPass renderPass);
}
