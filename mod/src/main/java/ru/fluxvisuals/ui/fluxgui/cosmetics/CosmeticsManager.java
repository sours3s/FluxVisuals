package ru.fluxvisuals.ui.fluxgui.cosmetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.fluxvisuals.utils.cosmetics.CosmeticEntry;
import ru.fluxvisuals.utils.cosmetics.CosmeticsRepository;
import ru.fluxvisuals.utils.cosmetics.CosmeticBridge;

/** Хранилище косметики GUI FluxVisuals на базе реальных аватаров Figura. */
public final class CosmeticsManager {
   public static final CosmeticsManager INSTANCE = new CosmeticsManager();

   private static final List<CosmeticItem> ITEMS = new ArrayList<>();
   private static boolean loaded;

   private CosmeticsManager() {
   }

   private static void ensureLoaded() {
      if (loaded) {
         return;
      }
      loaded = true;
      for (CosmeticEntry entry : CosmeticsRepository.all()) {
         ITEMS.add(new CosmeticItem(entry.displayName(), categoryOf(entry), entry));
      }
   }

   private static String categoryOf(CosmeticEntry entry) {
      String folder = entry.folder().getFileName().toString().toLowerCase(Locale.ROOT);
      if (folder.contains("wing") || folder.contains("halo")) {
         return "wings";
      }
      return switch (entry.kind()) {
         case HEAD -> "hats";
         case WEAPON -> "weapons";
         default -> "models";
      };
   }

   public List<CosmeticItem> getAllCosmetics() {
      ensureLoaded();
      return ITEMS;
   }

   public List<CosmeticItem> getByCategory(String category) {
      ensureLoaded();
      if (category == null || category.equals("all")) {
         return getAllCosmetics();
      }
      List<CosmeticItem> result = new ArrayList<>();
      for (CosmeticItem item : ITEMS) {
         if (category.equals(item.getCategory())) {
            result.add(item);
         }
      }
      return result;
   }

   public boolean isEquipped(CosmeticItem item) {
      return item != null && CosmeticBridge.isApplied(item.entry());
   }

   public void ensureModelLoaded(CosmeticItem item) {
      if (item != null) {
         item.getPreviewId();
      }
   }

   public void toggleEquip(CosmeticItem item) {
      if (item == null) {
         return;
      }
      if (isEquipped(item)) {
         CosmeticBridge.clear();
      } else {
         CosmeticBridge.apply(item.entry());
      }
   }
}
