package ru.fluxvisuals.screen.screens.main;

import ru.fluxvisuals.Client;
import ru.fluxvisuals.api.render.system.TextureUse;
import ru.fluxvisuals.api.render.system.sys2d.CRenderSystem;
import ru.fluxvisuals.screen.screens.main.widgets.CustomButton;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;

import static ru.fluxvisuals.MinecraftHolder.mc;


/**
 * Create by daun kvass
 */
public class TitleScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("fluxvisuals", "images/ui/title/title.png");
    private static final Identifier AVATAR_TEXTURE = Identifier.of("fluxvisuals", "images/ui/title/avatar.png");

    private static final TextureUse CUSTOM_FONT = TextureUse.SFMEDIUM;
    private static final float FONT_SIZE_SMALL = 7.5f;
    private static final float FONT_SIZE_LARGE = 12f;
    private static final float FONT_SIZE_TITLE = 14f;
    private static final float FONT_SIZE_BUTTON = 7.5f;

    private static final float LEFT_PADDING = 45f;
    private static final float BUTTON_WIDTH = 210f;
    private static final float BUTTON_HEIGHT = 24f;
    private static final float SPACING = 6f;

    private float fadeInProgress = 0f;
    private boolean fadingIn = true;
    private static final float FADE_IN_SPEED = 0.06f;

    private float quitFadeProgress = 0f;
    private boolean quitting = false;
    private static final float QUIT_FADE_SPEED = 0.025f;

    private final ArrayList<CustomButton> buttons = new ArrayList<>();
    private final ArrayList<String> buttonLabels = new ArrayList<>();

    public TitleScreen() {
        super(Text.empty());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateAnimations(delta);

        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, 0, 0, 0.0f, 0.0f, width, height, width, height);

        context.createNewRootLayer();

        Client.RENDERER.setDrawContext(context);

        float startY = height / 2f + 10f;

        int textAlpha = fadingIn ? (int) (fadeInProgress * 255) : 255;
        if (quitting) {
            textAlpha = (int) ((1f - quitFadeProgress) * 255);
        }
        textAlpha = Math.max(0, Math.min(255, textAlpha));
        float currentAlphaNorm = (float) textAlpha / 255f;

        renderAccountWidget(textAlpha, currentAlphaNorm);

        String username = mc.getSession().getUsername();

        float textY = startY - 80f;
        drawSafeText(
                "Давно не виделись, " + username,
                LEFT_PADDING, textY,
                CUSTOM_FONT, FONT_SIZE_SMALL,
                new Color(19, 255, 174, textAlpha)
        );

        textY += 12f;
        drawSafeText(
                "Приветствуем тебя в",
                LEFT_PADDING, textY,
                CUSTOM_FONT, FONT_SIZE_LARGE,
                new Color(255, 255, 255, textAlpha)
        );

        textY += 18f;
        drawSafeText(
                "godweer 1.21.11",
                LEFT_PADDING, textY,
                CUSTOM_FONT, FONT_SIZE_TITLE,
                new Color(19, 255, 174, textAlpha)
        );

        for (int i = 0; i < buttons.size(); i++) {
            CustomButton button = buttons.get(i);
            String label = buttonLabels.get(i);

            if (fadingIn) {
                float delay = i * 0.06f;
                float progress = Math.max(0f, Math.min(1f, (fadeInProgress - delay) / (1f - delay)));
                float slide = (1f - easeOutCubic(progress)) * 30f;
                button.setY(button.getBaseY() + slide);
                button.setAlpha(easeOutCubic(progress));
            }

            if (quitting) {
                button.setAlpha(1f - easeInQuad(quitFadeProgress));
            }

            float alpha = button.getAlpha();
            if (alpha > 0.01f) {
                float bx = button.getX();
                float by = button.getY();
                float bw = button.getWidth();
                float bh = button.getHeight();

                Vector4f round = new Vector4f(8f, 8f, 8f, 8f);

                boolean hovered = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;

                Color bgColor;
                Color textColor;
                Color outColor;

                if (hovered) {
                    bgColor = new Color(19, 255, 174, (int) (alpha * 18));
                    textColor = new Color(19, 255, 174, (int) (alpha * 255));
                    outColor = new Color(19, 255, 174, (int) (alpha * 20));
                } else {
                    bgColor = new Color(0, 0, 0, 10);
                    textColor = new Color(235, 235, 240, (int) (alpha * 230));
                    outColor = new Color(255, 255, 255, (int) (alpha * 4));
                }
                Client.RENDERER.blur(bx, by, bw, bh, round, 20f, alpha);
                Client.RENDERER.rect(bx, by, bw, bh, round, 1f, bgColor, bgColor, bgColor, bgColor);
                Client.RENDERER.outline(bx, by, bw, bh, 0.5f, round, new Vector2f(1), outColor, outColor, outColor, outColor);


                drawSafeTextCentered(label, bx + bw / 2f, by + bh / 2f, CUSTOM_FONT, FONT_SIZE_BUTTON, textColor);
            }
        }

        renderScreenOverlays(context);
    }


    private void renderAccountWidget(int textAlpha, float currentAlphaNorm) {
        float ax = 15f;
        float ay = 15f;
        float ah = 24f;

        String username = client.getSession() != null ? client.getSession().getUsername() : "Игрок";

        float nameWidth = Client.RENDERER.textWidth(username, CUSTOM_FONT, 7f);
        float titleWidth = Client.RENDERER.textWidth("Текущий аккаунт", CUSTOM_FONT, 4.5f);
        float maxTextWidth = Math.max(nameWidth, titleWidth);

        float avatarSize = 14f;
        float aw = 5f + avatarSize + 5f + maxTextWidth + 8f;

        Vector4f round = new Vector4f(6f, 6f, 6f, 6f);

        Client.RENDERER.blur(ax, ay, aw, ah, round, 15f, currentAlphaNorm);

        Color widgetBg = new Color(0, 0, 0, 0);
        Client.RENDERER.rect(ax, ay, aw, ah, round, 1f, widgetBg, widgetBg, widgetBg, widgetBg);

        Color widgetOutline = new Color(255, 255, 255, (int) (textAlpha * 0.08f));
        Client.RENDERER.outline(ax, ay, aw, ah, 0.5f, round, new Vector2f(1), widgetOutline, widgetOutline, widgetOutline, widgetOutline);

        float avatarX = ax + 5f;
        float avatarY = ay + (ah - avatarSize) / 2f;
        Color avatarColor = new Color(255, 255, 255, textAlpha);

        Client.RENDERER.texture(
                AVATAR_TEXTURE,
                avatarX, avatarY, avatarSize, avatarSize,
                1.5F,
                new Vector4f(0.0F, 0.0F, 1.0F, 1.0F),
                new Vector4f(7f),
                avatarColor, avatarColor, avatarColor, avatarColor
        );

        float labelX = avatarX + avatarSize + 5f;

        drawSafeText(
                "Текущий аккаунт",
                labelX, ay + 4.5f,
                CUSTOM_FONT, 4.5f,
                new Color(150, 150, 155, textAlpha)
        );

        drawSafeText(
                username,
                labelX, ay + 12f,
                CUSTOM_FONT, 7f,
                new Color(255, 255, 255, textAlpha)
        );
    }

    private void drawSafeText(String text, float x, float y, TextureUse font, float size, Color color) {
        if (Client.FONTS.get(font) != null) {
            Client.RENDERER.text(text, x, y, font, size, color);
        }
    }

    private void drawSafeTextCentered(String text, float x, float y, TextureUse font, float size, Color color) {
        if (Client.FONTS.get(font) != null) {
            Client.RENDERER.textCenteredStrict(text, x, y - 1f, font, size, color);
        }
    }

    private void renderScreenOverlays(DrawContext context) {
        if (fadingIn && fadeInProgress < 1f) {
            int alpha = (int) ((1f - easeOutCubic(fadeInProgress)) * 255);
            context.fill(0, 0, width, height, (Math.max(0, alpha) << 24));
        }
        if (quitting) {
            int alpha = (int) (easeInOutQuad(quitFadeProgress) * 255);
            context.fill(0, 0, width, height, (Math.min(255, alpha) << 24));
        }
    }

    private void updateAnimations(float delta) {
        if (fadingIn) {
            fadeInProgress += FADE_IN_SPEED * delta;
            if (fadeInProgress >= 1f) {
                fadeInProgress = 1f;
                fadingIn = false;
                buttons.forEach(b -> {
                    b.setY(b.getBaseY());
                    b.setAlpha(1f);
                });
            }
        }
        if (quitting) {
            quitFadeProgress += QUIT_FADE_SPEED * delta;
            if (quitFadeProgress >= 1f) {
                quitFadeProgress = 1f;
                client.scheduleStop();
            }
        }
    }

    private void startQuitFade() {
        if (!quitting) {
            quitting = true;
            quitFadeProgress = 0f;
        }
    }

    private float easeOutCubic(float x) { return 1f - (float) Math.pow(1f - x, 3); }
    private float easeInQuad(float x) { return x * x; }
    private float easeInOutQuad(float x) { return x < 0.5f ? 2f * x * x : 1f - (float) Math.pow(-2f * x + 2f, 2) / 2f; }

    @Override
    protected void init() {
        super.init();
        fadeInProgress = 0f;
        fadingIn = true;
        quitting = false;

        float margin = BUTTON_HEIGHT + SPACING;
        float startY = height / 2f + 10f;

        buttons.clear();
        buttonLabels.clear();

        addButton(LEFT_PADDING, startY + margin, BUTTON_WIDTH, BUTTON_HEIGHT,
                Text.translatable("menu.singleplayer").getString(),
                CustomButton.CustomButtonBuilder.ButtonType.MAIN,
                () -> client.setScreen(new SelectWorldScreen(this))
        );

        addButton(LEFT_PADDING, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Text.translatable("menu.multiplayer").getString(),
                CustomButton.CustomButtonBuilder.ButtonType.MAIN,
                () -> client.setScreen(new MultiplayerScreen(this))
        );

        addButton(LEFT_PADDING, startY + margin * 2, BUTTON_WIDTH, BUTTON_HEIGHT,
                "Account Switcher",
                CustomButton.CustomButtonBuilder.ButtonType.ALT,
                () -> client.setScreen(new AccountScreen(this))
        );

        float halfButtonWidth = (BUTTON_WIDTH - SPACING) / 2f;

        addButton(LEFT_PADDING, startY + margin * 3, halfButtonWidth, BUTTON_HEIGHT,
                Text.translatable("menu.options").getString(),
                CustomButton.CustomButtonBuilder.ButtonType.MAIN,
                () -> client.setScreen(new OptionsScreen(this, client.options))
        );

        addButton(LEFT_PADDING + halfButtonWidth + SPACING, startY + margin * 3, halfButtonWidth, BUTTON_HEIGHT,
                Text.translatable("menu.quit").getString(),
                CustomButton.CustomButtonBuilder.ButtonType.RED,
                this::startQuitFade
        );
    }

    private void addButton(float x, float y, float width, float height, String label, CustomButton.CustomButtonBuilder.ButtonType type, Runnable action) {
        buttons.add(CustomButton.CustomButtonBuilder.build(x, y, width, height, "", type, action));
        buttonLabels.add(label);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (quitting) return false;

        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        int button = click.button();

        buttons.forEach(b -> b.click((int) mouseX, (int) mouseY, button));
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}