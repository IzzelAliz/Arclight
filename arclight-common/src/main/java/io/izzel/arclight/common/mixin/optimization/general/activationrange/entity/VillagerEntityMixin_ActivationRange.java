package io.izzel.arclight.common.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Villager.class)
public abstract class VillagerEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow protected abstract void customServerAiStep();
    // @formatter:on

    @Override
    public void inactiveTick() {
        if (((WorldBridge) this.level).bridge$spigotConfig().tickInactiveVillagers
            && ((Villager) (Object) this).isEffectiveAi()) {
            this.customServerAiStep();
        }
        super.inactiveTick();
    }
}
