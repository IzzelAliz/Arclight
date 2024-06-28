package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.projectile.FishingHookBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.FishHook;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin extends ProjectileMixin implements FishingHookBridge {

    // @formatter:off
    @Shadow public Entity hookedIn;
    @Shadow private int nibble;
    @Shadow @Final private int luck;
    @Shadow public abstract Player getPlayerOwner();
    @Shadow private int timeUntilHooked;
    @Shadow private int timeUntilLured;
    @Shadow @Final private int lureSpeed;
    @Shadow public abstract void pullEntity(Entity p_150156_);
    // @formatter:on

    public int minWaitTime = 100;
    public int maxWaitTime = 600;
    public int minLureTime = 20;
    public int maxLureTime = 80;
    public float minLureAngle = 0.0F;
    public float maxLureAngle = 360.0F;
    public boolean applyLure = true;
    public boolean rainInfluenced = true;
    public boolean skyInfluenced = true;

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

    @Redirect(method = "catchingFish", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isRainingAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean arclight$rainInfluenced(Level level, BlockPos pos) {
        return this.rainInfluenced && level.isRainingAt(pos);
    }

    @Redirect(method = "catchingFish", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean arclight$skyInfluenced(Level instance, BlockPos blockPos) {
        return this.skyInfluenced && instance.canSeeSky(blockPos);
    }

    @Redirect(method = "catchingFish", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/util/Mth;nextFloat(Lnet/minecraft/util/RandomSource;FF)F"))
    private float arclight$lureAngleParam(RandomSource random, float p_216269_, float p_216270_) {
        return Mth.nextFloat(random, this.minLureAngle, this.maxLureAngle);
    }

    @Redirect(method = "catchingFish", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I"))
    private int arclight$lureTimeParam(RandomSource random, int p_216273_, int p_216274_) {
        return Mth.nextInt(random, this.minLureTime, this.maxLureTime);
    }

    @Redirect(method = "catchingFish", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I"))
    private int arclight$waitTimeParam(RandomSource random, int p_216273_, int p_216274_) {
        return Mth.nextInt(random, this.minWaitTime, this.maxWaitTime);
    }

    @Redirect(method = "catchingFish", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/projectile/FishingHook;lureSpeed:I"))
    private int arclight$waitTimeParam2(FishingHook instance) {
        return this.applyLure ? this.lureSpeed : 0;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;discard()V"))
    private void arclight$tickDespawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Inject(method = "shouldStopFishing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;discard()V"))
    private void arclight$fishDespawn(Player player, CallbackInfoReturnable<Boolean> cir) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Inject(method = "retrieve", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;pullEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$catchEntity(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        PlayerFishEvent fishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) this.getPlayerOwner()).bridge$getBukkitEntity(), this.hookedIn.bridge$getBukkitEntity(), (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.CAUGHT_ENTITY);
        Bukkit.getPluginManager().callEvent(fishEvent);
        if (fishEvent.isCancelled()) {
            cir.setReturnValue(0);
        }
    }

    @Decorate(method = "retrieve", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(DDD)V"))
    private void arclight$catchFish(ItemStack stack, @Local(ordinal = -1) ItemEntity itementity, @Local(allocate = "expToDrop") int expToDrop) throws Throwable {
        PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) this.getPlayerOwner()).bridge$getBukkitEntity(), itementity.bridge$getBukkitEntity(), (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.CAUGHT_FISH);
        playerFishEvent.setExpToDrop(this.random.nextInt(6) + 1);
        Bukkit.getPluginManager().callEvent(playerFishEvent);

        if (playerFishEvent.isCancelled()) {
            DecorationOps.cancel().invoke(0);
            return;
        }
        expToDrop = playerFishEvent.getExpToDrop();
        DecorationOps.blackhole().invoke(expToDrop);
    }

    @Decorate(method = "retrieve", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
        slice = @Slice(from = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDDI)Lnet/minecraft/world/entity/ExperienceOrb;")))
    private boolean arclight$spawnExpOrb(Level instance, Entity entity, ItemStack stack, @Local(allocate = "expToDrop") int expToDrop) throws Throwable {
        if (entity instanceof ExperienceOrb orb) {
            if (expToDrop <= 0) {
                return false;
            }
            orb.value = expToDrop;
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }

    @Inject(method = "retrieve", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;onGround()Z"))
    private void arclight$onGround(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        if (this.onGround()) {
            PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) this.getPlayerOwner()).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.IN_GROUND);
            Bukkit.getPluginManager().callEvent(playerFishEvent);

            if (playerFishEvent.isCancelled()) {
                cir.setReturnValue(0);
            }
        }
    }

    @Inject(method = "retrieve", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;discard()V"))
    private void arclight$reelIn(ItemStack itemStack, CallbackInfoReturnable<Integer> cir, Player player, int i) {
        if (i == 0) {
            PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.REEL_IN);
            Bukkit.getPluginManager().callEvent(playerFishEvent);
            if (playerFishEvent.isCancelled()) {
                cir.setReturnValue(0);
                return;
            }
        }
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }
}
