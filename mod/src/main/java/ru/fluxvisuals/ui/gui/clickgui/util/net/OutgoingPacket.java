package ru.fluxvisuals.ui.gui.clickgui.util.net;

public interface OutgoingPacket {
    int id();

    void encode(PacketBuffer packetBuffer);
}
