package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.BiConsumer;

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

    @Decorate(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;dropFromBlockInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Ljava/util/function/BiConsumer;)Z"))
    private boolean arclight$playerHarvest(ServerLevel serverLevel, ResourceKey<LootTable> lootTable, BlockState lootState, BlockEntity blockEntity, ItemStack tool, Entity entity, BiConsumer<ServerLevel, ItemStack> dropper,
                                           BlockState state, Level level, BlockPos pos, Player player) throws Throwable {
        var drops = new ArrayList<ItemStack>();
        boolean result = (boolean) DecorationOps.callsite().invoke(serverLevel, lootTable, lootState, blockEntity, tool, entity, (BiConsumer<ServerLevel, ItemStack>) (dropLevel, stack) -> drops.add(stack));
        PlayerHarvestBlockEvent event = CraftEventFactory.callPlayerHarvestBlockEvent(level, pos, player, InteractionHand.MAIN_HAND, drops);
        if (!event.isCancelled()) {
            for (org.bukkit.inventory.ItemStack itemStack : event.getItemsHarvested()) {
                dropper.accept(serverLevel, CraftItemStack.asNMSCopy(itemStack));
            }
        } else {
            return result;
        }
        return result;
    }
}
