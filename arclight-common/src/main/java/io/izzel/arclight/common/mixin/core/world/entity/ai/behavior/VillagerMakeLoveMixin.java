package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VillagerMakeLove.class)
public class VillagerMakeLoveMixin {

    @Redirect(method = "breed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/npc/Villager;"))
    private Villager arclight$entityBreed(Villager lona, ServerLevel world, AgeableMob anonymous) {
        Villager child = lona.getBreedOffspring(world, anonymous);
        if (child != null && !CraftEventFactory.callEntityBreedEvent(child, lona, anonymous, null, null, 0).isCancelled()) {
            ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
            return child;
        } else {
            return null;
        }
    }
}
