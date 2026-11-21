package ru.fluxvisuals.ui.gui.util.model;
import ru.fluxvisuals.ui.gui.events.impl.InputEvent;

public class InputEventContext {
    public final InputEvent inputEvent;
    public final float scaleFactor;
    public PixelPoint currentMousePosition;

    public InputEventContext(InputEvent inputEvent691, PixelPoint pixelPoint, float f) throws MatchException {
        this.inputEvent = inputEvent691;
        this.scaleFactor = f;
        this.currentMousePosition = pixelPoint;
        if (inputEvent691 instanceof CursorMoveInput) {
            try {
                this.currentMousePosition = ((CursorMoveInput) inputEvent691).mousePosition();
            } catch (Throwable th) {
                throw new MatchException(th.toString(), th);
            }
        }
    }

    public PixelPoint currentMousePosition() {
        return this.currentMousePosition;
    }

    public InputEventContext withMouseOffset(float f, float f2) {
        // OffsetInputContext not ported - return self for now
        return this;
    }

    public PixelPoint logicalMousePosition() {
        return toLogical(this.currentMousePosition, this.scaleFactor);
    }

    public boolean inArea(float f, float f2, float f3, float f4) {
        PixelPoint pixelPoint = logicalMousePosition();
        return ((float) pixelPoint.x()) >= f && ((float) pixelPoint.y()) >= f2 && ((float) pixelPoint.x()) <= f + f3 && ((float) pixelPoint.y()) <= f2 + f4;
    }

    public boolean inArea(PixelPoint pixelPoint, float f, float f2, float f3, float f4) {
        PixelPoint logical = toLogical(pixelPoint, this.scaleFactor);
        return ((float) logical.x()) >= f && ((float) logical.y()) >= f2 && ((float) logical.x()) <= f + f3 && ((float) logical.y()) <= f2 + f4;
    }

    public boolean inPhysicalArea(PixelPoint pixelPoint, float f, float f2, float f3, float f4) {
        return ((float) pixelPoint.x()) >= f && ((float) pixelPoint.y()) >= f2 && ((float) pixelPoint.x()) <= f + f3 && ((float) pixelPoint.y()) <= f2 + f4;
    }

    public PixelPoint toLogical(PixelPoint pixelPoint, float f) {
        return (f <= 0.0f || f == 1.0f) ? pixelPoint : new PixelPoint(pixelPoint.x() / f, pixelPoint.y() / f);
    }

    public InputEvent inputEvent() {
        return this.inputEvent;
    }

    public float scaleFactor() {
        return this.scaleFactor;
    }
}
