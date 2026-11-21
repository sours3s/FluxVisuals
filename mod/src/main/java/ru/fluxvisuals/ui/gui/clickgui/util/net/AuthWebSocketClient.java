package ru.fluxvisuals.ui.gui.clickgui.util.net;

import java.net.URI;
import java.nio.ByteBuffer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class AuthWebSocketClient extends WebSocketClient {
    public final WebSocketPacketReader packetReader;
    public final String authToken;

    public AuthWebSocketClient(URI uri, WebSocketPacketReader webSocketPacketReader, String str) {
        super(uri);
        this.packetReader = webSocketPacketReader;
        this.authToken = str;
    }

    public void onMessage(ByteBuffer byteBuffer) {
        try {
            this.packetReader.read(PacketCodec.fromBytes(byteBuffer.array()), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onOpen(ServerHandshake serverHandshake) {
        sendPacket(new AuthHandshakePacket(this.authToken));
    }

    public void sendPacket(SocketPacket socketPacket) {
        send(PacketCodec.sendData(socketPacket));
    }

    public void onMessage(String str) {
    }

    public void onClose(int i, String str, boolean z) {
    }

    public void onError(Exception exc) {
    }
}
