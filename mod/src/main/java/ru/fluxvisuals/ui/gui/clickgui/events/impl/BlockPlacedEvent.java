package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.Event;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class BlockPlacedEvent implements Event {
    public final Block block;
    public final BlockPos blockPos;

    public Block getBlock() {
        return this.block;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public BlockPlacedEvent(Block block, BlockPos blockPos) {
        this.block = block;
        this.blockPos = blockPos;
    }
}
