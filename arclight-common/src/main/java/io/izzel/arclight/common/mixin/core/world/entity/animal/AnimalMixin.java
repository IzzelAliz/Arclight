package io.izzel.arclight.common.mixin.core.world.entity.animal;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.entity.animal.AnimalBridge;
import io.izzel.arclight.common.bridge.core.world.entity.animal.AnimalFertilizeEggBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.AgeableMobMixin;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(Animal.class)
public abstract class AnimalMixin extends AgeableMobMixin implements AnimalBridge, AnimalFertilizeEggBridge {

    // @formatter:off
    @Shadow public InteractionResult mobInteract(Player playerIn, InteractionHand hand) { return null; }
    @Shadow public int inLove;
    @Shadow public abstract void resetLove();
    @Shadow @Nullable public abstract ServerPlayer getLoveCause();
    // @formatter:on

    public ItemStack breedItem;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void actuallyHurt(ServerLevel serverLevel, DamageSource arg, float f) {
        super.actuallyHurt(serverLevel, arg, f);
        if (!arclight$damageResult) {
            return;
        }
        this.resetLove();
    }

    @Inject(method = "setInLove(Lnet/minecraft/world/entity/player/Player;)V", cancellable = true, at = @At("HEAD"))
    private void arclight$enterLove(Player player, CallbackInfo ci) {
        EntityEnterLoveModeEvent event = CraftEventFactory.callEntityEnterLoveModeEvent(player, (Animal) (Object) this, 600);
        if (event.isCancelled()) {
            ci.cancel();
        } else {
            arclight$loveTime = event.getTicksInLove();
        }
    }

    private transient int arclight$loveTime;

    @Inject(method = "setInLove(Lnet/minecraft/world/entity/player/Player;)V", at = @At(value = "FIELD", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Animal;inLove:I"))
    private void arclight$inLove(Player player, CallbackInfo ci) {
        this.inLove = arclight$loveTime;
        if (player != null) {
            this.breedItem = player.getInventory().getSelectedItem();
        }
    }

    @Override
    public ItemStack bridge$getBreedItem() {
        return breedItem;
    }

    @Unique private Integer arclight$fertilizeEggExperience;

    @Override
    public @Nullable Integer bridge$setFertilizeEggExperience(@Nullable Integer experience) {
        Integer previous = this.arclight$fertilizeEggExperience;
        this.arclight$fertilizeEggExperience = experience;
        return previous;
    }

    private transient boolean arclight$breedCancelled;

    @WrapMethod(method = "spawnChildFromBreeding")
    private void arclight$breedingScope(ServerLevel level, Animal partner, Operation<Void> original) {
        this.arclight$breedCancelled = false;
        try {
            original.call(level, partner);
        } finally {
            this.arclight$breedCancelled = false;
        }
    }

    @WrapOperation(method = "spawnChildFromBreeding", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$spawnBreedingChild(ServerLevel level, Entity offspring, Operation<Void> original) {
        if (this.arclight$breedCancelled) {
            return;
        }
        ((WorldBridge) level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
        original.call(level, offspring);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void finalizeSpawnChildFromBreeding(ServerLevel worldserver, Animal entityanimal, @Nullable AgeableMob entityageable) {
        // CraftBukkit start - call EntityBreedEvent
        Optional<ServerPlayer> cause = Optional.ofNullable(this.getLoveCause()).or(() -> {
            return Optional.ofNullable(entityanimal.getLoveCause());
        });
        Integer fertilizeExperience = null;
        if (entityageable == null) {
            fertilizeExperience = this.arclight$fertilizeEggExperience;
            this.arclight$fertilizeEggExperience = null;
        }
        int experience = fertilizeExperience == null ? this.random.nextInt(7) + 1 : fertilizeExperience;
        if (entityageable != null) {
            org.bukkit.event.entity.EntityBreedEvent entityBreedEvent = CraftEventFactory.callEntityBreedEvent(entityageable, (Animal) (Object) this, entityanimal, cause.orElse(null), this.breedItem, experience);
            if (entityBreedEvent.isCancelled()) {
                this.arclight$breedCancelled = true;
                return;
            }
            experience = entityBreedEvent.getExperience();
        }
        cause.ifPresent((entityplayer) -> {
            // CraftBukkit end
            entityplayer.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(entityplayer, (Animal) (Object) this, entityanimal, entityageable);
        });
        this.setAge(6000);
        entityanimal.setAge(6000);
        this.resetLove();
        entityanimal.resetLove();
        worldserver.broadcastEntityEvent((Animal) (Object) this, (byte) 18);
        if (worldserver.getGameRules().get(GameRules.MOB_DROPS)) {
            // CraftBukkit start - use event experience
            if (experience > 0) {
                worldserver.addFreshEntity(new ExperienceOrb(worldserver, this.getX(), this.getY(), this.getZ(), experience));
            }
            // CraftBukkit end
        }
    }
}
