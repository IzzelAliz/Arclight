package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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
    @Shadow @Final public static Object2FloatMap<IItemProvider> CHANCES;
    @Shadow public static BlockState resetFillState(BlockState state, IWorld world, BlockPos pos) { return null; }
    @Shadow public static BlockState empty(BlockState state, World world, BlockPos pos) { return null; }
    // @formatter:on

    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "createInventory", at = @At(value = "NEW", target = "()Lnet/minecraft/block/ComposterBlock$EmptyInventory;"))
    public ComposterBlock.EmptyInventory arclight$newEmpty(BlockState blockState, IWorld world, BlockPos blockPos) {
        ComposterBlock.EmptyInventory inventory = new ComposterBlock.EmptyInventory();
        ((IInventoryBridge) inventory).setOwner(new CraftBlockInventoryHolder(world, blockPos, inventory));
        return inventory;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static BlockState attemptFill(BlockState state, ServerWorld world, ItemStack stack, BlockPos pos) {
        int i = state.get(LEVEL);
        if (i < 7 && CHANCES.containsKey(stack.getItem())) {
            double rand = world.rand.nextDouble();
            BlockState state1 = attemptCompost(state, DummyGeneratorAccess.INSTANCE, pos, stack, rand);

            if (state == state1 || CraftEventFactory.callEntityChangeBlockEvent(ArclightCaptures.getEntityChangeBlock(), pos, state1).isCancelled()) {
                return state;
            }

            state1 = attemptCompost(state, world, pos, stack, rand);
            stack.shrink(1);
            return state1;
        } else {
            return state;
        }
    }

    @Inject(method = "empty", cancellable = true, at = @At("HEAD"))
    private static void arclight$emptyComposter(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        Entity entity = ArclightCaptures.getEntityChangeBlock();
        if (entity != null) {
            BlockState blockState = resetFillState(state, DummyGeneratorAccess.INSTANCE, pos);
            if (CraftEventFactory.callEntityChangeBlockEvent(entity, pos, blockState).isCancelled()) {
                cir.setReturnValue(state);
            }
        }
    }

    private static BlockState d(BlockState state, World world, BlockPos pos, Entity entity) {
        ArclightCaptures.captureEntityChangeBlock(entity);
        return empty(state, world, pos);
    }

    private static BlockState b(BlockState state, IWorld world, BlockPos pos, ItemStack stack, double rand) {
        return attemptCompost(state, world, pos, stack, rand);
    }

    private static BlockState attemptCompost(BlockState state, IWorld world, BlockPos pos, ItemStack stack, double rand) {
        int i = state.get(LEVEL);
        float f = CHANCES.getFloat(stack.getItem());
        if ((i != 0 || !(f > 0.0F)) && !(rand < (double) f)) {
            return state;
        } else {
            int j = i + 1;
            BlockState blockstate = state.with(LEVEL, j);
            world.setBlockState(pos, blockstate, 3);
            if (j == 7) {
                world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), 20);
            }
            return blockstate;
        }
    }
}
