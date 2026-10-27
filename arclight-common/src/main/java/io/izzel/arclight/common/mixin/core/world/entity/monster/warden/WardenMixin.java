package io.izzel.arclight.common.mixin.core.world.entity.monster.warden;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.WardenAngerChangeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Warden.class)
public class WardenMixin {

    @Shadow private AngerManagement angerManagement;

    @Decorate(method = "increaseAngerAt(Lnet/minecraft/world/entity/Entity;IZ)V", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/warden/WardenAi;setDigCooldown(Lnet/minecraft/world/entity/LivingEntity;)V"),
        require = 1, allow = 1)
    private void arclight$angerChange(Entity entity, int amount, boolean playSound) throws Throwable {
        // The native AI/target checks have passed; a veto must precede even the dig cooldown.
        int activeAnger = this.angerManagement.getActiveAnger(entity);
        WardenAngerChangeEvent event = new WardenAngerChangeEvent(
            (org.bukkit.entity.Warden) ((EntityBridge) this).bridge$getBukkitEntity(),
            ((EntityBridge) entity).bridge$getBukkitEntity(), activeAnger, Math.min(150, activeAnger + amount));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        amount = event.getNewAnger() - activeAnger;
        DecorationOps.blackhole().invoke(amount);
    }

    @Inject(method = "applyDarknessAround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectUtil;addEffectToPlayersAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/effect/MobEffectInstance;I)Ljava/util/List;"))
    private static void arclight$reason(ServerLevel p_219376_, Vec3 p_219377_, Entity p_219378_, int p_219379_, CallbackInfo ci) {
        ArclightCaptures.captureEffectCause(EntityPotionEffectEvent.Cause.WARDEN);
    }
}
