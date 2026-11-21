package ru.fluxvisuals.ui.fluxgui.setting;

import ru.fluxvisuals.ui.fluxgui.util.friendscreen.FriendScreenClient$Entry;
import java.util.function.Supplier;

public class FriendRequestRowSetting extends Setting<Void> {
    private final int index;
    private final Supplier<FriendScreenClient$Entry> entrySupplier;
    private final Runnable onAccept;
    private final Runnable onDecline;

    public FriendRequestRowSetting(
        int index, Supplier<FriendScreenClient$Entry> entrySupplier, Runnable onAccept, Runnable onDecline, Supplier<Boolean> visibility
    ) {
        super("request_row_" + index, null, visibility);
        this.index = index;
        this.entrySupplier = entrySupplier;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
    }

    public int getIndex() {
        return this.index;
    }

    public FriendScreenClient$Entry getEntry() {
        return this.entrySupplier != null ? this.entrySupplier.get() : null;
    }

    public void accept() {
        if (this.onAccept != null) {
            this.onAccept.run();
        }
    }

    public void decline() {
        if (this.onDecline != null) {
            this.onDecline.run();
        }
    }
}
