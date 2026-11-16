package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.block.CompostItemEvent;
import io.papermc.paper.event.entity.EntityCompostItemEvent;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftBlockInventoryHolder;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.DummyLevelAccessor;
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
    private static final String ADD_LAYER = "addLayer(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/component/Compostable;)Lnet/minecraft/world/level/block/state/BlockState;";

    // @formatter:off
    @Shadow static BlockState empty(@Nullable Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos) { return null; }
    // @formatter:on

    @Redirect(method = "getContainer", at = @At(value = "NEW", target = "()Lnet/minecraft/world/level/block/ComposterBlock$EmptyContainer;"))
    public ComposterBlock.EmptyContainer arclight$newEmpty(BlockState blockState, LevelAccessor world, BlockPos blockPos) {
        ComposterBlock.EmptyContainer inventory = new ComposterBlock.EmptyContainer();
        ((IInventoryBridge) inventory).setOwner(new CraftBlockInventoryHolder(world, blockPos, inventory));
        return inventory;
    }

    @Decorate(method = ADD_LAYER, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/storage/loot/providers/number/ints/ResolvableInt;get(Lnet/minecraft/world/level/storage/loot/LootContext;I)I"), require = 1, allow = 1)
    private static int arclight$compostLayers(ResolvableInt provider, LootContext context, int fallback,
                                             Entity sourceEntity, BlockState state, ServerLevel level, BlockPos pos, Compostable compostable) throws Throwable {
        int layers = (int) DecorationOps.callsite().invoke(provider, context, fallback);
        var block = CraftBlock.at(level, pos);
        var item = CraftItemStack.asCraftMirror(ArclightCaptures.getCompostStack());
        CompostItemEvent event = sourceEntity == null
            ? new CompostItemEvent(block, item, layers)
            : new EntityCompostItemEvent(((EntityBridge) sourceEntity).bridge$getBukkitEntity(), block, item, layers);
        if (!event.callEvent()) {
            return (int) DecorationOps.cancel().invoke((BlockState) null);
        }
        return event.getLevelsToRaise();
    }

    @Decorate(method = ADD_LAYER, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1, allow = 1)
    private static boolean arclight$compostBlockChange(ServerLevel level, BlockPos pos, BlockState proposedState,
                                                      @Local(ordinal = 0) Entity sourceEntity) throws Throwable {
        if (sourceEntity != null && !CraftEventFactory.callEntityChangeBlockEvent(sourceEntity, pos, proposedState)) {
            return (boolean) DecorationOps.cancel().invoke((BlockState) null);
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, proposedState);
    }

    @Decorate(method = INSERT_ITEM, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/ComposterBlock;" + ADD_LAYER), require = 1, allow = 1)
    private static BlockState arclight$insertCompost(Entity sourceEntity, BlockState state, ServerLevel level, BlockPos pos, Compostable compostable,
                                                    @Local(ordinal = 0) ItemStack itemStack) throws Throwable {
        ItemStack previous = ArclightCaptures.getCompostStack();
        BlockState result;
        ArclightCaptures.setCompostStack(itemStack);
        try {
            result = (BlockState) DecorationOps.callsite().invoke(sourceEntity, state, level, pos, compostable);
        } finally {
            ArclightCaptures.setCompostStack(previous);
        }
        if (result == null) {
            return (BlockState) DecorationOps.cancel().invoke(state);
        }
        return result;
    }

    @Decorate(method = "useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/ComposterBlock;" + ADD_LAYER), require = 1, allow = 1)
    private BlockState arclight$playerCompost(Entity sourceEntity, BlockState state, ServerLevel level, BlockPos pos, Compostable compostable,
                                            @Local(ordinal = 0) ItemStack itemStack) throws Throwable {
        ItemStack previous = ArclightCaptures.getCompostStack();
        BlockState result;
        ArclightCaptures.setCompostStack(itemStack);
        try {
            result = (BlockState) DecorationOps.callsite().invoke(sourceEntity, state, level, pos, compostable);
        } finally {
            ArclightCaptures.setCompostStack(previous);
        }
        if (result == null) {
            return (BlockState) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
        }
        return result;
    }

    @Inject(method = "extractProduce", cancellable = true, at = @At("HEAD"))
    private static void arclight$emptyComposter(Entity entity, BlockState state, Level world, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (entity != null && !(entity instanceof Player)) {
            BlockState blockState = empty(entity, state, DummyLevelAccessor.INSTANCE, pos);
            if (!CraftEventFactory.callEntityChangeBlockEvent(entity, pos, blockState)) {
                cir.setReturnValue(state);
            }
        }
    }
}
