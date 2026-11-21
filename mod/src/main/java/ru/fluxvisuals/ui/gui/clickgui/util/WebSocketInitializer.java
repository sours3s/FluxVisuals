package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.net.AuthResponsePacket;
import ru.fluxvisuals.ui.gui.clickgui.config.CrashConfig;
import ru.fluxvisuals.ui.gui.clickgui.util.net.WebSocketPacketReader;

import java.util.concurrent.CountDownLatch;

public class WebSocketInitializer {
    public CrashConfig reference;

    public final CountDownLatch latch = new CountDownLatch(1);

    public final WebSocketPacketReader packetReader = (socketPacket, webSocketSession) -> {
        if (socketPacket instanceof AuthResponsePacket) {
            this.reference.setup((AuthResponsePacket) socketPacket);
            this.latch.countDown();
        }
    };

    public WebSocketInitializer(String str, boolean z) {
        this.reference = new CrashConfig(z);
    }

    public void execute() throws InterruptedException {
        this.latch.countDown();
        this.latch.await();
    }

    public CrashConfig getReference() {
        return this.reference;
    }

    public CountDownLatch getLatch() {
        return this.latch;
    }

    public WebSocketPacketReader getPacketReader() {
        return this.packetReader;
    }

    public void setReference(CrashConfig crashConfig) {
        this.reference = crashConfig;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WebSocketInitializer)) {
            return false;
        }
        WebSocketInitializer webSocketInitializer = (WebSocketInitializer) obj;
        if (!webSocketInitializer.canEqual(this)) {
            return false;
        }
        CrashConfig reference = getReference();
        CrashConfig reference2 = webSocketInitializer.getReference();
        if (reference == null) {
            if (reference2 != null) {
                return false;
            }
        } else if (!reference.equals(reference2)) {
            return false;
        }
        CountDownLatch latch = getLatch();
        CountDownLatch latch2 = webSocketInitializer.getLatch();
        if (latch == null) {
            if (latch2 != null) {
                return false;
            }
        } else if (!latch.equals(latch2)) {
            return false;
        }
        WebSocketPacketReader packetReader = getPacketReader();
        WebSocketPacketReader packetReader2 = webSocketInitializer.getPacketReader();
        if (packetReader == null) {
            return packetReader2 == null;
        }
        return packetReader.equals(packetReader2);
    }

    public boolean canEqual(Object obj) {
        return obj instanceof WebSocketInitializer;
    }

    public int hashCode() {
        CrashConfig reference = getReference();
        int iHashCode = (1 * 59) + (reference == null ? 43 : reference.hashCode());
        CountDownLatch latch = getLatch();
        int iHashCode2 = (iHashCode * 59) + (latch == null ? 43 : latch.hashCode());
        WebSocketPacketReader packetReader = getPacketReader();
        return (iHashCode2 * 59) + (packetReader == null ? 43 : packetReader.hashCode());
    }

    public String toString() {
        return "WebSocketInitializer(reference=" + String.valueOf(getReference()) + ", latch=" + String.valueOf(getLatch()) + ", packetReader=" + String.valueOf(getPacketReader()) + ")";
    }
}
