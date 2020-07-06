package io.izzel.arclight.common.mod.util;

import net.minecraft.block.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import org.bukkit.craftbukkit.v.block.CraftBlock;

public class ArclightBlockSnapshot extends CraftBlock {

    private final BlockState blockState;

    public ArclightBlockSnapshot(BlockSnapshot blockSnapshot, boolean current) {
        super(blockSnapshot.getWorld(), blockSnapshot.getPos());
        this.blockState = current ? blockSnapshot.getCurrentBlock() : blockSnapshot.getReplacedBlock();
    }

    @Override
    public BlockState getNMS() {
        return blockState;
    }

    public static ArclightBlockSnapshot fromBlockSnapshot(BlockSnapshot blockSnapshot, boolean current) {
        return new ArclightBlockSnapshot(blockSnapshot, current);
    }
}
