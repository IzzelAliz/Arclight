package io.izzel.arclight.impl.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.impl.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow protected abstract void updateAITasks();
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        if (((WorldBridge) this.world).bridge$spigotConfig().tickInactiveVillagers
            && ((VillagerEntity) (Object) this).isServerWorld()) {
            this.updateAITasks();
        }
    }
}
