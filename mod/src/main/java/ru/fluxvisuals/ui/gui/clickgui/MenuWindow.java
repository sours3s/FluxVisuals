package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.net.InputInterceptor;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayManager;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.net.TooltipService;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.WindowController;

public class MenuWindow extends AbstractWindow {
    public static float MENU_WIDTH = 920.0f;
    public static float MENU_HEIGHT = 550.0f;
    public static float EXPANDED_HEADER_HEIGHT = 256.0f;
    public static float COLLAPSED_HEADER_HEIGHT = 60.0f;
    public static final float[] scaleOptions = {0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f};
    public final GlTexture backgroundTexture;
    public boolean needsCentering;
    public final ToggleAnimator openAnimation;
    public final MenuHeaderContainer headerContainer;

    public final MenuContentArea contentArea;

    public final DraggableContainer draggableBehavior;

    public final SearchOverlay searchContainer;

    public final ActionConfirmDialog actionConfirmationDialogContainer;

    public final PlainContainer tooltipLayer;

    public final TooltipService tooltipService;

    public final OverlayManager priorityOverlayHandler;
    public boolean menuOpen;

    public MenuWindow() {
        super(MENU_WIDTH, MENU_HEIGHT);
        this.backgroundTexture = new GlTexture(new ClasspathResource("/textures/background.png"));
        this.needsCentering = true;
        this.openAnimation = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
        this.headerContainer = new MenuHeaderContainer();
        this.draggableBehavior = new DraggableContainer();
        this.searchContainer = new SearchOverlay();
        this.actionConfirmationDialogContainer = new ActionConfirmDialog();
        this.tooltipLayer = new PlainContainer();
        this.tooltipService = new TooltipService(this.tooltipLayer);
        this.priorityOverlayHandler = new OverlayManager();
        this.menuOpen = false;
        this.contentArea = new MenuContentArea();
        this.visible = false;
        addChild(this.draggableBehavior);
        addChild(this.contentArea);
        addChild(this.tooltipLayer);
        addChild(this.headerContainer);
        addChild(this.searchContainer);
        addChild(this.actionConfirmationDialogContainer);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        if (this.needsCentering) {
            center(layoutScaleContext);
            this.needsCentering = false;
        }
        if (this.openAnimation.isZero()) {
            this.headerContainer.visible(false);
            this.contentArea.visible(false);
            this.draggableBehavior.visible(false);
            return;
        }
        this.headerContainer.setPosition(x(), y());
        this.headerContainer.visible(true);
        this.contentArea.setPosition(x(), y() + COLLAPSED_HEADER_HEIGHT);
        this.contentArea.visible(true);
        this.searchContainer.setPosition(x(), y());
        this.searchContainer.setSize(MENU_WIDTH, MENU_HEIGHT);
        this.actionConfirmationDialogContainer.setPosition(x(), y());
        super.layout(layoutScaleContext);
        setSize(MENU_WIDTH, MENU_HEIGHT);
        this.draggableBehavior.setPosition(x(), y());
        this.draggableBehavior.setSize(width(), height());
        this.draggableBehavior.visible(true);
        super.onMenuDrag(this.draggableBehavior.isDragging());
    }

    @Override
    public void render(DrawCtx drawCtx) {
        InputInterceptor inputInterceptor = drawCtx.window();
        FluxVisuals.INSTANCE.windowController().backgroundAlpha(this.openAnimation.isZero() ? 0 : Math.round(this.openAnimation.smoothAnimation() * 150.0f));
        if (!this.openAnimation.isZero()) {
            if (this.visible) {
                inputInterceptor.interceptCursorfScreenNotPresent(true);
                inputInterceptor.interceptKeyboardIfScreenPresent(true);
            }
            float fMethod004 = slideOffset();
            ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
            colorStack115.push();
            drawCtx.matrixStack().push();
            if (!this.openAnimation.isOne()) {
                drawCtx.matrixStack().translate(0.0f, drawCtx.layoutContext().toPhysical(fMethod004), 0.0f);
                colorStack115.alpha(this.openAnimation.smoothAnimation());
            }
            renderBackground(drawCtx);
            boolean zExpandedState = this.headerContainer.expandedState();
            renderChild(this.draggableBehavior, drawCtx);
            renderChild(this.contentArea, drawCtx);
            if (zExpandedState) {
                this.priorityOverlayHandler.renderPopup(drawCtx);
                renderChild(this.tooltipLayer, drawCtx);
            }
            renderChild(this.headerContainer, drawCtx);
            if (!zExpandedState) {
                this.priorityOverlayHandler.renderPopup(drawCtx);
                renderChild(this.tooltipLayer, drawCtx);
            }
            renderChild(this.searchContainer, drawCtx);
            renderChild(this.actionConfirmationDialogContainer, drawCtx);
            colorStack115.pop();
            drawCtx.matrixStack().pop();
            if (inputInterceptor.cursorVisible()) {
                return;
            }
            this.openAnimation.state(false);
            this.menuOpen = false;
            close();
        }
    }

    public void renderChild(Widget widget, DrawCtx drawCtx) {
        if (widget instanceof WidgetParent) {
            ((WidgetParent) widget).render(drawCtx);
        }
    }

