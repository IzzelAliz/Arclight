package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftBlockInventoryHolder;
import org.bukkit.craftbukkit.v.util.DummyGeneratorAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {

    // @formatter:off
    @Shadow @Final public static IntegerProperty LEVEL;
    @Shadow @Final public static Object2FloatMap<ItemLike> COMPOSTABLES;
    @Shadow public static BlockState empty(BlockState state, LevelAccessor world, BlockPos pos) { return null; }
    @Shadow public static BlockState extractProduce(BlockState state, Level world, BlockPos pos) { return null; }
    // @formatter:on

    @SuppressWarnings({"InvalidMemberReference", "UnresolvedMixinReference", "MixinAnnotationTarget", "InvalidInjectorMethodSignature"})
    @Redirect(method = "getContainer", at = @At(value = "NEW", target = "()Lnet/minecraft/world/level/block/ComposterBlock$EmptyContainer;"))
    public ComposterBlock.EmptyContainer arclight$newEmpty(BlockState blockState, LevelAccessor world, BlockPos blockPos) {
        ComposterBlock.EmptyContainer inventory = new ComposterBlock.EmptyContainer();
        ((IInventoryBridge) inventory).setOwner(new CraftBlockInventoryHolder(world, blockPos, inventory));
        return inventory;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static BlockState insertItem(BlockState state, ServerLevel world, ItemStack stack, BlockPos pos) {
        int i = state.getValue(LEVEL);
        if (i < 7 && COMPOSTABLES.containsKey(stack.getItem())) {
            double rand = world.random.nextDouble();
            BlockState state1 = addItem(state, DummyGeneratorAccess.INSTANCE, pos, stack, rand);

            if (state == state1 || CraftEventFactory.callEntityChangeBlockEvent(ArclightCaptures.getEntityChangeBlock(), pos, state1).isCancelled()) {
                return state;
            }

            state1 = addItem(state, world, pos, stack, rand);
            stack.shrink(1);
            return state1;
        } else {
            return state;
        }
    }

    @Inject(method = "extractProduce", cancellable = true, at = @At("HEAD"))
    private static void arclight$emptyComposter(BlockState state, Level world, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        Entity entity = ArclightCaptures.getEntityChangeBlock();
        if (entity != null) {
            BlockState blockState = empty(state, DummyGeneratorAccess.INSTANCE, pos);
            if (CraftEventFactory.callEntityChangeBlockEvent(entity, pos, blockState).isCancelled()) {
                cir.setReturnValue(state);
            }
        }
    }

    private static BlockState extractProduce(BlockState state, Level world, BlockPos pos, Entity entity) {
        ArclightCaptures.captureEntityChangeBlock(entity);
        return extractProduce(state, world, pos);
    }

    private static BlockState addItem(BlockState state, LevelAccessor world, BlockPos pos, ItemStack stack, double rand) {
        int i = state.getValue(LEVEL);
        float f = COMPOSTABLES.getFloat(stack.getItem());
        if ((i != 0 || !(f > 0.0F)) && !(rand < (double) f)) {
            return state;
        } else {
            int j = i + 1;
            BlockState blockstate = state.setValue(LEVEL, j);
            world.setBlock(pos, blockstate, 3);
            if (j == 7) {
                world.scheduleTick(pos, state.getBlock(), 20);
            }
            return blockstate;
        }
    }
}
