package io.izzel.arclight.common.mixin.vanilla.world.item;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin_Vanilla {

    // @formatter:off
    @Shadow public abstract boolean hasAdventureModePlaceTagForBlock(Registry<Block> registry, BlockInWorld blockInWorld);
    @Shadow public abstract Item getItem();
    @Shadow public abstract ItemStack copy();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public InteractionResult useOn(UseOnContext useOnContext) {
        Player player = useOnContext.getPlayer();
        BlockPos blockPos = useOnContext.getClickedPos();
        BlockInWorld blockInWorld = new BlockInWorld(useOnContext.getLevel(), blockPos, false);
        if (player != null && !player.getAbilities().mayBuild && !this.hasAdventureModePlaceTagForBlock(useOnContext.getLevel().registryAccess().registryOrThrow(Registries.BLOCK), blockInWorld)) {
            return InteractionResult.PASS;
        } else {
            Item item = this.getItem();
            InteractionResult interactionResult;
            ItemStack oldStack = this.copy();
            if (!(item instanceof BucketItem || item instanceof SolidBucketItem)) {
                ((WorldBridge) useOnContext.getLevel()).bridge$platform$startCaptureBlockBreak();
            }
            interactionResult = item.useOn(useOnContext);
            ((WorldBridge) useOnContext.getLevel()).bridge$platform$endCaptureBlockBreak();
            if (player != null && interactionResult.shouldAwardStats()) {
                interactionResult = ArclightEventFactory.onBlockPlace(useOnContext, player, oldStack, (ItemStack) (Object) this, interactionResult);
                if (interactionResult != InteractionResult.FAIL) {
                    player.awardStat(Stats.ITEM_USED.get(item));
                }
            }

            ((WorldBridge) useOnContext.getLevel()).bridge$getCapturedBlockEntity().clear();
            ((WorldBridge) useOnContext.getLevel()).bridge$getCapturedBlockState().clear();
            return interactionResult;
        }
    }
}
