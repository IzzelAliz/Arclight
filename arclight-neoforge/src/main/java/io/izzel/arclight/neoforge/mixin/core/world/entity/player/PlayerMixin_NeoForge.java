package io.izzel.arclight.neoforge.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.neoforge.mixin.core.world.entity.LivingEntityMixin_NeoForge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import io.izzel.tools.product.Product6;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin_NeoForge extends LivingEntityMixin_NeoForge implements PlayerEntityBridge, IPlayerExtension {

    // @formatter:off
    @Shadow public abstract Abilities getAbilities();
    @Shadow public AbstractContainerMenu containerMenu;
    // @formatter:on

    @Shadow public abstract boolean isCreative();

    @Inject(method = "hurt", cancellable = true, at = @At("HEAD"))
    private void arclight$onPlayerAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!CommonHooks.onPlayerAttack((Player) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attack", cancellable = true, at = @At("HEAD"))
    private void arclight$onPlayerAttackTarget(Entity entity, CallbackInfo ci) {
        if (!CommonHooks.onPlayerAttackTarget((Player) (Object) this, entity)) {
            ci.cancel();
        }
    }

    @Override
    public Float bridge$forge$getCriticalHit(Player player, Entity target, boolean vanillaCritical, float damageModifier) {
        var hit = CommonHooks.getCriticalHit(player, target, vanillaCritical, damageModifier);
        return hit == null ? null : hit.getDamageModifier();
    }

    @Override
    public double bridge$forge$getEntityReach() {
        return this.getEntityReach();
    }

    @Override
    public Product3<Boolean, Boolean, Boolean> bridge$platform$onLeftClickBlock(BlockPos pos, Direction direction, ServerboundPlayerActionPacket.Action action) {
        var event = CommonHooks.onLeftClickBlock((Player) (Object) this, pos, direction, action);
        return Product.of(event.isCanceled(), event.getUseItem() == Event.Result.DENY, event.getUseBlock() == Event.Result.DENY);
    }

    @Override
    public Product6<Boolean, Boolean, Boolean, Boolean, Boolean, InteractionResult> bridge$platform$onRightClickBlock(InteractionHand hand, BlockPos pos, BlockHitResult hitResult) {
        var event = CommonHooks.onRightClickBlock((Player) (Object) this, hand, pos, hitResult);
        return Product.of(event.isCanceled(),
                event.getUseItem() == Event.Result.ALLOW, event.getUseItem() == Event.Result.DENY,
                event.getUseBlock() == Event.Result.ALLOW, event.getUseBlock() == Event.Result.DENY,
                event.getCancellationResult());
    }

    @Override
    public boolean bridge$platform$mayfly() {
        return this.mayFly();
    }
}
