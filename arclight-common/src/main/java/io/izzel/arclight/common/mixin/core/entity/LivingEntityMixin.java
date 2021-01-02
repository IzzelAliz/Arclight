package io.izzel.arclight.common.mixin.core.entity;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.stats.Stats;
import net.minecraft.util.CombatTracker;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
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
    @Shadow public abstract float getRotationYawHead();
    @Shadow protected abstract int getExperiencePoints(PlayerEntity player);
    @Shadow protected int recentlyHit;
    @Shadow protected abstract boolean canDropLoot();
    @Shadow protected abstract boolean isPlayer();
    @Shadow public PlayerEntity attackingPlayer;
    @Shadow public int deathTime;
    @Shadow protected boolean dead;
    @Shadow public abstract ModifiableAttributeInstance getAttribute(Attribute attribute);
    @Shadow public boolean potionsNeedUpdate;
    @Shadow public abstract boolean removePotionEffect(Effect effectIn);
    @Shadow public abstract boolean clearActivePotions();
    @Shadow @Final public static DataParameter<Float> HEALTH;
    @Shadow public abstract boolean isPotionActive(Effect potionIn);
    @Shadow public abstract boolean isSleeping();
    @Shadow public abstract void wakeUp();
    @Shadow protected int idleTime;
    @Shadow public abstract net.minecraft.item.ItemStack getItemStackFromSlot(EquipmentSlotType slotIn);
    @Shadow protected abstract boolean canBlockDamageSource(DamageSource damageSourceIn);
    @Shadow protected abstract void damageShield(float damage);
    @Shadow protected abstract void blockUsingShield(LivingEntity entityIn);
    @Shadow public float limbSwingAmount;
    @Shadow public float lastDamage;
    @Shadow public int maxHurtTime;
    @Shadow public int hurtTime;
    @Shadow public float attackedAtYaw;
    @Shadow public abstract void setRevengeTarget(@Nullable LivingEntity livingBase);
    @Shadow protected abstract void markVelocityChanged();
    @Shadow @Nullable protected abstract SoundEvent getDeathSound();
    @Shadow protected abstract float getSoundVolume();
    @Shadow protected abstract float getSoundPitch();
    @Shadow public abstract void onDeath(DamageSource cause);
    @Shadow protected abstract void playHurtSound(DamageSource source);
    @Shadow private DamageSource lastDamageSource;
    @Shadow private long lastDamageStamp;
    @Shadow protected abstract float applyArmorCalculations(DamageSource source, float damage);
    @Shadow public abstract net.minecraft.item.ItemStack getHeldItem(Hand hand);
    @Shadow @Nullable public abstract EffectInstance getActivePotionEffect(Effect potionIn);
    @Shadow protected abstract float applyPotionDamageCalculations(DamageSource source, float damage);
    @Shadow public abstract float getAbsorptionAmount();
    @Shadow public abstract void setAbsorptionAmount(float amount);
    @Shadow public abstract CombatTracker getCombatTracker();
    @Shadow @Final private AttributeModifierManager attributes;
    @Shadow public abstract boolean isOnLadder();
    @Shadow protected ItemStack activeItemStack;
    @Shadow public abstract void onItemPickup(Entity entityIn, int quantity);
    @Shadow protected abstract void spawnDrops(DamageSource damageSourceIn);
    @Shadow public abstract ItemStack getHeldItemMainhand();
    @Shadow public abstract void setSprinting(boolean sprinting);
    @Shadow public abstract void setLastAttackedEntity(Entity entityIn);
    @Shadow public abstract void setHeldItem(Hand hand, ItemStack stack);
    @Shadow public abstract boolean canEntityBeSeen(Entity entityIn);
    @Shadow @Nullable public abstract LivingEntity getAttackingEntity();
    @Shadow protected int scoreValue;
    @Shadow public abstract Collection<EffectInstance> getActivePotionEffects();
    @Shadow public abstract void setArrowCountInEntity(int count);
    @Shadow @Nullable public LivingEntity revengeTarget;
    @Shadow public CombatTracker combatTracker;
    @Shadow public abstract ItemStack getHeldItemOffhand();
    @Shadow public abstract Random getRNG();
    @Shadow public abstract Optional<BlockPos> getBedPosition();
    @Shadow @Final private static DataParameter<Integer> POTION_EFFECTS;
    @Shadow @Final private static DataParameter<Boolean> HIDE_PARTICLES;
    @Shadow @Final public Map<Effect, EffectInstance> activePotionsMap;
    @Shadow protected abstract void onFinishedPotionEffect(EffectInstance effect);
    @Shadow protected abstract void onChangedPotionEffect(EffectInstance id, boolean reapply);
    @Shadow protected abstract void updatePotionMetadata();
    @Shadow public abstract boolean isPotionApplicable(EffectInstance potioneffectIn);
    @Shadow protected abstract void onNewPotionEffect(EffectInstance id);
    @Shadow @Nullable public abstract EffectInstance removeActivePotionEffect(@Nullable Effect potioneffectin);
    @Shadow protected abstract void createWitherRose(@Nullable LivingEntity entitySource);
    @Shadow public abstract double getAttributeValue(Attribute attribute);
    @Shadow public abstract void applyKnockback(float strength, double ratioX, double ratioZ);
    @Shadow protected abstract void damageArmor(DamageSource damageSource, float damage);
    @Shadow protected abstract void playEquipSound(ItemStack stack);
    @Shadow public abstract boolean getShouldBeDead();
    @Shadow public abstract int getArrowCountInEntity();
    @Shadow @Final private static DataParameter<Integer> ARROW_COUNT_IN_ENTITY;
    // @formatter:on

    public int expToDrop;
    public int maxAirTicks;
    public boolean forceDrops;
    public CraftAttributeMap craftAttributes;
    public boolean collides;
    public boolean canPickUpLoot;
    public Set<UUID> collidableExemptions = new HashSet<>();

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setHealth(F)V"))
    private void arclight$muteHealth(LivingEntity entity, float health) {
        // do nothing
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends LivingEntity> type, World worldIn, CallbackInfo ci) {
        this.maxAirTicks = 300;
        this.collides = true;
        this.craftAttributes = new CraftAttributeMap(this.attributes);
        this.dataManager.set(HEALTH, (float) this.getAttributeValue(Attributes.MAX_HEALTH));
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void onDeathUpdate() {
        ++this.deathTime;
        if (this.deathTime >= 20 && !this.removed) {

            this.remove((Object) this instanceof ServerPlayerEntity); //Forge keep data until we revive player

            for (int k = 0; k < 20; ++k) {
                double d2 = this.rand.nextGaussian() * 0.02D;
                double d0 = this.rand.nextGaussian() * 0.02D;
                double d1 = this.rand.nextGaussian() * 0.02D;
                this.world.addParticle(ParticleTypes.POOF, this.getPosX() + (double) (this.rand.nextFloat() * this.getWidth() * 2.0F) - (double) this.getWidth(), this.getPosY() + (double) (this.rand.nextFloat() * this.getHeight()), this.getPosZ() + (double) (this.rand.nextFloat() * this.getWidth() * 2.0F) - (double) this.getWidth(), d2, d0, d1);
            }
        }
    }

    @Redirect(method = "spawnDrops", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropExperience()V"))
    private void arclight$dropLater(LivingEntity livingEntity) {
    }

    @Inject(method = "spawnDrops", at = @At("RETURN"))
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
            int i = bridge$getExpToDrop();
            while (i > 0) {
                int j = ExperienceOrbEntity.getXPSplit(i);
                i -= j;
                this.world.addEntity(new ExperienceOrbEntity(this.world, this.getPosX(), this.getPosY(), this.getPosZ(), j));
            }
            bridge$setExpToDrop(0);
        }
    }

    private boolean isTickingEffects = false;
    private List<Map.Entry<Either<EffectInstance, Effect>, EntityPotionEffectEvent.Cause>> effectsToProcess = Lists.newArrayList();

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void updatePotionEffects() {
        this.isTickingEffects = true;
        Iterator<Effect> iterator = this.activePotionsMap.keySet().iterator();

        try {
            while (iterator.hasNext()) {
                Effect effect = iterator.next();
                EffectInstance effectinstance = this.activePotionsMap.get(effect);
                if (!effectinstance.tick((LivingEntity) (Object) this, () -> {
                    onChangedPotionEffect(effectinstance, true);
                })) {
                    if (!this.world.isRemote && !MinecraftForge.EVENT_BUS.post(new PotionEvent.PotionExpiryEvent((LivingEntity) (Object) this, effectinstance))) {

                        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effectinstance, null, EntityPotionEffectEvent.Cause.EXPIRATION);
                        if (event.isCancelled()) {
                            continue;
                        }

                        iterator.remove();
                        this.onFinishedPotionEffect(effectinstance);
                    }
                } else if (effectinstance.getDuration() % 600 == 0) {
                    this.onChangedPotionEffect(effectinstance, false);
                }
            }
        } catch (ConcurrentModificationException ignored) {
        }

        isTickingEffects = false;
        for (Map.Entry<Either<EffectInstance, Effect>, EntityPotionEffectEvent.Cause> e : effectsToProcess) {
            Either<EffectInstance, Effect> either = e.getKey();
            EntityPotionEffectEvent.Cause cause = e.getValue();
            bridge$pushEffectCause(cause);
            if (either.left().isPresent()) {
                addPotionEffect(either.left().get());
            } else {
                removePotionEffect(either.right().get());
            }
        }
        effectsToProcess.clear();

        if (this.potionsNeedUpdate) {
            if (!this.world.isRemote) {
                this.updatePotionMetadata();
            }

            this.potionsNeedUpdate = false;
        }

        int i = this.dataManager.get(POTION_EFFECTS);
        boolean flag1 = this.dataManager.get(HIDE_PARTICLES);
        if (i > 0) {
            boolean flag;
            if (this.isInvisible()) {
                flag = this.rand.nextInt(15) == 0;
            } else {
                flag = this.rand.nextBoolean();
            }

            if (flag1) {
                flag &= this.rand.nextInt(5) == 0;
            }

            if (flag && i > 0) {
                double d0 = (double) (i >> 16 & 255) / 255.0D;
                double d1 = (double) (i >> 8 & 255) / 255.0D;
                double d2 = (double) (i >> 0 & 255) / 255.0D;
                this.world.addParticle(flag1 ? ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT, this.getPosX() + (this.rand.nextDouble() - 0.5D) * (double) this.getWidth(), this.getPosY() + this.rand.nextDouble() * (double) this.getHeight(), this.getPosZ() + (this.rand.nextDouble() - 0.5D) * (double) this.getWidth(), d0, d1, d2);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean addPotionEffect(EffectInstance effectInstanceIn) {
        EntityPotionEffectEvent.Cause cause = bridge$getEffectCause().orElse(EntityPotionEffectEvent.Cause.UNKNOWN);
        if (isTickingEffects) {
            effectsToProcess.add(Maps.immutableEntry(Either.left(effectInstanceIn), cause));
            return true;
        }

        if (!this.isPotionApplicable(effectInstanceIn)) {
            return false;
        } else {
            EffectInstance effectinstance = this.activePotionsMap.get(effectInstanceIn.getPotion());

            boolean override = false;
            if (effectinstance != null) {
                override = new EffectInstance(effectinstance).combine(effectInstanceIn);
            }

            EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effectinstance, effectInstanceIn, cause, override);
            if (event.isCancelled()) {
                return false;
            }

            MinecraftForge.EVENT_BUS.post(new PotionEvent.PotionAddedEvent((LivingEntity) (Object) this, effectinstance, effectInstanceIn));
            if (effectinstance == null) {
                this.activePotionsMap.put(effectInstanceIn.getPotion(), effectInstanceIn);
                this.onNewPotionEffect(effectInstanceIn);
                return true;
            } else if (event.isOverride()) {
                effectinstance.combine(effectInstanceIn);
                this.onChangedPotionEffect(effectinstance, true);
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressWarnings("unused") // mock
    public EffectInstance c(@Nullable Effect potioneffectin, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return removeActivePotionEffect(potioneffectin);
    }

    @Inject(method = "removeActivePotionEffect", cancellable = true, at = @At("HEAD"))
    public void arclight$clearActive(Effect effect, CallbackInfoReturnable<EffectInstance> cir) {
        EntityPotionEffectEvent.Cause cause = bridge$getEffectCause().orElse(EntityPotionEffectEvent.Cause.UNKNOWN);
        if (isTickingEffects) {
            effectsToProcess.add(Maps.immutableEntry(Either.right(effect), cause));
            cir.setReturnValue(null);
            return;
        }

        EffectInstance effectInstance = this.activePotionsMap.get(effect);
        if (effectInstance == null) {
            cir.setReturnValue(null);
            return;
        }

        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent((LivingEntity) (Object) this, effectInstance, null, cause);
        if (event.isCancelled()) {
            cir.setReturnValue(null);
        }
    }

    @Eject(method = "onLivingFall", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private boolean arclight$fall(LivingEntity livingEntity, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        boolean ret = livingEntity.attackEntityFrom(source, amount);
        cir.setReturnValue(ret);
        return ret;
    }

    @Override
    public boolean bridge$canPickUpLoot() {
        return canPickUpLoot;
    }

    @Override
    public float getBukkitYaw() {
        return getRotationYawHead();
    }

    public int getExpReward() {
        if (!this.world.isRemote && (this.recentlyHit > 0 || this.isPlayer()) && this.canDropLoot() && this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
            int exp = this.getExperiencePoints(this.attackingPlayer);
            return ForgeEventFactory.getExperienceDrop((LivingEntity) (Object) this, this.attackingPlayer, exp);
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

    @Inject(method = "readAdditional", at = @At("HEAD"))
    public void arclight$readMaxHealth(CompoundNBT compound, CallbackInfo ci) {
        if (compound.contains("Bukkit.MaxHealth")) {
            INBT nbtbase = compound.get("Bukkit.MaxHealth");
            if (nbtbase.getId() == 5) {
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(((FloatNBT) nbtbase).getDouble());
            } else if (nbtbase.getId() == 3) {
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(((IntNBT) nbtbase).getDouble());
            }
        }
    }

    @Inject(method = "clearActivePotions", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"))
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
        return !this.removed && this.dataManager.get(HEALTH) > 0.0F;
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

            double realHealth = MathHelper.clamp(health, 0.0F, player.getMaxHealth());
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
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!ForgeHooks.onLivingAttack((LivingEntity) (Object) this, source, amount)) return false;
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.world.isRemote) {
            return false;
        } else if (this.dead || this.removed || this.getHealth() <= 0.0F) {
            return false;
        } else if (source.isFireDamage() && this.isPotionActive(Effects.FIRE_RESISTANCE)) {
            return false;
        } else {
            if (this.isSleeping() && !this.world.isRemote) {
                this.wakeUp();
            }

            this.idleTime = 0;
            float f = amount;
            if (false && (source == DamageSource.ANVIL || source == DamageSource.FALLING_BLOCK) && !this.getItemStackFromSlot(EquipmentSlotType.HEAD).isEmpty()) {
                this.getItemStackFromSlot(EquipmentSlotType.HEAD).damageItem((int) (amount * 4.0F + this.rand.nextFloat() * amount * 2.0F), (LivingEntity) (Object) this, (p_213341_0_) -> {
                    p_213341_0_.sendBreakAnimation(EquipmentSlotType.HEAD);
                });
                amount *= 0.75F;
            }

            boolean flag = false;
            float f1 = 0.0F;

            if (false && amount > 0.0F && this.canBlockDamageSource(source)) {
                this.damageShield(amount);
                f1 = amount;
                amount = 0.0F;
                if (!source.isProjectile()) {
                    Entity entity = source.getImmediateSource();
                    if (entity instanceof LivingEntity) {
                        this.blockUsingShield((LivingEntity) entity);
                    }
                }

                flag = true;
            }

            this.limbSwingAmount = 1.5F;
            boolean flag1 = true;
            if ((float) this.hurtResistantTime > 10.0F) {
                if (amount <= this.lastDamage) {
                    this.forceExplosionKnockback = true;
                    return false;
                }

                if (!this.damageEntity0(source, amount - this.lastDamage)) {
                    return false;
                }
                this.lastDamage = amount;
                flag1 = false;
            } else {
                if (!this.damageEntity0(source, amount)) {
                    return false;
                }
                this.lastDamage = amount;
                this.hurtResistantTime = 20;
                this.maxHurtTime = 10;
                this.hurtTime = this.maxHurtTime;
            }

            if ((Object) this instanceof AnimalEntity) {
                ((AnimalEntity) (Object) this).resetInLove();
                if ((Object) this instanceof TameableEntity) {
                    ((TameableEntity) (Object) this).func_233687_w_(false);
                }
            }

            this.attackedAtYaw = 0.0F;
            Entity entity1 = source.getTrueSource();
            if (entity1 != null) {
                if (entity1 instanceof LivingEntity) {
                    this.setRevengeTarget((LivingEntity) entity1);
                }

                if (entity1 instanceof PlayerEntity) {
                    this.recentlyHit = 100;
                    this.attackingPlayer = (PlayerEntity) entity1;
                } else if (entity1 instanceof TameableEntity) {
                    TameableEntity wolfentity = (TameableEntity) entity1;
                    if (wolfentity.isTamed()) {
                        this.recentlyHit = 100;
                        LivingEntity livingentity = wolfentity.getOwner();
                        if (livingentity != null && livingentity.getType() == EntityType.PLAYER) {
                            this.attackingPlayer = (PlayerEntity) livingentity;
                        } else {
                            this.attackingPlayer = null;
                        }
                    }
                }
            }

            if (flag1) {
                if (flag) {
                    this.world.setEntityState((LivingEntity) (Object) this, (byte) 29);
                } else if (source instanceof EntityDamageSource && ((EntityDamageSource) source).getIsThornsDamage()) {
                    this.world.setEntityState((LivingEntity) (Object) this, (byte) 33);
                } else {
                    byte b0;
                    if (source == DamageSource.DROWN) {
                        b0 = 36;
                    } else if (source.isFireDamage()) {
                        b0 = 37;
                    } else if (source == DamageSource.SWEET_BERRY_BUSH) {
                        b0 = 44;
                    } else {
                        b0 = 2;
                    }

                    this.world.setEntityState((LivingEntity) (Object) this, b0);
                }

                if (source != DamageSource.DROWN && (!flag || amount > 0.0F)) {
                    this.markVelocityChanged();
                }

                if (entity1 != null) {
                    double d1 = entity1.getPosX() - this.getPosX();

                    double d0;
                    for (d0 = entity1.getPosZ() - this.getPosZ(); d1 * d1 + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D) {
                        d1 = (Math.random() - Math.random()) * 0.01D;
                    }

                    this.attackedAtYaw = (float) (MathHelper.atan2(d0, d1) * (double) (180F / (float) Math.PI) - (double) this.rotationYaw);
                    this.applyKnockback(0.4F, d1, d0);
                } else {
                    this.attackedAtYaw = (float) ((int) (Math.random() * 2.0D) * 180);
                }
            }

            if (this.getHealth() <= 0.0F) {
                if (!this.checkTotemDeathProtection(source)) {
                    SoundEvent soundevent = this.getDeathSound();
                    if (flag1 && soundevent != null) {
                        this.playSound(soundevent, this.getSoundVolume(), this.getSoundPitch());
                    }

                    this.onDeath(source);
                }
            } else if (flag1) {
                this.playHurtSound(source);
            }

            boolean flag2 = !flag || amount > 0.0F;
            if (flag2) {
                this.lastDamageSource = source;
                this.lastDamageStamp = this.world.getGameTime();
            }

            if ((Object) this instanceof ServerPlayerEntity) {
                CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayerEntity) (Object) this, source, f, amount, flag);
                if (f1 > 0.0F && f1 < 3.4028235E37F) {
                    ((ServerPlayerEntity) (Object) this).addStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f1 * 10.0F));
                }
            }

            if (entity1 instanceof ServerPlayerEntity) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayerEntity) entity1, (LivingEntity) (Object) this, source, f, amount, flag);
            }

            return flag2;
        }
    }

    @Inject(method = "damageEntity", cancellable = true, at = @At("HEAD"))
    public void arclight$redirectDamageEntity(DamageSource damageSrc, float damageAmount, CallbackInfo ci) {
        damageEntity0(damageSrc, damageAmount);
        ci.cancel();
    }

    protected boolean damageEntity0(DamageSource damagesource, float f) {
        if (!this.isInvulnerableTo(damagesource)) {
            final boolean human = (Object) this instanceof PlayerEntity;

            f = net.minecraftforge.common.ForgeHooks.onLivingHurt((LivingEntity) (Object) this, damagesource, f);
            if (f <= 0) return true;

            float originalDamage = f;
            Function<Double, Double> hardHat = f12 -> {
                if ((damagesource == DamageSource.ANVIL || damagesource == DamageSource.FALLING_BLOCK) && !this.getItemStackFromSlot(EquipmentSlotType.HEAD).isEmpty()) {
                    return -(f12 - (f12 * 0.75F));
                }
                return -0.0;
            };
            float hardHatModifier = hardHat.apply((double) f).floatValue();
            f += hardHatModifier;

            Function<Double, Double> blocking = f13 -> -((this.canBlockDamageSource(damagesource)) ? f13 : 0.0);
            float blockingModifier = blocking.apply((double) f).floatValue();
            f += blockingModifier;

            Function<Double, Double> armor = f14 -> -(f14 - this.applyArmorCalculations(damagesource, f14.floatValue()));
            float armorModifier = armor.apply((double) f).floatValue();
            f += armorModifier;

            Function<Double, Double> resistance = f15 -> {
                if (!damagesource.isDamageAbsolute() && this.isPotionActive(Effects.RESISTANCE) && damagesource != DamageSource.OUT_OF_WORLD) {
                    int i = (this.getActivePotionEffect(Effects.RESISTANCE).getAmplifier() + 1) * 5;
                    int j = 25 - i;
                    float f1 = f15.floatValue() * (float) j;
                    return -(f15 - (f1 / 25.0F));
                }
                return -0.0;
            };
            float resistanceModifier = resistance.apply((double) f).floatValue();
            f += resistanceModifier;

            Function<Double, Double> magic = f16 -> -(f16 - this.applyPotionDamageCalculations(damagesource, f16.floatValue()));
            float magicModifier = magic.apply((double) f).floatValue();
            f += magicModifier;

            Function<Double, Double> absorption = f17 -> -(Math.max(f17 - Math.max(f17 - this.getAbsorptionAmount(), 0.0F), 0.0F));
            float absorptionModifier = absorption.apply((double) f).floatValue();

            EntityDamageEvent event = CraftEventFactory.handleLivingEntityDamageEvent((LivingEntity) (Object) this, damagesource, originalDamage, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, hardHat, blocking, armor, resistance, magic, absorption);

            if (damagesource.getTrueSource() instanceof PlayerEntity) {
                ((PlayerEntity) damagesource.getTrueSource()).resetCooldown();
            }

            if (event.isCancelled()) {
                return false;
            }

            f = (float) event.getFinalDamage();

            // Resistance
            if (event.getDamage(EntityDamageEvent.DamageModifier.RESISTANCE) < 0) {
                float f3 = (float) -event.getDamage(EntityDamageEvent.DamageModifier.RESISTANCE);
                if (f3 > 0.0F && f3 < 3.4028235E37F) {
                    if ((Object) this instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) (Object) this).addStat(Stats.DAMAGE_RESISTED, Math.round(f3 * 10.0F));
                    } else if (damagesource.getTrueSource() instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) damagesource.getTrueSource()).addStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(f3 * 10.0F));
                    }
                }
            }

            // Apply damage to helmet
            if ((damagesource == DamageSource.ANVIL || damagesource == DamageSource.FALLING_BLOCK) && this.getItemStackFromSlot(EquipmentSlotType.HEAD) != null) {
                this.getItemStackFromSlot(EquipmentSlotType.HEAD).damageItem((int) (event.getDamage() * 4.0F + this.rand.nextFloat() * event.getDamage() * 2.0F), (LivingEntity) (Object) this, (entityliving) -> {
                    entityliving.sendBreakAnimation(EquipmentSlotType.HEAD);
                });
            }

            // Apply damage to armor
            if (!damagesource.isUnblockable()) {
                float armorDamage = (float) (event.getDamage() + event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) + event.getDamage(EntityDamageEvent.DamageModifier.HARD_HAT));
                this.damageArmor(damagesource, armorDamage);
            }

            // Apply blocking code // PAIL: steal from above
            if (event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0) {
                this.world.setEntityState((Entity) (Object) this, (byte) 29); // SPIGOT-4635 - shield damage sound
                this.damageShield((float) -event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING));
                Entity entity = damagesource.getImmediateSource();

                if (entity instanceof LivingEntity) {
                    this.blockUsingShield(((LivingEntity) entity));
                }
            }

            absorptionModifier = (float) -event.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
            this.setAbsorptionAmount(Math.max(this.getAbsorptionAmount() - absorptionModifier, 0.0F));
            float f2 = absorptionModifier;

            if (f2 > 0.0F && f2 < 3.4028235E37F && (Object) this instanceof PlayerEntity) {
                ((PlayerEntity) (Object) this).addStat(Stats.DAMAGE_ABSORBED, Math.round(f2 * 10.0F));
            }
            if (f2 > 0.0F && f2 < 3.4028235E37F && damagesource.getTrueSource() instanceof PlayerEntity) {
                ((PlayerEntity) damagesource.getTrueSource()).addStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f2 * 10.0F));
            }

            f = net.minecraftforge.common.ForgeHooks.onLivingDamage((LivingEntity) (Object) this, damagesource, f);

            if (f > 0 || !human) {
                if (human) {
                    // PAIL: Be sure to drag all this code from the EntityHuman subclass each update.
                    ((PlayerEntity) (Object) this).addExhaustion(damagesource.getHungerDamage());
                    if (f < 3.4028235E37F) {
                        ((PlayerEntity) (Object) this).addStat(Stats.DAMAGE_TAKEN, Math.round(f * 10.0F));
                    }
                }
                // CraftBukkit end
                float f3 = this.getHealth();

                this.setHealth(f3 - f);
                this.getCombatTracker().trackDamage(damagesource, f3, f);
                // CraftBukkit start
                if (!human) {
                    this.setAbsorptionAmount(this.getAbsorptionAmount() - f);
                }

                return true;
            } else {
                // Duplicate triggers if blocking
                if (event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0) {
                    if ((Object) this instanceof ServerPlayerEntity) {
                        CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayerEntity) (Object) this, damagesource, f, originalDamage, true);
                        f2 = (float) (-event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING));
                        if (f2 > 0.0f && f2 < 3.4028235E37f) {
                            ((ServerPlayerEntity) (Object) this).addStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(originalDamage * 10.0f));
                        }
                    }
                    if (damagesource.getTrueSource() instanceof ServerPlayerEntity) {
                        CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayerEntity) damagesource.getTrueSource(), (Entity) (Object) this, damagesource, f, originalDamage, true);
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

    @Redirect(method = "heal", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setHealth(F)V"))
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

    public boolean removeEffect(Effect effect, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return removePotionEffect(effect);
    }

    @Override
    public boolean bridge$removeEffect(Effect effect, EntityPotionEffectEvent.Cause cause) {
        return removeEffect(effect, cause);
    }

    public boolean addEffect(EffectInstance effect, EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return this.addPotionEffect(effect);
    }

    public boolean removeAllEffects(EntityPotionEffectEvent.Cause cause) {
        bridge$pushEffectCause(cause);
        return this.clearActivePotions();
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
        if (damageSourceIn.canHarmInCreative()) {
            return false;
        } else {
            net.minecraft.item.ItemStack itemstack = null;

            net.minecraft.item.ItemStack itemstack1 = null;
            for (Hand hand : Hand.values()) {
                itemstack1 = this.getHeldItem(hand);
                if (itemstack1.getItem() == Items.TOTEM_OF_UNDYING) {
                    itemstack = itemstack1.copy();
                    itemstack1.shrink(1);
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
                if (itemstack != null && (Object) this instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverplayerentity = (ServerPlayerEntity) (Object) this;
                    serverplayerentity.addStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                    CriteriaTriggers.USED_TOTEM.trigger(serverplayerentity, itemstack);
                }

                this.setHealth(1.0F);
                bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TOTEM);
                this.clearActivePotions();
                bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TOTEM);
                this.addPotionEffect(new EffectInstance(Effects.REGENERATION, 900, 1));
                bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TOTEM);
                this.addPotionEffect(new EffectInstance(Effects.ABSORPTION, 100, 1));
                bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TOTEM);
                this.addPotionEffect(new EffectInstance(Effects.FIRE_RESISTANCE, 800, 1));
                this.world.setEntityState((Entity) (Object) this, (byte) 35);
            }
            return !event.isCancelled();
        }
    }

    @Redirect(method = "applyArmorCalculations", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damageArmor(Lnet/minecraft/util/DamageSource;F)V"))
    public void arclight$muteDamageArmor(LivingEntity entity, DamageSource damageSource, float damage) {
    }

    @Redirect(method = "applyPotionDamageCalculations", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isPotionActive(Lnet/minecraft/potion/Effect;)Z"))
    public boolean arclight$mutePotion(LivingEntity livingEntity, Effect potionIn) {
        return false;
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setFlag(IZ)V"))
    public void arclight$stopGlide(LivingEntity livingEntity, int flag, boolean set) {
        if (set != livingEntity.getFlag(flag) && !CraftEventFactory.callToggleGlideEvent(livingEntity, set).isCancelled()) {
            livingEntity.setFlag(flag, set);
        }
    }

    @Redirect(method = "updateElytra", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setFlag(IZ)V"))
    public void arclight$toggleGlide(LivingEntity livingEntity, int flag, boolean set) {
        if (set != livingEntity.getFlag(flag) && !CraftEventFactory.callToggleGlideEvent(livingEntity, set).isCancelled()) {
            livingEntity.setFlag(flag, set);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canBeCollidedWith() {
        return !this.removed && this.collides;
    }

    /**
     * @author IzzrlAliz
     * @reason
     */
    @Overwrite
    public boolean canBePushed() {
        return this.isAlive() && !this.isOnLadder() && this.collides;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return this.canBePushed() && this.collides != this.collidableExemptions.contains(entity.getUniqueID());
    }

    @Eject(method = "onItemUseFinish", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;onItemUseFinish(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack arclight$itemConsume(ItemStack itemStack, World worldIn, LivingEntity entityLiving, CallbackInfo ci) {
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
                return CraftItemStack.asNMSCopy(event.getItem()).onItemUseFinish(worldIn, entityLiving);
            }
        }
        return itemStack.onItemUseFinish(worldIn, entityLiving);
    }

    @Eject(method = "attemptTeleport", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/entity/LivingEntity;setPositionAndUpdate(DDD)V"))
    private void arclight$entityTeleport(LivingEntity entity, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EntityTeleportEvent event = new EntityTeleportEvent(getBukkitEntity(), new Location(((WorldBridge) this.world).bridge$getWorld(), this.getPosX(), this.getPosY(), this.getPosZ()),
            new Location(((WorldBridge) this.world).bridge$getWorld(), x, y, z));
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            this.setPositionAndUpdate(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
        } else {
            this.setPositionAndUpdate(this.getPosX(), this.getPosY(), this.getPosZ());
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "spawnDrops", at = @At(value = "INVOKE", ordinal = 0, remap = false, target = "Lnet/minecraft/entity/LivingEntity;captureDrops(Ljava/util/Collection;)Ljava/util/Collection;"))
    private Collection<ItemEntity> arclight$captureIfNeed(LivingEntity livingEntity, Collection<ItemEntity> value) {
        Collection<ItemEntity> drops = livingEntity.captureDrops();
        // todo this instanceof ArmorStandEntity
        return drops == null ? livingEntity.captureDrops(value) : drops;
    }

    @Redirect(method = "spawnDrops", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V"))
    private void arclight$cancelEvent(Collection<ItemEntity> collection, Consumer<ItemEntity> action) {
        if (this instanceof ServerPlayerEntityBridge) {
            // recapture for ServerPlayerEntityMixin#onDeath
            this.captureDrops(collection);
        } else {
            collection.forEach(action);
        }
    }

    @Inject(method = "canEntityBeSeen", cancellable = true, at = @At("HEAD"))
    private void arclight$seeNoEvil(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        if (this.world != entityIn.world) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "applyFoodEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    public void arclight$foodEffectCause(ItemStack p_213349_1_, World p_213349_2_, LivingEntity livingEntity, CallbackInfo ci) {
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
    public boolean bridge$addEffect(EffectInstance effect, EntityPotionEffectEvent.Cause cause) {
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

    @Inject(method = "setArrowCountInEntity", cancellable = true, at = @At("HEAD"))
    private void arclight$onArrowChange(int count, CallbackInfo ci) {
        if (arclight$callArrowCountChange(count, false)) {
            ci.cancel();
        }
    }

    public final void setArrowCount(int count, boolean reset) {
        if (arclight$callArrowCountChange(count, reset)) {
            return;
        }
        this.dataManager.set(ARROW_COUNT_IN_ENTITY, count);
    }

    private boolean arclight$callArrowCountChange(int newCount, boolean reset) {
        return CraftEventFactory.callArrowBodyCountChangeEvent((LivingEntity) (Object) this, this.getArrowCountInEntity(), newCount, reset).isCancelled();
    }
}
