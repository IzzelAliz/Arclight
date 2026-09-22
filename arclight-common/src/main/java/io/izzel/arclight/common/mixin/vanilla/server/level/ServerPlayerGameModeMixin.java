package io.izzel.arclight.common.mixin.vanilla.server.level;

import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    // @formatter:off
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    // @formatter:on

    @Decorate(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canDestroyBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean arclight$blockBreakEvent(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) throws Throwable {
        boolean canDestroyBlock = (boolean) DecorationOps.callsite().invoke(stack, state, level, pos, player);
        return ArclightEventFactory.onBlockBreak((ServerPlayerGameMode) (Object) this, this.level, this.player, pos, state, !canDestroyBlock);
    }

    @Decorate(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$blockBreakDrops(Block block, Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) throws Throwable {
        if (!ArclightCaptures.getBlockBreakDropItems()) {
            return;
        }
        ArclightCaptures.pushVanillaBlockBreakExperienceSuppression(this.level, pos);
        try {
            DecorationOps.callsite().invoke(block, level, player, pos, state, blockEntity, tool);
        } finally {
            ArclightCaptures.popVanillaBlockBreakExperienceSuppression();
        }
    }
}
