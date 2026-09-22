package io.izzel.arclight.common.mixin.core.world.entity.player;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.food.FoodDataBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.mixin.core.world.entity.LivingEntityMixin;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.util.CraftVector;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.scoreboard.Team;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FrameNode;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerMixin extends LivingEntityMixin implements PlayerBridge {

    // @formatter:off
    @Shadow public abstract String getScoreboardName();
    @Shadow @Final private Abilities abilities;
    @Shadow public abstract float getAttackStrengthScale(float adjustTicks);
    @Shadow public abstract void resetAttackStrengthTicker();
    @Shadow public abstract SoundSource getSoundSource();
    @Shadow public abstract float getSpeed();
    @Shadow public abstract void crit(Entity entityHit);
    @Shadow public abstract void magicCrit(Entity entityHit);
    @Shadow public abstract void awardStat(Identifier p_195067_1_, int p_195067_2_);
    @Shadow public abstract void causeFoodExhaustion(float exhaustion);
    @Shadow public abstract Optional<net.minecraft.world.entity.animal.parrot.Parrot.Variant> getShoulderParrotRight();
    @Shadow public abstract Optional<net.minecraft.world.entity.animal.parrot.Parrot.Variant> getShoulderParrotLeft();
    @Shadow public abstract void setShoulderParrotRight(Optional<net.minecraft.world.entity.animal.parrot.Parrot.Variant> optional);
    @Shadow public abstract void setShoulderParrotLeft(Optional<net.minecraft.world.entity.animal.parrot.Parrot.Variant> optional);
    @Shadow public int experienceLevel;
    @Shadow @Final private Inventory inventory;
    @Shadow public AbstractContainerMenu containerMenu;
    @Shadow @Final public InventoryMenu inventoryMenu;
    @Shadow public abstract void awardStat(Stat<?> stat);
    @Shadow public abstract void awardStat(Identifier stat);
    @Shadow public abstract Component getDisplayName();
    @Shadow public float experienceProgress;
    @Shadow public int totalExperience;
    @Shadow protected FoodData foodData;
    @Shadow protected boolean isImmobile() { return false; }
    @Shadow protected PlayerEnderChestContainer enderChestInventory;
    @Shadow public abstract Either<net.minecraft.world.entity.player.Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos at);
    @Shadow public int sleepCounter;
    @Shadow public abstract GameProfile getGameProfile();
    @Shadow public abstract Inventory getInventory();
    @Shadow public abstract Abilities getAbilities();
    @Shadow public abstract void setLastDeathLocation(Optional<GlobalPos> p_219750_);
    @Shadow public abstract Optional<GlobalPos> getLastDeathLocation();
    @Shadow public abstract void setRemainingFireTicks(int p_36353_);
    @Shadow public abstract boolean isCreative();
    @Shadow public abstract FoodData getFoodData();
    @Shadow @Nullable public abstract ItemEntity drop(ItemStack arg, boolean bl);
    @Shadow public abstract boolean tryToStartFallFlying();
    // @formatter:on

    public boolean fauxSleeping;
    public int oldLevel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(CallbackInfo ci) {
        oldLevel = -1;
        ((FoodDataBridge) this.foodData).bridge$setEntityHuman((net.minecraft.world.entity.player.Player) (Object) this);
        ((IInventoryBridge) this.enderChestInventory).setOwner(this.getBukkitEntity());
    }

    @Override
    public boolean bridge$isFauxSleeping() {
        return fauxSleeping;
    }

    @Inject(method = "turtleHelmetTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$turtleHelmet(CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TURTLE_HELMET);
    }

    private transient boolean arclight$skipDropItemEvent;

    public ItemEntity drop(ItemStack itemstack, boolean flag, boolean flag1, boolean callEvent) {
        try {
            arclight$skipDropItemEvent = !callEvent;
            return this.drop(itemstack, flag1);
        } finally {
            arclight$skipDropItemEvent = false;
        }
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
        cancellable = true, at = @At("RETURN"))
    private void arclight$playerDropItem(ItemStack droppedItem, boolean traceItem, CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity itemEntity = cir.getReturnValue();
        if (arclight$skipDropItemEvent || itemEntity == null) {
            return;
        }
        Player player = (Player) this.getBukkitEntity();
        Item drop = (Item) itemEntity.bridge$getBukkitEntity();

        PlayerDropItemEvent event = new PlayerDropItemEvent(player, drop);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            org.bukkit.inventory.ItemStack cur = player.getInventory().getItemInHand();
            if (traceItem && (cur == null || cur.getAmount() == 0)) {
                // The complete stack was dropped
                player.getInventory().setItemInHand(drop.getItemStack());
            } else if (traceItem && cur.isSimilar(drop.getItemStack()) && cur.getAmount() < cur.getMaxStackSize() && drop.getItemStack().getAmount() == 1) {
                // Only one item is dropped
                cur.setAmount(cur.getAmount() + 1);
                player.getInventory().setItemInHand(cur);
            } else {
                // Fallback
                player.getInventory().addItem(drop.getItemStack());
            }
            cir.setReturnValue(null);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (this.isInvulnerableTo(serverLevel, source)) {
            return false;
        } else if (this.abilities.invulnerable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            this.noActionTime = 0;
            if (this.isDeadOrDying()) {
                return false;
            } else {
                var difficulty = this.level().getDifficulty();
                if (source.scalesWithDifficulty() && difficulty == Difficulty.PEACEFUL) {
                    return false;
                }
                amount = this.bridge$platform$scaleDamage(source, (net.minecraft.world.entity.player.Player) (Object) this, amount, difficulty);

                boolean damaged = super.hurtServer(serverLevel, source, amount);
                if (damaged) {
                    this.removeEntitiesOnShoulder();
                }
                return damaged;
                //return amount == 0.0F ? false : super.attackEntityFrom(source, amount);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canHarmPlayer(final net.minecraft.world.entity.player.Player entityhuman) {
        Team team;
        if (entityhuman instanceof ServerPlayer) {
            final ServerPlayer thatPlayer = (ServerPlayer) entityhuman;
            team = ((ServerPlayerBridge) thatPlayer).bridge$getBukkitEntity().getScoreboard().getPlayerTeam(((ServerPlayerBridge) thatPlayer).bridge$getBukkitEntity());
            if (team == null || team.allowFriendlyFire()) {
                return true;
            }
        } else {
            final OfflinePlayer thisPlayer = Bukkit.getOfflinePlayer(entityhuman.getScoreboardName());
            team = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(thisPlayer);
            if (team == null || team.allowFriendlyFire()) {
                return true;
            }
        }
        if ((Object) this instanceof ServerPlayer) {
            return !team.hasPlayer(((ServerPlayerBridge) this).bridge$getBukkitEntity());
        }
        return !team.hasPlayer(Bukkit.getOfflinePlayer(this.getScoreboardName()));
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onAttack()V"))
    private void arclight$skipResetAttackStrength(net.minecraft.world.entity.player.Player instance) {
    }

    @Decorate(method = "attack", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;deflectProjectile(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$nonLivingDamage(Entity entity, @Local(ordinal = -1) DamageSource damageSource, @Local(ordinal = 2) float enchantDamage) throws Throwable {
        if (entity.getType().is(net.minecraft.tags.EntityTypeTags.REDIRECTABLE_PROJECTILE)
            && entity instanceof net.minecraft.world.entity.projectile.Projectile
            && CraftEventFactory.handleNonLivingEntityDamageEvent(entity, damageSource, enchantDamage, false)) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Redirect(method = "doSweepAttack*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z")))
    private void arclight$skipKnockback(LivingEntity instance, double d, double e, double f) {
    }

    @Decorate(method = "doSweepAttack*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean arclight$applyKnockback(LivingEntity instance, ServerLevel serverLevel, DamageSource damageSource, float f) throws Throwable {
        var result = (boolean) DecorationOps.callsite().invoke(instance, serverLevel, damageSource, f);
        if (!result) {
            throw DecorationOps.jumpToLoopStart();
        }
        ((LivingEntityBridge) instance).bridge$pushKnockbackCause((Entity) (Object) this, EntityKnockbackEvent.KnockbackCause.SWEEP_ATTACK);
        instance.knockback(0.4f, Mth.sin(this.getYRot() * 0.017453292f), -Mth.cos(this.getYRot() * 0.017453292f));
        return result;
    }

    @Decorate(method = "causeExtraKnockback", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/entity/Entity;hurtMarked:Z"))
    private boolean arclight$velocityEvent(Entity entity, @Local(ordinal = -1) Vec3 deltaMovement) throws Throwable {
        boolean result = (boolean) DecorationOps.callsite().invoke(entity);
        if (result) {
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) entity.bridge$getBukkitEntity();
            org.bukkit.util.Vector velocity = CraftVector.toBukkit(deltaMovement);

            PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                result = false;
            } else if (!velocity.equals(event.getVelocity())) {
                player.setVelocity(event.getVelocity());
            }
        }
        return result;
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$foodExhaust(Entity entity, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.ATTACK);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;PLAYER_ATTACK_NODAMAGE:Lnet/minecraft/sounds/SoundEvent;")))
    private void arclight$updateInv(Entity entity, CallbackInfo ci) {
        if (this instanceof ServerPlayerBridge b) {
            b.bridge$getBukkitEntity().updateInventory();
        }
    }

    protected transient boolean arclight$forceSleep;

    public Either<net.minecraft.world.entity.player.Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos at, boolean force) {
        this.arclight$forceSleep = force;
        try {
            return this.startSleepInBed(at);
        } finally {
            this.arclight$forceSleep = false;
        }
    }

    @Override
    public Either<net.minecraft.world.entity.player.Player.BedSleepingProblem, Unit> bridge$trySleep(BlockPos at, boolean force) {
        return startSleepInBed(at, force);
    }

    @Inject(method = "stopSleepInBed", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;sleepCounter:I"))
    private void arclight$wakeup(boolean flag, boolean flag1, CallbackInfo ci) {
        BlockPos blockPos = this.getSleepingPos().orElse(null);
        if (this.bridge$getBukkitEntity() instanceof Player player) {
            Block bed;
            if (blockPos != null) {
                bed = CraftBlock.at(this.level(), blockPos);
            } else {
                bed = this.level().bridge$getWorld().getBlockAt(player.getLocation());
            }
            PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @Inject(method = "tryToStartFallFlying", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startFallFlying()V"))
    private void arclight$startGlidingEvent(CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.callToggleGlideEvent((net.minecraft.world.entity.player.Player) (Object) this, true).isCancelled()) {
            this.setSharedFlag(7, true);
            this.setSharedFlag(7, false);
            cir.setReturnValue(false);
        }
    }

    @Override
    public void stopFallFlying() {
        if (!CraftEventFactory.callToggleGlideEvent((net.minecraft.world.entity.player.Player) (Object) this, false).isCancelled()) {
            super.stopFallFlying();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void removeEntitiesOnShoulder() {
    }

    protected boolean respawnEntityOnShoulder(final Optional<net.minecraft.world.entity.animal.parrot.Parrot.Variant> parrotVariant) {
        if (this.level().isClientSide() || parrotVariant.isEmpty()) {
            return true;
        }
        var parrot = new net.minecraft.world.entity.animal.parrot.Parrot(EntityType.PARROT, this.level());
        parrot.setComponent(net.minecraft.core.component.DataComponents.PARROT_VARIANT, parrotVariant.get());
        parrot.setOwner((net.minecraft.world.entity.player.Player) (Object) this);
        parrot.setPos(this.getX(), this.getY() + 0.699999988079071, this.getZ());
        return ((ServerLevelBridge) this.level()).bridge$addEntitySerialized(parrot, CreatureSpawnEvent.SpawnReason.SHOULDER_ENTITY);
    }

    public CraftHumanEntity getBukkitEntity() {
        return (CraftHumanEntity) this.internal$getBukkitEntity();
    }

    @Override
    public CraftHumanEntity bridge$getBukkitEntity() {
        return (CraftHumanEntity) this.internal$getBukkitEntity();
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack, boolean silent) {
        if (slot == EquipmentSlot.MAINHAND) {
            this.equipEventAndSound(slot, this.inventory.setSelectedItem(stack), stack, silent);
        } else if (slot == EquipmentSlot.OFFHAND) {
            { ItemStack old = this.inventory.getItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND); this.inventory.setItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, stack); this.equipEventAndSound(slot, old, stack, silent); }
        } else if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            { int index = slot.getIndex() + 36; ItemStack old = this.inventory.getItem(index); this.inventory.setItem(index, stack); this.equipEventAndSound(slot, old, stack, silent); }
        }
    }

    @Decorate(method = "causeFoodExhaustion", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
    private void arclight$exhaustEvent(float amount) throws Throwable {
        EntityExhaustionEvent.ExhaustionReason reason = arclight$exhaustReason == null ? EntityExhaustionEvent.ExhaustionReason.UNKNOWN : arclight$exhaustReason;
        arclight$exhaustReason = null;
        EntityExhaustionEvent event = CraftEventFactory.callPlayerExhaustionEvent((net.minecraft.world.entity.player.Player) (Object) this, reason, amount);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        amount = event.getExhaustion();
        DecorationOps.blackhole().invoke(amount);
    }

    private EntityExhaustionEvent.ExhaustionReason arclight$exhaustReason;

    public void applyExhaustion(float f, EntityExhaustionEvent.ExhaustionReason reason) {
        bridge$pushExhaustReason(reason);
        this.causeFoodExhaustion(f);
    }

    @Override
    public void bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason reason) {
        arclight$exhaustReason = reason;
    }

    @Override
    public double bridge$platform$getBlockReach() {
        return isCreative() ? 5 : 4.5;
    }

}
