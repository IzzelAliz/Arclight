package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CraftBlockState.class)
public class CraftBlockStateMixin {

    @Shadow(remap = false) protected BlockState block;

    @Override
    public String toString() {
        return this.block.toString();
    }
}
