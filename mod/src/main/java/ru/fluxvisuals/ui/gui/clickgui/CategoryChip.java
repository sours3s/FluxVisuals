package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ModuleCategory;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;

public class CategoryChip {
    public final ModuleCategory category;
    public final ToggleAnimator hoverAnimator = new ToggleAnimator(160, Easings.LINEAR);
    public final ToggleAnimator selectedAnimator = new ToggleAnimator(180, Easings.EASE_IN_OUT_CUBIC);
    public float x;
    public float y;
    public float width;
    public float height;

    final CategoryChipBar parentBar;

    public CategoryChip(CategoryChipBar categoryChipBar, ModuleCategory moduleCategory) {
        this.parentBar = categoryChipBar;
        this.category = moduleCategory;
    }

    public ModuleCategory category() {
        return this.category;
    }

    public void bounds(float f, float f2, float f3, float f4) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
    }

    public boolean contains(float f, float f2) {
        return f >= this.x && f <= this.x + this.width && f2 >= this.y && f2 <= this.y + this.height;
    }

    public void selected(boolean z) {
        this.selectedAnimator.state(z);
    }

    public void render(DrawCtx drawCtx, ThemePalette themePalette, ColorStack colorStack) {
        float fSmoothAnimation = this.hoverAnimator.smoothAnimation();
        float fSmoothAnimation2 = this.selectedAnimator.smoothAnimation();
        int iComputeColor = colorStack.computeColor(themePalette.surfaceOutline().tone(400).argb());
        colorStack.computeColor(themePalette.surfaceOutline().tone(300).argb());
        drawCtx.fillOutlinedRoundedRect(this.x, this.y, this.width, this.height, 14.0f, 2.5f, iComputeColor, colorStack.computeColor(themePalette.surfaceBackground().tone(900).argb(), 0));
        GlTexture glTexture = this.category.icon();
        glTexture.setDimensions(12, 12);
        int iInterpolate = colorStack.interpolate(colorStack.interpolate(colorStack.computeColor(themePalette.text().tone(500).argb()), colorStack.computeColor(themePalette.text().tone(300).argb()), fSmoothAnimation), colorStack.computeColor(themePalette.text().tone(200).argb()), fSmoothAnimation2);
        float f = this.x + 12.0f;
        drawCtx.texture(glTexture, f, (this.y + (this.height / 2.0f)) - 6.0f, 12.0f, 12.0f, iInterpolate);
        drawCtx.text(this.parentBar.labelFont, this.category.displayName(), 12, f + 12.0f + 5.0f, (this.y + (this.height / 2.0f)) - (this.parentBar.labelFont.getHeight(12.0f) / 2.0f), iInterpolate);
    }
}
