package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ChatLimitType;

public class ChatLimitEvent extends CancellableEvent {
    public final ChatLimitType type;

    public ChatLimitType getType() {
        return this.type;
    }

    public ChatLimitEvent(ChatLimitType chatLimitType) {
        this.type = chatLimitType;
    }
}
