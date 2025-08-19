package io.izzel.arclight.common.mixin.core.world.entity;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerGamePacketListenerBridge;
import io.izzel.arclight.common.bridge.core.world.IWorldBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.util.IteratorUtil;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings({"ConstantConditions", "Guava"})
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin implements LivingEntityBridge {

    // @formatter:off
    @Shadow public abstract float getMaxHealth();
    @Shadow public abstract void heal(float healAmount);
    @Shadow public abstract float getHealth();
    @Shadow public abstract void setHealth(float health);
    @Shadow public abstract float getYHeadRot();
    @Shadow protected int lastHurtByPlayerTime;
    @Shadow protected abstract boolean shouldDropExperience();
    @Shadow protected abstract boolean isAlwaysExperienceDropper();
    @Shadow public net.minecraft.world.entity.player.Player lastHurtByPlayer;
    @Shadow public int deathTime;
    @Shadow public boolean effectsDirty;
    @Shadow public abstract boolean removeAllEffects();
    @Shadow @Final public static EntityDataAccessor<Float> DATA_HEALTH_ID;
    @Shadow public abstract boolean isSleeping();
    @Shadow protected int noActionTime;
    @Shadow public abstract net.minecraft.world.item.ItemStack getItemBySlot(EquipmentSlot slotIn);
    @Shadow @Nullable protected abstract SoundEvent getDeathSound();
    @Shadow public abstract net.minecraft.world.item.ItemStack getItemInHand(InteractionHand hand);
    @Shadow public abstract CombatTracker getCombatTracker();
    @Shadow @Final private AttributeMap attributes;
    @Shadow public abstract void take(Entity entityIn, int quantity);
    @Shadow public abstract ItemStack getMainHandItem();
    @Shadow @Nullable public LivingEntity lastHurtByMob;
    @Shadow public CombatTracker combatTracker;
    @Shadow public abstract ItemStack getOffhandItem();
    @Shadow public abstract Optional<BlockPos> getSleepingPos();
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID;
    @Shadow @Final public Map<MobEffect, MobEffectInstance> activeEffects;
    @Shadow public abstract boolean isDeadOrDying();
    @Shadow public abstract int getArrowCount();
    @Shadow @Final public static EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID;
    @Shadow public abstract void setItemSlot(EquipmentSlot slotIn, ItemStack stack);
    @Shadow public abstract void knockback(double p_147241_, double p_147242_, double p_147243_);
    @Shadow public abstract void stopUsingItem();
    @Shadow protected abstract boolean doesEmitEquipEvent(EquipmentSlot p_217035_);
    @Shadow protected abstract void verifyEquippedItem(ItemStack p_181123_);
    @Shadow public abstract boolean wasExperienceConsumed();
    @Shadow @Nullable protected abstract SoundEvent getHurtSound(DamageSource p_21239_);
    @Shadow protected abstract SoundEvent getFallDamageSound(int p_21313_);
    @Shadow protected abstract SoundEvent getDrinkingSound(ItemStack p_21174_);
    @Shadow public abstract SoundEvent getEatingSound(ItemStack p_21202_);
    @Shadow public abstract InteractionHand getUsedItemHand();
    @Shadow public int invulnerableDuration;
    @Shadow protected void actuallyHurt(DamageSource p_21240_, float p_21241_) {}
    @Shadow public abstract void skipDropExperience();
    @Shadow public abstract int getExperienceReward(ServerLevel serverLevel, @org.jetbrains.annotations.Nullable Entity entity);
    @Shadow @org.jetbrains.annotations.Nullable public abstract AttributeInstance getAttribute(Holder<Attribute> holder);
    @Shadow public abstract double getAttributeValue(Holder<Attribute> holder);
    @Shadow public abstract boolean removeEffect(Holder<MobEffect> holder);
    @Shadow public abstract boolean addEffect(MobEffectInstance mobEffectInstance, @org.jetbrains.annotations.Nullable Entity entity);
    @Shadow @org.jetbrains.annotations.Nullable public abstract MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> holder);
    @Shadow public abstract EquipmentSlot getEquipmentSlotForItem(ItemStack itemStack);
    @Shadow protected abstract void dropAllDeathLoot(ServerLevel serverLevel, DamageSource damageSource);
    @Shadow public abstract void onEquipItem(EquipmentSlot slot, ItemStack original, ItemStack newStack);
    // @formatter:on

    @Shadow public abstract boolean isDamageSourceBlocked(DamageSource arg);

    @Shadow public float lastHurt;

    @Shadow public abstract boolean hasEffect(Holder<MobEffect> arg);

    @Shadow @Nullable public abstract MobEffectInstance getEffect(Holder<MobEffect> arg);

    @Shadow public abstract float getAbsorptionAmount();

    @Shadow public abstract int getArmorValue();

    public int expToDrop;
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
        this.collides = true;
        this.craftAttributes = new CraftAttributeMap(this.attributes);
        this.entityData.set(DATA_HEALTH_ID, (float) this.getAttributeValue(Attributes.MAX_HEALTH));
    }

    public SoundEvent getHurtSound0(DamageSource damagesource) {
        return getHurtSound(damagesource);
    }

    public SoundEvent getDeathSound0() {
        return getDeathSound();
    }

    public SoundEvent getFallDamageSound0(int fallHeight) {
        return getFallDamageSound(fallHeight);
    }

    public SoundEvent getDrinkingSound0(ItemStack itemstack) {
        return getDrinkingSound(itemstack);
    }

    public SoundEvent getEatingSound0(ItemStack itemstack) {
        return getEatingSound(itemstack);
    }

    // Preserve NMS invocation for Citizens2
    @Invoker("detectEquipmentUpdates")
    public abstract void detectEquipmentUpdatesPublic();

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void dropExperience(Entity entity) {
        // if (!this.world.isRemote && (this.isPlayer() || this.recentlyHit > 0 && this.canDropLoot() && this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT))) {
        if (!((Object) this instanceof EnderDragon)) {
            int reward = this.bridge$forge$getExperienceDrop((LivingEntity) (Object) this, this.lastHurtByPlayer, this.expToDrop);
            ExperienceOrb.award((ServerLevel) this.level(), this.position(), reward);
            bridge$setExpToDrop(0);
        }
    }

    private boolean isTickingEffects = false;
    private final List<Map.Entry<Either<MobEffectInstance, Holder<MobEffect>>, EntityPotionEffectEvent.Cause>> effectsToProcess = Lists.newArrayList();

    @Inject(method = "tickEffects", at = @At("HEAD"))
    private void arclight$startTicking(CallbackInfo ci) {
        this.isTickingEffects = true;
    }

    @Decorate(method = "tickEffects", inject = true, at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"))
    private void arclight$effectExpire(@Local(ordinal = -1) MobEffectInstance mobeffect) throws Throwable {
        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, mobeffect, null, EntityPotionEffectEvent.Cause.EXPIRATION);
        if (event.isCancelled()) {
            throw DecorationOps.jumpToLoopStart();
        }
    }

    @Inject(method = "tickEffects", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/entity/LivingEntity;effectsDirty:Z"))
    private void arclight$pendingEffects(CallbackInfo ci) {
        isTickingEffects = false;
        for (var e : effectsToProcess) {
            var either = e.getKey();
            var cause = e.getValue();
            if (either.left().isPresent()) {
                addEffect(either.left().get(), cause);
            } else {
                removeEffect(either.right().get(), cause);
            }
        }
        effectsToProcess.clear();
    }

    private transient EntityPotionEffectEvent.Cause arclight$cause;

    public boolean addEffect(MobEffectInstance effect, EntityPotionEffectEvent.Cause cause) {
        return this.addEffect(effect, null, cause);
    }

    public boolean addEffect(MobEffectInstance effect, Entity entity, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return this.addEffect(effect, entity);
    }

    public boolean removeAllEffects(EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return this.removeAllEffects();
    }

    @Override
    public boolean bridge$removeAllEffects(EntityPotionEffectEvent.Cause cause) {
        return removeAllEffects(cause);
    }

    @Decorate(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", inject = true, at = @At(value = "JUMP", opcode = Opcodes.IFNE, ordinal = 0))
    private void arclight$addPendingEffects(MobEffectInstance mobEffectInstance, Entity entity,
                                            @Local(allocate = "cause") EntityPotionEffectEvent.Cause cause) throws Throwable {
        cause = bridge$getEffectCause().orElse(EntityPotionEffectEvent.Cause.UNKNOWN);
        if (isTickingEffects) {
            effectsToProcess.add(Maps.immutableEntry(Either.left(mobEffectInstance), cause));
            DecorationOps.cancel().invoke(true);
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Decorate(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", inject = true,
        at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private void arclight$effectAdd(MobEffectInstance mobEffectInstance, Entity entity, @Local(allocate = "cause") EntityPotionEffectEvent.Cause cause) throws Throwable {
        var event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, null, mobEffectInstance, cause, false);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Decorate(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;update(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean arclight$effectReplace(MobEffectInstance oldEffect, MobEffectInstance newEffect,
                                           MobEffectInstance mobEffectInstance, Entity entity, @Local(allocate = "cause") EntityPotionEffectEvent.Cause cause) throws Throwable {
        var override = new MobEffectInstance(oldEffect).update(newEffect);
        var event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, oldEffect, newEffect, cause, override);
        if (event.isCancelled()) {
            return (boolean) DecorationOps.cancel().invoke(false);
        }
        if (event.isOverride()) {
            var b = (boolean) DecorationOps.callsite().invoke(oldEffect, newEffect);
            DecorationOps.blackhole().invoke(b);
        }
        return event.isOverride();
    }

    public boolean removeEffect(Holder<MobEffect> effect, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return removeEffect(effect);
    }

    @Override
    public boolean bridge$removeEffect(Holder<MobEffect> effect, EntityPotionEffectEvent.Cause cause) {
        return removeEffect(effect, cause);
    }

    public MobEffectInstance removeEffectNoUpdate(@Nullable Holder<MobEffect> holder, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return removeEffectNoUpdate(holder);
    }

    @Inject(method = "removeEffectNoUpdate", cancellable = true, at = @At("HEAD"))
    public void arclight$clearActive(Holder<MobEffect> holder, CallbackInfoReturnable<MobEffectInstance> cir) {
        EntityPotionEffectEvent.Cause cause = bridge$getEffectCause().orElse(EntityPotionEffectEvent.Cause.UNKNOWN);
        if (isTickingEffects) {
            effectsToProcess.add(Maps.immutableEntry(Either.right(holder), cause));
            cir.setReturnValue(null);
            return;
        }

        MobEffectInstance effectInstance = this.activeEffects.get(holder);
        if (effectInstance == null) {
            cir.setReturnValue(null);
            return;
        }

        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effectInstance, null, cause);
        if (event.isCancelled()) {
            cir.setReturnValue(null);
        }
    }

    // Fix entity removing itself when effect is removed causes looping remove
    @Unique private boolean removing = false;

    @Inject(method = "remove", cancellable = true, at = @At("HEAD"))
    private void arclight$beginOnDeathMobEffects(Entity.RemovalReason removalReason, CallbackInfo ci) {
        if (removing) {
            ci.cancel();
            ArclightServer.LOGGER.warn("remove() is called recursively, skipping");
            return;
        }
        removing = true;
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void arclight$endOnDeathMobEffects(CallbackInfo ci) {
        removing = false;
    }

    @Decorate(method = "triggerOnDeathMobEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;onMobRemoved(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void arclight$fireRemoveEvents(MobEffectInstance instance, LivingEntity arg, Entity.RemovalReason arg2) throws Throwable {
        DecorationOps.callsite().invoke(instance, arg, arg2);
        CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, instance, null, EntityPotionEffectEvent.Cause.DEATH);
        // We don't care about your result because the entity is removed dead!
    }

    @Override
    public boolean bridge$canPickUpLoot() {
        return bukkitPickUpLoot;
    }

    @Override
    public float getBukkitYaw() {
        return getYHeadRot();
    }

    public int getExpReward(Entity entity) {
        if (this.level() instanceof ServerLevel serverLevel && !this.wasExperienceConsumed() && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerTime > 0 && this.shouldDropExperience() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
            int exp = this.getExperienceReward(serverLevel, entity);
            return this.bridge$forge$getExperienceDrop((LivingEntity) (Object) this, this.lastHurtByPlayer, exp);
        } else {
            return 0;
        }
    }

    @Override
    public int bridge$getExpReward(Entity entity) {
        return getExpReward(entity);
    }

    @Override
    public void bridge$setExpToDrop(int amount) {
        this.expToDrop = amount;
    }

    @Override
    public int bridge$getExpToDrop() {
        return this.expToDrop;
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

    @SuppressWarnings("unchecked")
    @Decorate(method = "removeAllEffects", at = @At(value = "INVOKE", target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;"))
    private Iterator<MobEffectInstance> arclight$clearReason(Collection<MobEffectInstance> instance) throws Throwable {
        var cause = bridge$getEffectCause().orElse(EntityPotionEffectEvent.Cause.UNKNOWN);
        return IteratorUtil.filter((Iterator<MobEffectInstance>) DecorationOps.callsite().invoke(instance), effect -> {
            EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effect, null, cause, EntityPotionEffectEvent.Action.CLEARED);
            return !event.isCancelled();
        });
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

    @Inject(method = "blockedByShield", at = @At("HEAD"))
    private void arclight$shieldKnockback(LivingEntity livingEntity, CallbackInfo ci) {
        this.bridge$pushKnockbackCause(null, EntityKnockbackEvent.KnockbackCause.SHIELD_BLOCK);
    }

    private transient Entity arclight$knockbackAttacker;
    private transient EntityKnockbackEvent.KnockbackCause arclight$knockbackCause;

    @Override
    public void bridge$pushKnockbackCause(Entity attacker, EntityKnockbackEvent.KnockbackCause cause) {
        this.arclight$knockbackAttacker = attacker;
        this.arclight$knockbackCause = cause;
    }

    public void knockback(double d, double e, double f, Entity attacker, EntityKnockbackEvent.KnockbackCause cause) {
        this.bridge$pushKnockbackCause(attacker, cause);
        this.knockback(d, e, f);
    }

    @Decorate(method = "knockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
    private void arclight$knockbackEvent(LivingEntity instance, double x, double y, double z, double d, double e, double f) throws Throwable {
        var attacker = arclight$knockbackAttacker;
        var cause = arclight$knockbackCause == null ? EntityKnockbackEvent.KnockbackCause.UNKNOWN : arclight$knockbackCause;
        arclight$knockbackAttacker = null;
        arclight$knockbackCause = null;
        var raw = (new Vec3(e, 0.0, f)).normalize().scale(d);
        var event = CraftEventFactory.callEntityKnockbackEvent(this.getBukkitEntity(), attacker, cause, d, raw, x, y, z);
        if (!event.isCancelled()) {
            DecorationOps.callsite().invoke(instance, event.getFinalKnockback().getX(), event.getFinalKnockback().getY(), event.getFinalKnockback().getZ());
        }
    }

    protected transient boolean arclight$damageResult;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void arclight$resetDamageResult(DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        arclight$damageResult = false;
    }

    @Decorate(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I"),
        slice = @Slice(to = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;BYPASSES_COOLDOWN:Lnet/minecraft/tags/TagKey;")))
    private int arclight$useInvulnerableDuration(LivingEntity instance) throws Throwable {
        int result = (int) DecorationOps.callsite().invoke(instance);
        return result + 10 - (int) (this.invulnerableDuration / 2.0F);
    }

    @Override
    public EntityDamageEvent arclight$fireEntityDamageEvent(DamageSource source, float original) {
        float damage = original;

        Function<Double, Double> blocking = f -> -((this.isDamageSourceBlocked(source)) ? f : 0.0);
        float blockingModifier = blocking.apply((double) damage).floatValue();
        damage += blockingModifier;

        Function<Double, Double> freezing = f -> {
            if (source.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                return -(f - (f * 5.0F));
            }
            return -0.0;
        };
        float freezingModifier = freezing.apply((double) damage).floatValue();
        damage += freezingModifier;

        Function<Double, Double> hardHat = f -> {
            if (source.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return -(f - (f * 0.75F));
            }
            return -0.0;
        };
        float hardHatModifier = hardHat.apply((double) damage).floatValue();
        damage += hardHatModifier;

        if ((float) this.invulnerableTime > (float) this.invulnerableDuration / 2.0F && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (damage <= this.lastHurt) {
                if (source.getEntity() instanceof net.minecraft.world.entity.player.Player) {
                    ((net.minecraft.world.entity.player.Player) source.getEntity()).resetAttackStrengthTicker();
                }
                return null;
            }
        }

        Function<Double, Double> armor = f -> {
            if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                return -(f - CombatRules.getDamageAfterAbsorb((LivingEntity) (Object) this, f.floatValue(), source, (float) this.getArmorValue(), (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
            }

            return -0.0;
        };
        float armorModifier = armor.apply((double) damage).floatValue();
        damage += armorModifier;

        Function<Double, Double> resistance = f -> {
            if (!source.is(DamageTypeTags.BYPASSES_EFFECTS) && this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int i = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f1 = f.floatValue() * (float) j;
                return -(f - (f1 / 25.0F));
            }
            return -0.0;
        };
        float resistanceModifier = resistance.apply((double) damage).floatValue();
        damage += resistanceModifier;

        Function<Double, Double> magic = f -> {
            float l;
            if (this.level() instanceof ServerLevel serverLevel) {
                l = EnchantmentHelper.getDamageProtection(serverLevel, (LivingEntity) (Object) this, source);
            } else {
                l = 0.0F;
            }

            if (l > 0.0F) {
                return -(f - CombatRules.getDamageAfterMagicAbsorb(f.floatValue(), l));
            }
            return -0.0;
        };
        float magicModifier = magic.apply((double) damage).floatValue();
        damage += magicModifier;

        Function<Double, Double> absorption = f -> -(Math.max(f - Math.max(f - this.getAbsorptionAmount(), 0.0F), 0.0F));
        float absorptionModifier = absorption.apply((double) damage).floatValue();

        return CraftEventFactory.handleLivingEntityDamageEvent((LivingEntity) (Object) this, source, original, freezingModifier, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, freezing, hardHat, blocking, armor, resistance, magic, absorption);
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;"))
    private void arclight$setDamageResult(DamageSource damageSource, float f, CallbackInfo ci) {
        arclight$damageResult = true;
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void arclight$knockbackCause(DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        this.bridge$pushKnockbackCause(damageSource.getEntity(), damageSource.getEntity() == null ? EntityKnockbackEvent.KnockbackCause.DAMAGE : EntityKnockbackEvent.KnockbackCause.ENTITY_ATTACK);
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
        if (damageSourceIn.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            net.minecraft.world.item.ItemStack itemstack = null;

            net.minecraft.world.item.ItemStack itemstack1 = ItemStack.EMPTY;
            org.bukkit.inventory.EquipmentSlot bukkitHand = null;
            for (InteractionHand hand : InteractionHand.values()) {
                itemstack1 = this.getItemInHand(hand);
                if (itemstack1.is(Items.TOTEM_OF_UNDYING) && this.bridge$forge$onLivingUseTotem((LivingEntity) (Object) this, damageSourceIn, itemstack1, hand)) {
                    itemstack = itemstack1.copy();
                    bukkitHand = CraftEquipmentSlot.getHand(hand);
                    // itemstack1.shrink(1);
                    break;
                }
            }

            EntityResurrectEvent event = new EntityResurrectEvent(this.getBukkitEntity(), bukkitHand);
            event.setCancelled(itemstack == null);
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                if (!itemstack1.isEmpty()) {
                    itemstack1.shrink(1);
                }
                if (itemstack != null && (Object) this instanceof ServerPlayer serverplayerentity) {
                    serverplayerentity.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                    CriteriaTriggers.USED_TOTEM.trigger(serverplayerentity, itemstack);
                    this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
                }

                this.setHealth(1.0F);
                this.removeAllEffects(EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1), EntityPotionEffectEvent.Cause.TOTEM);
                bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1), EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 1), EntityPotionEffectEvent.Cause.TOTEM);
                this.level().broadcastEntityEvent((Entity) (Object) this, (byte) 35);
            }
            return !event.isCancelled();
        }
    }

    @Inject(method = "createWitherRose", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$witherRoseDrop(LivingEntity livingEntity, CallbackInfo ci, boolean flag, ItemEntity
        itemEntity) {
        org.bukkit.event.entity.EntityDropItemEvent event = new org.bukkit.event.entity.EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) (itemEntity.bridge$getBukkitEntity()));
        CraftEventFactory.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Decorate(method = "createWitherRose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$fireWitherRoseForm(Level level, BlockPos pos, BlockState newState, int flags) throws Throwable {
        final var event = ArclightEventFactory.callBlockFormEvent(((IWorldBridge) level).bridge$getMinecraftWorld(), pos, newState, flags, null);
        if (event != null) {
            if (event.isCancelled()) {
                return false;
            }
            newState = ((CraftBlockState) event.getNewState()).getHandle();
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, newState, flags);
    }

    @Decorate(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setSharedFlag(IZ)V"))
    private void arclight$stopGlide(LivingEntity livingEntity, int flag, boolean set) throws Throwable {
        if (set != livingEntity.getSharedFlag(flag) && !CraftEventFactory.callToggleGlideEvent(livingEntity, set).isCancelled()) {
            DecorationOps.callsite().invoke(livingEntity, flag, set);
        }
    }

    @Decorate(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setSharedFlag(IZ)V"))
    private void arclight$toggleGlide(LivingEntity livingEntity, int flag, boolean set) throws Throwable {
        if (set != livingEntity.getSharedFlag(flag) && !CraftEventFactory.callToggleGlideEvent(livingEntity, set).isCancelled()) {
            DecorationOps.callsite().invoke(livingEntity, flag, set);
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

    @Decorate(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$itemConsume(ItemStack itemStack, Level worldIn, LivingEntity entityLiving) throws
        Throwable {
        if (this instanceof ServerPlayerEntityBridge) {
            final org.bukkit.inventory.ItemStack craftItem = CraftItemStack.asBukkitCopy(itemStack);
            final PlayerItemConsumeEvent event = new PlayerItemConsumeEvent((Player) this.getBukkitEntity(), craftItem, CraftEquipmentSlot.getHand(this.getUsedItemHand()));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ((ServerPlayerEntityBridge) this).bridge$getBukkitEntity().updateInventory();
                ((ServerPlayerEntityBridge) this).bridge$getBukkitEntity().updateScaledHealth();
                return (ItemStack) DecorationOps.cancel().invoke();
            } else if (!craftItem.equals(event.getItem())) {
                itemStack = CraftItemStack.asNMSCopy(event.getItem());
            }
        }
        return (ItemStack) DecorationOps.callsite().invoke(itemStack, worldIn, entityLiving);
    }

    @Decorate(method = "randomTeleport", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/LivingEntity;teleportTo(DDD)V"))
    private void arclight$entityTeleport(LivingEntity entity, double x, double y, double z) throws Throwable {
        if ((Object) this instanceof ServerPlayer) {
            (((ServerPlayer) (Object) this).connection).teleport(x, y, z, this.getYRot(), this.getXRot(), java.util.Collections.emptySet());
            if (!((ServerGamePacketListenerBridge) ((ServerPlayer) (Object) this).connection).bridge$teleportCancelled()) {
                DecorationOps.cancel().invoke(false);
                return;
            }
        } else {
            EntityTeleportEvent event = new EntityTeleportEvent(getBukkitEntity(), new Location(this.level().bridge$getWorld(), this.getX(), this.getY(), this.getZ()),
                new Location(this.level().bridge$getWorld(), x, y, z));
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                x = event.getTo().getX();
                y = event.getTo().getY();
                z = event.getTo().getZ();
            } else {
                x = this.getX();
                y = this.getY();
                z = this.getZ();
            }
            if (event.isCancelled()) {
                DecorationOps.cancel().invoke(false);
                return;
            }
            DecorationOps.callsite().invoke(entity, x, y, z);
        }
        DecorationOps.blackhole().invoke();
    }

    @Inject(method = "addEatEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    public void arclight$foodEffectCause(FoodProperties foodProperties, CallbackInfo ci) {
        this.bridge$pushEffectCause(EntityPotionEffectEvent.Cause.FOOD);
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

    public void setItemSlot(EquipmentSlot slot, ItemStack stack, boolean silent) {
        this.setItemSlot(slot, stack);
    }

    public void onEquipItem(EquipmentSlot slot, ItemStack stack, ItemStack stack1, boolean silent) {
        this.onEquipItem(slot, stack, stack1);
    }

    @Override
    public void bridge$setSlot(EquipmentSlot slot, ItemStack stack, boolean silent) {
        this.setItemSlot(slot, stack, silent);
    }

    protected void equipEventAndSound(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, boolean silent) {
        boolean flag = newItem.isEmpty() && oldItem.isEmpty();
        if (!flag && !ItemStack.isSameItemSameComponents(oldItem, newItem) && !this.firstTick) {
            Equipable equipable = Equipable.get(newItem);
            if (!this.level().isClientSide() && !this.isSpectator()) {
                if (!this.isSilent() && equipable != null && equipable.getEquipmentSlot() == slot && !silent) {
                    this.level().playSeededSound(null, this.getX(), this.getY(), this.getZ(), equipable.getEquipSound(), this.getSoundSource(), 1.0F, 1.0F, this.random.nextLong());
                }

                if (this.doesEmitEquipEvent(slot)) {
                    this.gameEvent(equipable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
                }
            }

        }
    }

    @Override
    public void bridge$playEquipSound(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, boolean silent) {
        this.equipEventAndSound(slot, oldItem, newItem, silent);
    }

    @Inject(method = "tickDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void arclight$killedCause(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
    }
}
