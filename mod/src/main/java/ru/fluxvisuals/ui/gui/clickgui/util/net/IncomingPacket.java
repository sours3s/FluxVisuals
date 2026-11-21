package ru.fluxvisuals.ui.gui.clickgui.util.net;

public interface IncomingPacket {
    void decode(PacketBuffer packetBuffer);

    void handle();
}
