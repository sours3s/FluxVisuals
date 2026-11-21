package ru.fluxvisuals.ui.gui.clickgui.util.net;

public class LoginPacket implements OutgoingPacket {
    public final String username;
    public final String password;
    public final byte[] token;

    public LoginPacket(String str, String str2, byte[] bArr) {
        this.username = str;
        this.password = str2;
        this.token = bArr;
    }

    @Override
    public int id() {
        return 1;
    }

    @Override
    public void encode(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.username);
        packetBuffer.writeString(this.password != null ? this.password : "");
        packetBuffer.writeByteArray(this.token);
    }
}
