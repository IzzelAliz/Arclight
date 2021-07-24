package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collections;

@Mixin(CaveVines.class)
public interface CaveVinesMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    static InteractionResult use(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(CaveVines.BERRIES)) {
            Entity entity = ArclightCaptures.getEntityChangeBlock();
            if (entity != null) {
                if (CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state.setValue(CaveVines.BERRIES, false)).isCancelled()) {
                    return InteractionResult.SUCCESS;
                }

                if (entity instanceof Player) {
                    PlayerHarvestBlockEvent event = CraftEventFactory.callPlayerHarvestBlockEvent(level, pos, (Player) entity, Collections.singletonList(new ItemStack(Items.GLOW_BERRIES, 1)));
                    if (event.isCancelled()) {
                        return InteractionResult.SUCCESS; // We need to return a success either way, because making it PASS or FAIL will result in a bug where cancelling while harvesting w/ block in hand places block
                    }
                    for (org.bukkit.inventory.ItemStack itemStack : event.getItemsHarvested()) {
                        Block.popResource(level, pos, CraftItemStack.asNMSCopy(itemStack));
                    }
                } else {
                    Block.popResource(level, pos, new ItemStack(Items.GLOW_BERRIES, 1));
                }
            }
            Block.popResource(level, pos, new ItemStack(Items.GLOW_BERRIES, 1));
            float f = Mth.randomBetween(level.random, 0.8F, 1.2F);
            level.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, f);
            level.setBlock(pos, state.setValue(CaveVines.BERRIES, Boolean.FALSE), 2);
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }
}
