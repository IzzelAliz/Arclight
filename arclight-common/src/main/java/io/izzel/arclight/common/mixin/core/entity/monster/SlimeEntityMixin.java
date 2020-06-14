package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.MobEntityMixin;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.SlimeEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(SlimeEntity.class)
public abstract class SlimeEntityMixin extends MobEntityMixin {

    // @formatter:off
    @Shadow public abstract int getSlimeSize();
    @Shadow public abstract EntityType<? extends SlimeEntity> getType();
    // @formatter:on


    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    @Override
    public void remove(boolean keepData) {
        int i = this.getSlimeSize();
        if (!this.world.isRemote && i > 1 && this.getHealth() <= 0.0f) {
            int j = 2 + this.rand.nextInt(3);
            SlimeSplitEvent event = new SlimeSplitEvent((Slime) this.getBukkitEntity(), j);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled() || event.getCount() <= 0) {
                super.remove(keepData);
                return;
            }
            j = event.getCount();
            List<LivingEntity> slimes = new ArrayList<>(j);
            for (int k = 0; k < j; ++k) {
                float f = (k % 2 - 0.5f) * i / 4.0f;
                float f2 = (k / 2 - 0.5f) * i / 4.0f;
                SlimeEntity entityslime = this.getType().create(this.world);
                if (this.hasCustomName()) {
                    entityslime.setCustomName(this.getCustomName());
                }
                if (this.isNoDespawnRequired()) {
                    entityslime.enablePersistence();
                }
                entityslime.setInvulnerable(this.isInvulnerable());
                entityslime.setSlimeSize(i / 2, true);
                entityslime.setLocationAndAngles(this.posX + f, this.posY + 0.5, this.posZ + f2, this.rand.nextFloat() * 360.0f, 0.0f);
                slimes.add(entityslime);
            }
            if (CraftEventFactory.callEntityTransformEvent((SlimeEntity) (Object) this, slimes, EntityTransformEvent.TransformReason.SPLIT).isCancelled()) {
                return;
            }
            for (LivingEntity living : slimes) {
                ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SLIME_SPLIT);
                this.world.addEntity(living);
            }
        }
        super.remove(keepData);
    }
}
