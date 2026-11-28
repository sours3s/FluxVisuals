package ru.fluxvisuals.render.nametags;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.render.EventRender3D;
import ru.fluxvisuals.module.impl.visuals.NameTags;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.ScreenBlur;
import ru.fluxvisuals.util.render.capture.EntityFramebufferCaptureManager;

/** World-space, depth-tested renderer for the ported NameTags shader stack. */
@Environment(EnvType.CLIENT)
public final class NameTagRenderer {
    private static final Comparator<PlayerEntity> DISTANCE_DESC =
          Comparator.comparingDouble((PlayerEntity player) -> player.squaredDistanceTo(MinecraftClient.getInstance().player)).reversed();

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    // Слоты 36..39: ботинки, ноги, грудь, шлем; отрисовка идёт от 39 к 36.
    private static final EquipmentSlot[] ARMOR_SLOTS = {
          EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    private static final Random RANDOM = Random.create();

    private static final float TEXT_SIZE = 14.0F;
    private static final float COUNT_SIZE = 8.0F;
    private static final float TAG_HEIGHT = 32.0F;
    private static final float TAG_Y = -16.0F;
    private static final float ICON_SIZE = 16.0F;
    private static final float ICON_GAP = 6.0F;
    private static final float PADDING = 6.0F;
    private static final float FRIEND_ICON_WIDTH = 14.0F;
    private static final float FRIEND_ICON_HEIGHT = 13.0F;
    private static final float BG_RADIUS = 8.0F;
    private static final float HP_BAR_HEIGHT = 20.0F;
    private static final float HP_BAR_RADIUS = 4.0F;

    // ThemePalette.INSTANCE: foreground=getValue7, background=getValue, border=getValue4,
    // elevated=getValue3, success=getValue8, caution=getValue10, danger=getValue11.
    private static final int FOREGROUND = -16119286;
    private static final int BACKGROUND = -1;
    private static final int ELEVATED = -657931;
    private static final int SUCCESS = -7934036;
    private static final int CAUTION = -148876;
    private static final int DANGER = -219739;

    private static final NameTagRenderer INSTANCE = new NameTagRenderer();

    private final NameTagData data = new NameTagData();
    private final Map<UUID, Boolean> visibleCache = new HashMap<>();
    private final Map<UUID, NameTagAnimations> animations = new HashMap<>();
    private final Map<String, Sprite> spriteCache = new HashMap<>();
    private final Map<String, GpuTextureView> textureViewCache = new HashMap<>();
    private final List<PlayerEntity> players = new ArrayList<>();
    private final Set<UUID> present = new HashSet<>();

    private Map<String, NameTagFont> fonts;
    private Map<String, NameTagSvg> icons;
    private GpuBuffer uniformBuffer;
    private long lastTime = System.currentTimeMillis();
    private long lastVisibleTick = -1L;
    private boolean initialized;
    private boolean failed;

    private NameTagRenderer() {
    }

    public static boolean hasFailed() {
        return INSTANCE.failed;
    }

    public static void clear() {
        INSTANCE.visibleCache.clear();
        INSTANCE.animations.clear();
        INSTANCE.spriteCache.clear();
        INSTANCE.textureViewCache.clear();
        INSTANCE.players.clear();
        INSTANCE.present.clear();
        INSTANCE.lastVisibleTick = -1L;
    }

    public static void render(NameTags module, EventRender3D event) {
        if (INSTANCE.failed || EntityFramebufferCaptureManager.getInstance().isExecutingCapturePass()) {
            return;
        }
        try {
            INSTANCE.renderInternal(module, event);
        } catch (Throwable throwable) {
            INSTANCE.failed = true;
            System.err.println("[FluxVisuals] NameTags shader render failed: " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    private void renderInternal(NameTags module, EventRender3D event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.gameRenderer == null) {
            return;
        }
        if (!initialize()) {
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        long now = System.currentTimeMillis();
        float delta = Math.min((now - lastTime) / 1000.0F, 0.1F);
        lastTime = now;

        collectPlayers(module, cameraPos);
        if (this.players.isEmpty()) {
            return;
        }

        Framebuffer framebuffer = mc.getFramebuffer();
        if (framebuffer == null || RenderSystem.tryGetDevice() == null) {
            return;
        }
        GpuTextureView colorView = RenderSystem.outputColorTextureOverride != null
              ? RenderSystem.outputColorTextureOverride
              : framebuffer.getColorAttachmentView();
        if (colorView == null) {
            return;
        }
        GpuTextureView depthView = framebuffer.useDepthAttachment
              ? (RenderSystem.outputDepthTextureOverride != null
                    ? RenderSystem.outputDepthTextureOverride
                    : framebuffer.getDepthAttachmentView())
              : null;

        Quaternionf rotation = camera.getRotation();
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(rotation);
        // Отрицательный масштаб по осям даёт ось Y вниз, как в матрице ванильного лейбла.
        Vector3f down = new Vector3f(0.0F, -1.0F, 0.0F).rotate(rotation);
        Matrix4f view = new Matrix4f(event.getMatrixStack().peek().getPositionMatrix());

        ensureUniformBuffer();
        float backgroundAlpha = module.background.get() ? module.opacity.get() / 100.0F : 0.0F;
        GpuTextureView blurred = null;
        if (backgroundAlpha > 0.001F) {
            // Kawase view has one mip level; refresh it every frame for the glass backdrop.
            ScreenBlur.capture(1);
            blurred = ScreenBlur.getBlurredView();
        }

        for (PlayerEntity player : this.players) {
            double px = MathHelper.lerp(event.getTickDelta(), player.lastRenderX, player.getX());
            double py = MathHelper.lerp(event.getTickDelta(), player.lastRenderY, player.getY());
            double pz = MathHelper.lerp(event.getTickDelta(), player.lastRenderZ, player.getZ());
            Vec3d anchor = new Vec3d(px, py + player.getHeight() + 0.5, pz);
            double distance = cameraPos.distanceTo(anchor);
            float scale = MathHelper.clamp((float) distance * 0.0025F, 0.02F, 0.1F) * module.size.get();

            UUID uuid = player.getUuid();
            NameTagAnimations state = this.animations.computeIfAbsent(uuid, key -> new NameTagAnimations());
            state.update(delta, scale, module.simplifyFar.get(), distance, module.simplifyRange.get(),
                  module.renderParts.isSelected("Левая рука"),
                  module.renderParts.isSelected("Правая рука"),
                  module.renderParts.isSelected("Броня"),
                  !module.hideEmptySlots.get() || !player.getOffHandStack().isEmpty(),
                  !module.hideEmptySlots.get() || !player.getMainHandStack().isEmpty(),
                  armorVisible(module, player));

            renderTag(module, player, uuid, anchor, view, right, down, state, backgroundAlpha,
                  colorView, depthView, blurred);
        }

        this.present.clear();
        for (PlayerEntity player : this.players) {
            this.present.add(player.getUuid());
        }
        this.animations.keySet().retainAll(this.present);
    }

    private void collectPlayers(NameTags module, Vec3d cameraPos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        this.players.clear();
        if (mc.world.getTime() != lastVisibleTick) {
            lastVisibleTick = mc.world.getTime();
            visibleCache.clear();
        }
        double maxDistanceSq = module.range.get() * (double) module.range.get();
        boolean friends = module.targets.get("Друзья");
        boolean enemies = module.targets.get("Враги");
        boolean armoredEnemies = module.targets.get("Враги только в броне");

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            double dx = player.getX() - cameraPos.x;
            double dy = player.getY() - cameraPos.y;
            double dz = player.getZ() - cameraPos.z;
            if (dx * dx + dy * dy + dz * dz > maxDistanceSq) continue;

            boolean friend = isFriend(player);
            if (friend ? !friends : !enemies || armoredEnemies && !hasArmor(player)) continue;
            Boolean visible = visibleCache.get(player.getUuid());
            if (visible == null) {
                visible = mc.player.canSee(player);
                visibleCache.put(player.getUuid(), visible);
            }
            if (!visible) continue;
            this.players.add(player);
        }
        this.players.sort(DISTANCE_DESC);
    }

    private void renderTag(NameTags module, PlayerEntity player, UUID uuid, Vec3d anchor,
                           Matrix4f view, Vector3f right, Vector3f down, NameTagAnimations state,
                           float backgroundAlpha, GpuTextureView colorView, GpuTextureView depthView,
                           GpuTextureView blurred) {
        String name = player.getName().getString();
        boolean friend = isFriend(player);
        String prefix = hasPrefix(player) ? prefixOf(player, name) : "";
        String hpText = hpText(player, module);
        int nameColor = friend ? SUCCESS : FOREGROUND;
        // Тексты измеряются атласом "a", а рисуются префикс/имя/HP атласом "c".
        NameTagFont measureFont = fonts.get("a");
        NameTagFont textFont = fonts.get("c");
        NameTagFont countFont = fonts.get("cc");

        boolean colorHp = module.renderParts.isSelected("Цветное HP");
        Metrics m = new Metrics(state, prefix, name, hpText, measureFont,
              player.getHealth(), player.getMaxHealth(), friend, colorHp);

        if (backgroundAlpha > 0.001F) {
            int backgroundFill = scaleAlpha(BACKGROUND, backgroundAlpha);
            if (blurred != null) {
                drawGlassRect(anchor, view, right, down, m.scale, m.baseX, TAG_Y, m.width, TAG_HEIGHT,
                      BG_RADIUS, blurred, colorView, depthView);
            } else {
                drawRect(anchor, view, right, down, m.scale, m.baseX, TAG_Y, m.width, TAG_HEIGHT,
                      BG_RADIUS, backgroundFill, 0, 0, 0.0F, colorView, depthView);
            }
        }

        float cursor = m.baseX + PADDING;
        ItemStack offHand = player.getOffHandStack();
        float offHandAlpha = m.leftHand * m.offHand;
        if (offHandAlpha > 0.001F) {
            drawItem(anchor, view, right, down, m.scale, offHand, cursor, -8.0F, ICON_SIZE,
                  offHandAlpha, module.itemCount.get(), module.hideEmptySlots.get(), nameColor,
                  countFont, colorView, depthView);
        }
        cursor += m.offHandSpan;

        if (friend) {
            drawIcon(anchor, view, right, down, m.scale, "friends", cursor, -6.5F,
                  FRIEND_ICON_WIDTH, FRIEND_ICON_HEIGHT, SUCCESS, 1.0F, colorView, depthView);
            cursor += 21.0F;
        }

        float textY = -7.0F;
        if (!prefix.isEmpty()) {
            drawText(anchor, view, right, down, m.scale, textFont, prefix, cursor, textY,
                  TEXT_SIZE, nameColor, NameTagPipelines.TEXT, 0.0F, colorView, depthView);
            cursor += m.prefixWidth + ICON_GAP;
        }
        drawText(anchor, view, right, down, m.scale, textFont, name, cursor, textY, TEXT_SIZE,
              nameColor, NameTagPipelines.TEXT, 0.0F, colorView, depthView);
        cursor += m.nameWidth + ICON_GAP;

        drawText(anchor, view, right, down, m.scale, textFont, hpText, cursor + 4.0F, textY, TEXT_SIZE,
              m.hpTextColor, NameTagPipelines.TEXT, 0.0F, colorView, depthView);

        if (backgroundAlpha > 0.001F) {
            drawRect(anchor, view, right, down, m.scale, m.hpBarX, -10.0F,
                  m.hpWidth + 8.0F, HP_BAR_HEIGHT, HP_BAR_RADIUS, m.hpBarColor, 0, 0, 0.0F,
                  colorView, depthView);
        }
        cursor += m.hpWidth + 8.0F;

        if (m.armor > 0.001F) {
            float slotCursor = cursor + ICON_GAP * m.armor * m.maxSlot;
            for (int i = ARMOR_SLOTS.length - 1; i >= 0; i--) {
                float progress = m.slotProgress[i] * m.armor;
                if (progress > 0.001F) {
                    ItemStack stack = player.getEquippedStack(ARMOR_SLOTS[i]);
                    drawItem(anchor, view, right, down, m.scale, stack, slotCursor, -8.0F, ICON_SIZE,
                          progress, module.itemCount.get(), module.hideEmptySlots.get(), nameColor,
                          countFont, colorView, depthView);
                }
                slotCursor += ICON_SIZE * progress;
                if (i > 0) {
                    slotCursor += ICON_GAP * Math.max(m.slotProgress[i], m.slotProgress[i - 1]) * m.armor;
                }
            }
        }

        ItemStack mainHand = player.getMainHandStack();
        float mainHandAlpha = m.rightHand * m.mainHand;
        if (mainHandAlpha > 0.001F) {
            float mainX = cursor + m.armorSpan + ICON_GAP * mainHandAlpha;
            drawItem(anchor, view, right, down, m.scale, mainHand, mainX, -8.0F, ICON_SIZE,
                  mainHandAlpha, module.itemCount.get(), module.hideEmptySlots.get(), nameColor,
                  countFont, colorView, depthView);
        }
    }

    private final class Metrics {
        final float scale;
        final float leftHand;
        final float rightHand;
        final float armor;
        final float offHand;
        final float mainHand;
        final float[] slotProgress;
        final float maxSlot;
        final String prefix;
        final String name;
        final String hpText;
        final float prefixWidth;
        final float nameWidth;
        final float hpWidth;
        final float offHandSpan;
        final float armorSpan;
        final float width;
        final float baseX;
        final float hpBarX;
        final int hpTextColor;
        final int hpBarColor;

        Metrics(NameTagAnimations state, String prefix, String name, String hpText,
                NameTagFont measureFont,
                float health, float maxHealth, boolean friend, boolean colorHp) {
            this.scale = state.scale();
            this.leftHand = state.leftHand();
            this.rightHand = state.rightHand();
            this.armor = state.armor();
            this.offHand = state.offHand();
            this.mainHand = state.mainHand();
            this.slotProgress = new float[4];
            float sum = 0.0F;
            float max = 0.0F;
            for (int i = 0; i < 4; i++) {
                this.slotProgress[i] = state.armorSlot(i);
                sum += this.slotProgress[i];
                max = Math.max(max, this.slotProgress[i]);
            }
            this.maxSlot = max;
            this.prefix = prefix;
            this.name = name;
            this.hpText = hpText;
            this.prefixWidth = prefix.isEmpty() ? 0.0F : measureFont.width(prefix, TEXT_SIZE);
            this.nameWidth = measureFont.width(name, TEXT_SIZE);
            this.hpWidth = measureFont.width(hpText, TEXT_SIZE);

            float prefixSpan = prefix.isEmpty() ? 0.0F : this.prefixWidth + ICON_GAP;
            float offHandSpan = (ICON_SIZE + ICON_GAP) * this.offHand * this.leftHand;
            float mainHandSpan = (ICON_GAP + ICON_SIZE) * this.mainHand * this.rightHand;
            float armorSlotsWidth = ICON_SIZE * sum;
            for (int i = 0; i < 3; i++) {
                armorSlotsWidth += ICON_GAP * Math.max(this.slotProgress[i], this.slotProgress[i + 1]);
            }
            float armorSpan = (ICON_GAP * max + armorSlotsWidth) * this.armor;
            this.offHandSpan = offHandSpan;
            this.armorSpan = armorSpan;

            float friendSpan = friend ? 21.0F : 0.0F;
            this.width = PADDING * 2.0F + offHandSpan + friendSpan + prefixSpan
                  + this.nameWidth + ICON_GAP + this.hpWidth + 8.0F + armorSpan + mainHandSpan;
            this.baseX = -this.width / 2.0F;
            this.hpBarX = this.baseX + PADDING + offHandSpan + friendSpan + prefixSpan
                  + this.nameWidth + ICON_GAP;

            int healthColor;
            if (friend) {
                healthColor = SUCCESS;
            } else if (!colorHp || health > 100.0F) {
                healthColor = 0;
            } else {
                healthColor = healthColor(maxHealth, health);
            }
            if (healthColor == 0) {
                this.hpTextColor = FOREGROUND;
                this.hpBarColor = ELEVATED;
            } else {
                this.hpTextColor = tint(1.0F, 0.38F, healthColor);
                this.hpBarColor = tint(0.55F, 0.9F, healthColor);
            }
        }
    }

    private Sprite sprite(ItemStack stack) {
        String key = stack.getItem() + ":" + stack.getComponents().hashCode();
        Sprite cached = this.spriteCache.get(key);
        if (cached != null) return cached;
        try {
            ItemRenderState state = new ItemRenderState();
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.getItemModelManager().clearAndUpdate(state, stack, net.minecraft.item.ItemDisplayContext.GUI,
                  mc.world, null, 0);
            Sprite sprite = state.getParticleSprite(RANDOM);
            if (sprite != null) {
                this.spriteCache.put(key, sprite);
            }
            return sprite;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private GpuTextureView textureView(Identifier id) {
        GpuTextureView cached = this.textureViewCache.get(id.toString());
        if (cached != null) return cached;
        try {
            AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(id);
            GpuTextureView view = texture.getGlTextureView();
            if (view != null) {
                this.textureViewCache.put(id.toString(), view);
            }
            return view;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void drawRect(Vec3d anchor, Matrix4f view, Vector3f right, Vector3f down, float scale,
                          float x, float y, float width, float height, float radius, int fill,
                          int border, int shadow, float borderWidth,
                          GpuTextureView colorView, GpuTextureView depthView) {
        float spread = 1.0F;
        float quadWidth = width + spread * 2.0F;
        float quadHeight = height + spread * 2.0F;
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        quad(buffer, anchor, view, right, down, scale, x - spread, y - spread,
              quadWidth, quadHeight, 0.0F, 1.0F, 0xFFFFFFFF);
        BuiltBuffer built = buffer.endNullable();
        if (built == null) return;

        data.reset();
        data.vec4(NameTagData.RECT_SIZE, width, height, quadWidth, quadHeight);
        data.vec4(NameTagData.RECT_CONTENT, spread, spread, borderWidth, 1.0F);
        data.vec4(NameTagData.CORNER_RADIUS, radius, radius, radius, radius);
        data.rgba(NameTagData.FILL_COLOR, fill);
        data.rgba(NameTagData.BORDER_COLOR, border);
        data.rgba(NameTagData.SHADOW_COLOR, shadow);
        data.vec4(NameTagData.SHADOW_PARAMS, 0.0F, 0.0F, 0.0F, 0.0F);
        draw(NameTagPipelines.RECT, built, colorView, depthView, null, "nametag_rect");
    }

    private void drawGlassRect(Vec3d anchor, Matrix4f view, Vector3f right, Vector3f down, float scale,
                               float x, float y, float width, float height, float radius,
                               GpuTextureView blurred,
                               GpuTextureView colorView, GpuTextureView depthView) {
        float spread = 1.0F;
        float quadWidth = width + spread * 2.0F;
        float quadHeight = height + spread * 2.0F;
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        quad(buffer, anchor, view, right, down, scale, x - spread, y - spread,
              quadWidth, quadHeight, 0.0F, 1.0F, 0xFFFFFFFF);
        BuiltBuffer built = buffer.endNullable();
        if (built == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        data.reset();
        data.vec4(NameTagData.RECT_SIZE, width, height, quadWidth, quadHeight);
        data.vec4(NameTagData.RECT_CONTENT, spread, spread, 0.0F, 1.0F);
        data.vec4(NameTagData.CORNER_RADIUS, radius, radius, radius, radius);
        data.rgba(NameTagData.FILL_COLOR, 0x11000000);
        data.rgba(NameTagData.BORDER_COLOR, 0x00000000);
        data.rgba(NameTagData.GLASS_TINT, 0x66303038);
        data.rgba(NameTagData.GLASS_GLOW_COLOR, 0x00000000);
        // Kawase backdrop is single-level; force LOD 0 to stay mipmap-complete.
        data.vec4(NameTagData.GLASS_PARAMS_0, 0.8F, 0.6F, 0.15F, 0.0F);
        data.vec4(NameTagData.GLASS_PARAMS_1, 1.0F, 0.1F, 0.0F, 0.0F);
        data.vec4(NameTagData.GLASS_PARAMS_2, 0.0F, 0.0F, 0.5F, 0.0F);
        data.vec4(NameTagData.GLASS_PARAMS_3, 0.4F, (float) (System.currentTimeMillis() % 100000L) / 1000.0F, 0.5F, 0.5F);
        data.vec4(NameTagData.RESOLUTION_PARAMS, mc.getWindow().getFramebufferWidth(),
              mc.getWindow().getFramebufferHeight(), 0.0F, 0.0F);
        draw(NameTagPipelines.LIQUIDGLASS, built, colorView, depthView, blurred, "nametag_liquidglass");
    }

    private void drawText(Vec3d anchor, Matrix4f view, Vector3f right, Vector3f down, float scale,
                          NameTagFont font, String text, float x, float y, float size, int color,
                          com.mojang.blaze3d.pipeline.RenderPipeline pipeline, float blur,
                          GpuTextureView colorView, GpuTextureView depthView) {
        if (text == null || text.isEmpty()) return;
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        emitText(buffer, anchor, view, right, down, scale, font, text, x, y, size, color);
        BuiltBuffer built = buffer.endNullable();
        if (built == null) return;

        data.reset();
        data.vec4(NameTagData.FONT_PARAMS, font.atlasWidth(), font.atlasHeight(), font.distanceRange(), 0.02F);
        data.vec4(NameTagData.BLUR_PARAMS, blur, 0.0F, 0.0F, 0.0F);
        draw(pipeline, built, colorView, depthView, font.atlasView(), "nametag_text");
    }

    private void emitText(BufferBuilder buffer, Vec3d anchor, Matrix4f view, Vector3f right,
                          Vector3f down, float scale, NameTagFont font, String text, float x,
                          float y, float size, int color) {
        float penX = x;
        int previous = -1;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            NameTagFont.Glyph glyph = font.glyph(codePoint);
            if (glyph == null) continue;
            Float kerning = font.kerning(previous, codePoint);
            if (kerning != null) {
                penX += kerning * size;
            }
            float topY = y - glyph.top * size;
            float bottomY = y - glyph.bottom * size;
            float rightX = penX + glyph.width * size;
            textVertex(buffer, anchor, view, right, down, scale, penX, topY, glyph.u0, glyph.v0, color);
            textVertex(buffer, anchor, view, right, down, scale, penX, bottomY, glyph.u0, glyph.v1, color);
            textVertex(buffer, anchor, view, right, down, scale, rightX, bottomY, glyph.u1, glyph.v1, color);
            textVertex(buffer, anchor, view, right, down, scale, rightX, topY, glyph.u1, glyph.v0, color);
            penX += glyph.advance * size;
            previous = codePoint;
        }
    }

    private void textVertex(BufferBuilder buffer, Vec3d anchor, Matrix4f view, Vector3f right,
                            Vector3f down, float scale, float x, float y, float u, float v, int color) {
        Vector3f position = worldPosition(anchor, right, down, x * scale, y * scale);
        view.transformPosition(position);
        buffer.vertex(position.x, position.y, position.z).texture(u, v).color(color);
    }

    private void drawIcon(Vec3d anchor, Matrix4f view, Vector3f right, Vector3f down, float scale,
                          String key, float x, float y, float width, float height, int color,
                          float alpha, GpuTextureView colorView, GpuTextureView depthView) {
        NameTagSvg icon = icons.get(key);
        if (icon == null || icon.segmentCount() == 0) return;
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        quad(buffer, anchor, view, right, down, scale, x, y, width, height, 0.0F, 1.0F, 0xFFFFFFFF);
        BuiltBuffer built = buffer.endNullable();
        if (built == null) return;

        data.reset();
        data.vec4(NameTagData.VIEW_BOX, 0.0F, 0.0F, icon.width, icon.height);
        data.vec4(NameTagData.SVG_PARAMS, 0.0F, icon.strokeWidth, icon.filled ? 1.0F : 0.0F, icon.segmentCount());
        data.rgba(NameTagData.SVG_COLOR, color);
        data.vec4(NameTagData.RECT_CONTENT, 0.0F, 0.0F, 0.0F, alpha);
        for (int i = 0; i < icon.segmentCount(); i++) {
            data.segment(i, icon.segments[i * 4], icon.segments[i * 4 + 1],
                  icon.segments[i * 4 + 2], icon.segments[i * 4 + 3]);
        }
        draw(NameTagPipelines.SVG, built, colorView, depthView, null, "nametag_svg");
    }

    private void drawItem(Vec3d anchor, Matrix4f view, Vector3f right, Vector3f down, float scale,
                          ItemStack stack, float x, float y, float size, float alpha,
                          boolean showCount, boolean hideEmpty, int color, NameTagFont countFont,
                          GpuTextureView colorView, GpuTextureView depthView) {
        if (alpha <= 0.001F) return;
        if (stack == null || stack.isEmpty()) {
            // Крестик пустого слота рисуется цветом имени, когда скрытие выключено.
            if (!hideEmpty) {
                drawIcon(anchor, view, right, down, scale, "close", x + 3.5F, y + 3.5F,
                      9.0F, 9.0F, color, alpha, colorView, depthView);
            }
            return;
        }
        Sprite sprite = sprite(stack);
        if (sprite == null) return;
        GpuTextureView atlas = textureView(sprite.getAtlasId());
        if (atlas == null) return;
        drawTexture(anchor, view, right, down, scale, atlas, x, y, size, size,
              sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(),
              withAlpha(FOREGROUND, alpha), colorView, depthView);
        if (showCount && stack.getCount() > 1) {
            String text = Integer.toString(stack.getCount());
            NameTagFont measureFont = fonts.get("aa");
            float width = measureFont != null ? measureFont.width(text, COUNT_SIZE)
                  : countFont.width(text, COUNT_SIZE);
            drawText(anchor, view, right, down, scale, countFont, text, x + size - width, 0.0F,
                  COUNT_SIZE, withAlpha(color, alpha), NameTagPipelines.TEXT, 0.0F,
                  colorView, depthView);
        }
    }

    private void drawTexture(Vec3d anchor, Matrix4f view, Vector3f right, Vector3f down, float scale,
                             GpuTextureView texture, float x, float y, float width, float height,
                             float u0, float v0, float u1, float v1, int color,
                             GpuTextureView colorView, GpuTextureView depthView) {
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        quadUV(buffer, anchor, view, right, down, scale, x, y, width, height, u0, v0, u1, v1, color);
        BuiltBuffer built = buffer.endNullable();
        if (built == null) return;

        data.reset();
        data.vec4(NameTagData.TEXTURE_PARAMS, width, height, 2.0F, 2.0F);
        data.vec4(NameTagData.TEXTURE_SMOOTH, 1.0F, 0.0F, 0.0F, 0.0F);
        data.vec4(NameTagData.RECT_CONTENT, 0.0F, 0.0F, 0.0F, 1.0F);
        draw(NameTagPipelines.TEXTURE, built, colorView, depthView, texture, "nametag_texture");
    }

    private static void quad(BufferBuilder buffer, Vec3d anchor, Matrix4f view, Vector3f right,
                             Vector3f down, float scale, float x, float y, float width, float height,
                             float u0, float v0, float color) {
        quadUV(buffer, anchor, view, right, down, scale, x, y, width, height,
              u0, v0, 1.0F, 1.0F, (int) color);
    }

    private static void quadUV(BufferBuilder buffer, Vec3d anchor, Matrix4f view, Vector3f right,
                               Vector3f down, float scale, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1, int color) {
        positionVertex(buffer, anchor, view, right, down, x * scale, y * scale, u0, v0, color);
        positionVertex(buffer, anchor, view, right, down, x * scale, (y + height) * scale, u0, v1, color);
        positionVertex(buffer, anchor, view, right, down, (x + width) * scale, (y + height) * scale, u1, v1, color);
        positionVertex(buffer, anchor, view, right, down, (x + width) * scale, y * scale, u1, v0, color);
    }

    private static void positionVertex(BufferBuilder buffer, Vec3d anchor, Matrix4f view,
                                       Vector3f right, Vector3f down, float x, float y,
                                       float u, float v, int color) {
        Vector3f position = worldPosition(anchor, right, down, x, y);
        view.transformPosition(position);
        buffer.vertex(position.x, position.y, position.z).texture(u, v).color(color);
    }

    private static Vector3f worldPosition(Vec3d anchor, Vector3f right, Vector3f down, float x, float y) {
        return new Vector3f(
              (float) (anchor.x + right.x * x + down.x * y),
              (float) (anchor.y + right.y * x + down.y * y),
              (float) (anchor.z + right.z * x + down.z * y)
        );
    }

    private void draw(com.mojang.blaze3d.pipeline.RenderPipeline pipeline, BuiltBuffer built,
                      GpuTextureView colorView, GpuTextureView depthView,
                      GpuTextureView samplerView, String passName) {
        GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(built.getBuffer());
        ShapeIndexBuffer indices = RenderSystem.getSequentialBuffer(built.getDrawParameters().comp_752());
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(
              IDENTITY, COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        com.mojang.blaze3d.systems.CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), data.data());

        try (RenderPass pass = encoder.createRenderPass(() -> passName, colorView, OptionalInt.empty(),
              depthView, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setUniform(NameTagData.UNIFORM_NAME, uniformBuffer.slice());
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indices.getIndexBuffer(built.getDrawParameters().comp_751()), indices.getIndexType());
            if (samplerView != null) {
                pass.bindTexture("Sampler0", samplerView,
                      RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            }
            pass.drawIndexed(0, 0, built.getDrawParameters().comp_751(), 1);
        } finally {
            built.close();
        }
    }

    private void ensureUniformBuffer() {
        if (uniformBuffer != null && !uniformBuffer.isClosed()) return;
        uniformBuffer = RenderSystem.getDevice().createBuffer(
              () -> "fluxvisuals:nametag_data",
              GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
              NameTagData.BUFFER_SIZE
        );
    }

    private boolean initialize() {
        if (initialized) return true;
        try {
            Map<String, NameTagFont> loadedFonts = new HashMap<>();
            for (String key : new String[]{"a", "aa", "c", "cc"}) {
                loadedFonts.put(key, NameTagFont.load(key));
            }
            Map<String, NameTagSvg> loadedIcons = new HashMap<>();
            loadedIcons.put("close", NameTagSvg.load("close"));
            loadedIcons.put("friends", NameTagSvg.load("friends"));
            this.fonts = loadedFonts;
            this.icons = loadedIcons;
            this.initialized = true;
            return true;
        } catch (Throwable throwable) {
            failed = true;
            System.err.println("[FluxVisuals] Failed to load NameTags resources: " + throwable.getMessage());
            throwable.printStackTrace();
            return false;
        }
    }

    private static boolean hasPrefix(PlayerEntity player) {
        String display = player.getDisplayName().getString();
        String name = player.getName().getString();
        return !display.equals(name);
    }

    private static String prefixOf(PlayerEntity player, String plainName) {
        String display = player.getDisplayName().getString();
        String prefix = display.replace(plainName, "").trim();
        return prefix.isEmpty() ? display : prefix;
    }

    private static boolean isFriend(PlayerEntity player) {
        return FluxVisualsClient.get != null && FluxVisualsClient.get.friendManager != null
              && FluxVisualsClient.get.friendManager.isFriend(player.getName().getString());
    }

    private static boolean hasArmor(PlayerEntity player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getEquippedStack(slot).isEmpty()) return true;
        }
        return false;
    }

    private static boolean[] armorVisible(NameTags module, PlayerEntity player) {
        boolean[] result = new boolean[4];
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            result[i] = !module.hideEmptySlots.get() || !player.getEquippedStack(ARMOR_SLOTS[i]).isEmpty();
        }
        return result;
    }

    private static int scaleAlpha(int argb, float factor) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * MathHelper.clamp(factor, 0.0F, 1.0F));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    private static int withAlpha(int argb, float factor) {
        return scaleAlpha(argb, factor);
    }

    private static int lerpArgb(int from, int to, float t) {
        float amount = MathHelper.clamp(t, 0.0F, 1.0F);
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * amount);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * amount);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * amount);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int healthColor(float maxHealth, float health) {
        if (maxHealth <= 0.0F) return SUCCESS;
        float ratio = MathHelper.clamp(health / maxHealth, 0.0F, 1.0F);
        if (ratio >= 0.5F) return lerpArgb(CAUTION, SUCCESS, (ratio - 0.5F) / 0.5F);
        return lerpArgb(DANGER, CAUTION, ratio / 0.5F);
    }

    /** Перекраска HP: getIntByFloatFloatInt(saturationScale, lightness, color). */
    private static int tint(float saturationScale, float lightness, int color) {
        float[] hsl = hsl(color);
        return hslToArgb(lightness, hsl[0], hsl[1] * saturationScale);
    }

    private static int hslToArgb(float lightness, float hue, float saturation) {
        float value = MathHelper.clamp(lightness, 0.0F, 1.0F);
        float sat = MathHelper.clamp(saturation, 0.0F, 1.0F);
        if (sat == 0.0F) {
            int gray = Math.round(value * 255.0F);
            return 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        }
        float low = value < 0.5F ? value * (1.0F + sat) : value + sat - value * sat;
        float high = 2.0F * value - low;
        int r = Math.round(hueComponent(high, low, hue + 0.33333334F) * 255.0F);
        int g = Math.round(hueComponent(high, low, hue) * 255.0F);
        int b = Math.round(hueComponent(high, low, hue - 0.33333334F) * 255.0F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static float hueComponent(float a, float b, float hue) {
        if (hue < 0.0F) hue += 1.0F;
        if (hue > 1.0F) hue -= 1.0F;
        if (hue < 0.16666667F) return a + (b - a) * 6.0F * hue;
        if (hue < 0.5F) return b;
        return hue < 0.6666667F ? a + (b - a) * (0.6666667F - hue) * 6.0F : a;
    }

    private static float[] hsl(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float lightness = (max + min) / 2.0F;
        if (max == min) return new float[]{0.0F, 0.0F, lightness};
        float delta = max - min;
        float saturation = lightness > 0.5F ? delta / (2.0F - max - min) : delta / (max + min);
        float hue;
        if (max == r) {
            hue = (g - b) / delta + (g < b ? 6.0F : 0.0F);
        } else if (max == g) {
            hue = (b - r) / delta + 2.0F;
        } else {
            hue = (r - g) / delta + 4.0F;
        }
        return new float[]{hue / 6.0F, saturation, lightness};
    }

    private static String hpText(PlayerEntity player, NameTags module) {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (module.hpMode.is("Сердца")) {
            if (maxHealth > 100.0F) return "?";
            return Math.round(maxHealth / 2.0F) + "/" + Math.round(health / 2.0F);
        }
        if (health > 100.0F) return "?";
        return Math.round(health) + "HP";
    }
}
