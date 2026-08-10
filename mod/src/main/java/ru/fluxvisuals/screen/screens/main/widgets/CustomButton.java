package ru.fluxvisuals.screen.screens.main.widgets;

import org.joml.Vector2f;
import org.joml.Vector4f;
import ru.fluxvisuals.Client;
import ru.fluxvisuals.api.render.RendererObject;
import ru.fluxvisuals.api.render.system.TextureUse;
import ru.fluxvisuals.vse.utils.client.client.ClientColors;
import ru.fluxvisuals.vse.utils.math.ColorUtility;
import ru.fluxvisuals.vse.utils.math.MathUtility;

import java.awt.*;

public class CustomButton extends RendererObject {
    private final Runnable onPress;
    private final String text;
    private final CustomButtonBuilder.ButtonType type;
    private Color hoverColor = new Color(150, 150, 150);
    private float anim = 0;
    private float baseY;
    private float alpha = 1f;
    public CustomButton(Runnable onPress, String text, CustomButtonBuilder.ButtonType type) {
        this.onPress = onPress;
        this.text = text;
        this.type = type;
        switch (type) {
            case MAIN -> hoverColor = ColorUtility.injectAlpha(ClientColors.MAIN_COLOR, 255);
            case RED -> hoverColor = new Color(255, 63, 63, 255);
            case ALT -> hoverColor = new Color(13, 54, 148, 255);
        }
    }

    public void setY(float y) {
        this.y = y;
    }
    public float getY() {
        return this.y;
    }
    public void setBaseY(float baseY) {
        this.baseY = baseY;
    }
    public float getBaseY() {
        return this.baseY;
    }
    public void setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
    }
    public float getAlpha() {
        return this.alpha;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (alpha <= 0.01f) return;
        anim = MathUtility.fastAnim(anim, MathUtility.mouseIn(x, y, width, height, mouseX, mouseY) ? 1 : 0, 10);
        Vector4f round = new Vector4f(8, 8, 8, 8);
        Color pillColor = new Color(10, 10, 13, 120) ;
        Client.RENDERER.rect(x, y, width, height, round, 1, pillColor, pillColor, pillColor, pillColor);
        Client.RENDERER.blur(x, y, width, height, round, 16, alpha);
        Color outline = new Color(255, 255, 255, 4);
        Client.RENDERER.outline(x, y, width, height, 0.5f, new Vector4f(7), new Vector2f(1), outline, outline, outline, outline);

        int textAlpha = (int)((120 + 130 * anim) * alpha);
        Client.RENDERER.textCentered(text, x + width / 2, y + height / 2 - 5.5f, TextureUse.SFMEDIUM, 9, ColorUtility.injectAlpha(ClientColors.FORE_COLOR, textAlpha));
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (onPress != null && MathUtility.mouseIn(x, y, width, height, mouseX, mouseY)) {
            onPress.run();
        }
        return super.click(mouseX, mouseY, button);
    }
    public void setX(float x) {
        this.x = x;
    }

    public float getX() {
        return this.x;
    }
    public static class CustomButtonBuilder {
        public static CustomButton build(float x, float y, float w, float h, String text, ButtonType type, Runnable onPress) {
            CustomButton button = new CustomButton(onPress, text, type);
            button.x = x ;
            button.y = y;
            button.baseY = y;
            button.width = w;
            button.height = h;
            return button;
        }

        public enum ButtonType {
            MAIN, ALT, RED
        }
    }
}