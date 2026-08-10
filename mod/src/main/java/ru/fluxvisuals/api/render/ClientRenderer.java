package ru.fluxvisuals.api.render;

import ru.fluxvisuals.api.render.system.IconUse;
import ru.fluxvisuals.api.render.system.ShaderUse;
import ru.fluxvisuals.api.render.system.TextureUse;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import ru.fluxvisuals.api.render.system.sys2d.CRenderSystem;
import ru.fluxvisuals.api.render.system.sys2d.TextureAtlas;
import ru.fluxvisuals.vse.utils.client.client.ClientColors;
import ru.fluxvisuals.vse.utils.client.client.ClientSettings;
import ru.fluxvisuals.vse.utils.math.MathUtility;
import ru.fluxvisuals.vse.utils.math.Rectangle;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.impl.util.StringUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import ru.fluxvisuals.Client;
import ru.fluxvisuals.api.render.msdf.MsdfFont;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static ru.fluxvisuals.MinecraftHolder.mc;
import static ru.fluxvisuals.MinecraftHolder.window;

@Getter
public class ClientRenderer {
    final MatrixStack stack = new MatrixStack();

    public ClientRenderer() {
    }
    ArrayList<Runnable> postTasks = new ArrayList<>();


    public void queueTask(Runnable task) {
        postTasks.add(task);
    }

    public void runTasks() {
        Iterator<Runnable> iterator = postTasks.iterator();
        while (iterator.hasNext()) {
            Runnable item = iterator.next();
            item.run();
            iterator.remove();
        }
    }

    CRenderSystem crenderSystem = new CRenderSystem();
    @Nullable
    @Setter
    DrawContext drawContext;

    public void render() {

        crenderSystem.layerTex(crenderSystem.getAtlas().getGlId()).render(CRenderSystem.RenderLayer.OVERLAY);
    }
    public void prepare() {
        crenderSystem.prepare();
    }

    public void invert_glass(float x, float y, float width, float height, Vector4f round, float smooth) {
        invert_glass(x, y, width, height, round, smooth, CRenderSystem.RenderLayer.BLUR);
    }

    public void invert_glass(float x, float y, float width, float height, Vector4f round, float smooth, CRenderSystem.RenderLayer layer) {
        CRenderSystem.RenderLayer prev = crenderSystem.layer();
        crenderSystem.layer(layer);
        crenderSystem.layerTex(((GlTextureView) mc.getFramebuffer().getColorAttachmentView()).texture().getGlId());

        crenderSystem.shader(ShaderUse.INVERT_GLASS)
                .uv(x / window.getScaledWidth(), 1 - (y / window.getScaledHeight()), (width / window.getScaledWidth()), -(height / window.getScaledHeight()))
                .rect(x, y, width, height)
                .color(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE)
                .round(round.x, round.y, round.z, round.w)
                .smoothness(smooth, smooth)
                .build();

        crenderSystem.layer(prev);
    }

    public void rect(float x, float y, float width, float height, Vector4f round, float smooth, Color topLeft, Color bottomLeft, Color bottomRight, Color topRight) {
        crenderSystem.shader(ShaderUse.RECTANGLE)
                .rect(x, y, width, height)
                .round(round.x, round.y, round.z, round.w)
                .smoothness(smooth, smooth)
                .color(topLeft, bottomLeft, bottomRight, topRight)
                .build();
    }
    public void rect(Rectangle r, Vector4f round, float smooth, Color color1, Color color2, Color color3, Color color4) {
        crenderSystem.shader(ShaderUse.RECTANGLE)
                .rect(r.getX(), r.getY(), r.getWidth(), r.getHeight())
                .round(round.x, round.y, round.z, round.w)
                .smoothness(smooth, smooth)
                .color(color1, color2, color3, color4)
                .build();
    }

    public void glow(float x, float y, float width, float height, Vector4f round, float smooth, Color color1, Color color2, Color color3, Color color4) {
        crenderSystem.shader(ShaderUse.GLOW)
                .rect(x, y, width, height)
                .round(round.x, round.y, round.z, round.w)
                .smoothness(smooth, smooth)
                .color(color1, color2, color3, color4)
                .build();
    }
    public void outline(float x, float y, float width, float height, float thickness, Vector4f round, Vector2f smooth, Color color1, Color color2, Color color3, Color color4) {
        crenderSystem.shader(ShaderUse.OUTLINE)
                .rect(x, y, width, height)
                .round(round.x, round.y, round.z, round.w)
                .smoothness(smooth.x, smooth.y)
                .thickness(thickness)
                .color(color1, color2, color3, color4)
                .build();
    }

