package ru.fluxvisuals.utils.cosmetics;

import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class CosmeticEntry {
   private final Path folder;
   private final String id;
   private final String displayName;
   private final CosmeticEntry.Kind kind;
   private int previewGlId = -1;

   CosmeticEntry(Path folder, String id, String displayName, CosmeticEntry.Kind kind) {
      this.folder = folder;
      this.id = id;
      this.displayName = displayName;
      this.kind = kind;
   }

   public Path folder() {
      return this.folder;
   }

   public String id() {
      return this.id;
   }

   public String displayName() {
      return this.displayName;
   }

   public CosmeticEntry.Kind kind() {
      return this.kind;
   }

   public int previewGlId() {
      return this.previewGlId;
   }

   public void setPreviewGlId(int glId) {
      this.previewGlId = glId;
   }

   @Environment(EnvType.CLIENT)
   public static enum Kind {
      MODEL("Models"),
      HEAD("Head"),
      WEAPON("Weapons");

      private final String tabName;

      private Kind(String tabName) {
         this.tabName = tabName;
      }

      public String tabName() {
         return this.tabName;
      }
   }
}
