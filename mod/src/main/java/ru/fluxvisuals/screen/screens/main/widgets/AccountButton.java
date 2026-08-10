package ru.fluxvisuals.screen.screens.main.widgets;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import org.joml.Vector4f;
import ru.fluxvisuals.Client;
import ru.fluxvisuals.api.render.RendererObject;
import ru.fluxvisuals.api.render.system.IconUse;
import ru.fluxvisuals.api.render.system.TextureUse;
import ru.fluxvisuals.screen.screens.account.Account;
import ru.fluxvisuals.vse.utils.client.client.ClientColors;
import ru.fluxvisuals.vse.utils.math.ColorUtility;
import ru.fluxvisuals.vse.utils.math.MathUtility;

import java.awt.*;

public class AccountButton extends RendererObject {

    private static final float BTN_SIZE = 9.8f;
    private static final float BTN_GAP = 2f;
    private static final Vector4f BTN_ROUND = new Vector4f(3, 3, 3, 3);
    private static final Vector2f SMOOTH = new Vector2f(1, 1);

    private final Account account;
    private final Runnable onLogin;
    private final Runnable onDelete;
    private final Runnable onCopy;
    private final Runnable onFavorite;
    private float hoverAnim = 0;
    private boolean selected = false;
    private boolean favorite = false;

    private final Identifier skinTexture = Identifier.of("fluxvisuals","images/ui/pic/a/kowk.png");

    public AccountButton(Account account, Runnable onLogin, Runnable onDelete, Runnable onCopy, Runnable onFavorite) {
        this.account = account;
        this.onLogin = onLogin;
        this.onDelete = onDelete;
        this.onCopy = onCopy;
        this.onFavorite = onFavorite;
    }

