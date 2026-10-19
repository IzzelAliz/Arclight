package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftBlockInventoryHolder;
import org.bukkit.craftbukkit.util.DummyGeneratorAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {

    private static final String INSERT_ITEM = "insertItem(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;";
    private static final String ADD_ITEM = "addItem(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/level/block/state/BlockState;";

    // @formatter:off
    @Shadow static BlockState empty(@Nullable Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos) { return null; }
    // @formatter:on

    @Redirect(method = "getContainer", at = @At(value = "NEW", target = "()Lnet/minecraft/world/level/block/ComposterBlock$EmptyContainer;"))
    public ComposterBlock.EmptyContainer arclight$newEmpty(BlockState blockState, LevelAccessor world, BlockPos blockPos) {
        ComposterBlock.EmptyContainer inventory = new ComposterBlock.EmptyContainer();
        ((IInventoryBridge) inventory).setOwner(new CraftBlockInventoryHolder(world, blockPos, inventory));
        return inventory;
    }

    private static boolean arclight$villagerInsert;

    /** Keep the native proposal and random draw inside addItem. */
    @WrapOperation(
        method = INSERT_ITEM,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ComposterBlock;addItem(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/level/block/state/BlockState;"),
        require = 1, expect = 1, allow = 1)
    private static BlockState arclight$scopeInsertAddItem(Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack itemStack,
            Operation<BlockState> original) {
        arclight$villagerInsert = sourceEntity instanceof Villager;
        try {
            return original.call(sourceEntity, state, level, pos, itemStack);
        } finally {
            arclight$villagerInsert = false;
        }
    }

    /** Fire against the native proposal before the real write, without sampling or replaying addItem. */
    @Inject(
        method = ADD_ITEM,
        cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
        require = 1, expect = 1, allow = 1)
    private static void arclight$villagerPrewriteEvent(Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack itemStack,
            CallbackInfoReturnable<BlockState> cir, @Local(name = "newState") BlockState proposedState) {
        if (!arclight$villagerInsert) {
            return;
        }
        if (!CraftEventFactory.callEntityChangeBlockEvent(sourceEntity, pos, proposedState)) {
            cir.setReturnValue(state);
        }
    }

    /** Only Villager no-op inserts skip consumption; other insertItem callers retain native behavior. */
    @Inject(
        method = INSERT_ITEM,
        cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
        require = 1, expect = 1, allow = 1)
    private static void arclight$noOpInsertDoesNotConsume(Entity sourceEntity, BlockState state, ServerLevel level, ItemStack itemStack, BlockPos pos,
            CallbackInfoReturnable<BlockState> cir, @Local(name = "newState") BlockState insertedState) {
        if (sourceEntity instanceof Villager && insertedState == state) {
            cir.setReturnValue(state);
        }
    }

    @Inject(method = "extractProduce", cancellable = true, at = @At("HEAD"))
    private static void arclight$emptyComposter(Entity entity, BlockState state, Level world, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (entity != null && !(entity instanceof Player)) {
            BlockState blockState = empty(entity, state, DummyGeneratorAccess.INSTANCE, pos);
            if (!CraftEventFactory.callEntityChangeBlockEvent(entity, pos, blockState)) {
                cir.setReturnValue(state);
            }
        }
    }
}
