package ru.fluxvisuals.utils.cosmetics;

import java.nio.file.Path;

public final class CosmeticEntry {
   private final Path folder;
   private final String id;
   private final String displayName;
   private final Kind kind;
   private int previewGlId = -1;
   private int previewWidth;
   private int previewHeight;

   CosmeticEntry(Path folder, String id, String displayName, Kind kind) {
      this.folder = folder;
      this.id = id;
      this.displayName = displayName;
      this.kind = kind;
   }

   public Path folder() { return folder; }
   public String id() { return id; }
   public String displayName() { return displayName; }
   public Kind kind() { return kind; }
   public int previewGlId() { return previewGlId; }
   public int previewWidth() { return previewWidth; }
   public int previewHeight() { return previewHeight; }

   public void setPreviewGlId(int glId) { this.previewGlId = glId; }
   public void setPreviewSize(int w, int h) { this.previewWidth = w; this.previewHeight = h; }

   public static enum Kind {
      MODEL("Models"),
      HEAD("Heads"),
      WEAPON("Weapons");

      private final String tabName;
      Kind(String tabName) { this.tabName = tabName; }
      public String tabName() { return tabName; }
   }
}
