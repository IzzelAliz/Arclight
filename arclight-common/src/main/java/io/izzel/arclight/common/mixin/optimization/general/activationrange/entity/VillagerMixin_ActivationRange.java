package io.izzel.arclight.common.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Villager.class)
public abstract class VillagerMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow protected abstract void customServerAiStep(ServerLevel level);
    // @formatter:on

    @Override
    public void inactiveTick() {
        if (this.level() instanceof ServerLevel serverLevel
            && ((WorldBridge) serverLevel).bridge$spigotConfig().tickInactiveVillagers
            && ((Villager) (Object) this).isEffectiveAi()) {
            this.customServerAiStep(serverLevel);
        }
        super.inactiveTick();
    }
}
