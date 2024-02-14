package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MagmaBlock.class)
public class MagmaBlockMixin {

    @Redirect(method = "stepOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;hotFloor()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$blockDamagePre(DamageSources instance, Level level, BlockPos blockPos) {
        return ((DamageSourceBridge) instance.hotFloor()).bridge$directBlock(CraftBlock.at(level, blockPos));
    }
}
