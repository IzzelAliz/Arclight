package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin {

    @ModifyVariable(method = "updatePower", name = "l", index = 8, at = @At(value = "JUMP", ordinal = 0, opcode = Opcodes.IF_ICMPEQ))
    public int arclight$blockRedstone(int l, World world, BlockPos pos, BlockState state) {
        int i = state.get(RedstoneWireBlock.POWER);
        if (i != l) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(CraftBlock.at(world, pos), i, l);
            Bukkit.getPluginManager().callEvent(event);
            return event.getNewCurrent();
        }
        return l;
    }
}
