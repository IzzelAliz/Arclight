package io.izzel.arclight.common.mixin.core.world.entity.npc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.common.bridge.core.world.entity.npc.VillagerBridge;
import io.izzel.arclight.common.bridge.core.world.item.trading.MerchantOfferBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.LevelEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.slf4j.Logger;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(net.minecraft.world.entity.npc.villager.Villager.class)
public abstract class VillagerMixin extends AbstractVillagerMixin implements VillagerBridge {

    @org.spongepowered.asm.mixin.Shadow protected abstract void customServerAiStep(ServerLevel level);
    @org.spongepowered.asm.mixin.Shadow protected abstract void maybeDecayGossip();
    @org.spongepowered.asm.mixin.Unique private boolean arclight$inactiveVillagerAi;

    @Override
    public void bridge$inactiveTick() {
        var villager = (net.minecraft.world.entity.npc.villager.Villager) (Object) this;
        if (villager.getUnhappyCounter() > 0) villager.setUnhappyCounter(villager.getUnhappyCounter() - 1);
        if (this.aware && villager.isEffectiveAi()) {
            boolean previous = this.arclight$inactiveVillagerAi;
            this.arclight$inactiveVillagerAi = !((WorldBridge) villager.level()).bridge$spigotConfig().tickInactiveVillagers;
            try {
                this.customServerAiStep((ServerLevel) villager.level());
            } finally {
                this.arclight$inactiveVillagerAi = previous;
            }
        }
        this.maybeDecayGossip();
        super.bridge$inactiveTick();
    }

    @com.llamalad7.mixinextras.injector.v2.WrapWithCondition(method = "customServerAiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private boolean arclight$inactiveBrain(net.minecraft.world.entity.ai.Brain<?> brain, ServerLevel level, net.minecraft.world.entity.LivingEntity entity) {
        return !this.arclight$inactiveVillagerAi;
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "customServerAiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;isNoAi()Z"))
    private boolean arclight$inactiveRaid(boolean noAi) {
        return noAi || this.arclight$inactiveVillagerAi;
    }

    @Inject(method = "customServerAiStep", cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/AbstractVillager;customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$inactiveSuperclass(ServerLevel level, CallbackInfo ci) {
        if (this.arclight$inactiveVillagerAi) ci.cancel();
    }


    @Override
    public ZombieVillager bridge$zombify(ServerLevel level, BlockPos blockPosition, boolean silent, CreatureSpawnEvent.SpawnReason spawnReason) {
        if (!this.bridge$forge$canZombify()) {
            return null;
        }
        net.minecraft.world.entity.npc.villager.Villager villager = (net.minecraft.world.entity.npc.villager.Villager) (Object) this;
        WorldBridge world = (WorldBridge) villager.level();
        CreatureSpawnEvent.SpawnReason previousReason = world.bridge$getAddEntityReason();
        EntityTransformEvent.TransformReason previousTransform = this.bridge$getTransformReason();
        world.bridge$pushAddEntityReason(spawnReason);
        this.bridge$pushTransformReason(EntityTransformEvent.TransformReason.INFECTION);
        try {
            // Keep preparation inside native convertTo's callback: Bukkit transform/spawn
            // observers must see the finalized zombie and the copied villager data.
            return villager.convertTo(EntityType.ZOMBIE_VILLAGER, ConversionParams.single(villager, true, true), zombie -> {
                this.bridge$forge$finalizeZombifySpawn(level, zombie);
                zombie.setVillagerData(villager.getVillagerData());
                zombie.setGossips(villager.getGossips().copy());
                zombie.setTradeOffers(villager.getOffers().copy());
                zombie.setVillagerXp(villager.getVillagerXp());
                ((LivingEntityBridge) villager).bridge$forge$onLivingConvert(villager, zombie);
                if (!silent) {
                    level.levelEvent(null, LevelEvent.SOUND_ZOMBIE_INFECTED, blockPosition, 0);
                }
                // Finalization can spawn a jockey and consume/replace the world reason.
                // Restore the supplied reason for the actual converted zombie.
                world.bridge$pushAddEntityReason(spawnReason);
            });
        } finally {
            this.bridge$pushTransformReason(previousTransform);
            world.bridge$pushAddEntityReason(previousReason);
        }
    }

    @Decorate(method = "die", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void arclight$logVillagerDeaths(Logger instance, String s, Object o1, Object o2) throws Throwable {
        if (!SpigotConfig.logVillagerDeaths) {
            return;
        }
        DecorationOps.callsite().invoke(instance, s, o1, o2);
    }

    @Inject(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$reason(CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.VILLAGER_TRADE);
    }

    @Redirect(method = "restock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V"))
    private void arclight$restock(MerchantOffer instance) {
        VillagerReplenishTradeEvent event = new VillagerReplenishTradeEvent((Villager) this.getBukkitEntity(), ((MerchantOfferBridge) instance).bridge$asBukkit());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            instance.resetUses();
        }
    }

    @Redirect(method = "catchUpDemand", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V"))
    private void arclight$replenish(MerchantOffer instance) {
        VillagerReplenishTradeEvent event = new VillagerReplenishTradeEvent((Villager) this.getBukkitEntity(), ((MerchantOfferBridge) instance).bridge$asBukkit());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            instance.resetUses();
        }
    }

    @Decorate(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob arclight$transformWitch(net.minecraft.world.entity.npc.villager.Villager villager, EntityType<? extends Mob> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion afterConversion) throws Throwable {
        ((WorldBridge) villager.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
        ((MobBridge) villager).bridge$pushTransformReason(EntityTransformEvent.TransformReason.LIGHTNING);
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.TRANSFORMATION);
        return (Mob) DecorationOps.callsite().invoke(villager, entityType, conversionParams, afterConversion);
    }

    @WrapOperation(method = "spawnGolemIfNeeded(Lnet/minecraft/server/level/ServerLevel;JI)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SpawnUtil;trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;Z)Ljava/util/Optional;"),
        require = 1, allow = 1)
    private <T extends Mob> Optional<T> arclight$ironGolemPreSpawn(EntityType<T> type, EntitySpawnReason spawnReason,
            ServerLevel level, BlockPos start, int attempts, int rangeXZ, int rangeY, SpawnUtil.Strategy strategy,
            boolean checkCollisions, Operation<Optional<T>> original) {
        return ArclightCaptures.withPreCreatureSpawn(CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE,
            () -> GolemSensor.golemDetected((net.minecraft.world.entity.npc.villager.Villager) (Object) this),
            () -> original.call(type, spawnReason, level, start, attempts, rangeXZ, rangeY, strategy, checkCollisions));
    }
}
