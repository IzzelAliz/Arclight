package io.izzel.arclight.common.mixin.core.world.entity;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.core.entity.AreaEffectCloudEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudEntityMixin extends EntityMixin implements AreaEffectCloudEntityBridge {

    // @formatter:off
    @Shadow private boolean fixedColor;
    @Shadow @Final private static EntityDataAccessor<Integer> DATA_COLOR;
    @Shadow public List<MobEffectInstance> effects;
    @Shadow private Potion potion;
    @Shadow public abstract void setPotion(Potion potionIn);
    @Shadow public abstract boolean isWaiting();
    @Shadow public abstract float getRadius();
    @Shadow public abstract ParticleOptions getParticle();
    @Shadow public abstract int getColor();
    @Shadow public int waitTime;
    @Shadow private int duration;
    @Shadow protected abstract void setWaiting(boolean ignoreRadius);
    @Shadow public float radiusPerTick;
    @Shadow public abstract void setRadius(float radiusIn);
    @Shadow @Final private Map<Entity, Integer> victims;
    @Shadow public int reapplicationDelay;
    @Shadow @Nullable public abstract LivingEntity getOwner();
    @Shadow public float radiusOnUse;
    @Shadow public int durationOnUse;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        super.tick();
        boolean flag = this.isWaiting();
        float f = this.getRadius();
        if (this.level.isClientSide) {
            if (flag && this.random.nextBoolean()) {
                return;
            }

            ParticleOptions particleoptions = this.getParticle();
            int i;
            float f1;
            if (flag) {
                i = 2;
                f1 = 0.2F;
            } else {
                i = Mth.ceil((float) Math.PI * f * f);
                f1 = f;
            }

            for (int j = 0; j < i; ++j) {
                float f2 = this.random.nextFloat() * ((float) Math.PI * 2F);
                float f3 = Mth.sqrt(this.random.nextFloat()) * f1;
                double d0 = this.getX() + (double) (Mth.cos(f2) * f3);
                double d2 = this.getY();
                double d4 = this.getZ() + (double) (Mth.sin(f2) * f3);
                double d5;
                double d6;
                double d7;
                if (particleoptions.getType() != ParticleTypes.ENTITY_EFFECT) {
                    if (flag) {
                        d5 = 0.0D;
                        d6 = 0.0D;
                        d7 = 0.0D;
                    } else {
                        d5 = (0.5D - this.random.nextDouble()) * 0.15D;
                        d6 = 0.01F;
                        d7 = (0.5D - this.random.nextDouble()) * 0.15D;
                    }
                } else {
                    int k = flag && this.random.nextBoolean() ? 16777215 : this.getColor();
                    d5 = (float) (k >> 16 & 255) / 255.0F;
                    d6 = (float) (k >> 8 & 255) / 255.0F;
                    d7 = (float) (k & 255) / 255.0F;
                }

                this.level.addAlwaysVisibleParticle(particleoptions, d0, d2, d4, d5, d6, d7);
            }
        } else {
            if (this.tickCount >= this.waitTime + this.duration) {
                this.discard();
                return;
            }

            boolean flag1 = this.tickCount < this.waitTime;
            if (flag != flag1) {
                this.setWaiting(flag1);
            }

            if (flag1) {
                return;
            }

            if (this.radiusPerTick != 0.0F) {
                f += this.radiusPerTick;
                if (f < 0.5F) {
                    this.discard();
                    return;
                }

                this.setRadius(f);
            }

            if (this.tickCount % 5 == 0) {
                this.victims.entrySet().removeIf((p_146784_) -> {
                    return this.tickCount >= p_146784_.getValue();
                });
                List<MobEffectInstance> list = Lists.newArrayList();

                for (MobEffectInstance mobeffectinstance : this.potion.getEffects()) {
                    list.add(new MobEffectInstance(mobeffectinstance.getEffect(), mobeffectinstance.getDuration() / 4, mobeffectinstance.getAmplifier(), mobeffectinstance.isAmbient(), mobeffectinstance.isVisible()));
                }

                list.addAll(this.effects);
                if (list.isEmpty()) {
                    this.victims.clear();
                } else {
                    List<LivingEntity> list1 = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
                    if (!list1.isEmpty()) {
                        List<org.bukkit.entity.LivingEntity> entities = new java.util.ArrayList<org.bukkit.entity.LivingEntity>(); // CraftBukkit
                        for (LivingEntity livingentity : list1) {
                            if (!this.victims.containsKey(livingentity) && livingentity.isAffectedByPotions()) {
                                double d8 = livingentity.getX() - this.getX();
                                double d1 = livingentity.getZ() - this.getZ();
                                double d3 = d8 * d8 + d1 * d1;
                                if (d3 <= (double) (f * f)) {
                                    entities.add(((LivingEntityBridge) livingentity).bridge$getBukkitEntity());
                                }
                            }
                        }
                        AreaEffectCloudApplyEvent event = CraftEventFactory.callAreaEffectCloudApplyEvent((AreaEffectCloud) (Object) this, entities);
                        if (!event.isCancelled()) {
                            for (org.bukkit.entity.LivingEntity entity : event.getAffectedEntities()) {
                                if (entity instanceof CraftLivingEntity) {
                                    net.minecraft.world.entity.LivingEntity livingentity = ((CraftLivingEntity) entity).getHandle();

                                    this.victims.put(livingentity, this.tickCount + this.reapplicationDelay);

                                    for (MobEffectInstance mobeffectinstance1 : list) {
                                        if (mobeffectinstance1.getEffect().isInstantenous()) {
                                            mobeffectinstance1.getEffect().applyInstantenousEffect((AreaEffectCloud) (Object) this, this.getOwner(), livingentity, mobeffectinstance1.getAmplifier(), 0.5D);
                                        } else {
                                            livingentity.addEffect(new MobEffectInstance(mobeffectinstance1), (AreaEffectCloud) (Object) this);
                                        }
                                    }

                                    if (this.radiusOnUse != 0.0F) {
                                        f += this.radiusOnUse;
                                        if (f < 0.5F) {
                                            this.discard();
                                            return;
                                        }

                                        this.setRadius(f);
                                    }

                                    if (this.durationOnUse != 0) {
                                        this.duration += this.durationOnUse;
                                        if (this.duration <= 0) {
                                            this.discard();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }


    public void refreshEffects() {
        if (!this.fixedColor) {
            this.getEntityData().set(DATA_COLOR, PotionUtils.getColor(PotionUtils.getAllEffects(this.potion, this.effects)));
        }
    }

    public String getPotionType() {
        return Registry.POTION.getKey(this.potion).toString();
    }

    public void setPotionType(final String string) {
        this.setPotion(Registry.POTION.get(new ResourceLocation(string)));
    }

    @Override
    public void bridge$refreshEffects() {
        refreshEffects();
    }
}
