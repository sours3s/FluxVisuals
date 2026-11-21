package ru.fluxvisuals.ui.fluxgui.cosmetics;

import net.minecraft.util.Identifier;

/** Элемент косметики GUI FluxVisuals. */
public final class CosmeticItem {
    private final String name;
    private final String category;

    public CosmeticItem(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getSlug() {
        return name.toLowerCase().replace(' ', '_');
    }

    public BedrockModel getModel() {
        return new BedrockModel();
    }

    public Identifier getPreviewId() {
        return Identifier.of("fluxvisuals", "textures/gui/fluxvisuals_logo.png");
    }

    public Identifier getTextureId() {
        return getPreviewId();
    }

    public Object getAnimation() {
        return null;
    }
}
