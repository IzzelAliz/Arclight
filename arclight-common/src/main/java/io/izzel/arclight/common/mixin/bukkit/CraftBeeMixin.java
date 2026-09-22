package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.world.entity.BeeBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;
import org.bukkit.craftbukkit.v.entity.CraftBee;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftBee.class, remap = false)
public abstract class CraftBeeMixin {

    @Redirect(method = "setHive(Lorg/bukkit/Location;)V", remap = false, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/animal/bee/Bee;hivePos:Lnet/minecraft/core/BlockPos;", remap = true))
    private void arclight$setHivePos(Bee instance, BlockPos pos) {
        instance.setHivePos(pos);
    }

    @Redirect(method = "setHasNectar(Z)V", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;setHasNectar(Z)V", remap = true))
    private void arclight$setHasNectar(Bee instance, boolean nectar) {
        ((BeeBridge) instance).bridge$setHasNectar(nectar);
    }

    @Redirect(method = "setHasStung(Z)V", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;setHasStung(Z)V", remap = true))
    private void arclight$setHasStung(Bee instance, boolean stung) {
        ((BeeBridge) instance).bridge$setHasStung(stung);
    }

    @Redirect(method = "getCannotEnterHiveTicks()I", remap = false, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/entity/animal/bee/Bee;stayOutOfHiveCountdown:I", remap = true))
    private int arclight$getCannotEnterHiveTicks(Bee instance) {
        return ((BeeBridge) instance).bridge$getCannotEnterHiveTicks();
    }
}
