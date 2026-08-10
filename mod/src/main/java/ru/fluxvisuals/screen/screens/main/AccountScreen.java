package ru.fluxvisuals.screen.screens.main;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import org.joml.Vector4f;
import ru.fluxvisuals.Client;
import ru.fluxvisuals.api.render.system.IconUse;
import ru.fluxvisuals.api.render.system.TextureUse;
import ru.fluxvisuals.screen.screens.account.Account;
import ru.fluxvisuals.screen.screens.account.AccountManager;
import ru.fluxvisuals.screen.screens.main.widgets.AccountButton;
import ru.fluxvisuals.screen.screens.main.widgets.CustomButton;
import ru.fluxvisuals.vse.utils.client.client.ClientColors;
import ru.fluxvisuals.vse.utils.math.ColorUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AccountScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("fluxvisuals", "images/ui/title/title.png");

    private final Screen parent;
    private final AccountManager accountManager;
    private List<AccountButton> accountButtons = new ArrayList<>();
    private CustomButton backButton;
    private CustomButton addButton;
    private String inputText = "";
    private boolean inputFocused = false;
    private float inputCursorBlink = 0;
    private float fadeInProgress = 0f;
    private boolean fadingIn = true;
    private static final float FADE_IN_SPEED = 0.1f;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private float panelX, panelY, panelW, panelH;
    private static final float ACC_BTN_W = 85;
    private static final float ACC_BTN_H = 22;
    private static final float ACC_BTN_GAP = 4;
    private int columns = 4;
    private boolean needsRefresh = false;

    public AccountScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
        this.accountManager = AccountManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        fadeInProgress = 0f;
        fadingIn = true;
        scrollOffset = 0f;
        targetScrollOffset = 0f;
        columns = Math.max(3, (int)((width * 0.7f) / (ACC_BTN_W + ACC_BTN_GAP)));
        panelW = columns * (ACC_BTN_W + ACC_BTN_GAP) + 16;
        panelH = Math.min(height * 0.7f, 300);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        float btnW = 70;
        float btnH = 18;
        float btnY = panelY + panelH - btnH - 8;
        addButton = CustomButton.CustomButtonBuilder.build(
                panelX + panelW / 2 - btnW - 4, btnY,
                btnW, btnH,
                "Add",
                CustomButton.CustomButtonBuilder.ButtonType.MAIN,
                this::addOfflineAccount
        );
        backButton = CustomButton.CustomButtonBuilder.build(
                panelX + panelW / 2 + 4, btnY,
                btnW, btnH,
                "Back",
                CustomButton.CustomButtonBuilder.ButtonType.RED,
                () -> client.setScreen(parent)
        );
        refreshAccountList();
    }

    private void refreshAccountList() {
        accountButtons.clear();
        List<Account> accounts = accountManager.getAccounts();
        accounts.sort((a, b) -> {
            boolean af = accountManager.isFavorite(a.getUsername());
            boolean bf = accountManager.isFavorite(b.getUsername());
            return java.lang.Boolean.compare(bf, af);
        });
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            AccountButton btn = AccountButton.AccountButtonBuilder.build(
                    0, 0,
                    ACC_BTN_W, ACC_BTN_H,
                    account,
                    () -> loginAccount(account),
                    () -> deleteAccount(account),
                    () -> copyAccount(account),
                    () -> favoriteAccount(account)
            );
            String currentName = MinecraftClient.getInstance().getSession().getUsername();
            btn.setSelected(account.getUsername().equals(currentName));
            btn.setFavorite(accountManager.isFavorite(account.getUsername()));
            accountButtons.add(btn);
        }
    }

    private void loginAccount(Account account) {
        if (accountManager.login(account)) {
            needsRefresh = true;
        }
    }

    private void deleteAccount(Account account) {
        accountManager.removeAccount(account);
        needsRefresh = true;
    }

    private void copyAccount(Account account) {
        client.keyboard.setClipboard(account.getUsername());
    }

    private void favoriteAccount(Account account) {
        accountManager.toggleFavorite(account.getUsername());
        needsRefresh = true;
    }

    private void addOfflineAccount() {
        if (inputText.trim().isEmpty()) return;
        if (accountManager.loginOffline(inputText.trim())) {
            inputText = "";
            needsRefresh = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (needsRefresh) {
            needsRefresh = false;
            refreshAccountList();
        }
        if (fadingIn) {
            fadeInProgress += FADE_IN_SPEED * delta;
            if (fadeInProgress >= 1f) {
                fadeInProgress = 1f;
                fadingIn = false;
            }
        }
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.25f;
        float animAlpha = easeOutCubic(fadeInProgress);
        float animSlide = (1f - easeOutCubic(fadeInProgress)) * 20f;
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE,
                0, 0,
                0.0f, 0.0f,
                width, height,
                width, height
        );
        float realPanelY = panelY + animSlide;
        Vector4f round = new Vector4f(8, 8, 8, 8);
        Vector2f smooth = new Vector2f(1, 1);
        Color panelBg = ColorUtility.injectAlpha(ClientColors.BACK_COLOR, (int)(235 * animAlpha));
        Client.RENDERER.blur(panelX, realPanelY, panelW, panelH, round, 14, animAlpha);
        Client.RENDERER.outline(panelX, realPanelY, panelW, panelH, 0, round, smooth, panelBg, panelBg, panelBg, panelBg);
        String currentName = MinecraftClient.getInstance().getSession().getUsername();
        Client.RENDERER.textCentered("Accounts", panelX + panelW / 2, realPanelY + 8, TextureUse.SFMEDIUM, 10, ColorUtility.injectAlpha(ClientColors.FORE_COLOR, (int)(255 * animAlpha)));
        Client.RENDERER.textCentered(currentName, panelX + panelW / 2, realPanelY + 20, TextureUse.SFMEDIUM, 8, ColorUtility.injectAlpha(ClientColors.BRAIN_COLOR, (int)(200 * animAlpha)));
        float inputW = 100;
        float inputH = 16;
        float inputX = panelX + panelW - inputW - 10;
        float inputY = realPanelY + 10;
        Vector4f inputRound = new Vector4f(6, 6, 6, 6);
        Color inputBg = inputFocused
                ? new Color(255, 255, 255, (int)(50 * animAlpha))
                : new Color(255, 255, 255, (int)(25 * animAlpha));
        Color inputBorder = new Color(255, 255, 255, (int)((inputFocused ? 100 : 55) * animAlpha));
        Client.RENDERER.blur(inputX, inputY, inputW, inputH, inputRound, 28, animAlpha);
        Client.RENDERER.rect(inputX, inputY, inputW, inputH, inputRound, 1, inputBg, inputBg, inputBg, inputBg);
        Client.RENDERER.outline(inputX, inputY, inputW, inputH, 0.5f, inputRound, new Vector2f(1, 1), inputBorder, inputBorder, inputBorder, inputBorder);
        Client.RENDERER.text(IconUse.SEARCH.glyph, inputX + 3, inputY + inputH / 2 - 3.5f, TextureUse.ICONS, 7, new Color(255, 255, 255, (int)(255 * animAlpha)));
        String displayText = inputText.isEmpty() && !inputFocused ? "Add nick..." : inputText;
        Color textColor = inputText.isEmpty() && !inputFocused
                ? new Color(255, 255, 255, (int)(200 * animAlpha))
                : new Color(255, 255, 255, (int)(255 * animAlpha));
        Client.RENDERER.text(displayText, inputX + 16, inputY + inputH / 2 - 3, TextureUse.SFMEDIUM, 7, textColor);
        if (inputFocused) {
            inputCursorBlink += delta * 0.15f;
            if (((int)inputCursorBlink) % 2 == 0) {
                float cursorX = inputX + 16 + Client.RENDERER.textWidth(inputText, TextureUse.SFMEDIUM, 7);
                context.fill((int)cursorX, (int)(inputY + 3), (int)(cursorX + 1), (int)(inputY + inputH - 3), new Color(255, 255, 255, (int)(180 * animAlpha)).getRGB());
            }
        }
        float gridX = panelX + 8;
        float gridY = realPanelY + 35;
        float gridH = panelH - 70;
        Vector4f gridRound = new Vector4f(6, 6, 6, 6);
        Color gridBg = new Color(0, 0, 0, (int)(60 * animAlpha));
        Color gridBorder = new Color(255, 255, 255, (int)(10 * animAlpha));
        Client.RENDERER.blur(panelX + 6, gridY, panelW - 12, gridH, gridRound, 10, animAlpha);
        Client.RENDERER.rect(panelX + 6, gridY, panelW - 12, gridH, gridRound, 1, gridBg, gridBg, gridBg, gridBg);
        Client.RENDERER.outline(panelX + 6, gridY, panelW - 12, gridH, 0.5f, gridRound, new Vector2f(1, 1), gridBorder, gridBorder, gridBorder, gridBorder);
        context.enableScissor((int)panelX, (int)gridY, (int)(panelX + panelW), (int)(gridY + gridH));
        if (accountButtons.isEmpty()) {
            Client.RENDERER.textCentered("No accounts", panelX + panelW / 2, gridY + gridH / 2 - 8, TextureUse.SFMEDIUM, 8, ColorUtility.injectAlpha(ClientColors.FORE_COLOR, (int)(70 * animAlpha)));
            Client.RENDERER.textCentered("Enter nick and press Enter", panelX + panelW / 2, gridY + gridH / 2 + 4, TextureUse.SFMEDIUM, 6, ColorUtility.injectAlpha(ClientColors.FORE_COLOR, (int)(40 * animAlpha)));
        } else {
            for (int i = 0; i < accountButtons.size(); i++) {
                AccountButton btn = accountButtons.get(i);
                int col = i % columns;
                int row = i / columns;
                float btnX = gridX + col * (ACC_BTN_W + ACC_BTN_GAP);
                float btnY = gridY + row * (ACC_BTN_H + ACC_BTN_GAP) - scrollOffset;
                btn.setX(btnX);
                btn.setY(btnY);
                btn.renderWithContext(context, mouseX, mouseY);
            }
        }
        context.disableScissor();
        Client.RENDERER.textCentered(" 67 ", panelX + panelW / 2, realPanelY + panelH - 30, TextureUse.SFMEDIUM, 6, ColorUtility.injectAlpha(ClientColors.FORE_COLOR, (int)(40 * animAlpha)));
        float btnY = realPanelY + panelH - 18 - 8;
        addButton.setY(btnY);
        backButton.setY(btnY);
        addButton.render(mouseX, mouseY);
        backButton.render(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        int button = click.button();

        float animSlide = (1f - easeOutCubic(fadeInProgress)) * 20f;
        float realPanelY = panelY + animSlide;
        float inputW = 100;
        float inputH = 16;
        float inputX = panelX + panelW - inputW - 10;
        float inputY = realPanelY + 10;
        inputFocused = mouseX >= inputX && mouseX <= inputX + inputW
                && mouseY >= inputY && mouseY <= inputY + inputH;
        List<AccountButton> buttonsCopy = new ArrayList<>(accountButtons);
        for (AccountButton btn : buttonsCopy) {
            if (btn.click((int)mouseX, (int)mouseY, button)) {
                break;
            }
        }
        if (button == 0) {
            addButton.click((int)mouseX, (int)mouseY, button);
            backButton.click((int)mouseX, (int)mouseY, button);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.comp_4795();
        int scanCode = input.comp_4796();
        int modifiers = input.comp_4797();

        if (inputFocused) {
            boolean ctrl = (modifiers & 2) != 0;
            if (keyCode == 257) {
                addOfflineAccount();
                return true;
            }
            if (keyCode == 259) {
                if (ctrl) {
                    inputText = "";
                } else if (!inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                return true;
            }
            if (keyCode == 256) {
                inputFocused = false;
                return true;
            }
            if (ctrl && keyCode == 86) {
                String clipboard = client.keyboard.getClipboard();
                if (clipboard != null) {
                    StringBuilder sb = new StringBuilder(inputText);
                    for (char c : clipboard.toCharArray()) {
                        if (sb.length() >= 16) break;
                        if (Character.isLetterOrDigit(c) || c == '_') {
                            sb.append(c);
                        }
                    }
                    inputText = sb.toString();
                }
                return true;
            }
            if (ctrl && keyCode == 65) {
                inputText = "";
                return true;
            }
            if (ctrl && keyCode == 67) {
                if (!inputText.isEmpty()) {
                    client.keyboard.setClipboard(inputText);
                }
                return true;
            }
        } else {
            if (keyCode == 256) {
                client.setScreen(parent);
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.comp_4793();
        if (inputFocused && inputText.length() < 16) {
            if (Character.isLetterOrDigit(chr) || chr == '_') {
                inputText += chr;
                return true;
            }
        }
        return super.charTyped(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int rows = (int) Math.ceil((double) accountButtons.size() / columns);
        float contentHeight = rows * (ACC_BTN_H + ACC_BTN_GAP);
        float gridH = panelH - 70;
        float maxScroll = Math.max(0, contentHeight - gridH);
        targetScrollOffset -= verticalAmount * 20;
        targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset));
        return true;
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1f - x, 3);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !inputFocused;
    }
}