package io.izzel.arclight.common.mixin.core.world.effect;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.entity.ElderGuardianAppearanceEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ElderGuardian;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Predicate;

@Mixin(MobEffectUtil.class)
public class MobEffectUtilMixin {

    @Decorate(method = "addEffectToPlayersAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/effect/MobEffectInstance;I)Ljava/util/List;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getPlayers(Ljava/util/function/Predicate;)Ljava/util/List;"),
        require = 1, allow = 1)
    private static List<ServerPlayer> arclight$elderAppearance(ServerLevel level, Predicate<ServerPlayer> predicate,
                                                            @Local(ordinal = 0) Entity source,
                                                            @Local(allocate = "arclightAreaEffectCause") EntityPotionEffectEvent.Cause cause) throws Throwable {
        // Consume before invoking listeners; the same method-local cause reaches only accepted players.
        cause = ArclightCaptures.getEffectCause();
        DecorationOps.blackhole().invoke(cause);
        if (cause == EntityPotionEffectEvent.Cause.ATTACK && source instanceof ElderGuardian guardian) {
            // Paper tests appearance after native eligibility, before both addEffect and packet selection.
            predicate = predicate.and(player -> new ElderGuardianAppearanceEvent(
                (org.bukkit.entity.ElderGuardian) ((EntityBridge) guardian).bridge$getBukkitEntity(),
                ((ServerPlayerBridge) player).bridge$getBukkitEntity()).callEvent());
        }
        return (List<ServerPlayer>) DecorationOps.callsite().invoke(level, predicate);
    }

    @Decorate(method = "addEffectToPlayersAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/effect/MobEffectInstance;I)Ljava/util/List;",
        inject = true, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"),
        require = 1, allow = 1)
    private static void arclight$pushCause(@Local(ordinal = -1) List<ServerPlayer> players,
                                         @Local(allocate = "arclightAreaEffectCause") EntityPotionEffectEvent.Cause cause) {
        if (cause != null) {
            for (ServerPlayer player : players) {
                ((ServerPlayerBridge) player).bridge$pushEffectCause(cause);
            }
        }
    }
}
