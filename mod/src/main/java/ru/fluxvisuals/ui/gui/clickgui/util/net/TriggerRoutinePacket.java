package ru.fluxvisuals.ui.gui.clickgui.util.net;
import ru.fluxvisuals.ui.gui.clickgui.util.model.AuthStub;

public class TriggerRoutinePacket implements IncomingPacket {
    @Override
    public void decode(PacketBuffer packetBuffer) {
    }

    @Override
    public void handle() {
        try {
            AuthStub.drakula();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
