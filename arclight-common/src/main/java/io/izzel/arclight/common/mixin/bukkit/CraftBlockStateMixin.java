package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftBlockState.class)
public class CraftBlockStateMixin {

    @Shadow(remap = false) protected BlockState data;

    @Decorate(method = "update(ZZ)Z", at = @At(value = "INVOKE", target = "setTypeAndData", remap = false), remap = false)
    private boolean arclight$checkSetBlock(CraftBlock block, BlockState data, boolean physics) throws Throwable {
        boolean success = (boolean) DecorationOps.callsite().invoke(block, data, physics);
        // https://github.com/IzzelAliz/Arclight/issues/2149
        // A rejected block write must not send updates to a nonexistent mod chunk holder.
        if (!success) {
            return (boolean) DecorationOps.cancel().invoke(false);
        }
        return true;
    }

    @Override
    public String toString() {
        return this.data.toString();
    }
}
