package io.izzel.arclight.common.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public class MobDeathMixin {
    @WrapOperation(method = "dropCustomDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$deferEquipment(Mob mob, EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        ((DeathEventBridge) mob).bridge$deferDeathChange(() -> original.call(mob, slot, stack));
    }

    @WrapOperation(method = "dropFromLootTable", at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.PUTFIELD,
        target = "Lnet/minecraft/world/entity/Mob;lootTable:Ljava/util/Optional;"))
    private void arclight$deferLootTable(Mob mob, java.util.Optional<?> table, Operation<Void> original) {
        ((DeathEventBridge) mob).bridge$deferDeathChange(() -> original.call(mob, table));
    }
}
