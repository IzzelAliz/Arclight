package io.izzel.arclight.common.mixin.v1_15.entity.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntityMixin_1_15 {

    // @formatter:off
    @Shadow public abstract ItemStack getItemStackFromSlot(EquipmentSlotType slotIn);
    // @formatter:on

    @Inject(method = "func_226529_a_", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;abilities:Lnet/minecraft/entity/player/PlayerAbilities;"))
    public void arclight$manipulateEvent(PlayerEntity playerEntity, EquipmentSlotType slotType, ItemStack itemStack, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        ItemStack itemStack1 = this.getItemStackFromSlot(slotType);

        org.bukkit.inventory.ItemStack armorStandItem = CraftItemStack.asCraftMirror(itemStack1);
        org.bukkit.inventory.ItemStack playerHeldItem = CraftItemStack.asCraftMirror(itemStack);

        Player player = ((ServerPlayerEntityBridge) playerEntity).bridge$getBukkitEntity();
        ArmorStand self = (ArmorStand) ((EntityBridge) this).bridge$getBukkitEntity();

        EquipmentSlot slot = CraftEquipmentSlot.getSlot(slotType);
        PlayerArmorStandManipulateEvent event = new PlayerArmorStandManipulateEvent(player, self, playerHeldItem, armorStandItem, slot);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }
}
