package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.bukkit.craftbukkit.v.entity.CraftEnderDragon;
import org.bukkit.entity.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = CraftEnderDragon.class, remap = false)
public class CraftEnderDragonMixin {

    @Inject(method = "getPhase", at = @At("HEAD"))
    public void arclight$getPhase(CallbackInfoReturnable<EnderDragon.Phase> cir) {
        checkAndUpdateDragonPhase();
    }

    @Inject(method = "getBukkitPhase", at = @At("HEAD"))
    private static void arclight$getBukkitPhase(EnderDragonPhase phase, CallbackInfoReturnable<EnderDragon.Phase> cir) {
        checkAndUpdateDragonPhase();
    }

    private static void checkAndUpdateDragonPhase() {
        var forgeCount = EnderDragonPhase.getCount();
        if (EnderDragon.Phase.values().length != forgeCount) {
            var newTypes = new ArrayList<EnderDragon.Phase>();
            for (var id = EnderDragon.Phase.values().length; id < forgeCount; id++) {
                var name = "MOD_PHASE_" + id;
                var newPhase = EnumHelper.makeEnum(EnderDragon.Phase.class, name, id, List.of(), List.of());
                newTypes.add(newPhase);
                ArclightMod.LOGGER.debug("Registered {} as ender dragon phase {}", name, newPhase);
            }
            EnumHelper.addEnums(EnderDragon.Phase.class, newTypes);
        }
    }
}
