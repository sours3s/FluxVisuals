package ru.fluxvisuals.ui.gui.clickgui.util.model;

public final class OffsetInputContext extends InputEventContext {
    public final InputEventContext delegate;
    public final float offsetX;
    public final float offsetY;

    public OffsetInputContext(InputEventContext inputEventContext, float f, float f2) {
        super(inputEventContext.inputEvent(), inputEventContext.currentMousePosition(), inputEventContext.scaleFactor());
        this.delegate = inputEventContext;
        this.offsetX = f;
        this.offsetY = f2;
    }

    @Override
    public PixelPoint currentMousePosition() {
        return this.delegate.currentMousePosition();
    }

    @Override
    public PixelPoint logicalMousePosition() {
        PixelPoint pixelPoint = this.delegate.logicalMousePosition();
        return new PixelPoint(pixelPoint.x() + this.offsetX, pixelPoint.y() + this.offsetY);
    }

    @Override
    public boolean inArea(float f, float f2, float f3, float f4) {
        PixelPoint pixelPoint = logicalMousePosition();
        return ((float) pixelPoint.x()) >= f && ((float) pixelPoint.y()) >= f2 && ((float) pixelPoint.x()) <= f + f3 && ((float) pixelPoint.y()) <= f2 + f4;
    }
}
