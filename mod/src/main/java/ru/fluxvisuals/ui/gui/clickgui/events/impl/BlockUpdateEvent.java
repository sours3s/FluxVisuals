package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.Event;
import ru.fluxvisuals.ui.gui.clickgui.util.model.BlockUpdateEntry;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BlockUpdateType;

import java.util.List;

public final class BlockUpdateEvent implements Event {
    public final List<BlockUpdateEntry> list;
    public final BlockUpdateType type;

    public BlockUpdateEvent(List<BlockUpdateEntry> list, BlockUpdateType blockUpdateType) {
        this.list = list;
        this.type = blockUpdateType;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "list=" + this.list + ", " + "type=" + this.type + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.list, this.type);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockUpdateEvent)) return false;
        BlockUpdateEvent o = (BlockUpdateEvent) obj;
        return java.util.Objects.equals(this.list, o.list) && java.util.Objects.equals(this.type, o.type);
    }
public List<BlockUpdateEntry> list() {
        return this.list;
    }

    public BlockUpdateType type() {
        return this.type;
    }
}
