package ru.fluxvisuals.module.hud;

import java.util.ArrayDeque;
import java.util.Deque;

/** Клики в секунду (ЛКМ + ПКМ). */
public class CpsModule extends SimpleHudModule {
    private final Deque<Long> clicks = new ArrayDeque<>();
    private boolean wasDown;

    public CpsModule() {
        super("CPS", "Клики в секунду");
    }

    @Override
    public void onTick() {
        var mc = mc();
        if (mc.player == null) return;
        boolean down = mc.options.attackKey.isPressed() || mc.options.useKey.isPressed();
        if (down && !wasDown) {
            clicks.addLast(System.currentTimeMillis());
        }
        wasDown = down;
    }

    @Override
    protected String getText() {
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && clicks.peekFirst() < now - 1000) clicks.pollFirst();
        return "CPS " + clicks.size();
    }
}
