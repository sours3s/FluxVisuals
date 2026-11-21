package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DragRenderContext;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.setting.ModeSetting;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.ModuleStateEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScreenClickContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.type.NotificationDirection;
import ru.fluxvisuals.ui.gui.clickgui.util.type.NotificationType;
import ru.fluxvisuals.ui.gui.clickgui.util.StencilBufferUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Stopwatch;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class NotificationWidget extends Draggable {
    public float width;
    public float height;
    public final GlTexture backgroundTexture;
    public static final float rowHeight = 27.0f;
    public static final int horizontalPadding = 8;
    public static final int iconSize = 12;
    public static final int verticalPadding = 8;
    public Notification previewNotification;
    public final AnimatedFloat previewAnimation;
    public static final Translation placeholderText = Lang.NOTIFICATIONS_PLACEHOLDER;
    public int previewTypeIndex;
    public final Stopwatch previewCycleStopwatch;
    public final List<ModuleStateEvent> pendingModuleEvents;
    public final Stopwatch moduleEventStopwatch;
    public final MsdfFont font;

    public final ModeSetting<NotificationDirection> direction;

    public final BooleanSetting itemPickUp;

    public NotificationWidget(BooleanSupplier booleanSupplier) {
        super("Notifications", booleanSupplier);
        this.backgroundTexture = new GlTexture(new ClasspathResource("/textures/hud_background.png"));
        this.previewAnimation = new AnimatedFloat(200, Easings.LINEAR);
        this.previewTypeIndex = 0;
        this.previewCycleStopwatch = new Stopwatch();
        this.pendingModuleEvents = new ArrayList();
        this.moduleEventStopwatch = new Stopwatch();
        this.font = Fonts.INTER_BOLD.get();
        this.direction = new ModeSetting(Lang.WIDGET_NOTIFICATIONS_DIRECTION, Lang.WIDGET_NOTIFICATIONS_DIRECTION_DESC).values(NotificationDirection.class);
        this.itemPickUp = new BooleanSetting(Lang.WIDGET_NOTIFICATIONS_ITEM_PICK_UP, Lang.WIDGET_NOTIFICATIONS_ITEM_PICK_UP_DESC);
        setExcludeFromSnapGrid(true);
        addSettings(this.direction, this.itemPickUp);
        this.y = 50.0f;
    }

    @Override
    public void layout(DragRenderContext dragRenderContext) {
    }

    @Override
    public void render(DragRenderContext dragRenderContext) throws MatchException {
        boolean z;
        if (isVisible()) {
            MatrixStack matrixStack = dragRenderContext.matrixStack();
            DrawEngine drawEngine154 = dragRenderContext.drawEngine();
            ColorStack colorStack115 = drawEngine154.colorStack();
            float fScreenHeight = dragRenderContext.resolution().screenHeight() / 2.0f;
            switch (((NotificationDirection) this.direction.currentValue()).ordinal()) {
                case 0:
                    z = this.y + (this.height / 2.0f) > fScreenHeight;
                    break;
                case 1:
                    z = true;
                    break;
                case 2:
                    z = false;
                    break;
                default:
                    throw new MatchException((String) null, (Throwable) null);
            }
            boolean z2 = z;
            float f = 0.0f;
            float fMax = 0.0f;
            List<Notification> notifications = FluxVisuals.INSTANCE.notificationRepository().getNotifications();
            if (this.previewNotification != null) {
                float fAnimatedValue = this.previewNotification.valueAnimation().animatedValue();
                if (fAnimatedValue > 0.0f) {
                    float widthWithStyles = 36.0f + this.font.getWidthWithStyles(this.previewNotification.message(), 12.0f) + 4.0f;
                    this.x = (dragRenderContext.resolution().screenWidth() / 2.0f) - (widthWithStyles / 2.0f);
                    renderNotification(dragRenderContext.drawEngine(), dragRenderContext.theme().palette(), dragRenderContext.matrixStack(), dragRenderContext.drawEngine().colorStack(), this.previewNotification, this.x, this.y, widthWithStyles, rowHeight, fAnimatedValue);
                    this.width = widthWithStyles;
                    this.height = rowHeight;
                    return;
                }
            }
            for (int i = 0; i < notifications.size(); i++) {
                Notification notification = z2 ? notifications.get((notifications.size() - 1) - i) : notifications.get(i);
                float fAnimatedValue2 = notification.valueAnimation().animatedValue();
                if (fAnimatedValue2 > 0.0f) {
                    float widthWithStyles2 = 28.0f + this.font.getWidthWithStyles(notification.message(), 12.0f) + 8.0f + 2.0f;
                    this.x = (dragRenderContext.resolution().screenWidth() / 2.0f) - (widthWithStyles2 / 2.0f);
                    fMax = Math.max(fMax, widthWithStyles2);
                    renderNotification(drawEngine154, dragRenderContext.theme().palette(), matrixStack, colorStack115, notification, this.x, z2 ? this.y - f : this.y + f, widthWithStyles2, rowHeight, fAnimatedValue2);
                    f += 30.0f * fAnimatedValue2;
                }
            }
            this.width = fMax;
            this.height = rowHeight;
        }
    }

    public void renderNotification(DrawEngine drawEngine, ThemePalette themePalette, MatrixStack matrixStack, ColorStack colorStack, Notification notification, float f, float f2, float f3, float f4, float f5) {
        int iComputeColor = colorStack.computeColor(themePalette.surfaceBackground().tone(801).argb(), f5);
        int iComputeColor2 = colorStack.computeColor(themePalette.surfaceBackground().tone(900).argb(), f5);
        drawEngine.roundedRectangle(matrixStack.peek().getPositionMatrix(), f, f2, f3, f4, 8.0f, 2.5f, colorStack.computeColor(themePalette.surfaceOutline().tone(600).argb(), f5), iComputeColor2, iComputeColor2, iComputeColor, iComputeColor);
        drawEngine.texture(matrixStack.peek().getPositionMatrix(), f, f2, f3, f4, drawEngine.bindTexture(this.backgroundTexture.textureWithSTB()), colorStack.computeColor(f5, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK));
        float f6 = f + 8.0f;
        if (notification.itemStack() == null || notification.itemStack().isEmpty()) {
            drawEngine.texture(matrixStack.peek().getPositionMatrix(), f6, (f2 + (f4 / 2.0f)) - 6.0f, 12.0f, 12.0f, drawEngine.bindTexture(notification.type().getTexture().textureWithSTB()), resolve(notification.type(), themePalette, colorStack, f5));
        } else {
            drawEngine.itemStack(matrixStack.peek().getPositionMatrix(), notification.itemStack(), f6, (f2 + (f4 / 2.0f)) - 6.0f, 0.375f, f5);
        }
        drawEngine.msdfText(matrixStack.peek().getPositionMatrix(), this.font, notification.message(), f6 + 20.0f, (f2 + (f4 / 2.0f)) - (this.font.getHeight(12.0f) / 2.0f), 12.0f, colorStack.computeColor(themePalette.text().tone(400).argb(), f5));
    }

    @Override
    public void animate(WeightedEngine weightedEngine) {
        if (isVisible()) {
            FluxVisuals.INSTANCE.notificationRepository().animate(weightedEngine);
            this.previewAnimation.animate(weightedEngine);
        }
    }

    @Override
    public void update() {
        if (isVisible()) {
            synchronized (this.pendingModuleEvents) {
                if (!this.pendingModuleEvents.isEmpty()) {
                    ArrayList<ModuleStateEvent> arrayList = new ArrayList(this.pendingModuleEvents);
                    this.pendingModuleEvents.clear();
                    TreeSet treeSet = new TreeSet(String.CASE_INSENSITIVE_ORDER);
                    TreeSet treeSet2 = new TreeSet(String.CASE_INSENSITIVE_ORDER);
                    for (ModuleStateEvent moduleStateEvent : arrayList) {
                        String name = moduleStateEvent.module().getName();
                        if (moduleStateEvent.moduleState()) {
                            treeSet.add(name);
                        } else {
                            treeSet2.add(name);
                        }
                    }
                    if (!treeSet.isEmpty()) {
                        List list = treeSet.stream().limit(4).toList();
                        FluxVisuals.INSTANCE.notificationRepository().post(NotificationType.MODULE_ENABLED, (Text) Text.literal(list.size() == 1 ? Lang.NOTIFICATIONS_ENABLED_SINGLE.effective().replace("{module}", (CharSequence) list.getFirst()) : Lang.NOTIFICATIONS_ENABLED_MULTIPLE.effective().replace("{modules}", String.join(", ", list)).replace("{extra}", treeSet.size() > 4 ? Lang.NOTIFICATIONS_MORE.effective().replace("{count}", String.valueOf(treeSet.size() - 4)) : "")), 3L, TimeUnit.SECONDS);
                        treeSet.clear();
                    }
                    if (!treeSet2.isEmpty()) {
                        List list2 = treeSet2.stream().limit(4).toList();
                        FluxVisuals.INSTANCE.notificationRepository().post(NotificationType.MODULE_DISABLED, (Text) Text.literal(list2.size() == 1 ? Lang.NOTIFICATIONS_DISABLED_SINGLE.effective().replace("{module}", (CharSequence) list2.getFirst()) : Lang.NOTIFICATIONS_DISABLED_MULTIPLE.effective().replace("{modules}", String.join(", ", list2)).replace("{extra}", treeSet2.size() > 4 ? Lang.NOTIFICATIONS_MORE.effective().replace("{count}", String.valueOf(treeSet2.size() - 4)) : "")), 3L, TimeUnit.SECONDS);
                        treeSet2.clear();
                    }
                }
            }
            FluxVisuals.INSTANCE.notificationRepository().tick();
            boolean z = Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen;
            boolean z2 = !FluxVisuals.INSTANCE.notificationRepository().getNotifications().isEmpty();
            if (!z || z2) {
                if (this.previewNotification != null) {
                    this.previewAnimation.destination(0.0f);
                    if (this.previewAnimation.animatedValue() <= 0.01f) {
                        this.previewNotification = null;
                        return;
                    }
                    return;
                }
                return;
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (this.previewNotification == null) {
                this.previewAnimation.destination(1.0f);
                this.previewNotification = new Notification(this.previewAnimation, NotificationType.INFO, Text.literal(placeholderText.effective()), jCurrentTimeMillis, Long.MAX_VALUE, null);
                this.previewCycleStopwatch.reset();
                return;
            }
            this.previewAnimation.destination(1.0f);
            if (this.previewCycleStopwatch.hasElapsed(700L, TimeUnit.MILLISECONDS)) {
                NotificationType[] notificationTypes = NotificationType.values();
                this.previewTypeIndex = (this.previewTypeIndex + 1) % notificationTypes.length;
                this.previewNotification = new Notification(this.previewAnimation, notificationTypes[this.previewTypeIndex], Text.literal(placeholderText.effective()), jCurrentTimeMillis, Long.MAX_VALUE, null);
                this.previewCycleStopwatch.reset();
            }
        }
    }

    public int resolve(NotificationType notificationType, ThemePalette themePalette, ColorStack colorStack, float f) throws MatchException {
        switch (notificationType) {
            case INFO:
                return colorStack.computeColor(themePalette.accent().argb(), f);
            case SUCCESS:
            case MODULE_ENABLED:
                return colorStack.computeColor(ThemePalette.deepGreen.argb(), f);
            case WARNING:
                return colorStack.computeColor(ThemePalette.darkGolden.argb(), f);
            case ERROR:
            case MODULE_DISABLED:
                return colorStack.computeColor(ThemePalette.darkRed.argb(), f);
            case EVENT:
                return colorStack.computeColor(themePalette.text().tone(50).argb(), f);
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    public boolean click(ScreenClickContext screenClickContext, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput mouseMoveInput, boolean z) {
        return z && !mouseMoveInput.intercepted();
    }

    public BooleanSetting getItemPickUp() {
        return this.itemPickUp;
    }
}