    public void renderBackground(DrawCtx drawCtx) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.colorStack();
        drawCtx.fillOutlinedRoundedRect(x(), y(), width(), height(), 8.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(700).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb()));
        drawCtx.texture(this.backgroundTexture, x(), y(), MENU_WIDTH, height(), colorStack115.computeColor(16777215));
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimation.animate(weightedEngine);
        boolean zIsZero = this.openAnimation.isZero();
        this.visible = !zIsZero;
        if (!zIsZero) {
            super.animation(weightedEngine);
            return;
        }
        this.headerContainer.visible(false);
        this.contentArea.visible(false);
        this.draggableBehavior.visible(false);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = this.menuOpen;
        boolean zHandleInput = z;
        boolean zExpandedState = this.headerContainer.expandedState();
        if (z2) {
            boolean zHandleInput2 = zHandleInput | this.actionConfirmationDialogContainer.handleInput(inputEventContext, zHandleInput);
            boolean zHandleInput3 = zHandleInput2 | this.searchContainer.handleInput(inputEventContext, zHandleInput2);
            if (!zExpandedState) {
                zHandleInput3 |= this.priorityOverlayHandler.handlePopupInput(inputEventContext, zHandleInput3);
            }
            boolean zHandleInput4 = zHandleInput3 | this.headerContainer.handleInput(inputEventContext, zHandleInput3);
            if (zExpandedState) {
                zHandleInput4 |= this.priorityOverlayHandler.handlePopupInput(inputEventContext, zHandleInput4);
            }
            boolean zHandleInput5 = zHandleInput4 | this.contentArea.handleInput(inputEventContext, zHandleInput4);
            zHandleInput = zHandleInput5 | this.draggableBehavior.handleInput(inputEventContext, zHandleInput5);
        }
        if (!zHandleInput) {
            InputEvent inputEvent691 = inputEventContext.inputEvent();
            if (inputEvent691 instanceof KeyInput) {
                KeyInput keyInput = (KeyInput) inputEvent691;
                WindowController windowController686 = FluxVisuals.INSTANCE.windowController();
                if (keyInput.keyAction().press()) {
                    int iKeyCode = keyInput.keyCode();
                    if (iKeyCode == 260 || iKeyCode == 344) {
                        this.menuOpen = !this.menuOpen;
                        this.openAnimation.state(this.menuOpen);
                        if (this.menuOpen) {
                            windowController686.showCursor(windowController686.window());
                            windowController686.recenterMouse();
                        } else {
                            windowController686.revertCursor(windowController686.window());
                            windowController686.recenterMouse();
                            net.minecraft.client.MinecraftClient.getInstance().setScreen(null);
                        }
                        toggle();
                        return true;
                    }
                    if (iKeyCode == 256 && visible()) {
                        this.menuOpen = false;
                        this.openAnimation.state(false);
                        windowController686.revertCursor(windowController686.window());
                        windowController686.recenterMouse();
                        close();
                        net.minecraft.client.MinecraftClient.getInstance().setScreen(null);  // закрыть экран при ESC
                        return true;
                    }
                }
            }
        }
        return zHandleInput;
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        if (this.openAnimation.isZero()) {
            return;
        }
        OverlayCommandQueue class677Var2 = new OverlayCommandQueue();
        super.collectBlurElements(class677Var2);
        overlayCommandQueue.record(drawCtx -> {
            ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
            colorStack115.push();
            drawCtx.matrixStack().push();
            if (!this.openAnimation.isOne()) {
                drawCtx.matrixStack().translate(0.0f, drawCtx.layoutContext().toPhysical(slideOffset()), 0.0f);
                colorStack115.alpha(this.openAnimation.smoothAnimation());
            }
            renderBackground(drawCtx);
            class677Var2.renderRecorded(drawCtx);
            drawCtx.matrixStack().pop();
            colorStack115.pop();
        });
    }

    @Override
    public void close() {
        ColorPickerWindow colorPickerWindow = (ColorPickerWindow) FluxVisuals.INSTANCE.windowController().getWindow(ColorPickerWindow.class);
        if (colorPickerWindow != null) {
            colorPickerWindow.closePicker();
        }
        this.draggableBehavior.setDragging(false);
        super.close();
    }

    public void collapseExpandedHeader() {
        if (this.headerContainer.expandedState()) {
            this.headerContainer.exchange();
        }
    }

    public void requestCentering() {
        this.needsCentering = true;
    }

    public float slideOffset() {
        return (1.0f - this.openAnimation.smoothAnimation()) * 35.0f;
    }

    public MenuContentArea contentArea() {
        return this.contentArea;
    }

    public DraggableContainer draggableBehavior() {
        return this.draggableBehavior;
    }

    public SearchOverlay searchContainer() {
        return this.searchContainer;
    }

    public ActionConfirmDialog actionConfirmationDialogContainer() {
        return this.actionConfirmationDialogContainer;
    }

    public TooltipService tooltipService() {
        return this.tooltipService;
    }

    public OverlayManager priorityOverlayHandler() {
        return this.priorityOverlayHandler;
    }
}
