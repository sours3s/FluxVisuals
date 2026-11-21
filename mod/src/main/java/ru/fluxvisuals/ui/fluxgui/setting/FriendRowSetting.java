package ru.fluxvisuals.ui.fluxgui.setting;

import ru.fluxvisuals.ui.fluxgui.util.friendscreen.FriendScreenClient$Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FriendRowSetting extends Setting<Void> {
    private final int index;
    private final Supplier<FriendScreenClient$Entry> entrySupplier;
    private final Supplier<Boolean> isHiddenSupplier;
    private final Runnable onToggle;
    private final Supplier<Integer> bindSupplier;
    private final Consumer<Integer> onBindChanged;

    public FriendRowSetting(
        int index,
        Supplier<FriendScreenClient$Entry> entrySupplier,
        Supplier<Boolean> isHiddenSupplier,
        Runnable onToggle,
        Supplier<Integer> bindSupplier,
        Consumer<Integer> onBindChanged,
        Supplier<Boolean> visibility
    ) {
        super("friend_row_" + index, null, visibility);
        this.index = index;
        this.entrySupplier = entrySupplier;
        this.isHiddenSupplier = isHiddenSupplier;
        this.onToggle = onToggle;
        this.bindSupplier = bindSupplier;
        this.onBindChanged = onBindChanged;
    }

    public int getIndex() {
        return this.index;
    }

    public FriendScreenClient$Entry getEntry() {
        return this.entrySupplier != null ? this.entrySupplier.get() : null;
    }

    public boolean isHidden() {
        return this.isHiddenSupplier != null && this.isHiddenSupplier.get();
    }

    public void toggle() {
        if (this.onToggle != null) {
            this.onToggle.run();
        }
    }

    public int getKey() {
        return this.bindSupplier != null ? this.bindSupplier.get() : 0;
    }

    public void setKey(int key) {
        if (this.onBindChanged != null) {
            this.onBindChanged.accept(key);
        }
    }
}
