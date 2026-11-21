package ru.fluxvisuals.ui.gui.clickgui.util.net;

import java.nio.charset.StandardCharsets;

public class ServerMessagePacket implements IncomingPacket {
    public String text;

    @Override
    public void decode(PacketBuffer packetBuffer) {
        byte[] bArr = new byte[packetBuffer.readableBytes()];
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = (byte) packetBuffer.readByte();
        }
        this.text = new String(bArr, StandardCharsets.UTF_8);
    }

    @Override
    public void handle() {
    }
}
