package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.entity.passive.AnimalEntityBridge;
import io.izzel.arclight.common.bridge.entity.passive.FoxEntityBridge;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.ai.goal.BreedGoal;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.stats.Stats;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityBreedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net.minecraft.entity.passive.FoxEntity.MateGoal")
public abstract class FoxEntity_MateGoalMixin extends BreedGoal {

    public FoxEntity_MateGoalMixin(AnimalEntity animal, double speedIn) {
        super(animal, speedIn);
    }

    /**
     * @author IzzekAkuz
     * @reason
     */
    @Overwrite
    protected void spawnBaby() {
        ServerWorld serverworld = (ServerWorld) this.world;
        FoxEntity foxentity = (FoxEntity) this.animal.func_241840_a(serverworld, this.targetMate);
        final BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(animal, targetMate, foxentity);
        final boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
        foxentity = (FoxEntity) event.getChild();
        if (cancelled) {
            //Reset the "inLove" state for the animals
            this.animal.setGrowingAge(6000);
            this.targetMate.setGrowingAge(6000);
            this.animal.resetInLove();
            this.targetMate.resetInLove();
            return;
        }
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

            this.animal.setGrowingAge(6000);
            this.targetMate.setGrowingAge(6000);
            this.animal.resetInLove();
            this.targetMate.resetInLove();
            foxentity.setGrowingAge(-24000);
            foxentity.setLocationAndAngles(this.animal.getPosX(), this.animal.getPosY(), this.animal.getPosZ(), 0.0F, 0.0F);
            serverworld.func_242417_l(foxentity);
            this.world.setEntityState(this.animal, (byte) 18);
            if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                if (experience > 0) {
                    this.world.addEntity(new ExperienceOrbEntity(this.world, this.animal.getPosX(), this.animal.getPosY(), this.animal.getPosZ(), experience));
                }
            }

        }
    }
}
