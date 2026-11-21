package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import ru.fluxvisuals.ui.gui.clickgui.externall.theme.ThemeColor;
import ru.fluxvisuals.ui.gui.clickgui.externall.colors.ColorUtil;
import net.minecraft.util.Identifier;

public class GuiBackgroundRenderer {

    private final GrayscalePipeline grayscalePipeline = new GrayscalePipeline();
    private final MenuRaysPipeline raysPipeline = new MenuRaysPipeline();

    // Default effects enabled (can be customized later)
    private static final boolean GRAYSCALE_ENABLED = true;
    private static final boolean BLUR_ENABLED = true;
    private static final boolean DARKEN_ENABLED = false;
    private static final boolean SHADER_ENABLED = false;

    public void render(float mouseX, float mouseY, int screenWidth, int screenHeight, float globalAnim, float scale, boolean exit) {
        if (globalAnim <= 0.001f) return;

        if (GRAYSCALE_ENABLED) grayscalePipeline.draw(globalAnim);
        if (BLUR_ENABLED) {
            if (!ScreenBlur.isReady()) {
                ScreenBlur.capture(1);
            }
            RenderUtil.Blur.blur(0, 0, screenWidth, screenHeight, globalAnim, 0, ColorUtil.getColor(0, 0));
        }

        if (DARKEN_ENABLED) {
            RenderUtil.Images.texture(Identifier.of("client", "textures/frame/background.png"), 0, 0, screenWidth, screenHeight, ColorUtil.multAlpha(ThemeColor.getHudColor(), globalAnim));
        }

        if (SHADER_ENABLED) {
            int rayBase = ThemeColor.getHudColor();
            raysPipeline.draw(globalAnim, ColorUtil.replAlpha(ColorUtil.multDark(rayBase, 0.5F), 0.9F * globalAnim), ColorUtil.replAlpha(rayBase, 0.9F * globalAnim), 0.1F, 0.08F, 0.26F);
        }
    }
}

