package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DragRenderContext;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.type.HudInfoType;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScreenClickContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.setting.NumberSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.OrderedEnumSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.SeparatorSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.ServerUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Theme;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.util.math.MatrixStack;

public class WatermarkWidget extends Draggable {
    float computedWidth;
    float computedHeight;
    final GlTexture logoTexture;
    final GlTexture playerIcon;
    final GlTexture computerIcon;
    final GlTexture timeIcon;
    final GlTexture wifiIcon;
    final GlTexture cloudIcon;
    final GlTexture backgroundTexture;
    final MsdfFont font;
    public final OrderedEnumSetting<HudInfoType> hudLines;
    public final NumberSetting scale;
    public final BooleanSetting use12hFormat;

    public static final WatermarkStar[] stars = {new WatermarkStar(19.5f, 0.0f, 6.0f, 6.0f, 0.1f), new WatermarkStar(9.5f, 0.0f, 6.0f, 6.0f, 0.1f), new WatermarkStar(3.0f, 5.0f, 6.0f, 6.0f, 0.1f), new WatermarkStar(3.5f, -3.0f, 6.0f, 6.0f, 0.1f), new WatermarkStar(6.0f, 14.0f, 6.0f, 6.0f, 0.1f), new WatermarkStar(10.5f, 20.0f, 6.0f, 6.0f, 0.1f), new WatermarkStar(19.5f, 16.0f, 7.0f, 7.0f, 0.1f), new WatermarkStar(24.5f, 12.0f, 7.0f, 7.0f, 0.1f)};

    public WatermarkWidget(BooleanSupplier booleanSupplier) {
        super("Watermark", booleanSupplier);
        this.logoTexture = new GlTexture(new ClasspathResource("/icons/menu/new/logotype.png"));
        this.playerIcon = new GlTexture(new ClasspathResource("/icons/menu/new/tabs/player.png"));
        this.computerIcon = new GlTexture(new ClasspathResource("/icons/menu/new/computer.png"));
        this.timeIcon = new GlTexture(new ClasspathResource("/icons/menu/new/time.png"));
        this.wifiIcon = new GlTexture(new ClasspathResource("/icons/menu/new/wifi.png"));
        this.cloudIcon = new GlTexture(new ClasspathResource("/icons/menu/new/cloud.png"));
        this.backgroundTexture = new GlTexture(new ClasspathResource("/textures/hud_background.png"));
        this.font = Fonts.INTER_BOLD.get();
        this.hudLines = new OrderedEnumSetting(Lang.WIDGET_WATERMARK_HUD_LINES).values(HudInfoType.class).ordered(HudInfoType.LATENCY, HudInfoType.FRAMERATE, HudInfoType.CURRENT_TIME, HudInfoType.SERVER_NAME).selected(HudInfoType.LATENCY, HudInfoType.FRAMERATE);
        this.scale = new NumberSetting(Lang.WIDGET_WATERMARK_SCALE).currentValue(1.0f).range(0.5f, 2.0f).step(0.05f);
        this.use12hFormat = new BooleanSetting(Lang.WIDGET_WATERMARK_USE_12H_FORMAT).visible(() -> {
            return Boolean.valueOf(this.hudLines.isSelected(HudInfoType.CURRENT_TIME));
        });
        addSettings(this.scale, this.use12hFormat, new SeparatorSetting(), this.hudLines);
        this.x = 10.0f;
        this.y = 10.0f;
    }

    @Override
    public void layout(DragRenderContext dragRenderContext) {
        String strMethod005;
        if (isVisible()) {
            Mc mc = Mc.INSTANCE;
            float width = this.x + 8.0f + 19.0f + 8.0f + 12.0f + 3.0f + this.font.getWidth("https://t.me/soezproject", 11.0f) + 8.0f;
            for (HudInfoType e : this.hudLines.orderedValues()) {
                if (e != HudInfoType.NICKNAME && this.hudLines.isSelected(e) && (strMethod005 = textFor(e, mc)) != null && !strMethod005.isEmpty()) {
                    width += 12.0f + 3.0f + this.font.getWidth(strMethod005, fontSizeFor(e)) + 8.0f;
                }
            }
            this.computedWidth = width - this.x;
            this.computedHeight = 23.0f;
        }
    }

