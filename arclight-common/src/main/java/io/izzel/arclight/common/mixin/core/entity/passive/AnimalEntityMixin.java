package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.entity.passive.AnimalEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.AgeableEntityMixin;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin extends AgeableEntityMixin implements AnimalEntityBridge {

    // @formatter:off
    @Shadow public ActionResultType func_230254_b_(PlayerEntity p_230254_1_, Hand p_230254_2_) { return null; }
    @Shadow public int inLove;
    @Shadow public abstract void resetInLove();
    @Shadow @Nullable public abstract ServerPlayerEntity getLoveCause();
    // @formatter:on

    public ItemStack breedItem;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return super.attackEntityFrom(source, amount);
    }

    @Inject(method = "setInLove(Lnet/minecraft/entity/player/PlayerEntity;)V", cancellable = true, at = @At("HEAD"))
    private void arclight$enterLove(PlayerEntity player, CallbackInfo ci) {
        EntityEnterLoveModeEvent event = CraftEventFactory.callEntityEnterLoveModeEvent(player, (AnimalEntity) (Object) this, 600);
        if (event.isCancelled()) {
            ci.cancel();
        } else {
            arclight$loveTime = event.getTicksInLove();
        }
    }

    private transient int arclight$loveTime;

    @Inject(method = "setInLove(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At(value = "FIELD", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/passive/AnimalEntity;inLove:I"))
    private void arclight$inLove(PlayerEntity player, CallbackInfo ci) {
        this.inLove = arclight$loveTime;
        if (player != null) {
            this.breedItem = player.inventory.getCurrentItem();
        }
    }

    @Override
    public ItemStack bridge$getBreedItem() {
        return breedItem;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void func_234177_a_(ServerWorld world, AnimalEntity animalEntity) {
        AgeableEntity child = this.func_241840_a(world, animalEntity);
        final BabyEntitySpawnEvent event = new BabyEntitySpawnEvent((AnimalEntity) (Object) this, animalEntity, child);
        final boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
        child = event.getChild();
        if (cancelled) {
            //Reset the "inLove" state for the animals
            this.setGrowingAge(6000);
            animalEntity.setGrowingAge(6000);
            this.resetInLove();
            animalEntity.resetInLove();
            return;
        }
        if (child != null) {
            if (child instanceof TameableEntity && ((TameableEntity) child).isTamed()) {
                child.persistenceRequired = true;
            }
            ServerPlayerEntity serverplayerentity = this.getLoveCause();
            if (serverplayerentity == null && animalEntity.getLoveCause() != null) {
                serverplayerentity = animalEntity.getLoveCause();
            }

            int experience = this.getRNG().nextInt(7) + 1;
            org.bukkit.event.entity.EntityBreedEvent entityBreedEvent = CraftEventFactory.callEntityBreedEvent(child, (AnimalEntity) (Object) this, animalEntity, serverplayerentity, this.breedItem, experience);
            if (entityBreedEvent.isCancelled()) {
                return;
            }
            experience = entityBreedEvent.getExperience();

            if (serverplayerentity != null) {
                serverplayerentity.addStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(serverplayerentity, (AnimalEntity) (Object) this, animalEntity, child);
            }

            this.setGrowingAge(6000);
            animalEntity.setGrowingAge(6000);
            this.resetInLove();
            animalEntity.resetInLove();
            child.setChild(true);
            child.setLocationAndAngles(this.getPosX(), this.getPosY(), this.getPosZ(), 0.0F, 0.0F);
            ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
            world.func_242417_l(child);
            world.setEntityState((AnimalEntity) (Object) this, (byte) 18);
            if (world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                world.addEntity(new ExperienceOrbEntity(world, this.getPosX(), this.getPosY(), this.getPosZ(), experience));
            }

        }
    }
}
