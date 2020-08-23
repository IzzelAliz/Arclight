package io.izzel.arclight.common.mixin.core.entity;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.entity.AreaEffectCloudEntityBridge;
import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Mixin(AreaEffectCloudEntity.class)
@Implements(@Interface(iface = AreaEffectCloudEntityBridge.Hack.class, prefix = "hack$"))
public abstract class AreaEffectCloudEntityMixin extends EntityMixin implements AreaEffectCloudEntityBridge {

    // @formatter:off
    @Shadow private boolean colorSet;
    @Shadow @Final private static DataParameter<Integer> COLOR;
    @Shadow public List<EffectInstance> effects;
    @Shadow private Potion potion;
    @Shadow public abstract void setPotion(Potion potionIn);
    @Shadow public abstract boolean shouldIgnoreRadius();
    @Shadow public abstract float getRadius();
    @Shadow public abstract IParticleData getParticleData();
    @Shadow public abstract int getColor();
    @Shadow public int waitTime;
    @Shadow private int duration;
    @Shadow protected abstract void setIgnoreRadius(boolean ignoreRadius);
    @Shadow public float radiusPerTick;
    @Shadow public abstract void setRadius(float radiusIn);
    @Shadow @Final private Map<Entity, Integer> reapplicationDelayMap;
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
        boolean flag = this.shouldIgnoreRadius();
        float f = this.getRadius();
        if (this.world.isRemote) {
            IParticleData iparticledata = this.getParticleData();
            if (flag) {
                if (this.rand.nextBoolean()) {
                    for (int i = 0; i < 2; ++i) {
                        float f1 = this.rand.nextFloat() * ((float) Math.PI * 2F);
                        float f2 = MathHelper.sqrt(this.rand.nextFloat()) * 0.2F;
                        float f3 = MathHelper.cos(f1) * f2;
                        float f4 = MathHelper.sin(f1) * f2;
                        if (iparticledata.getType() == ParticleTypes.ENTITY_EFFECT) {
                            int j = this.rand.nextBoolean() ? 16777215 : this.getColor();
                            int k = j >> 16 & 255;
                            int l = j >> 8 & 255;
                            int i1 = j & 255;
                            this.world.addOptionalParticle(iparticledata, this.getPosX() + (double) f3, this.getPosY(), this.getPosZ() + (double) f4, (float) k / 255.0F, (float) l / 255.0F, (float) i1 / 255.0F);
                        } else {
                            this.world.addOptionalParticle(iparticledata, this.getPosX() + (double) f3, this.getPosY(), this.getPosZ() + (double) f4, 0.0D, 0.0D, 0.0D);
                        }
                    }
                }
            } else {
                float f5 = (float) Math.PI * f * f;

                for (int k1 = 0; (float) k1 < f5; ++k1) {
                    float f6 = this.rand.nextFloat() * ((float) Math.PI * 2F);
                    float f7 = MathHelper.sqrt(this.rand.nextFloat()) * f;
                    float f8 = MathHelper.cos(f6) * f7;
                    float f9 = MathHelper.sin(f6) * f7;
                    if (iparticledata.getType() == ParticleTypes.ENTITY_EFFECT) {
                        int l1 = this.getColor();
                        int i2 = l1 >> 16 & 255;
                        int j2 = l1 >> 8 & 255;
                        int j1 = l1 & 255;
                        this.world.addOptionalParticle(iparticledata, this.getPosX() + (double) f8, this.getPosY(), this.getPosZ() + (double) f9, (float) i2 / 255.0F, (float) j2 / 255.0F, (float) j1 / 255.0F);
                    } else {
                        this.world.addOptionalParticle(iparticledata, this.getPosX() + (double) f8, this.getPosY(), this.getPosZ() + (double) f9, (0.5D - this.rand.nextDouble()) * 0.15D, 0.01F, (0.5D - this.rand.nextDouble()) * 0.15D);
                    }
                }
            }
        } else {
            if (this.ticksExisted >= this.waitTime + this.duration) {
                this.remove();
                return;
            }

            boolean flag1 = this.ticksExisted < this.waitTime;
            if (flag != flag1) {
                this.setIgnoreRadius(flag1);
            }

            if (flag1) {
                return;
            }

            if (this.radiusPerTick != 0.0F) {
                f += this.radiusPerTick;
                if (f < 0.5F) {
                    this.remove();
                    return;
                }

                this.setRadius(f);
            }

            if (this.ticksExisted % 5 == 0) {

                this.reapplicationDelayMap.entrySet().removeIf(entry -> this.ticksExisted >= entry.getValue());

                List<EffectInstance> effects = Lists.newArrayList();

                for (EffectInstance effectinstance1 : this.potion.getEffects()) {
                    effects.add(new EffectInstance(effectinstance1.getPotion(), effectinstance1.getDuration() / 4, effectinstance1.getAmplifier(), effectinstance1.isAmbient(), effectinstance1.doesShowParticles()));
                }

                effects.addAll(this.effects);
                if (effects.isEmpty()) {
                    this.reapplicationDelayMap.clear();
                } else {
                    List<LivingEntity> list = this.world.getEntitiesWithinAABB(LivingEntity.class, this.getBoundingBox());
                    if (!list.isEmpty()) {
                        List<LivingEntity> entities = new java.util.ArrayList<>();
                        for (LivingEntity livingentity : list) {
                            if (!this.reapplicationDelayMap.containsKey(livingentity) && livingentity.canBeHitWithPotion()) {
                                double d0 = livingentity.getPosX() - this.getPosX();
                                double d1 = livingentity.getPosZ() - this.getPosZ();
                                double d2 = d0 * d0 + d1 * d1;
                                if (d2 <= (double) (f * f)) {
                                    entities.add(livingentity);
                                }
                            }
                        }

                        AreaEffectCloudApplyEvent event = CraftEventFactory.callAreaEffectCloudApplyEvent((AreaEffectCloudEntity) (Object) this, Lists.transform(entities, living -> ((LivingEntityBridge) living).bridge$getBukkitEntity()));
                        if (!event.isCancelled()) {
                            for (org.bukkit.entity.LivingEntity entity : event.getAffectedEntities()) {
                                if (entity instanceof CraftLivingEntity) {
                                    LivingEntity livingentity = ((CraftLivingEntity) entity).getHandle();

                                    this.reapplicationDelayMap.put(livingentity, this.ticksExisted + this.reapplicationDelay);

                                    for (EffectInstance effectinstance : effects) {
                                        if (effectinstance.getPotion().isInstant()) {
                                            effectinstance.getPotion().affectEntity((AreaEffectCloudEntity) (Object) this, this.getOwner(), livingentity, effectinstance.getAmplifier(), 0.5D);
                                        } else {
                                            ((LivingEntityBridge) livingentity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.AREA_EFFECT_CLOUD);
                                            livingentity.addPotionEffect(new EffectInstance(effectinstance));
                                        }
                                    }

                                    if (this.radiusOnUse != 0.0F) {
                                        f += this.radiusOnUse;
                                        if (f < 0.5F) {
                                            this.remove();
                                            return;
                                        }

                                        this.setRadius(f);
                                    }

                                    if (this.durationOnUse != 0) {
                                        this.duration += this.durationOnUse;
                                        if (this.duration <= 0) {
                                            this.remove();
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
        if (!this.colorSet) {
            this.getDataManager().set(COLOR, PotionUtils.getPotionColorFromEffectList(PotionUtils.mergeEffects(this.potion, this.effects)));
        }
    }

    public String hack$getType() {
        return Registry.POTION.getKey(this.potion).toString();
    }

    public void hack$setType(final String string) {
        this.setPotion(Registry.POTION.getOrDefault(new ResourceLocation(string)));
    }

    @Override
    public void bridge$refreshEffects() {
        refreshEffects();
    }
}
