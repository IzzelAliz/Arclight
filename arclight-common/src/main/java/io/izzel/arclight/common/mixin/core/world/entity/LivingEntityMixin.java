package io.izzel.arclight.common.mixin.core.world.entity;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.PotionEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings({"ConstantConditions", "Guava"})
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin implements LivingEntityBridge {

    // @formatter:off
    @Shadow public abstract float getMaxHealth();
    @Shadow public abstract void heal(float healAmount);
    @Shadow public abstract float getHealth();
    @Shadow public abstract void setHealth(float health);
    @Shadow public abstract float getYHeadRot();
    @Shadow protected abstract int getExperienceReward(net.minecraft.world.entity.player.Player player);
    @Shadow protected int lastHurtByPlayerTime;
    @Shadow protected abstract boolean shouldDropExperience();
    @Shadow protected abstract boolean isAlwaysExperienceDropper();
    @Shadow public net.minecraft.world.entity.player.Player lastHurtByPlayer;
    @Shadow public int deathTime;
    @Shadow protected boolean dead;
    @Shadow public abstract AttributeInstance getAttribute(Attribute attribute);
    @Shadow public boolean effectsDirty;
    @Shadow public abstract boolean removeEffect(MobEffect effectIn);
    @Shadow public abstract boolean removeAllEffects();
    @Shadow @Final public static EntityDataAccessor<Float> DATA_HEALTH_ID;
    @Shadow public abstract boolean hasEffect(MobEffect potionIn);
    @Shadow public abstract boolean isSleeping();
    @Shadow public abstract void stopSleeping();
    @Shadow protected int noActionTime;
    @Shadow public abstract net.minecraft.world.item.ItemStack getItemBySlot(EquipmentSlot slotIn);
    @Shadow public abstract boolean isDamageSourceBlocked(DamageSource damageSourceIn);
    @Shadow protected abstract void hurtCurrentlyUsedShield(float damage);
    @Shadow protected abstract void blockUsingShield(LivingEntity entityIn);
    @Shadow public float animationSpeed;
    @Shadow public float lastHurt;
    @Shadow public int hurtDuration;
    @Shadow public int hurtTime;
    @Shadow public float hurtDir;
    @Shadow public abstract void setLastHurtByMob(@Nullable LivingEntity livingBase);
    @Shadow protected abstract void markHurt();
    @Shadow @Nullable protected abstract SoundEvent getDeathSound();
    @Shadow protected abstract float getSoundVolume();
    @Shadow public abstract float getVoicePitch();
    @Shadow public abstract void die(DamageSource cause);
    @Shadow protected abstract void playHurtSound(DamageSource source);
    @Shadow private DamageSource lastDamageSource;
    @Shadow private long lastDamageStamp;
    @Shadow protected abstract float getDamageAfterArmorAbsorb(DamageSource source, float damage);
    @Shadow public abstract net.minecraft.world.item.ItemStack getItemInHand(InteractionHand hand);
    @Shadow @Nullable public abstract MobEffectInstance getEffect(MobEffect potionIn);
    @Shadow protected abstract float getDamageAfterMagicAbsorb(DamageSource source, float damage);
    @Shadow public abstract float getAbsorptionAmount();
    @Shadow public abstract void setAbsorptionAmount(float amount);
    @Shadow public abstract CombatTracker getCombatTracker();
    @Shadow @Final private AttributeMap attributes;
    @Shadow public abstract boolean onClimbable();
    @Shadow protected ItemStack useItem;
    @Shadow public abstract void take(Entity entityIn, int quantity);
    @Shadow protected abstract void dropAllDeathLoot(DamageSource damageSourceIn);
    @Shadow public abstract ItemStack getMainHandItem();
    @Shadow public abstract void setSprinting(boolean sprinting);
    @Shadow public abstract void setLastHurtMob(Entity entityIn);
    @Shadow public abstract void setItemInHand(InteractionHand hand, ItemStack stack);
    @Shadow @Nullable public abstract LivingEntity getKillCredit();
    @Shadow protected int deathScore;
    @Shadow public abstract Collection<MobEffectInstance> getActiveEffects();
    @Shadow public abstract void setArrowCount(int count);
    @Shadow @Nullable public LivingEntity lastHurtByMob;
    @Shadow public CombatTracker combatTracker;
    @Shadow public abstract ItemStack getOffhandItem();
    @Shadow public abstract Random getRandom();
    @Shadow public abstract Optional<BlockPos> getSleepingPos();
    @Shadow @Final private static EntityDataAccessor<Integer> DATA_EFFECT_COLOR_ID;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID;
    @Shadow @Final public Map<MobEffect, MobEffectInstance> activeEffects;
    @Shadow protected abstract void onEffectRemoved(MobEffectInstance effect);
    @Shadow protected abstract void updateInvisibilityStatus();
    @Shadow public abstract boolean canBeAffected(MobEffectInstance potioneffectIn);
    @Shadow @Nullable public abstract MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect potioneffectin);
    @Shadow protected abstract void createWitherRose(@Nullable LivingEntity entitySource);
    @Shadow public abstract double getAttributeValue(Attribute attribute);
    @Shadow protected abstract void hurtArmor(DamageSource damageSource, float damage);
    @Shadow public abstract boolean isDeadOrDying();
    @Shadow public abstract int getArrowCount();
    @Shadow @Final public static EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID;
    @Shadow public abstract void setItemSlot(EquipmentSlot slotIn, ItemStack stack);
    @Shadow protected abstract void onEffectUpdated(MobEffectInstance p_147192_, boolean p_147193_, @org.jetbrains.annotations.Nullable Entity p_147194_);
    @Shadow protected abstract void onEffectAdded(MobEffectInstance p_147190_, @org.jetbrains.annotations.Nullable Entity p_147191_);
    @Shadow public abstract void knockback(double p_147241_, double p_147242_, double p_147243_);
    @Shadow protected abstract void equipEventAndSound(ItemStack p_147219_);
    @Shadow public abstract boolean canAttack(LivingEntity p_21171_);
    @Shadow public abstract boolean hasLineOfSight(Entity p_147185_);
    @Shadow protected abstract void hurtHelmet(DamageSource p_147213_, float p_147214_);
    @Shadow public abstract void stopUsingItem();
    // @formatter:on

    public int expToDrop;
    public int maxAirTicks;
    public boolean forceDrops;
    public CraftAttributeMap craftAttributes;
    public boolean collides;
    public boolean bukkitPickUpLoot;
    public Set<UUID> collidableExemptions = new HashSet<>();

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void arclight$muteHealth(LivingEntity entity, float health) {
        // do nothing
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends LivingEntity> type, Level worldIn, CallbackInfo ci) {
        this.maxAirTicks = 300;
        this.collides = true;
        this.craftAttributes = new CraftAttributeMap(this.attributes);
        this.entityData.set(DATA_HEALTH_ID, (float) this.getAttributeValue(Attributes.MAX_HEALTH));
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 20 && !this.isRemoved() && !this.level.isClientSide()) {
            this.level.broadcastEntityEvent((LivingEntity) (Object) this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Redirect(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience()V"))
    private void arclight$dropLater(LivingEntity livingEntity) {
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void arclight$dropLast(DamageSource damageSourceIn, CallbackInfo ci) {
        this.dropExperience();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void dropExperience() {
        // if (!this.world.isRemote && (this.isPlayer() || this.recentlyHit > 0 && this.canDropLoot() && this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT))) {
        if (true) {
            ExperienceOrb.award((ServerLevel) this.level, this.position(), this.expToDrop);
            bridge$setExpToDrop(0);
        }
    }

    private boolean isTickingEffects = false;
    private List<Map.Entry<Either<MobEffectInstance, MobEffect>, EntityPotionEffectEvent.Cause>> effectsToProcess = Lists.newArrayList();

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void tickEffects() {
        this.isTickingEffects = true;
        Iterator<MobEffect> iterator = this.activeEffects.keySet().iterator();

        try {
            while (iterator.hasNext()) {
                MobEffect effect = iterator.next();
                MobEffectInstance effectinstance = this.activeEffects.get(effect);
                if (!effectinstance.tick((LivingEntity) (Object) this, () -> {
                    onEffectUpdated(effectinstance, true, null);
                })) {
                    if (!this.level.isClientSide && !MinecraftForge.EVENT_BUS.post(new PotionEvent.PotionExpiryEvent((LivingEntity) (Object) this, effectinstance))) {

                        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effectinstance, null, EntityPotionEffectEvent.Cause.EXPIRATION);
                        if (event.isCancelled()) {
                            continue;
                        }

                        iterator.remove();
                        this.onEffectRemoved(effectinstance);
                    }
                } else if (effectinstance.getDuration() % 600 == 0) {
                    this.onEffectUpdated(effectinstance, false, null);
                }
            }
        } catch (ConcurrentModificationException ignored) {
        }

        isTickingEffects = false;
        for (Map.Entry<Either<MobEffectInstance, MobEffect>, EntityPotionEffectEvent.Cause> e : effectsToProcess) {
            Either<MobEffectInstance, MobEffect> either = e.getKey();
            EntityPotionEffectEvent.Cause cause = e.getValue();
            bridge$pushEffectCause(cause);
            if (either.left().isPresent()) {
                addEffect(either.left().get(), cause);
            } else {
                removeEffect(either.right().get(), cause);
            }
        }
        effectsToProcess.clear();

        if (this.effectsDirty) {
            if (!this.level.isClientSide) {
                this.updateInvisibilityStatus();
            }

            this.effectsDirty = false;
        }

        int i = this.entityData.get(DATA_EFFECT_COLOR_ID);
        boolean flag1 = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
        if (i > 0) {
            boolean flag;
            if (this.isInvisible()) {
                flag = this.random.nextInt(15) == 0;
            } else {
                flag = this.random.nextBoolean();
            }

            if (flag1) {
                flag &= this.random.nextInt(5) == 0;
            }

            if (flag && i > 0) {
                double d0 = (double) (i >> 16 & 255) / 255.0D;
                double d1 = (double) (i >> 8 & 255) / 255.0D;
                double d2 = (double) (i >> 0 & 255) / 255.0D;
                this.level.addParticle(flag1 ? ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT, this.getX() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(), this.getY() + this.random.nextDouble() * (double) this.getBbHeight(), this.getZ() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(), d0, d1, d2);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean addEffect(MobEffectInstance effectInstanceIn, Entity entity) {
        EntityPotionEffectEvent.Cause cause = bridge$getEffectCause().orElse(EntityPotionEffectEvent.Cause.UNKNOWN);
        if (isTickingEffects) {
            effectsToProcess.add(Maps.immutableEntry(Either.left(effectInstanceIn), cause));
            return true;
        }

        if (!this.canBeAffected(effectInstanceIn)) {
            return false;
        } else {
            MobEffectInstance effectinstance = this.activeEffects.get(effectInstanceIn.getEffect());

            boolean override = false;
            if (effectinstance != null) {
                override = new MobEffectInstance(effectinstance).update(effectInstanceIn);
            }

            EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effectinstance, effectInstanceIn, cause, override);
            if (event.isCancelled()) {
                return false;
            }

            MinecraftForge.EVENT_BUS.post(new PotionEvent.PotionAddedEvent((LivingEntity) (Object) this, effectinstance, effectInstanceIn, entity));
            if (effectinstance == null) {
                this.activeEffects.put(effectInstanceIn.getEffect(), effectInstanceIn);
                this.onEffectAdded(effectInstanceIn, entity);
                return true;
            } else if (event.isOverride()) {
                effectinstance.update(effectInstanceIn);
                this.onEffectUpdated(effectinstance, true, entity);
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressWarnings("unused") // mock
    public MobEffectInstance c(@Nullable MobEffect potioneffectin, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return removeEffectNoUpdate(potioneffectin);
    }

    @Inject(method = "removeEffectNoUpdate", cancellable = true, at = @At("HEAD"))
    public void arclight$clearActive(MobEffect effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        EntityPotionEffectEvent.Cause cause = bridge$getEffectCause().orElse(EntityPotionEffectEvent.Cause.UNKNOWN);
        if (isTickingEffects) {
            effectsToProcess.add(Maps.immutableEntry(Either.right(effect), cause));
            cir.setReturnValue(null);
            return;
        }

        MobEffectInstance effectInstance = this.activeEffects.get(effect);
        if (effectInstance == null) {
            cir.setReturnValue(null);
            return;
        }

        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effectInstance, null, cause);
        if (event.isCancelled()) {
            cir.setReturnValue(null);
        }
    }

    @Override
    public boolean bridge$canPickUpLoot() {
        return bukkitPickUpLoot;
    }

    @Override
    public float getBukkitYaw() {
        return getYHeadRot();
    }

    public int getExpReward() {
        if (!this.level.isClientSide && (this.lastHurtByPlayerTime > 0 || this.isAlwaysExperienceDropper()) && this.shouldDropExperience() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            int exp = this.getExperienceReward(this.lastHurtByPlayer);
            return ForgeEventFactory.getExperienceDrop((LivingEntity) (Object) this, this.lastHurtByPlayer, exp);
        } else {
            return 0;
        }
    }

    @Override
    public int bridge$getExpReward() {
        return getExpReward();
    }

    @Override
    public void bridge$setExpToDrop(int amount) {
        this.expToDrop = amount;
    }

    @Override
    public int bridge$getExpToDrop() {
        return this.expToDrop;
    }

    @Override
    public boolean bridge$isForceDrops() {
        return forceDrops;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    public void arclight$readMaxHealth(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("Bukkit.MaxHealth")) {
            Tag nbtbase = compound.get("Bukkit.MaxHealth");
            if (nbtbase.getId() == 5) {
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(((FloatTag) nbtbase).getAsDouble());
            } else if (nbtbase.getId() == 3) {
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(((IntTag) nbtbase).getAsDouble());
            }
        }
    }

    @Inject(method = "removeAllEffects", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"))
    public void arclight$clearReason(CallbackInfoReturnable<Boolean> cir) {
        arclight$action = EntityPotionEffectEvent.Action.CLEARED;
    }

    private transient EntityPotionEffectEvent.Action arclight$action;

    @Override
    public EntityPotionEffectEvent.Action bridge$getAndResetAction() {
        try {
            return arclight$action;
        } finally {
            arclight$action = null;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean isAlive() {
        return !this.isRemoved() && this.entityData.get(DATA_HEALTH_ID) > 0.0F;
    }

    @Inject(method = "getHealth", cancellable = true, at = @At("HEAD"))
    public void arclight$scaledHealth(CallbackInfoReturnable<Float> cir) {
        if (this instanceof ServerPlayerEntityBridge && ((ServerPlayerEntityBridge) this).bridge$initialized()) {
            cir.setReturnValue((float) ((ServerPlayerEntityBridge) this).bridge$getBukkitEntity().getHealth());
        }
    }

    @Inject(method = "setHealth", cancellable = true, at = @At("HEAD"))
    public void arclight$setScaled(float health, CallbackInfo ci) {
        if (this instanceof ServerPlayerEntityBridge && ((ServerPlayerEntityBridge) this).bridge$initialized()) {
            CraftPlayer player = ((ServerPlayerEntityBridge) this).bridge$getBukkitEntity();

            double realHealth = Mth.clamp(health, 0.0F, player.getMaxHealth());
            player.setRealHealth(realHealth);

            player.updateScaledHealth(false);
            player.setRealHealth(realHealth);
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean hurt(DamageSource source, float amount) {
        if (!ForgeHooks.onLivingAttack((LivingEntity) (Object) this, source, amount)) return false;
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.level.isClientSide) {
            return false;
        } else if (this.dead || this.isRemoved() || this.getHealth() <= 0.0F) {
            return false;
        } else if (source.isFire() && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        } else {
            if (this.isSleeping() && !this.level.isClientSide) {
                this.stopSleeping();
            }

            this.noActionTime = 0;
            float f = amount;
            if (false && (source == DamageSource.ANVIL || source == DamageSource.FALLING_BLOCK) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                this.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak((int) (amount * 4.0F + this.random.nextFloat() * amount * 2.0F), (LivingEntity) (Object) this, (p_213341_0_) -> {
                    p_213341_0_.broadcastBreakEvent(EquipmentSlot.HEAD);
                });
                amount *= 0.75F;
            }

            boolean flag = f > 0.0F && this.isDamageSourceBlocked(source); // Copied from below
            float f1 = 0.0F;

            if (false && amount > 0.0F && this.isDamageSourceBlocked(source)) {
                this.hurtCurrentlyUsedShield(amount);
                f1 = amount;
                amount = 0.0F;
                if (!source.isProjectile()) {
                    Entity entity = source.getDirectEntity();
                    if (entity instanceof LivingEntity) {
                        this.blockUsingShield((LivingEntity) entity);
                    }
                }

                flag = true;
            }

            this.animationSpeed = 1.5F;
            boolean flag1 = true;
            if ((float) this.invulnerableTime > 10.0F) {
                if (amount <= this.lastHurt) {
                    this.forceExplosionKnockback = true;
                    return false;
                }

                if (!this.damageEntity0(source, amount - this.lastHurt)) {
                    return false;
                }
                this.lastHurt = amount;
                flag1 = false;
            } else {
                if (!this.damageEntity0(source, amount)) {
                    return false;
                }
                this.lastHurt = amount;
                this.invulnerableTime = 20;
                this.hurtDuration = 10;
                this.hurtTime = this.hurtDuration;
            }

            if ((Object) this instanceof Animal) {
                ((Animal) (Object) this).resetLove();
                if ((Object) this instanceof TamableAnimal) {
                    ((TamableAnimal) (Object) this).setOrderedToSit(false);
                }
            }

            this.hurtDir = 0.0F;
            Entity entity1 = source.getEntity();
            if (entity1 != null) {
                if (entity1 instanceof LivingEntity) {
                    this.setLastHurtByMob((LivingEntity) entity1);
                }

                if (entity1 instanceof net.minecraft.world.entity.player.Player) {
                    this.lastHurtByPlayerTime = 100;
                    this.lastHurtByPlayer = (net.minecraft.world.entity.player.Player) entity1;
                } else if (entity1 instanceof TamableAnimal wolfentity) {
                    if (wolfentity.isTame()) {
                        this.lastHurtByPlayerTime = 100;
                        LivingEntity livingentity = wolfentity.getOwner();
                        if (livingentity != null && livingentity.getType() == EntityType.PLAYER) {
                            this.lastHurtByPlayer = (net.minecraft.world.entity.player.Player) livingentity;
                        } else {
                            this.lastHurtByPlayer = null;
                        }
                    }
                }
            }

            if (flag1) {
                if (flag) {
                    this.level.broadcastEntityEvent((LivingEntity) (Object) this, (byte) 29);
                } else if (source instanceof EntityDamageSource && ((EntityDamageSource) source).isThorns()) {
                    this.level.broadcastEntityEvent((LivingEntity) (Object) this, (byte) 33);
                } else {
                    byte b0;
                    if (source == DamageSource.DROWN) {
                        b0 = 36;
                    } else if (source.isFire()) {
                        b0 = 37;
                    } else if (source == DamageSource.SWEET_BERRY_BUSH) {
                        b0 = 44;
                    } else {
                        b0 = 2;
                    }

                    this.level.broadcastEntityEvent((LivingEntity) (Object) this, b0);
                }

                if (source != DamageSource.DROWN && (!flag || amount > 0.0F)) {
                    this.markHurt();
                }

                if (entity1 != null) {
                    double d1 = entity1.getX() - this.getX();

                    double d0;
                    for (d0 = entity1.getZ() - this.getZ(); d1 * d1 + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D) {
                        d1 = (Math.random() - Math.random()) * 0.01D;
                    }

                    this.hurtDir = (float) (Mth.atan2(d0, d1) * (double) (180F / (float) Math.PI) - (double) this.getYRot());
                    this.knockback(0.4F, d1, d0);
                } else {
                    this.hurtDir = (float) ((int) (Math.random() * 2.0D) * 180);
                }
            }

            if (this.getHealth() <= 0.0F) {
                if (!this.checkTotemDeathProtection(source)) {
                    SoundEvent soundevent = this.getDeathSound();
                    if (flag1 && soundevent != null) {
                        this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
                    }

                    this.die(source);
                }
            } else if (flag1) {
                this.playHurtSound(source);
            }

            boolean flag2 = !flag || amount > 0.0F;
            if (flag2) {
                this.lastDamageSource = source;
                this.lastDamageStamp = this.level.getGameTime();
            }

            if ((Object) this instanceof ServerPlayer) {
                CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer) (Object) this, source, f, amount, flag);
                if (f1 > 0.0F && f1 < 3.4028235E37F) {
                    ((ServerPlayer) (Object) this).awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f1 * 10.0F));
                }
            }

            if (entity1 instanceof ServerPlayer) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer) entity1, (LivingEntity) (Object) this, source, f, amount, flag);
            }

            return flag2;
        }
    }

    @Inject(method = "actuallyHurt", cancellable = true, at = @At("HEAD"))
    public void arclight$redirectDamageEntity(DamageSource damageSrc, float damageAmount, CallbackInfo ci) {
        damageEntity0(damageSrc, damageAmount);
        ci.cancel();
    }

    protected boolean damageEntity0(DamageSource damagesource, float f) {
        if (!this.isInvulnerableTo(damagesource)) {
            final boolean human = (Object) this instanceof net.minecraft.world.entity.player.Player;

            f = net.minecraftforge.common.ForgeHooks.onLivingHurt((LivingEntity) (Object) this, damagesource, f);
            if (f <= 0) return true;

            float originalDamage = f;
            Function<Double, Double> hardHat = f12 -> {
                if ((damagesource == DamageSource.ANVIL || damagesource == DamageSource.FALLING_BLOCK) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                    return -(f12 - (f12 * 0.75F));
                }
                return -0.0;
            };
            float hardHatModifier = hardHat.apply((double) f).floatValue();
            f += hardHatModifier;

            Function<Double, Double> blocking = f13 -> -((this.isDamageSourceBlocked(damagesource)) ? f13 : 0.0);
            float blockingModifier = blocking.apply((double) f).floatValue();
            f += blockingModifier;

            Function<Double, Double> armor = f14 -> -(f14 - this.getDamageAfterArmorAbsorb(damagesource, f14.floatValue()));
            float armorModifier = armor.apply((double) f).floatValue();
            f += armorModifier;

            Function<Double, Double> resistance = f15 -> {
                if (!damagesource.isBypassMagic() && this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && damagesource != DamageSource.OUT_OF_WORLD) {
                    int i = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                    int j = 25 - i;
                    float f1 = f15.floatValue() * (float) j;
                    return -(f15 - (f1 / 25.0F));
                }
                return -0.0;
            };
            float resistanceModifier = resistance.apply((double) f).floatValue();
            f += resistanceModifier;

            Function<Double, Double> magic = f16 -> -(f16 - this.getDamageAfterMagicAbsorb(damagesource, f16.floatValue()));
            float magicModifier = magic.apply((double) f).floatValue();
            f += magicModifier;

            Function<Double, Double> absorption = f17 -> -(Math.max(f17 - Math.max(f17 - this.getAbsorptionAmount(), 0.0F), 0.0F));
            float absorptionModifier = absorption.apply((double) f).floatValue();

            EntityDamageEvent event = CraftEventFactory.handleLivingEntityDamageEvent((LivingEntity) (Object) this, damagesource, originalDamage, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, hardHat, blocking, armor, resistance, magic, absorption);
            if (damagesource.getEntity() instanceof net.minecraft.world.entity.player.Player) {
                ((net.minecraft.world.entity.player.Player) damagesource.getEntity()).resetAttackStrengthTicker();
            }

            if (event.isCancelled()) {
                return false;
            }

            f = (float) event.getFinalDamage();

            // Resistance
            if (event.getDamage(EntityDamageEvent.DamageModifier.RESISTANCE) < 0) {
                float f3 = (float) -event.getDamage(EntityDamageEvent.DamageModifier.RESISTANCE);
                if (f3 > 0.0F && f3 < 3.4028235E37F) {
                    if ((Object) this instanceof ServerPlayer) {
                        ((ServerPlayer) (Object) this).awardStat(Stats.DAMAGE_RESISTED, Math.round(f3 * 10.0F));
                    } else if (damagesource.getEntity() instanceof ServerPlayer) {
                        ((ServerPlayer) damagesource.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(f3 * 10.0F));
                    }
                }
            }

            // Apply damage to helmet
            if (damagesource.isDamageHelmet() && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                this.hurtHelmet(damagesource, f);
            }

            // Apply damage to armor
            if (!damagesource.isBypassArmor()) {
                float armorDamage = (float) (event.getDamage() + event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) + event.getDamage(EntityDamageEvent.DamageModifier.HARD_HAT));
                this.hurtArmor(damagesource, armorDamage);
            }

            // Apply blocking code // PAIL: steal from above
            if (event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0) {
                this.level.broadcastEntityEvent((Entity) (Object) this, (byte) 29); // SPIGOT-4635 - shield damage sound
                this.hurtCurrentlyUsedShield((float) -event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING));
                Entity entity = damagesource.getDirectEntity();

                if (entity instanceof LivingEntity) {
                    this.blockUsingShield(((LivingEntity) entity));
                }
            }

            absorptionModifier = (float) -event.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
            this.setAbsorptionAmount(Math.max(this.getAbsorptionAmount() - absorptionModifier, 0.0F));
            float f2 = absorptionModifier;

            if (f2 > 0.0F && f2 < 3.4028235E37F && (Object) this instanceof net.minecraft.world.entity.player.Player) {
                ((net.minecraft.world.entity.player.Player) (Object) this).awardStat(Stats.DAMAGE_ABSORBED, Math.round(f2 * 10.0F));
            }
            if (f2 > 0.0F && f2 < 3.4028235E37F && damagesource.getEntity() instanceof net.minecraft.world.entity.player.Player) {
                ((net.minecraft.world.entity.player.Player) damagesource.getEntity()).awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f2 * 10.0F));
            }

            f = net.minecraftforge.common.ForgeHooks.onLivingDamage((LivingEntity) (Object) this, damagesource, f);

            if (f > 0 || !human) {
                if (human) {
                    // PAIL: Be sure to drag all this code from the EntityHuman subclass each update.
                    ((PlayerEntityBridge) this).bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.DAMAGED);
                    ((net.minecraft.world.entity.player.Player) (Object) this).causeFoodExhaustion(damagesource.getFoodExhaustion());
                    if (f < 3.4028235E37F) {
                        ((net.minecraft.world.entity.player.Player) (Object) this).awardStat(Stats.DAMAGE_TAKEN, Math.round(f * 10.0F));
                    }
                }
                // CraftBukkit end
                float f3 = this.getHealth();

                this.getCombatTracker().recordDamage(damagesource, f3, f);
                this.setHealth(f3 - f); // Forge: moved to fix MC-121048
                // CraftBukkit start
                if (!human) {
                    this.setAbsorptionAmount(this.getAbsorptionAmount() - f);
                }
                this.gameEvent(GameEvent.ENTITY_DAMAGED, damagesource.getEntity());

                return true;
            } else {
                // Duplicate triggers if blocking
                if (event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0) {
                    if ((Object) this instanceof ServerPlayer) {
                        CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer) (Object) this, damagesource, f, originalDamage, true);
                        f2 = (float) (-event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING));
                        if (f2 > 0.0f && f2 < 3.4028235E37f) {
                            ((ServerPlayer) (Object) this).awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(originalDamage * 10.0f));
                        }
                    }
                    if (damagesource.getEntity() instanceof ServerPlayer) {
                        CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer) damagesource.getEntity(), (Entity) (Object) this, damagesource, f, originalDamage, true);
                    }

                    return false;
                } else {
                    return originalDamage > 0;
                }
                // CraftBukkit end
            }
        }
        return false; // CraftBukkit
    }

    private transient EntityRegainHealthEvent.RegainReason arclight$regainReason;

    public void heal(float healAmount, EntityRegainHealthEvent.RegainReason regainReason) {
        bridge$pushHealReason(regainReason);
        this.heal(healAmount);
    }

    @Redirect(method = "heal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    public void arclight$healEvent(LivingEntity livingEntity, float health) {
        EntityRegainHealthEvent.RegainReason regainReason = arclight$regainReason == null ? EntityRegainHealthEvent.RegainReason.CUSTOM : arclight$regainReason;
        arclight$regainReason = null;
        float f = this.getHealth();
        float amount = health - f;
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(this.getBukkitEntity(), amount, regainReason);
        if (this.valid) {
            Bukkit.getPluginManager().callEvent(event);
        }

        if (!event.isCancelled()) {
            this.setHealth(this.getHealth() + (float) event.getAmount());
        }
    }

    @Inject(method = "heal", at = @At(value = "RETURN"))
    public void arclight$resetReason(float healAmount, CallbackInfo ci) {
        arclight$regainReason = null;
    }

    private transient EntityPotionEffectEvent.Cause arclight$cause;

    public boolean removeEffect(MobEffect effect, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return removeEffect(effect);
    }

    @Override
    public boolean bridge$removeEffect(MobEffect effect, EntityPotionEffectEvent.Cause cause) {
        return removeEffect(effect, cause);
    }

    public boolean addEffect(MobEffectInstance effect, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return this.addEffect(effect, (Entity) null);
    }

    public boolean removeAllEffects(EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return this.removeAllEffects();
    }

    @Override
    public boolean bridge$removeAllEffects(EntityPotionEffectEvent.Cause cause) {
        return removeAllEffects(cause);
    }

    public CraftLivingEntity getBukkitEntity() {
        return (CraftLivingEntity) internal$getBukkitEntity();
    }

    @Override
    public CraftLivingEntity bridge$getBukkitEntity() {
        return (CraftLivingEntity) internal$getBukkitEntity();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private boolean checkTotemDeathProtection(DamageSource damageSourceIn) {
        if (damageSourceIn.isBypassInvul()) {
            return false;
        } else {
            net.minecraft.world.item.ItemStack itemstack = null;

            net.minecraft.world.item.ItemStack itemstack1 = ItemStack.EMPTY;
            for (InteractionHand hand : InteractionHand.values()) {
                itemstack1 = this.getItemInHand(hand);
                if (itemstack1.getItem() == Items.TOTEM_OF_UNDYING) {
                    itemstack = itemstack1.copy();
                    // itemstack1.shrink(1);
                    break;
                }
            }

            EntityResurrectEvent event = new EntityResurrectEvent(this.getBukkitEntity());
            event.setCancelled(itemstack == null);
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                if (!itemstack1.isEmpty()) {
                    itemstack1.shrink(1);
                }
                if (itemstack != null && (Object) this instanceof ServerPlayer serverplayerentity) {
                    serverplayerentity.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                    CriteriaTriggers.USED_TOTEM.trigger(serverplayerentity, itemstack);
                }

                this.setHealth(1.0F);
                this.removeAllEffects(EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1), EntityPotionEffectEvent.Cause.TOTEM);
                bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1), EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 1), EntityPotionEffectEvent.Cause.TOTEM);
                this.level.broadcastEntityEvent((Entity) (Object) this, (byte) 35);
            }
            return !event.isCancelled();
        }
    }

    @Inject(method = "createWitherRose", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$witherRoseDrop(LivingEntity livingEntity, CallbackInfo ci, boolean flag, ItemEntity itemEntity) {
        org.bukkit.event.entity.EntityDropItemEvent event = new org.bukkit.event.entity.EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) (((EntityBridge) itemEntity).bridge$getBukkitEntity()));
        CraftEventFactory.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "createWitherRose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$fireWitherRoseForm(Level instance, BlockPos pPos, BlockState pNewState, int pFlags) {
        return CraftEventFactory.handleBlockFormEvent(instance, pPos, pNewState, 3, (Entity) (Object) this);
    }

    @Redirect(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    public void arclight$muteDamageArmor(LivingEntity entity, DamageSource damageSource, float damage) {
    }

    @Redirect(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    public boolean arclight$mutePotion(LivingEntity livingEntity, MobEffect potionIn) {
        return false;
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setSharedFlag(IZ)V"))
    public void arclight$stopGlide(LivingEntity livingEntity, int flag, boolean set) {
        if (set != livingEntity.getSharedFlag(flag) && !CraftEventFactory.callToggleGlideEvent(livingEntity, set).isCancelled()) {
            livingEntity.setSharedFlag(flag, set);
        }
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setSharedFlag(IZ)V"))
    public void arclight$toggleGlide(LivingEntity livingEntity, int flag, boolean set) {
        if (set != livingEntity.getSharedFlag(flag) && !CraftEventFactory.callToggleGlideEvent(livingEntity, set).isCancelled()) {
            livingEntity.setSharedFlag(flag, set);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean isPickable() {
        return !this.isRemoved() && this.collides;
    }

    /**
     * @author IzzrlAliz
     * @reason
     */
    @Overwrite
    public boolean isPushable() {
        return this.isAlive() && !this.onClimbable() && this.collides;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return this.isPushable() && this.collides != this.collidableExemptions.contains(entity.getUUID());
    }

    @Eject(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$itemConsume(ItemStack itemStack, Level worldIn, LivingEntity entityLiving, CallbackInfo ci) {
        if (this instanceof ServerPlayerEntityBridge) {
            final org.bukkit.inventory.ItemStack craftItem = CraftItemStack.asBukkitCopy(itemStack);
            final PlayerItemConsumeEvent event = new PlayerItemConsumeEvent((Player) this.getBukkitEntity(), craftItem);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ((ServerPlayerEntityBridge) this).bridge$getBukkitEntity().updateInventory();
                ((ServerPlayerEntityBridge) this).bridge$getBukkitEntity().updateScaledHealth();
                ci.cancel();
                return null;
            } else if (!craftItem.equals(event.getItem())) {
                return CraftItemStack.asNMSCopy(event.getItem()).finishUsingItem(worldIn, entityLiving);
            }
        }
        return itemStack.finishUsingItem(worldIn, entityLiving);
    }

    @Eject(method = "randomTeleport", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/LivingEntity;teleportTo(DDD)V"))
    private void arclight$entityTeleport(LivingEntity entity, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EntityTeleportEvent event = new EntityTeleportEvent(getBukkitEntity(), new Location(((WorldBridge) this.level).bridge$getWorld(), this.getX(), this.getY(), this.getZ()),
            new Location(((WorldBridge) this.level).bridge$getWorld(), x, y, z));
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            this.teleportTo(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
        } else {
            this.teleportTo(this.getX(), this.getY(), this.getZ());
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "dropAllDeathLoot", at = @At(value = "INVOKE", ordinal = 0, remap = false, target = "Lnet/minecraft/world/entity/LivingEntity;captureDrops(Ljava/util/Collection;)Ljava/util/Collection;"))
    private Collection<ItemEntity> arclight$captureIfNeed(LivingEntity livingEntity, Collection<ItemEntity> value) {
        Collection<ItemEntity> drops = livingEntity.captureDrops();
        // todo this instanceof ArmorStandEntity
        return drops == null ? livingEntity.captureDrops(value) : drops;
    }

    @Redirect(method = "dropAllDeathLoot", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V"))
    private void arclight$cancelEvent(Collection<ItemEntity> collection, Consumer<ItemEntity> action) {
        if (this instanceof ServerPlayerEntityBridge) {
            // recapture for ServerPlayerEntityMixin#onDeath
            this.captureDrops(collection);
        } else {
            collection.forEach(action);
        }
    }

    @Inject(method = "addEatEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    public void arclight$foodEffectCause(ItemStack p_213349_1_, Level p_213349_2_, LivingEntity livingEntity, CallbackInfo ci) {
        ((LivingEntityBridge) livingEntity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.FOOD);
    }

    @Override
    public void bridge$heal(float healAmount, EntityRegainHealthEvent.RegainReason regainReason) {
        this.heal(healAmount, regainReason);
    }

    @Override
    public void bridge$pushHealReason(EntityRegainHealthEvent.RegainReason regainReason) {
        arclight$regainReason = regainReason;
    }

    @Override
    public void bridge$pushEffectCause(EntityPotionEffectEvent.Cause cause) {
        arclight$cause = cause;
    }

    @Override
    public boolean bridge$addEffect(MobEffectInstance effect, EntityPotionEffectEvent.Cause cause) {
        return addEffect(effect, cause);
    }

    @Override
    public Optional<EntityPotionEffectEvent.Cause> bridge$getEffectCause() {
        try {
            return Optional.ofNullable(arclight$cause);
        } finally {
            arclight$cause = null;
        }
    }

    @Inject(method = "setArrowCount", cancellable = true, at = @At("HEAD"))
    private void arclight$onArrowChange(int count, CallbackInfo ci) {
        if (arclight$callArrowCountChange(count, false)) {
            ci.cancel();
        }
    }

    public final void setArrowCount(int count, boolean reset) {
        if (arclight$callArrowCountChange(count, reset)) {
            return;
        }
        this.entityData.set(DATA_ARROW_COUNT_ID, count);
    }

    private boolean arclight$callArrowCountChange(int newCount, boolean reset) {
        return CraftEventFactory.callArrowBodyCountChangeEvent((LivingEntity) (Object) this, this.getArrowCount(), newCount, reset).isCancelled();
    }

    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack, boolean silent) {
        this.setItemSlot(slotIn, stack);
    }

    @Override
    public void bridge$setSlot(EquipmentSlot slotIn, ItemStack stack, boolean silent) {
        this.setItemSlot(slotIn, stack, silent);
    }

    protected void equipEventAndSound(ItemStack stack, boolean silent) {
        if (!silent) {
            this.equipEventAndSound(stack);
        }
    }

    @Override
    public void bridge$playEquipSound(ItemStack stack, boolean silent) {
        this.equipEventAndSound(stack, silent);
    }
}
