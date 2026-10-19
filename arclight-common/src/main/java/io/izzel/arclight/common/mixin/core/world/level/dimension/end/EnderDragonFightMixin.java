package io.izzel.arclight.common.mixin.core.world.level.dimension.end;

import io.izzel.arclight.common.bridge.core.world.level.dimension.end.EnderDragonFightBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Preserve the native void respawn paths and derive Bukkit's result from the transition. */
@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin extends SavedData implements EnderDragonFightBridge {

    @Shadow public DragonRespawnStage respawnStage;
    @Shadow private boolean dragonKilled;
    @Shadow public boolean hasPreviouslyKilledDragon;
    @Shadow public abstract void tryRespawn();

    @Invoker("respawnDragon")
    public abstract void arclight$invokeRespawnDragon(List<EndCrystal> crystals);

    @Override
    public boolean bridge$tryRespawn() {
        // Keep the native portal/crystal search; do not count an already-active fight as success.
        boolean eligible = this.dragonKilled && this.respawnStage == null;
        this.tryRespawn();
        return eligible && this.respawnStage == DragonRespawnStage.START;
    }

    @Override
    public boolean bridge$respawnDragon(List<EndCrystal> crystals) {
        boolean eligible = this.dragonKilled && this.respawnStage == null;
        this.arclight$invokeRespawnDragon(crystals);
        return eligible && this.respawnStage == DragonRespawnStage.START;
    }

    @Decorate(method = "onCrystalDestroyed(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;Lnet/minecraft/world/damagesource/DamageSource;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"))
    private boolean arclight$matchesRespawnCrystal(List<EntityReference<EndCrystal>> references, Object candidate) throws Throwable {
        boolean contained = (boolean) DecorationOps.callsite().invoke(references, candidate);
        // Native stores EntityReference values, while the destruction callback passes an EndCrystal.
        if (contained || !(candidate instanceof EndCrystal crystal)) {
            return contained;
        }
        return references.stream().anyMatch(reference -> reference.matches(crystal));
    }

    @Override
    public void bridge$setPreviouslyKilled(boolean killed) {
        if (this.hasPreviouslyKilledDragon != killed) {
            this.hasPreviouslyKilledDragon = killed;
            this.setDirty();
        }
    }
}
