package io.izzel.arclight.neoforge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Mob.class)
public abstract class MobMixin_NeoForge extends LivingEntityMixin_NeoForge implements MobBridge {

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
}
