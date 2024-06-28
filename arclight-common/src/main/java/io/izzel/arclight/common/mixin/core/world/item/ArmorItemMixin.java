package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.server.block.DispenserBlockHooks;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmorItem.class)
public class ArmorItemMixin {

    @Decorate(method = "dispenseArmor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void arclight$dispense(LivingEntity instance, EquipmentSlot equipmentSlot, ItemStack stack, BlockSource blockSource, ItemStack itemStack, @Local(ordinal = -1) ItemStack itemstack1) throws Throwable {
        Level world = blockSource.level();
        org.bukkit.block.Block block = CraftBlock.at(world, blockSource.pos());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

        BlockDispenseArmorEvent event = new BlockDispenseArmorEvent(block, craftItem.clone(), (CraftLivingEntity) instance.bridge$getBukkitEntity());
        if (!DispenserBlockHooks.isEventFired()) {
            Bukkit.getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            itemStack.grow(1);
            DecorationOps.cancel().invoke(false);
            return;
        }

        if (!event.getItem().equals(craftItem)) {
            itemStack.grow(1);
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            DispenseItemBehavior idispensebehavior = DispenserBlock.DISPENSER_REGISTRY.get(eventStack.getItem());
            if (idispensebehavior != DispenseItemBehavior.NOOP && idispensebehavior != ArmorItem.DISPENSE_ITEM_BEHAVIOR) {
                idispensebehavior.dispense(blockSource, eventStack);
                DecorationOps.cancel().invoke(true);
                return;
            }
        }

        DecorationOps.callsite().invoke(instance, equipmentSlot, CraftItemStack.asNMSCopy(event.getItem()));
    }
}
