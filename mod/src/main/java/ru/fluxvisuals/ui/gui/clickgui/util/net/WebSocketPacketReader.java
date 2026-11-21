package ru.fluxvisuals.ui.gui.clickgui.util.net;

import org.springframework.web.socket.WebSocketSession;

public interface WebSocketPacketReader {
    void read(SocketPacket socketPacket, WebSocketSession webSocketSession);
}
