package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

import net.minecraft.network.packet.Packet;

public class PacketSendEvent extends CancellableEvent {
    public final Packet<?> packet;

    public Packet<?> getPacket() {
        return this.packet;
    }

    public PacketSendEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
