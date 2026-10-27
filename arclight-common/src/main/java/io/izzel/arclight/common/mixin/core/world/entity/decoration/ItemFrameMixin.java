package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.decoration.ItemFrameBridge;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent.ItemFrameChangeAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin extends BlockAttachedEntityMixin implements ItemFrameBridge {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<ItemStack> DATA_ITEM;
    @Shadow public abstract ItemStack getItem();
    @Shadow protected abstract void onItemChanged(ItemStack p_218866_);
    @Shadow public abstract net.minecraft.sounds.SoundEvent getAddItemSound();
    // @formatter:on

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V"))
    private void arclight$damageNonLiving(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ItemFrame) (Object) this, source, amount, false) || this.isRemoved()) {
            cir.setReturnValue(true);
            return;
        }
        // Item removal is distinct from breaking the frame: keep the existing damage veto first.
        if (source.getEntity() instanceof Player player) {
            var event = new PlayerItemFrameChangeEvent((org.bukkit.entity.Player) ((EntityBridge) player).bridge$getBukkitEntity(),
                (org.bukkit.entity.ItemFrame) this.getBukkitEntity(), CraftItemStack.asBukkitCopy(this.getItem()), ItemFrameChangeAction.REMOVE);
            if (!event.callEvent()) {
                cir.setReturnValue(true);
                return;
            }
            // The native drop consumer reads the approved item; do not add a placement sound.
            this.setItem(CraftItemStack.asNMSCopy(event.getItemStack()), false, false);
        }
    }

    @Decorate(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;", require = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$placeItem(ItemFrame frame, ItemStack stack, Player player, InteractionHand hand, Vec3 location) throws Throwable {
        var event = new PlayerItemFrameChangeEvent((org.bukkit.entity.Player) ((EntityBridge) player).bridge$getBukkitEntity(),
            (org.bukkit.entity.ItemFrame) this.getBukkitEntity(), CraftItemStack.asBukkitCopy(stack), ItemFrameChangeAction.PLACE);
        if (!event.callEvent()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            return;
        }
        // Only replace the framed stack: native interaction still consumes the original held item.
        DecorationOps.callsite().invoke(frame, CraftItemStack.asNMSCopy(event.getItemStack()));
    }

    @Decorate(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;", inject = true, require = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;getRotateItemSound()Lnet/minecraft/sounds/SoundEvent;"))
    private void arclight$rotateItem(Player player, InteractionHand hand, Vec3 location) throws Throwable {
        var event = new PlayerItemFrameChangeEvent((org.bukkit.entity.Player) ((EntityBridge) player).bridge$getBukkitEntity(),
            (org.bukkit.entity.ItemFrame) this.getBukkitEntity(), CraftItemStack.asBukkitCopy(this.getItem()), ItemFrameChangeAction.ROTATE);
        if (!event.callEvent()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            return;
        }
        this.setItem(CraftItemStack.asNMSCopy(event.getItemStack()), false, false);
    }

    @Override
    public void setItem(ItemStack itemstack, final boolean flag, final boolean playSound) {
        if (!itemstack.isEmpty()) {
            itemstack = itemstack.copy();
            itemstack.setCount(1);
        }
        this.onItemChanged(itemstack);
        this.getEntityData().set(DATA_ITEM, itemstack);
        if (!itemstack.isEmpty() && playSound) {
            this.playSound(this.getAddItemSound(), 1.0f, 1.0f);
        }
        if (flag && this.pos != null) {
            this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static AABB createBoundingBoxStatic(BlockPos pos, Direction direction, boolean hasMap) {
        Vec3 center = Vec3.atCenterOf(pos).relative(direction, -0.46875D);
        double width = hasMap ? 1.0D : 0.75D;
        Direction.Axis axis = direction.getAxis();
        return AABB.ofSize(center, axis == Direction.Axis.X ? 0.0625D : width,
            axis == Direction.Axis.Y ? 0.0625D : width, axis == Direction.Axis.Z ? 0.0625D : width);
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static AABB calculateBoundingBoxStatic(BlockPos blockPos, Direction direction) {
        float f = 0.46875F;
        Vec3 vec3d = Vec3.atCenterOf(blockPos).relative(direction, -0.46875D);
        Direction.Axis axis = direction.getAxis();
        double d0 = axis == Direction.Axis.X ? 0.0625D : 0.75D;
        double d1 = axis == Direction.Axis.Y ? 0.0625D : 0.75D;
        double d2 = axis == Direction.Axis.Z ? 0.0625D : 0.75D;
        return AABB.ofSize(vec3d, d0, d1, d2);
    }
}
