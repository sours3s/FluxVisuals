package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.events.api.Event;
import ru.fluxvisuals.ui.gui.clickgui.events.api.EventCallback;
import ru.fluxvisuals.ui.gui.clickgui.util.type.EventPriority;
import ru.fluxvisuals.ui.gui.clickgui.events.api.RegisteredListener;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDispatcher {
    public final Map<Class<? extends Event>, List<RegisteredListener<?>>> listeners = new HashMap();

    public <T extends Event> void dispatch(T r8) {
        List<RegisteredListener<?>> list = this.listeners.get(r8.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }
        int n = list.size();
        for (int i = 0; i < n; i++) {
            RegisteredListener<?> registeredListener = list.get(i);
            EventCallback eventCallback = registeredListener.callback();
            try {
                eventCallback.call(r8);
            } catch (Throwable th) {
                FluxVisuals.LOGGER.error("EventDispatcher: exception in callback {} (priority={}) for event {}", eventCallback.getClass().getName(), registeredListener.priority(), r8.getClass().getName(), th);
            }
            if (r8 instanceof CancellableEvent && ((CancellableEvent) r8).isStopProgression()) {
                break;
            }
        }
    }

    public <T extends Event> void register(Class<T> cls, EventCallback<T> eventCallback) {
        register(cls, eventCallback, EventPriority.NORMAL);
    }

    public <T extends Event> void register(Class<T> cls, EventCallback<T> eventCallback, EventPriority eventPriority) {
        List<RegisteredListener<?>> listComputeIfAbsent = this.listeners.computeIfAbsent(cls, cls2 -> {
            return new ArrayList();
        });
        listComputeIfAbsent.add(new RegisteredListener<>(eventCallback, eventPriority));
        listComputeIfAbsent.sort(Comparator.comparing(registeredListener -> {
            return registeredListener.priority();
        }));
    }

    public <T extends Event> void unregister(Class<T> cls, EventCallback<T> eventCallback) {
        List<RegisteredListener<?>> list = this.listeners.get(cls);
        if (list != null) {
            list.removeIf(registeredListener -> {
                return registeredListener.callback() == eventCallback;
            });
        }
    }
}
