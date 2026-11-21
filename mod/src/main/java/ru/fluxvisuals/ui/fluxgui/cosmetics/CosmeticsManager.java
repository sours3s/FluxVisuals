package ru.fluxvisuals.ui.fluxgui.cosmetics;

import java.util.List;

/** Хранилище косметики GUI FluxVisuals. */
public final class CosmeticsManager {
    public static final CosmeticsManager INSTANCE = new CosmeticsManager();

    private CosmeticsManager() {
    }

    public List<CosmeticItem> getAllCosmetics() {
        return List.of();
    }

    public List<CosmeticItem> getByCategory(String category) {
        return List.of();
    }

    public boolean isEquipped(CosmeticItem item) {
        return false;
    }

    public void ensureModelLoaded(CosmeticItem item) {
    }

    public void toggleEquip(CosmeticItem item) {
    }
}
