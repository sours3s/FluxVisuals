package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class TextRender$1 extends LinkedHashMap<String, Float> {
    TextRender$1(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }

    @Override
    protected boolean removeEldestEntry(Entry<String, Float> eldest) {
        return this.size() > 2048;
    }
}
