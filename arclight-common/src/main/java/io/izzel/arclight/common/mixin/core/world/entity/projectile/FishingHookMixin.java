package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin extends ProjectileMixin {

    // @formatter:off
    @Shadow public Entity hookedIn;
    @Shadow private int nibble;
    @Shadow @Final private int luck;
    @Shadow public abstract Player getPlayerOwner();
    @Shadow private int timeUntilHooked;
    @Shadow private int timeUntilLured;
    @Shadow @Final private int lureSpeed;
    @Shadow protected abstract void pullEntity(Entity p_150156_);
    // @formatter:on

    public int minWaitTime = 100;
    public int maxWaitTime = 600;
    public boolean applyLure = true;

    @Redirect(method = "checkCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private void arclight$collide(FishingHook fishingHook, HitResult hitResult) {
        this.preOnHit(hitResult);
    }

    @Inject(method = "catchingFish", at = @At(value = "FIELD", shift = At.Shift.AFTER, ordinal = 0, target = "Lnet/minecraft/world/entity/projectile/FishingHook;timeUntilHooked:I"))
    private void arclight$attemptFail(BlockPos blockPos, CallbackInfo ci) {
        PlayerFishEvent event = new PlayerFishEvent(((ServerPlayerEntityBridge) this.getPlayerOwner()).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.FAILED_ATTEMPT);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Inject(method = "catchingFish", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private void arclight$fishBite(BlockPos blockPos, CallbackInfo ci) {
        PlayerFishEvent event = new PlayerFishEvent(((ServerPlayerEntityBridge) this.getPlayerOwner()).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.BITE);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "catchingFish", at = @At("RETURN"))
    private void arclight$modifyWaitingTime(BlockPos p_37146_, CallbackInfo ci) {
        if (this.nibble <= 0 && this.timeUntilHooked <= 0 && this.timeUntilLured <= 0) {
            this.timeUntilLured = Mth.nextInt(this.random, this.minWaitTime, this.maxWaitTime);
            this.timeUntilLured -= (this.applyLure) ? this.lureSpeed * 20 * 5 : 0;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public int retrieve(ItemStack stack) {
        Player playerentity = this.getPlayerOwner();
        if (!this.level.isClientSide && playerentity != null) {
            int i = 0;
            ItemFishedEvent event = null;
            if (this.hookedIn != null) {
                PlayerFishEvent fishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) playerentity).bridge$getBukkitEntity(), ((EntityBridge) this.hookedIn).bridge$getBukkitEntity(), (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.CAUGHT_ENTITY);
                Bukkit.getPluginManager().callEvent(fishEvent);
                if (fishEvent.isCancelled()) {
                    return 0;
                }
                this.pullEntity(this.hookedIn);
                CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) playerentity, stack, (FishingHook) (Object) this, Collections.emptyList());
                this.level.broadcastEntityEvent((FishingHook) (Object) this, (byte) 31);
                i = this.hookedIn instanceof ItemEntity ? 3 : 5;
            } else if (this.nibble > 0) {
                LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerLevel) this.level)).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.TOOL, stack).withParameter(LootContextParams.THIS_ENTITY, (FishingHook) (Object) this).withRandom(this.random).withLuck((float) this.luck + playerentity.getLuck());
                LootTable loottable = this.level.getServer().getLootTables().get(BuiltInLootTables.FISHING);
                List<ItemStack> list = loottable.getRandomItems(lootcontext$builder.create(LootContextParamSets.FISHING));
                event = new ItemFishedEvent(list, this.onGround ? 2 : 1, (FishingHook) (Object) this);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled()) {
                    this.discard();
                    return event.getRodDamage();
                }
                CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) playerentity, stack, (FishingHook) (Object) this, list);

                for (ItemStack itemstack : list) {
                    ItemEntity itementity = new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), itemstack);
                    PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) playerentity).bridge$getBukkitEntity(), ((EntityBridge) itementity).bridge$getBukkitEntity(), (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.CAUGHT_FISH);
                    playerFishEvent.setExpToDrop(this.random.nextInt(6) + 1);
                    Bukkit.getPluginManager().callEvent(playerFishEvent);

                    if (playerFishEvent.isCancelled()) {
                        return 0;
                    }
                    double d0 = playerentity.getX() - this.getX();
                    double d1 = playerentity.getY() - this.getY();
                    double d2 = playerentity.getZ() - this.getZ();
                    double d3 = 0.1D;
                    itementity.setDeltaMovement(d0 * 0.1D, d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D, d2 * 0.1D);
                    this.level.addFreshEntity(itementity);
                    if (playerFishEvent.getExpToDrop() > 0) {
                        playerentity.level.addFreshEntity(new ExperienceOrb(playerentity.level, playerentity.getX(), playerentity.getY() + 0.5D, playerentity.getZ() + 0.5D, playerFishEvent.getExpToDrop()));
                    }
                    if (itemstack.is(ItemTags.FISHES)) {
                        playerentity.awardStat(Stats.FISH_CAUGHT, 1);
                    }
                }

                i = 1;
            }

            if (this.onGround) {
                PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) playerentity).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.IN_GROUND);
                Bukkit.getPluginManager().callEvent(playerFishEvent);

                if (playerFishEvent.isCancelled()) {
                    return 0;
                }
                i = 2;
            }

            if (i == 0) {
                PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) playerentity).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.REEL_IN);
                Bukkit.getPluginManager().callEvent(playerFishEvent);
                if (playerFishEvent.isCancelled()) {
                    return 0;
                }
            }

            this.discard();
            return event == null ? i : event.getRodDamage();
        } else {
            return 0;
        }
    }
}
