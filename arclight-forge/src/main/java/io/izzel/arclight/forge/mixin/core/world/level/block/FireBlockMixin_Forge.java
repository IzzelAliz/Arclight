package io.izzel.arclight.forge.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireBlockMixin_Forge {

    @Inject(method = "tryCatchFire", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void arclight$blockBurn(Level worldIn, BlockPos pos, int chance, RandomSource random, int age, Direction face, CallbackInfo ci) {
        Block theBlock = CraftBlock.at(worldIn, pos);
        Block sourceBlock = CraftBlock.at(worldIn, pos.relative(face));
        BlockBurnEvent event = new BlockBurnEvent(theBlock, sourceBlock);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        if (worldIn.getBlockState(pos).getBlock() instanceof TntBlock && !CraftEventFactory.callTNTPrimeEvent(worldIn, pos, TNTPrimeEvent.PrimeCause.FIRE, null, pos.relative(face))) {
            ci.cancel();
        }
    }
}
