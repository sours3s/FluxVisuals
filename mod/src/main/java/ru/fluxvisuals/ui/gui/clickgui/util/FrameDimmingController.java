package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.AbstractFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;

public class FrameDimmingController {
    public final AnimatedFloat dimAnimation = new AnimatedFloat(400, Easings.EASE_IN_OUT_CUBIC);
    public AbstractFrame focusedFrame = null;
    public long focusStartTime = -1;
    public static final long FOCUS_DURATION = 2000;

    public void focusFrame(AbstractFrame abstractFrame) {
        if (abstractFrame == null) {
            return;
        }
        this.focusedFrame = abstractFrame;
        this.focusStartTime = System.currentTimeMillis();
        this.dimAnimation.destination(1.0f);
    }

    public void clearFocus() {
        this.focusedFrame = null;
        this.focusStartTime = -1L;
        this.dimAnimation.destination(0.0f);
    }

    public void animate(WeightedEngine weightedEngine) {
        if (this.focusedFrame != null && this.focusStartTime > 0 && System.currentTimeMillis() - this.focusStartTime >= FOCUS_DURATION) {
            clearFocus();
        }
        this.dimAnimation.animate(weightedEngine);
    }

    public float getDimmingForFrame(AbstractFrame abstractFrame) {
        if (this.focusedFrame == null || abstractFrame == this.focusedFrame) {
            return 0.0f;
        }
        return this.dimAnimation.animatedValue();
    }

    public boolean isDimming() {
        return this.dimAnimation.animatedValue() > 0.01f;
    }

    public AbstractFrame focusedFrame() {
        return this.focusedFrame;
    }
}