    @Override
    public void render(DragRenderContext dragRenderContext) throws MatchException {
        if (isVisible()) {
            Mc mc = Mc.INSTANCE;
            DrawEngine drawEngine154 = dragRenderContext.drawEngine();
            ColorStack colorStack115 = drawEngine154.colorStack();
            MatrixStack matrixStack = dragRenderContext.matrixStack();
            Theme theme760 = dragRenderContext.theme();
            ThemePalette themePalette = theme760.palette();
            float fCurrentValue = this.scale.currentValue();
            matrixStack.push();
            matrixStack.translate(this.x, this.y, 0.0f);
            matrixStack.scale(fCurrentValue, fCurrentValue, 1.0f);
            matrixStack.translate(-this.x, -this.y, 0.0f);
            float f = this.y + (this.computedHeight / 2.0f);
            int iComputeColor = colorStack115.computeColor(themePalette.surfaceBackground().tone(801).argb());
            int iComputeColor2 = colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb());
            drawEngine154.roundedRectangle(matrixStack.peek().getPositionMatrix(), this.x, this.y, this.computedWidth, this.computedHeight, 8.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), iComputeColor2, iComputeColor2, iComputeColor, iComputeColor);
            drawEngine154.beginStencil();
            drawEngine154.roundedRectangle(matrixStack.peek().getPositionMatrix(), this.x, this.y + 2.0f, this.computedWidth, this.computedHeight - 4.0f, 8.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), iComputeColor2, iComputeColor2, iComputeColor, iComputeColor);
            drawEngine154.prepareStencil(1);
            drawEngine154.texture(matrixStack.peek().getPositionMatrix(), this.x, this.y, this.computedWidth, this.computedHeight, drawEngine154.bindTexture(this.backgroundTexture.textureWithSTB()), colorStack115.white());
            float fMethod002 = renderLogo(drawEngine154, matrixStack, colorStack115, theme760, themePalette);
            drawEngine154.textureVerticalC(matrixStack.peek().getPositionMatrix(), this.playerIcon, fMethod002, f, 12, 12, colorStack115.computeColor(themePalette.accent().argb()));
            float f2 = fMethod002 + 15.0f;
            String strUsername = "https://t.me/soezproject";
            drawEngine154.msdfFont(matrixStack.peek().getPositionMatrix(), this.font, strUsername, f2, f - (this.font.getHeight(11.0f) / 2.0f), 11.0f, 0.05f, colorStack115.computeColor(themePalette.text().tone(300).argb()));
            float width = f2 + this.font.getWidth(strUsername, 11.0f) + 8.0f;
            for (HudInfoType e : this.hudLines.orderedValues()) {
                if (e != HudInfoType.NICKNAME && this.hudLines.isSelected(e)) {
                    GlTexture class073VarMethod004 = iconFor(e);
                    String strMethod005 = textFor(e, mc);
                    if (strMethod005 != null && !strMethod005.isEmpty()) {
                        drawEngine154.textureVerticalC(matrixStack.peek().getPositionMatrix(), class073VarMethod004, width, f, 12, 12, colorStack115.computeColor(themePalette.accent().argb()));
                        float f3 = width + 15.0f;
                        int iMethod003 = fontSizeFor(e);
                        drawEngine154.msdfFont(matrixStack.peek().getPositionMatrix(), this.font, strMethod005, f3, f - (this.font.getHeight(iMethod003) / 2.0f), iMethod003, 0.05f, colorStack115.computeColor(themePalette.text().tone(300).argb()));
                        width = f3 + this.font.getWidth(strMethod005, iMethod003) + 8.0f;
                    }
                }
            }
            drawEngine154.endStencil();
            matrixStack.pop();
        }
    }

    public float renderLogo(DrawEngine drawEngine, MatrixStack matrixStack, ColorStack colorStack, Theme theme, ThemePalette themePalette) {
        float f = this.x + 8.0f;
        float f2 = this.y + 2.0f;
        drawEngine.roundedRectangle(matrixStack.peek().getPositionMatrix(), f, f2, 19.0f, 19.0f, 8.0f, colorStack.computeColor(1052690));
        drawEngine.roundedRectangle(matrixStack.peek().getPositionMatrix(), f, f2, 19.0f, 19.0f, 8.0f, colorStack.computeColor(theme.palette().accentBright().argb(), 0.03f));
        drawEngine.radialRoundedRectangle(matrixStack.peek().getPositionMatrix(), f, f2, 19.0f, 19.0f, 8.0f, colorStack.computeColor(themePalette.accentBright().argb(), 0.3f), colorStack.computeColor(themePalette.accentBright().argb(), 0.0f));
        int iBindTexture = drawEngine.bindTexture(this.logoTexture.textureWithSTB());
        int iArgb = themePalette.accentBright().argb();
        for (WatermarkStar watermarkStar : stars) {
            drawEngine.texture(matrixStack.peek().getPositionMatrix(), this.x + watermarkStar.xOff(), this.y + watermarkStar.yOff(), watermarkStar.w(), watermarkStar.h(), iBindTexture, colorStack.computeColor(iArgb, watermarkStar.alpha()));
        }
        drawEngine.textureVerticalCHorizontalC(matrixStack.peek().getPositionMatrix(), this.logoTexture, f + (19.0f / 2.0f), f2 + (19.0f / 2.0f), 11, 11, colorStack.computeColor(themePalette.accentBright().argb()));
        return f + 19.0f + 8.0f;
    }

    @Override
    public boolean click(ScreenClickContext screenClickContext, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput mouseMoveInput, boolean z) {
        return z && !mouseMoveInput.intercepted();
    }

    @Override
    public void animate(WeightedEngine weightedEngine) {
    }

    @Override
    public void update() {
    }

    public GlTexture iconFor(HudInfoType hudInfoType) throws MatchException {
        switch (hudInfoType.ordinal()) {
            case 0:
                return this.playerIcon;
            case 1:
                return this.wifiIcon;
            case 2:
                return this.computerIcon;
            case 3:
                return this.timeIcon;
            case 4:
                return this.cloudIcon;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    public int fontSizeFor(HudInfoType hudInfoType) {
        switch (hudInfoType) {
            case NICKNAME:
                return 11;
            default:
                return 10;
        }
    }

    public String textFor(HudInfoType hudInfoType, Mc mc) throws MatchException {
        switch (hudInfoType.ordinal()) {
            case 0:
                return "https://t.me/soezproject";
            case 1:
                return getPing() + "ms";
            case 2:
                return mc.getCurrentFps() + " FPS";
            case 3:
                return formatTime();
            case 4:
                return serverAddress();
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    public String formatTime() {
        return LocalTime.now().format(this.use12hFormat.isValue() ? DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH) : DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String serverAddress() {
        String serverIp = ServerUtil.getServerIp();
        return serverIp == null ? "" : serverIp.toLowerCase();
    }

    public int getPing() {
        if (Mc.INSTANCE.isWorldLoaded()) {
            return ((Integer) Mc.INSTANCE.getNetworkHandler().getPlayerList().stream().filter(playerListEntry -> {
                return playerListEntry.getProfile().id().equals(Mc.INSTANCE.getPlayer().getUuid());
            }).findFirst().map((v0) -> {
                return v0.getLatency();
            }).orElse(0)).intValue();
        }
        return 0;
    }

    @Override
    public float width() {
        return this.computedWidth * this.scale.currentValue();
    }

    @Override
    public float height() {
        return this.computedHeight * this.scale.currentValue();
    }
}