    public void line(float x1, float y1, float x2, float y2, float width, float smooth, float round, Color color1, Color color2) {
        float d = x2 - x1;
        float e = y2 - y1;
        double dist = Math.sqrt(d * d + e * e);

        float angle = (float) ((float) Math.atan2(e, d) * MathUtility.TO_DEGREES);

        getStack().push();

        getStack().translate(MathUtility.scaledX(x1), MathUtility.scaledY(y1), 0);
        getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        rect(-width / 2f, -width / 2f, (float) dist, width, new Vector4f(round), smooth, color1, color1, color2, color2);

        getStack().pop();
    }

    public void blur(float x, float y, float width, float height, Vector4f round, float blurRadius, float smooth) {
        blur(x, y, width, height, round, blurRadius, smooth, CRenderSystem.RenderLayer.BLUR);
    }
    public void blur(float x, float y, float width, float height, Vector4f round, float blurRadius, float smooth, CRenderSystem.RenderLayer layer) {
        CRenderSystem.RenderLayer prev = crenderSystem.layer();
        crenderSystem.layer(layer);
        crenderSystem.layerTex(((GlTextureView)mc.getFramebuffer().getColorAttachmentView()).texture().getGlId());
        crenderSystem.shader(ShaderUse.BLUR).uv(x / window.getScaledWidth(), 1 - (y / window.getScaledHeight()), (width / window.getScaledWidth()), -(height / window.getScaledHeight())).rect(x, y, width, height).color(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE).round(round.x, round.y, round.z, round.w).blur(blurRadius).smoothness(smooth, smooth).build();
        crenderSystem.layer(prev);
    }
    public void text(String text, float x, float y, TextureUse font, float size, Color color) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        msdfFont.applyGlyphs(new Matrix4f(), text, size, 0, 0.3f, x, y + msdfFont.getMetrics().baselineHeight() * size, 0, color, color, color, color, font);
    }
    public void text(String text, float x, float y, TextureUse font, float size, Color color1, Color color2, Color color3, Color color4) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        msdfFont.applyGlyphs(new Matrix4f(), text, size, 0, 0.3f, x, y + msdfFont.getMetrics().baselineHeight() * size, 0, color1, color2, color3, color4, font);
    }
    public void text(Text origin, float x, float y, TextureUse font, float size) {
        float off = 0;


        for (Text text: origin.getWithStyle(origin.getStyle())) {
            off += _text(text, x + off, y, font, size);
        }
    }
    private float _text(Text origin, float x, float y, TextureUse font, float size) {
        List<Text> texts = origin.getWithStyle(origin.getStyle());
        if (texts.size() <= 1) {
            Color c = origin.getStyle().getColor() == null ? ClientColors.FORE_COLOR : new Color(origin.getStyle().getColor().getRgb());
            text(Formatting.strip(origin.getString()), x, y, font, size, c);
            return textWidth(Formatting.strip(origin.getString()), font, size);
        }
        float offset = 0;
        for (Text text: texts) {
            offset += _text(text, x, y, font, size);
        }
        return offset;
    }

    public void text(IconUse icon, float x, float y, TextureUse font, float size, Color color) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        msdfFont.applyGlyphs(new Matrix4f(), icon.glyph, size, 0, 0.3f, x, y + msdfFont.getMetrics().baselineHeight() * size, 0, color, color, color, color, font);
    }
    public void text(IconUse icon, float x, float y, TextureUse font, float size, Color color1, Color color2, Color color3, Color color4) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        msdfFont.applyGlyphs(new Matrix4f(), icon.glyph, size, 0, 0.3f, x, y + msdfFont.getMetrics().baselineHeight() * size, 0, color1, color2, color3, color4, font);
    }
    public void text(IconUse icon, float x, float y, TextureUse font, float size, float smooth, Color color) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        msdfFont.applyGlyphs(new Matrix4f(), icon.glyph, size, 0, 0.3f, x, y + msdfFont.getMetrics().baselineHeight() * size, 0, color, font, smooth);
    }

    public void textCentered(String text, float x, float y, TextureUse font, float size, Color color) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        if (msdfFont == null) return;

        float textWidth = msdfFont.getWidth(text, size);

        this.text(text, x - textWidth / 2f, y, font, size, color);
    }
    public void textCenteredStrict(String text, float x, float y, TextureUse font, float size, Color color) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        if (msdfFont == null) return;

        float textWidth = msdfFont.getWidth(text, size);

        this.text(text, x - textWidth / 2f - 1, y - textHeight(font, 8) / 2F, font, size, color);
    }

    public float textWidth(String text, TextureUse font, float size) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        if (msdfFont == null) return 0f;

        return msdfFont.getWidth(text, size);
    }

    public float wrappedTextWidth(String text, TextureUse font, float size, int wrap) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        if (msdfFont == null) return 0f;

        float width = 0f;
        for (String line : StringUtil.wrapLines(text, wrap).split("\n")) {
            width = Math.max(width, msdfFont.getWidth(line, size));
        }
        return width;
    }

    public float textHeight(TextureUse font, float size) {
        MsdfFont msdfFont = Client.FONTS.get(font);
        if (msdfFont == null) return 0f;

        return msdfFont.getHeight(size);
    }

    public void texture(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        float a = 1e-3F;
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth).uv(uv.u0 + a, uv.v0 + a, uv.u1 - a, uv.v1 - a)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }

    public void textureByKey(String atlasKey, float x, float y, float width, float height, float smooth, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getUV(atlasKey);
        if (uv == null) return;
        float a = 1e-3F;
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth)
                .uv(uv.u0 + a, uv.v0 + a, uv.u1 - a, uv.v1 - a)
                .round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }

    public void textureByAtlas(TextureAtlas customAtlas, String atlasKey, float x, float y, float width, float height, float smooth, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        TextureAtlas.UV uv = customAtlas.getUV(atlasKey);
        if (uv == null) return;
        float a = 1e-3F;
        int prevLayer = system.getAtlas().getGlId();
        system.layerTex(customAtlas.getGlId());
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth)
                .uv(uv.u0 + a, uv.v0 + a, uv.u1 - a, uv.v1 - a)
                .round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
        system.layerTex(prevLayer);
    }
    public void texture(Identifier id, float x, float y, float width, float height, float smooth, Vector4f uv2, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        float a = 1e-3F;

        float ux = uv.u0 + a;
        float uy = uv.v0 + a;
        float uw = uv.u1 - a;
        float uh = uv.v1 - a;

        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth)
                .uv(ux + (uv2.x / uv.width), uy + (uv2.y / uv.height), uw - ((1F - uv2.z) / uv.width), uh - (1F - uv2.z) / uv.height)
                .round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }

    public void textureRaw(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth).uv(uv.u0, uv.v0, uv.u1, uv.v1).round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }
    public void textureRaw(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round, Vector4f tex, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth).uv(uv.u0 + tex.x, uv.v0 + tex.y, tex.z, tex.w).round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }

    public void texture(Item item, float x, float y, float width, float height, float smooth, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        Identifier itemId = Registries.ITEM.getId(item);
        Identifier texturePath;
        
        if (item instanceof BlockItem) {
            texturePath = itemId.withPrefixedPath("textures/block/").withSuffixedPath(".png");
        } else {
            texturePath = itemId.withPrefixedPath("textures/item/").withSuffixedPath(".png");
        }
        
        textureRaw(texturePath, x, y, width, height, smooth, round, color1, color2, color3, color4);
    }

    public void itemStack(ItemStack stack, float x, float y, float size) {
        if (stack == null || stack.isEmpty()) return;
        
        DrawContext context = getDrawContext();

        if (context == null) {
            try {
                texture(stack.getItem(), x, y, size, size, 1, new Vector4f(0), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
            } catch (Exception e) {
                rect(x, y, size, size, new Vector4f(2), 1, Color.GRAY, Color.GRAY, Color.GRAY, Color.GRAY);
            }
            return;
        }
        
        try {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            float scale = size / 16f;
            matrices.translate(x, y);
            matrices.scale(scale, scale);
            context.drawItem(stack, 0, 0);
            matrices.popMatrix();
        } catch (Exception e) {
            try {
                texture(stack.getItem(), x, y, size, size, 1, new Vector4f(0), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
            } catch (Exception ex) {
                rect(x, y, size, size, new Vector4f(2), 1, Color.GRAY, Color.GRAY, Color.GRAY, Color.GRAY);
            }
        }
    }

    public void renderHead(SkinTextures textures, float hX, float hY, float size) {
        Client.RENDERER.queueTask(() -> {
            DrawContext context = Client.RENDERER.getDrawContext();
            if (context != null) {
                PlayerSkinDrawer.draw(context, textures, (int) hX, (int) hY, (int) size);
            }
        });
    }
    public void drawModuleRect(float x, float y, float width, float height) {
        Color thumbBackColor = ClientColors.GUI_STROKE;
        Client.RENDERER.rect(x, y, width, height, new Vector4f(7), 1, thumbBackColor, thumbBackColor, thumbBackColor, thumbBackColor);
    }
    public void drawModuleRect(Rectangle rect) {
        drawModuleRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }
    public void drawHudRect(float x, float y, float width, float height) {
        Client.RENDERER.blur(x, y, width, height, new Vector4f(8), 15f, 1f);
        Color bgColor = new Color(10, 10, 12, 180);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(8), 1, bgColor, bgColor, bgColor, bgColor);
        Color outlineColor = new Color(255, 255, 255, 5);
        Client.RENDERER.outline(x, y, width, height, 0.5f, new Vector4f(8), new Vector2f(1), outlineColor, outlineColor, outlineColor, outlineColor);
    }
    public void outlined(float x, float y, float width, float height) {
        Color bgColor = ClientColors.GUI_BACKGROUND;
        Color thumbBackColor = ClientColors.GUI_STROKE;

        Client.RENDERER.rect(x, y, width, height, new Vector4f(8), 1, bgColor, bgColor, bgColor, bgColor);
        Client.RENDERER.outline(x, y, width, height, 0, new Vector4f(8), new Vector2f(1), thumbBackColor, thumbBackColor, thumbBackColor, thumbBackColor);
    }

    public void shader(Rectangle r, Vector4f round, float smooth, ShaderUse shader) {
        crenderSystem.shader(shader)
                .rect(r.getX(), r.getY(), r.getWidth(), r.getHeight())
                .round(round.x, round.y, round.z, round.w)
                .smoothness(smooth, smooth)
                .build();
    }
    public void shader(Rectangle r, Vector4f round, float smooth, ShaderUse shader, Color a) {
        crenderSystem.shader(shader)
                .rect(r.getX(), r.getY(), r.getWidth(), r.getHeight())
                .round(round.x, round.y, round.z, round.w)
                .color(a, a, a, a)
                .smoothness(smooth, smooth)
                .build();
    }

    public void backIcon(IconUse icon, float x, float y, float rectSize, float iconXOff, float iconYOff, float iconSize, int color1, int color2) {
        Color themeColor = ClientSettings.INSTANCE.getColor(color1);
        Color secondaryColor = ClientSettings.INSTANCE.getColor(color2);

        Client.RENDERER.rect(x + 4, y + 4, rectSize, rectSize, new Vector4f(4), 1, secondaryColor, themeColor, themeColor, secondaryColor);
        Client.RENDERER.text(icon, x + iconXOff, y + iconYOff, TextureUse.ICONS, iconSize, ClientColors.ICON_FOREGROUND_COLOR);
    }

    public void backIcon(IconUse icon, float x, float y) {
        backIcon(icon, x, y, 12, 4.2f, 5.5f, 8, 0, 90);
    }

    public float textLined(String text, int limit, float x, float y, TextureUse tex, float size, Color color) {
        String[] l = StringUtil.wrapLines(text, limit).split("\n");

        if (l.length <= 1) {
            text(text, x, y, tex, size, color);
            return 0;
        }

        float off = 0;
        for (String line : l) {
            text(line, x, y + off, tex, size, color);
            off += size;
        }
        return off - size;
    }

    public void toCamera(MatrixStack matrixStack) {
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        matrixStack.translate(-cam.x, -cam.y, -cam.z);
    }
    public void toWorld(MatrixStack matrixStack) {
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        matrixStack.translate(cam.x, cam.y, cam.z);
    }
    
    public void setupOrientationMatrix(MatrixStack matrix, float x, float y, float z) {
        Vec3d renderPos = mc.gameRenderer.getCamera().getCameraPos();
        matrix.translate(x - renderPos.x, y - renderPos.y, z - renderPos.z);
    }
}