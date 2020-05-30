package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.SilverfishBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SilverfishBlock.class)
public class SilverfishBlockMixin {

    @Inject(method = "spawnAdditionalDrops", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$spawn(BlockState state, World worldIn, BlockPos pos, ItemStack stack, CallbackInfo ci) {
        ((WorldBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SILVERFISH_BLOCK);
    }
}
