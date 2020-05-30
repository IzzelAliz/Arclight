package io.izzel.arclight.common.mixin.core.entity.boss.dragon.phase;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.IPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseManager;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.entity.CraftEnderDragon;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PhaseManager.class)
public abstract class PhaseManagerMixin {

    // @formatter:off
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private EnderDragonEntity dragon;
    @Shadow private IPhase phase;
    @Shadow public abstract <T extends IPhase> T getPhase(PhaseType<T> phaseIn);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void setPhase(PhaseType<?> phaseIn) {
        if (this.phase == null || phaseIn != this.phase.getType()) {
            if (this.phase != null) {
                this.phase.removeAreaEffect();
            }

            EnderDragonChangePhaseEvent event = new EnderDragonChangePhaseEvent(
                (CraftEnderDragon) ((EntityBridge) this.dragon).bridge$getBukkitEntity(),
                (this.phase == null) ? null : CraftEnderDragon.getBukkitPhase(this.phase.getType()),
                CraftEnderDragon.getBukkitPhase(phaseIn)
            );
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            phaseIn = CraftEnderDragon.getMinecraftPhase(event.getNewPhase());

            this.phase = this.getPhase(phaseIn);
            if (!this.dragon.world.isRemote) {
                this.dragon.getDataManager().set(EnderDragonEntity.PHASE, phaseIn.getId());
            }

            LOGGER.debug("Dragon is now in phase {} on the {}", phaseIn, this.dragon.world.isRemote ? "client" : "server");
            this.phase.initPhase();
        }
    }
}
