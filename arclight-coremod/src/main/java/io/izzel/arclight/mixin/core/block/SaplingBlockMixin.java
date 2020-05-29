package io.izzel.arclight.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.world.StructureGrowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.izzel.arclight.mod.util.ArclightBlockPopulator;
import io.izzel.arclight.mod.util.ArclightCaptures;

import java.util.List;
import java.util.Random;

@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin {

    // @formatter:off
    @Shadow public abstract void grow(IWorld worldIn, BlockPos pos, BlockState state, Random rand);
    // @formatter:on

    @SuppressWarnings("unchecked")
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/SaplingBlock;grow(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Ljava/util/Random;)V"))
    public void arclight$treeGrow(SaplingBlock saplingBlock, IWorld worldIn, BlockPos pos, BlockState state, Random rand) {
        BlockStateListPopulator populator = new ArclightBlockPopulator(worldIn.getWorld());
        this.grow(populator, pos, state, rand);
        if (populator.getBlocks().size() > 0) {
            TreeType treeType = ArclightCaptures.getTreeType();
            Location location = CraftBlock.at(worldIn, pos).getLocation();
            StructureGrowEvent event = new StructureGrowEvent(location, treeType, false, null, (List<org.bukkit.block.BlockState>) (Object) populator.getList());
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                populator.updateList();
            }
        }
    }
}
