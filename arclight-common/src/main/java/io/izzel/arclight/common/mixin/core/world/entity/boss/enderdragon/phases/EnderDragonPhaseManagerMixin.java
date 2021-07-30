package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon.phases;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.entity.CraftEnderDragon;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EnderDragonPhaseManager.class)
public abstract class EnderDragonPhaseManagerMixin {

    // @formatter:off
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private EnderDragon dragon;
    @Shadow private DragonPhaseInstance currentPhase;
    @Shadow public abstract <T extends DragonPhaseInstance> T getPhase(EnderDragonPhase<T> phaseIn);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void setPhase(EnderDragonPhase<?> phaseIn) {
        if (this.currentPhase == null || phaseIn != this.currentPhase.getPhase()) {
            if (this.currentPhase != null) {
                this.currentPhase.end();
            }

            EnderDragonChangePhaseEvent event = new EnderDragonChangePhaseEvent(
                (CraftEnderDragon) ((EntityBridge) this.dragon).bridge$getBukkitEntity(),
                (this.currentPhase == null) ? null : CraftEnderDragon.getBukkitPhase(this.currentPhase.getPhase()),
                CraftEnderDragon.getBukkitPhase(phaseIn)
            );
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            phaseIn = CraftEnderDragon.getMinecraftPhase(event.getNewPhase());

            this.currentPhase = this.getPhase(phaseIn);
            if (!this.dragon.level.isClientSide) {
                this.dragon.getEntityData().set(EnderDragon.DATA_PHASE, phaseIn.getId());
            }

            LOGGER.debug("Dragon is now in phase {} on the {}", phaseIn, this.dragon.level.isClientSide ? "client" : "server");
            this.currentPhase.begin();
        }
    }
}
