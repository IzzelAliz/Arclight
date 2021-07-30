package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.entity.passive.AnimalEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.passive.FoxEntityBridge;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityBreedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net.minecraft.world.entity.animal.Fox$FoxBreedGoal")
public abstract class Fox_BreedGoalMixin extends BreedGoal {

    public Fox_BreedGoalMixin(Animal animal, double speedIn) {
        super(animal, speedIn);
    }

    /**
     * @author IzzekAkuz
     * @reason
     */
    @Overwrite
    protected void breed() {
        ServerLevel serverworld = (ServerLevel) this.level;
        Fox foxentity = (Fox) this.animal.getBreedOffspring(serverworld, this.partner);
        final BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(animal, partner, foxentity);
        final boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
        foxentity = (Fox) event.getChild();
        if (cancelled) {
            //Reset the "inLove" state for the animals
            this.animal.setAge(6000);
            this.partner.setAge(6000);
            this.animal.resetLove();
            this.partner.resetLove();
            return;
        }
        if (foxentity != null) {
            ServerPlayer serverplayerentity = this.animal.getLoveCause();
            ServerPlayer serverplayerentity1 = this.partner.getLoveCause();
            ServerPlayer serverplayerentity2 = serverplayerentity;
            if (serverplayerentity != null) {
                ((FoxEntityBridge) foxentity).bridge$addTrustedUUID(serverplayerentity.getUUID());
            } else {
                serverplayerentity2 = serverplayerentity1;
            }

            if (serverplayerentity1 != null && serverplayerentity != serverplayerentity1) {
                ((FoxEntityBridge) foxentity).bridge$addTrustedUUID(serverplayerentity1.getUUID());
            }
            int experience = this.animal.getRandom().nextInt(7) + 1;
            final EntityBreedEvent entityBreedEvent = CraftEventFactory.callEntityBreedEvent(foxentity, this.animal, this.partner, serverplayerentity, ((AnimalEntityBridge) this.animal).bridge$getBreedItem(), experience);
            if (entityBreedEvent.isCancelled()) {
                return;
            }
            experience = entityBreedEvent.getExperience();
            if (serverplayerentity2 != null) {
                serverplayerentity2.awardStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(serverplayerentity2, this.animal, this.partner, foxentity);
            }

            this.animal.setAge(6000);
            this.partner.setAge(6000);
            this.animal.resetLove();
            this.partner.resetLove();
            foxentity.setAge(-24000);
            foxentity.moveTo(this.animal.getX(), this.animal.getY(), this.animal.getZ(), 0.0F, 0.0F);
            serverworld.addFreshEntityWithPassengers(foxentity);
            this.level.broadcastEntityEvent(this.animal, (byte) 18);
            if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                if (experience > 0) {
                    this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), experience));
                }
            }

        }
    }
}
