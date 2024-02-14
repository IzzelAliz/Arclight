package io.izzel.arclight.fabric.mixin.core.world.level.block;

import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin_Fabric {

   // @Inject(method = "checkBurnOut", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
   // private void arclight$blockBurn(Level worldIn, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
   //     Block theBlock = CraftBlock.at(worldIn, pos);
   //     Block sourceBlock = CraftBlock.at(worldIn, pos.relative(face));
   //     BlockBurnEvent event = new BlockBurnEvent(theBlock, sourceBlock);
   //     Bukkit.getPluginManager().callEvent(event);
   //     if (event.isCancelled()) {
   //         ci.cancel();
   //         return;
   //     }
   //     if (worldIn.getBlockState(pos).getBlock() instanceof TntBlock && !CraftEventFactory.callTNTPrimeEvent(worldIn, pos, TNTPrimeEvent.PrimeCause.FIRE, null, pos.relative(face))) {
   //         ci.cancel();
   //     }
   // }
}
