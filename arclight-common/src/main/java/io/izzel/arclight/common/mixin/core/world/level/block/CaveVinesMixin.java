package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.LinkedList;
import java.util.List;

@Mixin(CaveVines.class)
public interface CaveVinesMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    static InteractionResult use(Entity sourceEntity, BlockState state, Level level, BlockPos pos) {
        if (state.getValue(CaveVines.BERRIES)) {
            if (level instanceof ServerLevel serverLevel) {
                if (!CraftEventFactory.callEntityChangeBlockEvent(sourceEntity, pos, state.setValue(CaveVines.BERRIES, false))) {
                    return InteractionResult.SUCCESS;
                }

                if (sourceEntity instanceof Player player) {
                    List<ItemStack> dropped = new LinkedList<>();
                    Block.dropFromBlockInteractLootTable(serverLevel, BuiltInLootTables.HARVEST_CAVE_VINE, state, level.getBlockEntity(pos), null, sourceEntity, (serverLevel1, itemStack) -> {
                        dropped.add(itemStack);
                    });
                    PlayerHarvestBlockEvent event = CraftEventFactory.callPlayerHarvestBlockEvent(level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND, dropped);
                    if (event.isCancelled()) {
                        return InteractionResult.SUCCESS; // We need to return a success either way, because making it PASS or FAIL will result in a bug where cancelling while harvesting w/ block in hand places block
                    }
                    for (org.bukkit.inventory.ItemStack itemStack : event.getItemsHarvested()) {
                        Block.popResource(level, pos, CraftItemStack.asNMSCopy(itemStack));
                    }
                } else {
                    Block.dropFromBlockInteractLootTable(serverLevel, BuiltInLootTables.HARVEST_CAVE_VINE, state, level.getBlockEntity(pos), null, sourceEntity, (serverLevel1, itemStack) -> {
                        Block.popResource(serverLevel1, pos, itemStack);
                    });
                }

                float f = Mth.randomBetween(serverLevel.random, 0.8F, 1.2F);
                serverLevel.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, f);
                var newState = state.setValue(CaveVines.BERRIES, Boolean.FALSE);
                serverLevel.setBlock(pos, newState, 2);
                serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }
}
