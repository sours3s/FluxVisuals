package ru.fluxvisuals.ui.clickgui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import ru.fluxvisuals.ui.fluxgui.client.gui.screen.ClickGui;
import ru.fluxvisuals.ui.fluxgui.module.ModuleManager;

/** Тонкая обёртка, чтобы правый Shift из KeyboardMixin открывал ClickGUI FluxVisuals. */
public final class FluxClickGuiScreen extends Screen {
    private final ClickGui clickGui;

    public FluxClickGuiScreen() {
        super(Text.literal("FluxVisuals ClickGui"));
        ModuleManager.INSTANCE.sync();
        this.clickGui = new ClickGui();
    }

    @Override
    protected void init() {
        super.init();
        this.clickGui.prepareForWrapper(this.width, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.clickGui.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return this.clickGui.keyPressed(input) || super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        return this.clickGui.keyReleased(input) || super.keyReleased(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return this.clickGui.charTyped(input) || super.charTyped(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return this.clickGui.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return this.clickGui.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return this.clickGui.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.clickGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        this.clickGui.requestClose();
    }

    @Override
    public void removed() {
        this.clickGui.removed();
        super.removed();
    }
}
