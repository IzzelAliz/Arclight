package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.entity.passive.AnimalEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.ai.goal.BreedGoalMixin;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.stats.Stats;
import net.minecraft.world.GameRules;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import io.izzel.arclight.common.bridge.entity.passive.FoxEntityBridge;

@Mixin(targets = "net.minecraft.entity.passive.FoxEntity.MateGoal")
public class FoxEntity_MateGoalMixin extends BreedGoalMixin {

    /**
     * @author IzzekAkuz
     * @reason
     */
    @Overwrite
    protected void spawnBaby() {
        FoxEntity foxentity = (FoxEntity) this.animal.createChild(this.targetMate);
        if (foxentity != null) {
            ServerPlayerEntity serverplayerentity = this.animal.getLoveCause();
            ServerPlayerEntity serverplayerentity1 = this.targetMate.getLoveCause();
            ServerPlayerEntity serverplayerentity2 = serverplayerentity;
            if (serverplayerentity != null) {
                ((FoxEntityBridge) foxentity).bridge$addTrustedUUID(serverplayerentity.getUniqueID());
            } else {
                serverplayerentity2 = serverplayerentity1;
            }

            if (serverplayerentity1 != null && serverplayerentity != serverplayerentity1) {
                ((FoxEntityBridge) foxentity).bridge$addTrustedUUID(serverplayerentity1.getUniqueID());
            }
            int experience = this.animal.getRNG().nextInt(7) + 1;
            final EntityBreedEvent entityBreedEvent = CraftEventFactory.callEntityBreedEvent(foxentity, this.animal, this.targetMate, serverplayerentity, ((AnimalEntityBridge) this.animal).bridge$getBreedItem(), experience);
            if (entityBreedEvent.isCancelled()) {
                return;
            }
            experience = entityBreedEvent.getExperience();
            if (serverplayerentity2 != null) {
                serverplayerentity2.addStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(serverplayerentity2, this.animal, this.targetMate, foxentity);
            }

            int i = 6000;
            this.animal.setGrowingAge(6000);
            this.targetMate.setGrowingAge(6000);
            this.animal.resetInLove();
            this.targetMate.resetInLove();
            foxentity.setGrowingAge(-24000);
            foxentity.setLocationAndAngles(this.animal.posX, this.animal.posY, this.animal.posZ, 0.0F, 0.0F);
            ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
            this.world.addEntity(foxentity);
            this.world.setEntityState(this.animal, (byte) 18);
            if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                if (experience > 0) {
                    this.world.addEntity(new ExperienceOrbEntity(this.world, this.animal.posX, this.animal.posY, this.animal.posZ, experience));
                }
            }

        }
    }
}
