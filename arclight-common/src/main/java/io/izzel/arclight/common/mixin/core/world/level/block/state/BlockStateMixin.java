package io.izzel.arclight.common.mixin.core.world.level.block.state;

import io.izzel.arclight.common.bridge.core.world.level.block.state.BlockStateBridge;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public class BlockStateMixin implements BlockStateBridge {

    @Override
    public CraftBlockData asBlockData() {
        return CraftBlockData.createData((BlockState) (Object) this);
    }

    @Override
    public Material getBukkitMaterial() {
        return CraftMagicNumbers.getMaterial(((BlockState) (Object) this).getBlock());
    }
}
