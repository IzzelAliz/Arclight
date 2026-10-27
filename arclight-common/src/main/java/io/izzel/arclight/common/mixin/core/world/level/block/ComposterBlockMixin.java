package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.block.CompostItemEvent;
import io.papermc.paper.event.entity.EntityCompostItemEvent;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftBlockInventoryHolder;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.DummyLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
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

    // The two loaders obtain chance differently. Exactly one provider must survive.
    // Level zero short-circuits the native RNG; Paper nevertheless draws once before its event.
    @Group(name = "arclight$compostChance", min = 1, max = 1)
    @Decorate(method = ADD_ITEM, at = @At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/objects/Object2FloatMap;getFloat(Ljava/lang/Object;)F", remap = false), require = 0, allow = 1)
    private static float arclight$compostMapChance(Object2FloatMap<?> compostables, Object item,
                                                  Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack itemStack) throws Throwable {
        float chance = (float) DecorationOps.callsite().invoke(compostables, item);
        if (state.getValue(ComposterBlock.LEVEL) == 0 && chance > 0.0F) {
            CompostItemEvent event = arclight$compostEvent(sourceEntity, state, level, pos, itemStack, chance, level.getRandom().nextDouble());
            if (!event.callEvent()) {
                return (float) DecorationOps.cancel().invoke((BlockState) null);
            }
            if (!event.willRaiseLevel()) {
                return (float) DecorationOps.cancel().invoke(state);
            }
        }
        return chance;
    }

    @Group(name = "arclight$compostChance", min = 1, max = 1)
    @Decorate(method = ADD_ITEM, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/ComposterBlock;getValue(Lnet/minecraft/world/item/ItemStack;)F"), require = 0, allow = 1)
    private static float arclight$compostDataMapChance(ItemStack item,
                                                      Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack itemStack) throws Throwable {
        float chance = (float) DecorationOps.callsite().invoke(item);
        if (state.getValue(ComposterBlock.LEVEL) == 0 && chance > 0.0F) {
            CompostItemEvent event = arclight$compostEvent(sourceEntity, state, level, pos, itemStack, chance, level.getRandom().nextDouble());
            if (!event.callEvent()) {
                return (float) DecorationOps.cancel().invoke((BlockState) null);
            }
            if (!event.willRaiseLevel()) {
                return (float) DecorationOps.cancel().invoke(state);
            }
        }
        return chance;
    }

    @Decorate(method = ADD_ITEM, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/RandomSource;nextDouble()D"), require = 1, allow = 1)
    private static double arclight$compostRandom(RandomSource random,
                                                 Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack itemStack,
                                                 @Local(ordinal = 0) float chance) throws Throwable {
        double roll = (double) DecorationOps.callsite().invoke(random);
        CompostItemEvent event = arclight$compostEvent(sourceEntity, state, level, pos, itemStack, chance, roll);
        if (!event.callEvent()) {
            return (double) DecorationOps.cancel().invoke((BlockState) null);
        }
        if (!event.willRaiseLevel()) {
            return (double) DecorationOps.cancel().invoke(state);
        }
        // Only this comparison consumes chance/roll. Force its accepted edge after dispatch,
        // retaining the native increment, write, game event and level-seven scheduled tick.
        chance = 1.0F;
        DecorationOps.blackhole().invoke(chance);
        return 0.0D;
    }

    @Unique
    private static CompostItemEvent arclight$compostEvent(Entity sourceEntity, BlockState state, LevelAccessor level,
                                                         BlockPos pos, ItemStack itemStack, float chance, double roll) {
        boolean willRaiseLevel = !((state.getValue(ComposterBlock.LEVEL) != 0 || chance <= 0.0F) && roll >= (double) chance);
        var block = CraftBlock.at(level, pos);
        var item = CraftItemStack.asCraftMirror(itemStack);
        return sourceEntity == null
            ? new CompostItemEvent(block, item, willRaiseLevel)
            : new EntityCompostItemEvent(((EntityBridge) sourceEntity).bridge$getBukkitEntity(), block, item, willRaiseLevel);
    }

    @Decorate(method = ADD_ITEM, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 1, allow = 1)
    private static boolean arclight$compostBlockChange(LevelAccessor level, BlockPos pos, BlockState proposedState, int flags,
                                                      @Local(ordinal = 0) Entity sourceEntity) throws Throwable {
        if (sourceEntity != null && !CraftEventFactory.callEntityChangeBlockEvent(sourceEntity, pos, proposedState)) {
            return (boolean) DecorationOps.cancel().invoke((BlockState) null);
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, proposedState, flags);
    }

    @Decorate(method = INSERT_ITEM, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/ComposterBlock;addItem(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private static BlockState arclight$insertCompost(Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack itemStack) throws Throwable {
        BlockState result = (BlockState) DecorationOps.callsite().invoke(sourceEntity, state, level, pos, itemStack);
        if (result == null) {
            return (BlockState) DecorationOps.cancel().invoke(state);
        }
        // A non-cancelled failed roll still consumes an item, including for villagers.
        return result;
    }

    @Decorate(method = "useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/ComposterBlock;addItem(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private BlockState arclight$playerCompost(Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack itemStack) throws Throwable {
        BlockState result = (BlockState) DecorationOps.callsite().invoke(sourceEntity, state, level, pos, itemStack);
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
