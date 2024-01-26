package io.izzel.arclight.forge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Mob.class)
public abstract class MobMixin_Forge extends LivingEntityMixin_Forge implements MobEntityBridge {

    @Override
    public AgeableMob bridge$forge$onBabyEntitySpawn(Mob partner, @Nullable AgeableMob proposedChild) {
        var event = new BabyEntitySpawnEvent((Mob) (Object) this, partner, proposedChild);
        var cancelled = MinecraftForge.EVENT_BUS.post(event);
        return cancelled ? null : event.getChild();
    }

    @Override
    public boolean bridge$common$animalTameEvent(Player player) {
        return true;
    }
}
