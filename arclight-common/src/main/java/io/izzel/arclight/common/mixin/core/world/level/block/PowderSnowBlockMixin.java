package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @ModifyExpressionValue(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private static Object arclight$deferGriefing(Object original) {
        // The real rule becomes the event default after mayInteract has passed.
        return Boolean.TRUE;
    }

    @Inject(method = "*", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private static void arclight$entityChangeBlock(Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(entity, pos, Blocks.AIR.defaultBlockState(), !(((ServerLevel) level).getGameRules().get(GameRules.MOB_GRIEFING) || entity instanceof Player))) {
            ci.cancel();
        }
    }
}
