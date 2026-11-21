package ru.fluxvisuals.ui.fluxgui.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Заглушка раздела чата GUI FluxVisuals. */
public final class IrcChatMessengerScreen extends Screen {
    public IrcChatMessengerScreen() {
        super(Text.literal("FluxVisuals Chat"));
    }
}
