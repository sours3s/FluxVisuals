package ru.fluxvisuals.ui.draggable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.util.render.animation.AnimationSystem;
import ru.fluxvisuals.util.render.animation.Easings;
import ru.fluxvisuals.util.render.animation.SpringConfig;
import ru.fluxvisuals.util.render.animation.SpringFloatAnimator;
import ru.fluxvisuals.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class DraggableManager {
   private static final DraggableManager INSTANCE = new DraggableManager();

   private static final int DRAG_MOUSE_BUTTON = GLFW.GLFW_MOUSE_BUTTON_LEFT;

   private static final float DRAGGING_ALPHA_MULTIPLIER = 0.65F;
   private static final float ALPHA_POSITION_TOLERANCE = 5.0E-4F;
   private static final float ALPHA_VELOCITY_TOLERANCE = 5.0E-4F;
   private static final SpringConfig DRAG_ALPHA_SPRING = SpringConfig.of(2.5F, 0.9F);

   public static final float MIN_SCALE = 0.7F;
   public static final float MAX_SCALE = 1.4F;
   public static final float DEFAULT_SCALE = 1.0F;
   private static final float SCALE_STEP_PER_NOTCH = 0.05F;
   private static final float SCALE_POSITION_TOLERANCE = 1.0E-4F;
   private static final float SCALE_VELOCITY_TOLERANCE = 1.0E-4F;
   private static final SpringConfig SCALE_SPRING = SpringConfig.of(2.6F, 0.85F);

   private final Map<String, DraggableManager.DragState> dragStates = new HashMap<>();
   private final Map<String, DraggableManager.NormalizedPosition> persistedPositions = new ConcurrentHashMap<>();
   private final Map<String, Float> persistedScales = new ConcurrentHashMap<>();
   private final Map<String, DraggableManager.BoundsRecord> lastBounds = new ConcurrentHashMap<>();

   private Renderer2D renderer;
   private boolean frameActive;
   private float cursorX;
   private float cursorY;
   private boolean pointerValid;
   private boolean pointerPressed;
   private boolean pointerJustPressed;
   private boolean prevPointerPressed;
   private boolean chatOpen;
   private boolean ctrlHeld;
   private int viewportWidth;
   private int viewportHeight;
   private String activeDragId;

   private DraggableManager() {
   }

   public static DraggableManager getInstance() {
      return INSTANCE;
   }

   public boolean isEditMode() {
      return this.chatOpen;
   }

   public boolean isCtrlHeld() {
      return this.ctrlHeld;
   }

   public void beginFrame(MinecraftClient client, Renderer2D renderer, int viewportWidth, int viewportHeight) {
      this.renderer = Objects.requireNonNull(renderer, "renderer");
      this.viewportWidth = Math.max(0, viewportWidth);
      this.viewportHeight = Math.max(0, viewportHeight);
      this.frameActive = true;
      this.pointerValid = false;
      boolean wasPressed = this.prevPointerPressed;
      this.pointerPressed = false;
      this.pointerJustPressed = false;
      this.chatOpen = false;
      this.ctrlHeld = false;
      this.cursorX = 0.0F;
      this.cursorY = 0.0F;
      if (client != null) {
         this.chatOpen = client.currentScreen instanceof ChatScreen;
         Window window = client.getWindow();
         if (window != null) {
            long handle = window.getHandle();
            if (handle != 0L) {
               double[] cx = new double[1];
               double[] cy = new double[1];
               GLFW.glfwGetCursorPos(handle, cx, cy);
               double rawX = cx[0];
               double rawY = cy[0];
               if (Double.isFinite(rawX) && Double.isFinite(rawY)) {
                  float fx = (float)rawX;
                  float fy = (float)rawY;
                  if (Float.isFinite(fx) && Float.isFinite(fy)) {
                     this.cursorX = fx;
                     this.cursorY = fy;
                     this.pointerValid = true;
                  }
               }

               if (this.pointerValid && this.chatOpen) {
                  this.pointerPressed = GLFW.glfwGetMouseButton(handle, DRAG_MOUSE_BUTTON) == GLFW.GLFW_PRESS;
                  this.ctrlHeld = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                     || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
               }
            }
         }
      }

      this.pointerJustPressed = this.pointerPressed && !wasPressed;
      this.prevPointerPressed = this.pointerPressed;
      // Release the active drag lock when the mouse is released or chat closed.
      if (!this.pointerPressed || !this.chatOpen) {
         this.activeDragId = null;
      }
   }

   public void endFrame() {
      this.frameActive = false;
      this.renderer = null;
      this.pointerValid = false;
      this.pointerPressed = false;
      this.chatOpen = false;
      this.ctrlHeld = false;
      this.viewportWidth = 0;
      this.viewportHeight = 0;
   }

   public DraggableManager.DragSession beginDrag(String id, float preferredX, float preferredY, float baseWidth, float baseHeight) {
      this.ensureFrame();
      Objects.requireNonNull(id, "id");
      if (Float.isFinite(preferredX)
         && Float.isFinite(preferredY)
         && Float.isFinite(baseWidth)
         && Float.isFinite(baseHeight)
         && !(baseWidth <= 0.0F)
         && !(baseHeight <= 0.0F)
         && this.viewportWidth > 0
         && this.viewportHeight > 0) {

         String persistedId = normalizeId(id);
         DraggableManager.DragState state = this.dragStates.computeIfAbsent(id, key -> new DraggableManager.DragState());

         // Resolve persisted scale and update animator target.
         float persistedScale = persistedId == null ? DEFAULT_SCALE
            : this.persistedScales.getOrDefault(persistedId, DEFAULT_SCALE);
         persistedScale = clamp(persistedScale, MIN_SCALE, MAX_SCALE);
         state.scaleAnimator.setTarget(persistedScale);
         float currentScale = clamp(state.scaleAnimator.getValue(), MIN_SCALE, MAX_SCALE);

         float scaledWidth = baseWidth * currentScale;
         float scaledHeight = baseHeight * currentScale;

         DraggableManager.NormalizedPosition persisted = persistedId == null ? null : this.persistedPositions.get(persistedId);
         boolean wasDragging = state.dragging;
         boolean canInteract = this.pointerValid && this.chatOpen;

         float resolvedPreferredX = this.resolvePreferredX(persisted, preferredX, scaledWidth);
         float resolvedPreferredY = this.resolvePreferredY(persisted, preferredY, scaledHeight);
         if (!state.dragging) {
            state.positionX = resolvedPreferredX;
            state.positionY = resolvedPreferredY;
         }

         if (!this.pointerPressed || !canInteract || this.ctrlHeld) {
            state.dragging = false;
            if (id.equals(this.activeDragId)) {
               this.activeDragId = null;
            }
         } else if (!state.dragging) {
            // Allow grabbing only on the press frame and when no other element is already being dragged.
            boolean canGrab = this.pointerJustPressed && this.activeDragId == null;
            if (canGrab) {
               boolean hovered = containsPoint(this.cursorX, this.cursorY, resolvedPreferredX, resolvedPreferredY, scaledWidth, scaledHeight);
               if (hovered) {
                  state.dragging = true;
                  state.offsetX = this.cursorX - resolvedPreferredX;
                  state.offsetY = this.cursorY - resolvedPreferredY;
                  this.activeDragId = id;
               }
            }
         }

         float maxX = Math.max(0.0F, this.viewportWidth - scaledWidth);
         float maxY = Math.max(0.0F, this.viewportHeight - scaledHeight);
         boolean activelyDragging = state.dragging && this.pointerPressed && canInteract && !this.ctrlHeld
            && id.equals(this.activeDragId);
         float positionX;
         float positionY;
         if (activelyDragging) {
            float rawX = this.cursorX - state.offsetX;
            float rawY = this.cursorY - state.offsetY;
            positionX = clamp(rawX, 0.0F, maxX);
            positionY = clamp(rawY, 0.0F, maxY);
            state.positionX = positionX;
            state.positionY = positionY;
         } else {
            float restingX = state.positionX;
            float restingY = state.positionY;
            positionX = clamp(restingX, 0.0F, maxX);
            positionY = clamp(restingY, 0.0F, maxY);
            state.positionX = positionX;
            state.positionY = positionY;
            state.dragging = false;
         }

         if (wasDragging && !activelyDragging) {
            this.storePersistedPosition(persistedId, positionX, positionY);
         }

         // Record bounds for scroll hit-testing across frames.
         if (persistedId != null) {
            this.lastBounds.put(persistedId, new DraggableManager.BoundsRecord(positionX, positionY, scaledWidth, scaledHeight));
         }

         // Hover detection for visual highlight (e.g. while ctrl held).
         boolean hoveredForScale = canInteract && this.ctrlHeld
            && containsPoint(this.cursorX, this.cursorY, positionX, positionY, scaledWidth, scaledHeight);

         // Alpha animation.
         float targetAlpha = activelyDragging ? DRAGGING_ALPHA_MULTIPLIER : 1.0F;
         state.alphaAnimator.setTarget(targetAlpha);
         float animatedAlpha = clamp01(state.alphaAnimator.getValue());
         boolean alphaAdjusted = false;
         if (shouldApplyAnimatedAlpha(state, animatedAlpha, targetAlpha, activelyDragging)) {
            this.renderer.pushAlpha(animatedAlpha);
            alphaAdjusted = true;
         }

         // Apply scale around top-left corner so position stays anchored.
         boolean scaleAdjusted = false;
         if (Math.abs(currentScale - 1.0F) > 1.0E-4F) {
            this.renderer.pushScale(currentScale, currentScale, positionX, positionY);
            scaleAdjusted = true;
         }

         return new DraggableManager.DragSession(
            id, positionX, positionY, baseWidth, baseHeight, currentScale,
            activelyDragging, hoveredForScale, alphaAdjusted, scaleAdjusted
         );
      } else {
         this.disposeDragState(this.dragStates.remove(id));
         return new DraggableManager.DragSession(id, preferredX, preferredY, baseWidth, baseHeight, DEFAULT_SCALE, false, false, false, false);
      }
   }

   public void endDrag(DraggableManager.DragSession session) {
      if (session != null) {
         if (session.scaleAdjusted() && this.renderer != null) {
            this.renderer.popScale();
         }
         if (session.alphaAdjusted() && this.renderer != null) {
            this.renderer.popAlpha();
         }

         if (!session.dragging()) {
            DraggableManager.DragState state = this.dragStates.get(session.id());
            if (state == null) {
               this.dragStates.remove(session.id());
               return;
            }

            if (this.canDisposeState(state)) {
               this.disposeDragState(this.dragStates.remove(session.id()));
            }
         }
      }
   }

   /**
    * Handle a scroll event; returns true if the scroll was consumed by a HUD scale change.
    */
   public boolean handleScroll(double cursorX, double cursorY, double verticalDelta, boolean ctrlHeld) {
      if (!ctrlHeld || verticalDelta == 0.0 || this.lastBounds.isEmpty()) {
         return false;
      }
      if (!Double.isFinite(cursorX) || !Double.isFinite(cursorY)) {
         return false;
      }

      String hitId = null;
      float bestArea = Float.MAX_VALUE;
      for (Entry<String, DraggableManager.BoundsRecord> entry : this.lastBounds.entrySet()) {
         DraggableManager.BoundsRecord b = entry.getValue();
         if (b == null) continue;
         if (cursorX >= b.x && cursorX <= b.x + b.width && cursorY >= b.y && cursorY <= b.y + b.height) {
            float area = b.width * b.height;
            if (area < bestArea) {
               bestArea = area;
               hitId = entry.getKey();
            }
         }
      }
      if (hitId == null) {
         return false;
      }

      float current = this.persistedScales.getOrDefault(hitId, DEFAULT_SCALE);
      float delta = (float)(verticalDelta) * SCALE_STEP_PER_NOTCH;
      float updated = clamp(current + delta, MIN_SCALE, MAX_SCALE);
      if (Math.abs(updated - current) < 1.0E-4F) {
         return true;
      }
      this.persistedScales.put(hitId, updated);
      DraggableManager.DragState state = this.dragStates.get(hitId);
      if (state != null) {
         state.scaleAnimator.setTarget(updated);
      }
      try {
         if (FluxVisualsClient.get != null && FluxVisualsClient.get.configManager != null) {
            FluxVisualsClient.get.configManager.autoSave();
         }
      } catch (Exception ignored) {
      }
      return true;
   }

   public void loadNormalizedPositions(Map<String, DraggableManager.NormalizedPosition> positions) {
      this.disposeAllDragStates();
      this.dragStates.clear();
      this.persistedPositions.clear();
      if (positions != null && !positions.isEmpty()) {
         for (Entry<String, DraggableManager.NormalizedPosition> entry : positions.entrySet()) {
            String id = normalizeId(entry.getKey());
            DraggableManager.NormalizedPosition position = entry.getValue();
            if (id != null && position != null) {
               try {
                  this.persistedPositions.put(id, new DraggableManager.NormalizedPosition(position.x(), position.y()));
               } catch (IllegalArgumentException ignored) {
               }
            }
         }
      }
   }

   public Map<String, DraggableManager.NormalizedPosition> snapshotNormalizedPositions() {
      if (this.persistedPositions.isEmpty()) {
         return Map.of();
      } else {
         List<String> keys = new ArrayList<>(this.persistedPositions.keySet());
         keys.sort(String.CASE_INSENSITIVE_ORDER);
         Map<String, DraggableManager.NormalizedPosition> snapshot = new LinkedHashMap<>();

         for (String key : keys) {
            DraggableManager.NormalizedPosition position = this.persistedPositions.get(key);
            if (position != null) {
               snapshot.put(key, position);
            }
         }

         return Collections.unmodifiableMap(snapshot);
      }
   }

   public void loadScales(Map<String, Float> scales) {
      this.persistedScales.clear();
      if (scales != null) {
         for (Entry<String, Float> entry : scales.entrySet()) {
            String id = normalizeId(entry.getKey());
            Float value = entry.getValue();
            if (id != null && value != null && Float.isFinite(value)) {
               this.persistedScales.put(id, clamp(value, MIN_SCALE, MAX_SCALE));
            }
         }
      }
   }

   public Map<String, Float> snapshotScales() {
      if (this.persistedScales.isEmpty()) {
         return Map.of();
      }
      List<String> keys = new ArrayList<>(this.persistedScales.keySet());
      keys.sort(String.CASE_INSENSITIVE_ORDER);
      Map<String, Float> snapshot = new LinkedHashMap<>();
      for (String key : keys) {
         Float value = this.persistedScales.get(key);
         if (value != null) {
            snapshot.put(key, value);
         }
      }
      return Collections.unmodifiableMap(snapshot);
   }

   private void ensureFrame() {
      if (!this.frameActive || this.renderer == null) {
         throw new IllegalStateException("beginFrame must be called before beginDrag");
      }
   }

   private float resolvePreferredX(DraggableManager.NormalizedPosition persisted, float fallback, float width) {
      if (persisted != null && this.viewportWidth > 0) {
         float max = Math.max(0.0F, this.viewportWidth - width);
         return clamp(persisted.x() * this.viewportWidth, 0.0F, max);
      } else {
         return fallback;
      }
   }

   private float resolvePreferredY(DraggableManager.NormalizedPosition persisted, float fallback, float height) {
      if (persisted != null && this.viewportHeight > 0) {
         float max = Math.max(0.0F, this.viewportHeight - height);
         return clamp(persisted.y() * this.viewportHeight, 0.0F, max);
      } else {
         return fallback;
      }
   }

   private void storePersistedPosition(String normalizedId, float x, float y) {
      if (this.viewportWidth > 0 && this.viewportHeight > 0) {
         if (normalizedId != null) {
            float normalizedX = x / this.viewportWidth;
            float normalizedY = y / this.viewportHeight;
            if (Float.isFinite(normalizedX) && Float.isFinite(normalizedY)) {
               DraggableManager.NormalizedPosition updated;
               try {
                  updated = new DraggableManager.NormalizedPosition(normalizedX, normalizedY);
               } catch (IllegalArgumentException var10) {
                  return;
               }

               DraggableManager.NormalizedPosition previous = this.persistedPositions.get(normalizedId);
               if (!approximatelyEquals(previous, updated)) {
                  this.persistedPositions.put(normalizedId, updated);

                  try {
                     if (FluxVisualsClient.get != null && FluxVisualsClient.get.configManager != null) {
                        FluxVisualsClient.get.configManager.autoSave();
                     }
                  } catch (Exception ignored) {
                  }
               }
            }
         }
      }
   }

   private static boolean approximatelyEquals(DraggableManager.NormalizedPosition first, DraggableManager.NormalizedPosition second) {
      if (first == second) {
         return true;
      } else {
         return first != null && second != null && Math.abs(first.x() - second.x()) <= 5.0E-4F && Math.abs(first.y() - second.y()) <= 5.0E-4F;
      }
   }

   private static boolean approximatelyEquals(float first, float second, float tolerance) {
      return Math.abs(first - second) <= tolerance;
   }

   private static SpringFloatAnimator createAlphaAnimator() {
      SpringFloatAnimator animator = new SpringFloatAnimator(
         AnimationSystem.getInstance(), DRAG_ALPHA_SPRING, 1.0F, 0.0F, 1.0F, ALPHA_POSITION_TOLERANCE, ALPHA_VELOCITY_TOLERANCE
      );
      animator.setOutputTransform(Easings.SMOOTH_STEP);
      return animator;
   }

   private static SpringFloatAnimator createScaleAnimator() {
      return new SpringFloatAnimator(
         AnimationSystem.getInstance(), SCALE_SPRING, DEFAULT_SCALE, MIN_SCALE, MAX_SCALE, SCALE_POSITION_TOLERANCE, SCALE_VELOCITY_TOLERANCE
      );
   }

   private static boolean shouldApplyAnimatedAlpha(DraggableManager.DragState state, float animatedAlpha, float targetAlpha, boolean activelyDragging) {
      if (state == null) {
         return false;
      } else if (activelyDragging) {
         return true;
      } else if (animatedAlpha < 0.999F) {
         return true;
      } else {
         return targetAlpha < 0.999F || !state.alphaAnimator.isSettled();
      }
   }

   private boolean canDisposeState(DraggableManager.DragState state) {
      if (state == null) {
         return true;
      }
      float alphaTarget = state.alphaAnimator.getTarget();
      float alphaRaw = state.alphaAnimator.getRawValue();
      boolean alphaResting = approximatelyEquals(alphaTarget, 1.0F, ALPHA_POSITION_TOLERANCE)
         && approximatelyEquals(alphaRaw, 1.0F, ALPHA_POSITION_TOLERANCE)
         && state.alphaAnimator.isSettled();
      boolean scaleResting = approximatelyEquals(state.scaleAnimator.getTarget(), DEFAULT_SCALE, SCALE_POSITION_TOLERANCE)
         && approximatelyEquals(state.scaleAnimator.getRawValue(), DEFAULT_SCALE, SCALE_POSITION_TOLERANCE)
         && state.scaleAnimator.isSettled();
      return alphaResting && scaleResting;
   }

   private void disposeAllDragStates() {
      if (!this.dragStates.isEmpty()) {
         for (DraggableManager.DragState state : this.dragStates.values()) {
            this.disposeDragState(state);
         }
      }
   }

   private void disposeDragState(DraggableManager.DragState state) {
      if (state != null) {
         state.alphaAnimator.snapTo(1.0F);
         state.scaleAnimator.snapTo(DEFAULT_SCALE);
      }
   }

   private static boolean containsPoint(float px, float py, float x, float y, float width, float height) {
      return px >= x && px <= x + width && py >= y && py <= y + height;
   }

   private static float clamp(float value, float min, float max) {
      if (value < min) {
         return min;
      }
      return value > max ? max : value;
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      }
      return value > 1.0F ? 1.0F : value;
   }

   private static String normalizeId(String id) {
      if (id == null) {
         return null;
      }
      String trimmed = id.trim();
      return trimmed.isEmpty() ? null : trimmed;
   }

   @Environment(EnvType.CLIENT)
   public static final class DragSession {
      private final String id;
      private final float positionX;
      private final float positionY;
      private final float width;
      private final float height;
      private final float scale;
      private final boolean dragging;
      private final boolean hoveredForScale;
      private final boolean alphaAdjusted;
      private final boolean scaleAdjusted;

      private DragSession(String id, float positionX, float positionY, float width, float height, float scale,
                          boolean dragging, boolean hoveredForScale, boolean alphaAdjusted, boolean scaleAdjusted) {
         this.id = id;
         this.positionX = positionX;
         this.positionY = positionY;
         this.width = width;
         this.height = height;
         this.scale = scale;
         this.dragging = dragging;
         this.hoveredForScale = hoveredForScale;
         this.alphaAdjusted = alphaAdjusted;
         this.scaleAdjusted = scaleAdjusted;
      }

      public String id() {
         return this.id;
      }

      public float positionX() {
         return this.positionX;
      }

      public float positionY() {
         return this.positionY;
      }

      public float width() {
         return this.width;
      }

      public float height() {
         return this.height;
      }

      public float scale() {
         return this.scale;
      }

      public float scaledWidth() {
         return this.width * this.scale;
      }

      public float scaledHeight() {
         return this.height * this.scale;
      }

      public boolean dragging() {
         return this.dragging;
      }

      public boolean hoveredForScale() {
         return this.hoveredForScale;
      }

      public boolean alphaAdjusted() {
         return this.alphaAdjusted;
      }

      public boolean scaleAdjusted() {
         return this.scaleAdjusted;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class DragState {
      private boolean dragging;
      private float offsetX;
      private float offsetY;
      private float positionX;
      private float positionY;
      private final SpringFloatAnimator alphaAnimator = DraggableManager.createAlphaAnimator();
      private final SpringFloatAnimator scaleAnimator = DraggableManager.createScaleAnimator();
   }

   @Environment(EnvType.CLIENT)
   public static final class NormalizedPosition {
      private final float x;
      private final float y;

      public NormalizedPosition(float x, float y) {
         if (Float.isFinite(x) && Float.isFinite(y)) {
            this.x = DraggableManager.clamp01(x);
            this.y = DraggableManager.clamp01(y);
         } else {
            throw new IllegalArgumentException("Normalized coordinates must be finite");
         }
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class BoundsRecord {
      final float x;
      final float y;
      final float width;
      final float height;

      BoundsRecord(float x, float y, float width, float height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }
   }
}
