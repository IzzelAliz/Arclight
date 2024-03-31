package io.izzel.arclight.common.bridge.core.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import io.izzel.tools.product.Product6;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.event.entity.EntityExhaustionEvent;

public interface PlayerEntityBridge extends LivingEntityBridge {

    boolean bridge$isFauxSleeping();

    @Override
    CraftHumanEntity bridge$getBukkitEntity();

    Either<Player.BedSleepingProblem, Unit> bridge$trySleep(BlockPos at, boolean force);

    void bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason reason);

    double bridge$platform$getBlockReach();

    boolean bridge$platform$isCloseEnough(Entity entity, double dist);

    boolean bridge$platform$canReach(BlockPos pos, double padding);

    boolean bridge$platform$canReach(Entity entity, double padding);

    boolean bridge$platform$canReach(Vec3 entityHitVec, double padding);

    default Float bridge$forge$getCriticalHit(Player player, Entity target, boolean vanillaCritical, float damageModifier) {
        return damageModifier;
    }

    default double bridge$forge$getEntityReach() {
        return ((Player) this).isCreative() ? 6.0 : 3.0;
    }

    default Product3<Boolean /* Cancelled */, Boolean /* DenyItem */, Boolean /* DenyBlock */>
    bridge$platform$onLeftClickBlock(BlockPos pos, Direction direction, ServerboundPlayerActionPacket.Action action) {
        return Product.of(false, false, false);
    }

    default Product6<Boolean /* Cancelled */,
            Boolean /* AllowItem */, Boolean /* DenyItem */,
            Boolean /* AllowBlock */, Boolean /* DenyBlock */,
            InteractionResult /* CancellationResult */>
    bridge$platform$onRightClickBlock(InteractionHand hand, BlockPos pos, BlockHitResult hitResult) {
        return Product.of(false, false, false, false, false, InteractionResult.PASS);
    }

    default boolean bridge$platform$mayfly() {
        return ((Player) this).getAbilities().mayfly;
    }
}
