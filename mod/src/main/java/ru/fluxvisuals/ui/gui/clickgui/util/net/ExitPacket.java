package ru.fluxvisuals.ui.gui.clickgui.util.net;

public class ExitPacket implements IncomingPacket {
    @Override
    public void decode(PacketBuffer packetBuffer) {
    }

    @Override
    public void handle() {
        System.exit(-1);
    }
}
