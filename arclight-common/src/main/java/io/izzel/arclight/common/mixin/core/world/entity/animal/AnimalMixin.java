package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.entity.passive.AnimalEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.AgeableMobMixin;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
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
import java.util.Optional;

@Mixin(Animal.class)
public abstract class AnimalMixin extends AgeableMobMixin implements AnimalEntityBridge {

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
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
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
            this.breedItem = player.getInventory().getSelected();
        }
    }

    @Override
    public ItemStack bridge$getBreedItem() {
        return breedItem;
    }

    @Inject(method = "spawnChildFromBreeding", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$reason(ServerLevel level, Animal p_27565_, CallbackInfo ci) {
        ((WorldBridge) level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
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
        int experience = this.getRandom().nextInt(7) + 1;
        if (entityageable != null) {
            org.bukkit.event.entity.EntityBreedEvent entityBreedEvent = CraftEventFactory.callEntityBreedEvent(entityageable, (Animal) (Object) this, entityanimal, cause.orElse(null), this.breedItem, experience);
            if (entityBreedEvent.isCancelled()) {
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
        if (worldserver.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            // CraftBukkit start - use event experience
            if (experience > 0) {
                worldserver.addFreshEntity(new ExperienceOrb(worldserver, this.getX(), this.getY(), this.getZ(), experience));
            }
            // CraftBukkit end
        }
    }
}
