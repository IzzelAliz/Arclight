package io.izzel.arclight.neoforge.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin_NeoForge implements WorldBridge {

    // @formatter:off
    @Shadow(remap = false) public abstract void markAndNotifyBlock(BlockPos arg, @Nullable LevelChunk arg2, BlockState j, BlockState k, int j2, int k2);
    @Shadow public abstract ResourceKey<Level> dimension();
    @Shadow(remap = false) public boolean restoringBlockSnapshots;
    // @formatter:on

    @Inject(method = "markAndNotifyBlock", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;updateNeighbourShapes(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V"))
    private void arclight$callBlockPhysics(BlockPos pos, LevelChunk chunk, BlockState blockstate, BlockState state, int flags, int recursionLeft, CallbackInfo ci) {
        try {
            if (this.bridge$getWorld() != null) {
                BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at((LevelAccessor) this, pos), CraftBlockData.fromData(state));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    ci.cancel();
                }
            }
        } catch (StackOverflowError e) {
            bridge$setLastPhysicsProblem(pos);
        }
    }

    @Inject(method = "markAndNotifyBlock", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;onBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void arclight$preventPoiUpdate(BlockPos p_46605_, LevelChunk levelchunk, BlockState blockstate, BlockState p_46606_, int p_46607_, int p_46608_, CallbackInfo ci) {
        if (this.bridge$preventPoiUpdated()) {
            ci.cancel();
        }
    }

    @Override
    public void bridge$forge$notifyAndUpdatePhysics(BlockPos pos, LevelChunk chunk, BlockState oldBlock, BlockState newBlock, int i, int j) {
        this.markAndNotifyBlock(pos, chunk, oldBlock, newBlock, i, j);
    }

    @Override
    public boolean bridge$forge$onBlockPlace(BlockPos pos, LivingEntity livingEntity, Direction direction) {
        return EventHooks.onBlockPlace(livingEntity, BlockSnapshot.create(this.dimension(), (Level) (Object) this, pos), direction);
    }

    @Override
    public boolean bridge$forge$mobGriefing(Entity entity) {
        return EventHooks.getMobGriefingEvent((Level) (Object) this, entity);
    }

    @Override
    public ItemStack bridge$forge$potionBrewMix(ItemStack a, ItemStack b) {
        return BrewingRecipeRegistry.getOutput(a, b);
    }

    @Override
    public void bridge$forge$onPotionBrewed(NonNullList<ItemStack> stacks) {
        EventHooks.onPotionBrewed(stacks);
    }

    @Override
    public boolean bridge$forge$restoringBlockSnapshots() {
        return this.restoringBlockSnapshots;
    }
}
