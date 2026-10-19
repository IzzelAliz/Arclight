package io.izzel.arclight.neoforge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;

@Mixin(Mob.class)
public abstract class MobMixin_NeoForge extends LivingEntityMixin_NeoForge implements MobBridge {

    @Shadow(remap = false) protected boolean preventConversionSpawns;

    @Override
    public void bridge$neoforge$setPreventConversionSpawns(boolean value) {
        this.preventConversionSpawns = value;
    }

    @Override
    public boolean bridge$neoforge$preventConversionSpawns() {
        return this.preventConversionSpawns;
    }

    @Override
    public AgeableMob bridge$forge$onBabyEntitySpawn(Mob partner, @Nullable AgeableMob proposedChild) {
        var event = new BabyEntitySpawnEvent((Mob) (Object) this, partner, proposedChild);
        var cancelled = NeoForge.EVENT_BUS.post(event).isCanceled();
        return cancelled ? null : event.getChild();
    }

    @Override
    public boolean bridge$common$animalTameEvent(Player player) {
        return true;
    }

    // Read the native branch flag once. A late callback-enabled bypass must clear
    // deferred equipment before source discard and its callbacks, not at RETURN.
    @WrapOperation(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Mob;preventConversionSpawns:Z", opcode = Opcodes.GETFIELD, remap = false),
        require = 1, expect = 1, allow = 1)
    private <T extends Mob> boolean arclight$hoglinEquipmentNeoBypass(Mob source, Operation<Boolean> original,
            EntityType<T> entityType, ConversionParams params, EntitySpawnReason spawnReason,
            ConversionParams.AfterConversion<T> afterConversion) {
        boolean bypass = original.call(source);
        if (bypass) {
            ((MobBridge) source).bridge$clearDeferredConversionEquipment();
        }
        return bypass;
    }
}
