package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.net.IncomingPacket;
import ru.fluxvisuals.ui.gui.clickgui.util.net.PacketBuffer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class InboundPacketHandler extends SimpleChannelInboundHandler<byte[]> {
    public void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bArr) {
        if (bArr.length == 0) {
            return;
        }
        int i = bArr[0] & 255;
        IncomingPacket incomingPacket = PacketRegistry.createIncoming(i);
        if (incomingPacket == null) {
            System.out.println("[Client] Unknown packet: 0x" + Integer.toHexString(i));
        } else {
            incomingPacket.decode(PacketBuffer.wrap(bArr, 1));
            incomingPacket.handle();
        }
    }

    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable th) {
    }
}
