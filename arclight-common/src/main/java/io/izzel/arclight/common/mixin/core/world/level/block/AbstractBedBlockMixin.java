package io.izzel.arclight.common.mixin.core.world.level.block;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(AbstractBedBlock.class)
public abstract class AbstractBedBlockMixin {

    @Shadow protected abstract InteractionResult destroyOnUse(BlockState state, Level level, BlockPos pos, Player player);

    @Decorate(method = "useWithoutItem", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BedRule;destroyOnUse()Z"))
    private boolean arclight$deferDestruction(BedRule rule, BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) throws Throwable {
        boolean destroy = (boolean) DecorationOps.callsite().invoke(rule);
        if (destroy && state.getValue(AbstractBedBlock.OCCUPIED)) {
            InteractionResult result = this.destroyOnUse(state, level, pos, player);
            return (boolean) DecorationOps.cancel().invoke(result);
        }
        return false;
    }

    @Decorate(method = "useWithoutItem", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Either;ifLeft(Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;"))
    private Either<Player.BedSleepingProblem, Unit> arclight$failEnter(Either<Player.BedSleepingProblem, Unit> result,
            Consumer<Player.BedSleepingProblem> original, BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit, @Local(ordinal = 0) BedRule rule) throws Throwable {
        Consumer<Player.BedSleepingProblem> handler = problem -> {
            var event = CraftEventFactory.callPlayerBedFailEnterEvent(player, pos, rule, problem);
            if (event.isCancelled()) {
                return;
            }
            if (event.getWillExplode()) {
                this.destroyOnUse(state, level, pos, player);
            }
            if (event.getMessage() != null) {
                player.sendOverlayMessage(PaperAdventure.asVanilla(event.getMessage()));
            }
        };
        return (Either<Player.BedSleepingProblem, Unit>) DecorationOps.callsite().invoke(result, handler);
    }
}
