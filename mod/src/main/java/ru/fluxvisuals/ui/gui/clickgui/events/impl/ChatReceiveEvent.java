package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.ChatDecorator;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ChatMessageType;

import net.minecraft.text.Text;

public class ChatReceiveEvent extends CancellableEvent {
    public final String message;
    public final Text textData;

    public final ChatMessageType type;

    public final ChatDecorator applyChatDecoration;

    public String message() {
        return this.message;
    }

    public Text textData() {
        return this.textData;
    }

    public ChatMessageType type() {
        return this.type;
    }

    public ChatDecorator applyChatDecoration() {
        return this.applyChatDecoration;
    }

    public ChatReceiveEvent(String str, Text text, ChatMessageType chatMessageType, ChatDecorator chatDecorator) {
        this.message = str;
        this.textData = text;
        this.type = chatMessageType;
        this.applyChatDecoration = chatDecorator;
    }
}