    public Account getAccount() { return account; }
    public void setSelected(boolean v) { this.selected = v; }
    public void setFavorite(boolean v) { this.favorite = v; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public float getX() { return x; }
    public float getY() { return y; }

    public void renderWithContext(DrawContext context, int mouseX, int mouseY) {
        boolean hovered = MathUtility.mouseIn(x, y, width, height, mouseX, mouseY);
        hoverAnim = MathUtility.fastAnim(hoverAnim, hovered ? 1 : 0, 12);

        Vector4f round = new Vector4f(4, 4, 4, 4);
        int bgAlpha = (int)(30 + 20 * hoverAnim);
        Color bgColor = new Color(0, 0, 0, bgAlpha);
        Client.RENDERER.blur(x, y, width, height, round, 8, 1);
        Client.RENDERER.rect(x, y, width, height, round, 1, bgColor, bgColor, bgColor, bgColor);
        Color borderColor;
        if (selected) {
            borderColor = ColorUtility.injectAlpha(ClientColors.BRAIN_COLOR, (int)(100 + 60 * hoverAnim));
        } else if (favorite) {
            borderColor = new Color(255, 200, 0, (int)(80 + 60 * hoverAnim));
        } else {
            borderColor = new Color(255, 255, 255, (int)(15 + 20 * hoverAnim));
        }
        Client.RENDERER.outline(x, y, width, height, 0.5f, round, SMOOTH, borderColor, borderColor, borderColor, borderColor);
        float headSize = 14;
        float headX = x +2 ;
        float headY = y + (height - headSize) / 2 + 1;
        try {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skinTexture,
                    (int)headX, (int)headY, 8.0f, 8.0f, (int)headSize, (int)headSize, 64, 64);
        } catch (Exception e) {
            Color avatarBg = new Color(40, 40, 40, 180);
            Client.RENDERER.rect(headX, headY, headSize, headSize, new Vector4f(3,3,3,3), 1, avatarBg, avatarBg, avatarBg, avatarBg);
            String initial = account.getUsername().substring(0, 1).toUpperCase();
            Client.RENDERER.textCentered(initial, headX + headSize / 2, headY + headSize / 2 - 3, TextureUse.SFMEDIUM, 7, Color.WHITE);
        }
        float totalBtns = 3 * BTN_SIZE + 2 * BTN_GAP;
        float btnsX = x + width - totalBtns - 4;
        float btnsY = y + (height - BTN_SIZE) / 2 + 2;

        float starX = btnsX;
        float copyX = btnsX + BTN_SIZE + BTN_GAP;
        float delX  = btnsX + 2 * (BTN_SIZE + BTN_GAP);

        boolean starHov = MathUtility.mouseIn(starX, btnsY, BTN_SIZE, BTN_SIZE, mouseX, mouseY);
        boolean copyHov = MathUtility.mouseIn(copyX, btnsY, BTN_SIZE, BTN_SIZE, mouseX, mouseY);
        boolean delHov  = MathUtility.mouseIn(delX,  btnsY, BTN_SIZE, BTN_SIZE, mouseX, mouseY);

        Client.RENDERER.blur(starX, btnsY, BTN_SIZE, BTN_SIZE, BTN_ROUND, 6, 1);
       // Client.RENDERER.rect(starX, btnsY, BTN_SIZE, BTN_SIZE, BTN_ROUND, 1, starBg, starBg, starBg, starBg);
        Color starIcon = favorite ? new Color(255, 201, 0) : new Color(255, 255, 255, starHov ? 200 : 100);
        Client.RENDERER.textCenteredStrict(IconUse.STAR.glyph, starX + BTN_SIZE / 2.05f, btnsY + BTN_SIZE /1.35f, TextureUse.ICONS, 6, starIcon);
        Color copyBg = new Color(255, 255, 255, copyHov ? 30 : 12);
        Client.RENDERER.blur(copyX, btnsY, BTN_SIZE, BTN_SIZE, BTN_ROUND, 6, 1);
        //Client.RENDERER.rect(copyX, btnsY, BTN_SIZE, BTN_SIZE, BTN_ROUND, 1, copyBg, copyBg, copyBg, copyBg);
        Color copyIcon = new Color(255, 255, 255, copyHov ? 200 : 100);
        Client.RENDERER.textCenteredStrict(IconUse.LINK.glyph, copyX + BTN_SIZE /  2.05f, btnsY + BTN_SIZE / 1.35f, TextureUse.ICONS, 6, copyIcon);
        Color delBg = delHov ? new Color(220, 50, 50, 120) : new Color(255, 255, 255, 12);
        Client.RENDERER.blur(delX, btnsY, BTN_SIZE, BTN_SIZE, BTN_ROUND, 6, 1);
      //  Client.RENDERER.rect(delX, btnsY, BTN_SIZE, BTN_SIZE, BTN_ROUND, 1, delBg, delBg, delBg, delBg);
        Color delIcon = delHov ? new Color(255, 100, 100) : new Color(255, 255, 255, 100);
        Client.RENDERER.textCenteredStrict(IconUse.CROSS.glyph, delX + BTN_SIZE /  2.05f, btnsY + BTN_SIZE / 1.35f, TextureUse.ICONS, 6, delIcon);
        float textX = headX + headSize + 3;
        float textY = y + height / 2 - 3;
        float maxTextWidth = btnsX - textX - 2;

        String displayName = account.getUsername();
        while (Client.RENDERER.textWidth(displayName, TextureUse.SFMEDIUM, 7) > maxTextWidth && displayName.length() > 1) {
            displayName = displayName.substring(0, displayName.length() - 1);
        }
        if (!displayName.equals(account.getUsername())) displayName += "..";

        Color nameColor = favorite
                ? new Color(255, 210, 0, (int)(180 + 75 * hoverAnim))
                : new Color(255, 255, 255, (int)(130 + 125 * hoverAnim));
        Client.RENDERER.text(displayName, textX, textY, TextureUse.SFMEDIUM, 7, nameColor);
    }

    @Override
    public void render(int mouseX, int mouseY) {}

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!MathUtility.mouseIn(x, y, width, height, mouseX, mouseY)) return false;

        float totalBtns = 3 * BTN_SIZE + 2 * BTN_GAP;
        float btnsX = x + width - totalBtns - 4;
        float btnsY = y + (height - BTN_SIZE) / 2 + 2;

        if (button == 0) {
            if (MathUtility.mouseIn(btnsX, btnsY, BTN_SIZE, BTN_SIZE, mouseX, mouseY)) {
                if (onFavorite != null) onFavorite.run();
                return true;
            }
            if (MathUtility.mouseIn(btnsX + BTN_SIZE + BTN_GAP, btnsY, BTN_SIZE, BTN_SIZE, mouseX, mouseY)) {
                if (onCopy != null) onCopy.run();
                return true;
            }
            if (MathUtility.mouseIn(btnsX + 2 * (BTN_SIZE + BTN_GAP), btnsY, BTN_SIZE, BTN_SIZE, mouseX, mouseY)) {
                if (onDelete != null) onDelete.run();
                return true;
            }
            if (onLogin != null) onLogin.run();
            return true;
        }
        return false;
    }

    public static class AccountButtonBuilder {
        public static AccountButton build(float x, float y, float w, float h, Account account,
                                          Runnable onLogin, Runnable onDelete, Runnable onCopy, Runnable onFavorite) {
            AccountButton btn = new AccountButton(account, onLogin, onDelete, onCopy, onFavorite);
            btn.x = x; btn.y = y; btn.width = w; btn.height = h;
            return btn;
        }
    }
}
