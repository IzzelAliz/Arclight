package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mixin.core.world.SimpleContainerMixin;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ComposterBlock.InputContainer.class)
public abstract class ComposterBlock_InputContainerMixin extends SimpleContainerMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
    public void arclight$setOwner(BlockState blockState, LevelAccessor world, BlockPos blockPos, CallbackInfo ci) {
        this.setOwner(new CraftBlockInventoryHolder(world, blockPos, this));
    }

    @Decorate(method = "setChanged()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/ComposterBlock;addLayer(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/component/Compostable;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private BlockState arclight$compostContents(Entity sourceEntity, BlockState state, ServerLevel level, BlockPos pos, Compostable compostable,
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
            return (BlockState) DecorationOps.cancel().invoke();
        }
        return result;
    }
}
