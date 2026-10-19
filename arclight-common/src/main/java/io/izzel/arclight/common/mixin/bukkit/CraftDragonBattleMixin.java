package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.world.level.dimension.end.EnderDragonFightBridge;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.bukkit.craftbukkit.boss.CraftDragonBattle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Fix only the compiled CraftBukkit call sites; the native methods still return void. */
@Mixin(value = CraftDragonBattle.class, remap = false)
public abstract class CraftDragonBattleMixin {

    @Redirect(method = "initiateRespawn()V", remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;tryRespawn()Z", remap = false))
    private boolean arclight$tryRespawn(EnderDragonFight fight) {
        return ((EnderDragonFightBridge) fight).bridge$tryRespawn();
    }

    @Redirect(method = "initiateRespawn(Ljava/util/Collection;)Z", remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;respawnDragon(Ljava/util/List;)Z", remap = false))
    private boolean arclight$respawnDragon(EnderDragonFight fight, List<EndCrystal> crystals) {
        return ((EnderDragonFightBridge) fight).bridge$respawnDragon(crystals);
    }

    @Redirect(method = "setPreviouslyKilled(Z)V", remap = false,
        at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;hasPreviouslyKilledDragon:Z", remap = true))
    private void arclight$setPreviouslyKilled(EnderDragonFight fight, boolean killed) {
        ((EnderDragonFightBridge) fight).bridge$setPreviouslyKilled(killed);
    }
}
