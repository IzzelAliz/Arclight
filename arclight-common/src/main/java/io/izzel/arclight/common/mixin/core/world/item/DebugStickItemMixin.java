package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DebugStickItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DebugStickItem.class)
public abstract class DebugStickItemMixin {
    @ModifyExpressionValue(method = "handleInteraction(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/item/ItemStack;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;canUseGameMasterBlocks()Z"))
    private boolean arclight$debugStickPermission(boolean original, ServerPlayer player, BlockState state, LevelAccessor level, BlockPos pos, boolean cycle, ItemStack stack) {
        return original
            || (player.getAbilities().instabuild && ((ServerPlayerBridge) player).bridge$getBukkitEntity().hasPermission("minecraft.debugstick"))
            || ((ServerPlayerBridge) player).bridge$getBukkitEntity().hasPermission("minecraft.debugstick.always");
    }
}
