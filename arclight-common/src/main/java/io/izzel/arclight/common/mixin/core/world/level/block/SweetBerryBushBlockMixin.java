package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;

@Mixin(SweetBerryBushBlock.class)
public class SweetBerryBushBlockMixin {

    @Eject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$cropGrow(ServerLevel world, BlockPos pos, BlockState newState, int flags, CallbackInfo ci) {
        if (!CraftEventFactory.handleBlockGrowEvent(world, pos, newState, flags)) {
            ci.cancel();
        }
        return true;
    }

    @Redirect(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;sweetBerryBush()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$blockDamage(DamageSources instance, BlockState blockState, Level level, BlockPos blockPos) {
        return ((DamageSourceBridge) instance.sweetBerryBush()).bridge$directBlock(CraftBlock.at(level, blockPos));
    }

    @Eject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SweetBerryBushBlock;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$playerHarvest(Level worldIn, BlockPos pos, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir,
                                        BlockState state, Level worldIn1, BlockPos pos1, Player player, InteractionHand hand) {
        PlayerHarvestBlockEvent event = CraftEventFactory.callPlayerHarvestBlockEvent(worldIn, pos, player, hand, Collections.singletonList(stack));
        if (!event.isCancelled()) {
            for (org.bukkit.inventory.ItemStack itemStack : event.getItemsHarvested()) {
                Block.popResource(worldIn, pos, CraftItemStack.asNMSCopy(itemStack));
            }
        } else {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
