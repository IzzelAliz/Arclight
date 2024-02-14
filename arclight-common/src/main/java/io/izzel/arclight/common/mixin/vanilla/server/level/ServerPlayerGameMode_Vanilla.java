package io.izzel.arclight.common.mixin.vanilla.server.level;

import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameMode_Vanilla {

    // @formatter:off
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    @Shadow private GameType gameModeForPlayer;
    @Shadow public abstract boolean isCreative();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean destroyBlock(BlockPos blockPos) {
        BlockEntity blockEntity;
        Block block;
        BlockState blockState2;
        boolean bl;
        BlockState blockState = this.level.getBlockState(blockPos);
        if (!ArclightEventFactory.onBlockBreak((ServerPlayerGameMode) (Object) this, this.level, this.player, blockPos, blockState, !this.player.getMainHandItem().getItem().canAttackBlock(blockState, this.level, blockPos, this.player))) {
            return false;
        } else {
            blockEntity = this.level.getBlockEntity(blockPos);
            block = blockState.getBlock();
            if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks()) {
                this.level.sendBlockUpdated(blockPos, blockState, blockState, 3);
                return false;
            } else if (this.player.blockActionRestricted(this.level, blockPos, this.gameModeForPlayer)) {
                return false;
            } else {
                blockState2 = block.playerWillDestroy(this.level, blockPos, blockState, this.player);
                bl = this.level.removeBlock(blockPos, false);
                if (bl) {
                    block.destroy(this.level, blockPos, blockState2);
                }

                if (this.isCreative()) {
                    return true;
                } else {
                    ItemStack itemStack = this.player.getMainHandItem();
                    ItemStack itemStack2 = itemStack.copy();
                    boolean bl2 = this.player.hasCorrectToolForDrops(blockState2);
                    itemStack.mineBlock(this.level, blockState2, blockPos, this.player);
                    if (bl && bl2 && ArclightCaptures.getBlockBreakDropItems()) {
                        block.playerDestroy(this.level, this.player, blockPos, blockState2, blockEntity, itemStack2);
                    }
                    return true;
                }
            }
        }
    }
}
