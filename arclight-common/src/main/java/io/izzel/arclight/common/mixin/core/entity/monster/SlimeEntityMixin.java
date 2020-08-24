package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.MobEntityMixin;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.util.text.ITextComponent;
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
        if (!this.world.isRemote && i > 1 && this.getShouldBeDead() && !this.removed) {
            ITextComponent itextcomponent = this.getCustomName();
            boolean flag = this.isAIDisabled();
            float f = (float) i / 4.0F;
            int j = i / 2;
            int k = 2 + this.rand.nextInt(3);

            SlimeSplitEvent event = new SlimeSplitEvent((Slime) this.getBukkitEntity(), k);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled() || event.getCount() <= 0) {
                super.remove(keepData);
                return;
            }
            k = event.getCount();
            List<LivingEntity> slimes = new ArrayList<>(k);

            for (int l = 0; l < k; ++l) {
                float f1 = ((float) (l % 2) - 0.5F) * f;
                float f2 = ((float) (l / 2) - 0.5F) * f;
                SlimeEntity slimeentity = this.getType().create(this.world);
                if (this.isNoDespawnRequired()) {
                    slimeentity.enablePersistence();
                }

                slimeentity.setCustomName(itextcomponent);
                slimeentity.setNoAI(flag);
                slimeentity.setInvulnerable(this.isInvulnerable());
                slimeentity.setSlimeSize(j, true);
                slimeentity.setLocationAndAngles(this.getPosX() + (double) f1, this.getPosY() + 0.5D, this.getPosZ() + (double) f2, this.rand.nextFloat() * 360.0F, 0.0F);
                slimes.add(slimeentity);
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
