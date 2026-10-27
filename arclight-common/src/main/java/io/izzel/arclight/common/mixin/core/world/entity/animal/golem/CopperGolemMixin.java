package io.izzel.arclight.common.mixin.core.world.entity.animal.golem;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(CopperGolem.class)
public abstract class CopperGolemMixin {

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$statueChangeBlock(ServerLevel level, BlockPos pos, BlockState state, int flags) throws Throwable {
        if (!CraftEventFactory.callEntityChangeBlockEvent((CopperGolem) (Object) this, pos, state)) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, state, flags);
    }

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;dropPreservedEquipment(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$statueForceDrops(CopperGolem golem, ServerLevel level) throws Throwable {
        var bridge = (EntityBridge) golem;
        boolean previous = bridge.bridge$isForceDrops();
        bridge.bridge$setForceDrops(true);
        try {
            DecorationOps.callsite().invoke(golem, level);
        } finally {
            bridge.bridge$setForceDrops(previous);
        }
    }

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;discard()V"))
    private void arclight$statueRemoveCause(CopperGolem golem) throws Throwable {
        ((EntityBridge) golem).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
        DecorationOps.callsite().invoke(golem);
    }

    @Unique private List<ItemStack> arclight$shearDrops;

    @Decorate(method = "mobInteract", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$onShear(CopperGolem golem, ServerLevel level, SoundSource source, ItemStack tool, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaultDrops = new ArrayList<>(1);
        defaultDrops.add(golem.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).copy());
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, golem, tool, hand, defaultDrops);
        if (event != null && event.isCancelled()) {
            ((PlayerBridge) player).bridge$cancelCopperShear(golem);
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }

        List<ItemStack> drops = event == null ? defaultDrops : CraftItemStack.asNMSCopy(event.getDrops());
        List<ItemStack> previousDrops = this.arclight$shearDrops;
        this.arclight$shearDrops = drops;
        try {
            DecorationOps.callsite().invoke(golem, level, source, tool);
        } finally {
            this.arclight$shearDrops = previousDrops;
        }
    }

    @WrapOperation(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"), require = 1, allow = 1)
    private ItemEntity arclight$dropShearedItems(CopperGolem golem, ServerLevel level, ItemStack stack, float height, Operation<ItemEntity> original) {
        List<ItemStack> drops = this.arclight$shearDrops;
        if (drops == null) {
            return original.call(golem, level, stack, height);
        }
        this.arclight$shearDrops = List.of();
        var bridge = (EntityBridge) golem;
        boolean previousForceDrops = bridge.bridge$isForceDrops();
        bridge.bridge$setForceDrops(true);
        try {
            ItemEntity result = null;
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    result = original.call(golem, level, drop, height);
                }
            }
            return result;
        } finally {
            bridge.bridge$setForceDrops(previousForceDrops);
        }
    }
}
