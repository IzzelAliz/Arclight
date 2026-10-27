package io.izzel.arclight.common.mixin.core.world.entity;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.EnumMap;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipmentEventMixin {

    @Decorate(method = "collectEquipmentChanges()Ljava/util/Map;", require = 1, allow = 1, inject = true, at = @At("HEAD"))
    private void arclight$beginEquipmentChanges(
        @Local(allocate = "arclightEquipmentChanges") Map<org.bukkit.inventory.EquipmentSlot, Pair<org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack>> equipmentChanges
    ) throws Throwable {
        equipmentChanges = null;
        DecorationOps.blackhole().invoke(equipmentChanges);
    }

    @Decorate(method = "collectEquipmentChanges()Ljava/util/Map;", require = 1, allow = 1, at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", remap = false))
    private Object arclight$collectEquipmentChange(
        Map<EquipmentSlot, ItemStack> changedItems, Object key, Object value,
        @Local(ordinal = 0) ItemStack previous,
        @Local(allocate = "arclightEquipmentChanges") Map<org.bukkit.inventory.EquipmentSlot, Pair<org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack>> equipmentChanges
    ) throws Throwable {
        EquipmentSlot slot = (EquipmentSlot) key;
        ItemStack current = (ItemStack) value;
        // This accepted-change edge follows NeoForge's native equipment event and
        // precedes the removal of the old location-based effects on both loaders.
        org.bukkit.inventory.ItemStack oldItem = CraftItemStack.asBukkitCopy(previous);
        org.bukkit.inventory.ItemStack newItem = CraftItemStack.asBukkitCopy(current);
        if ((Object) this instanceof ServerPlayer && slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            new PlayerArmorChangeEvent(
                (Player) ((LivingEntityBridge) this).bridge$getBukkitEntity(),
                PlayerArmorChangeEvent.SlotType.valueOf(slot.name()), oldItem, newItem
            ).callEvent();
        }
        if (equipmentChanges == null) {
            equipmentChanges = new EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);
            DecorationOps.blackhole().invoke(equipmentChanges);
        }
        Object result = DecorationOps.callsite().invoke(changedItems, key, value);
        equipmentChanges.put(CraftEquipmentSlot.getSlot(slot), Pair.of(oldItem, newItem));
        return result;
    }

    @Decorate(method = "collectEquipmentChanges()Ljava/util/Map;", require = 1, allow = 1, at = @At("RETURN"))
    private void arclight$notifyEquipmentChanges(
        Map<EquipmentSlot, ItemStack> changedItems,
        @Local(allocate = "arclightEquipmentChanges") Map<org.bukkit.inventory.EquipmentSlot, Pair<org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack>> equipmentChanges
    ) throws Throwable {
        // The complete native attribute/enchantment pass has finished, but hand
        // swap detection and equipment packets still consume the original map.
        if (equipmentChanges != null) {
            ArclightEventFactory.callEquipmentChangeEvent(((LivingEntityBridge) this).bridge$getBukkitEntity(), equipmentChanges);
        }
        DecorationOps.callsite().invoke(changedItems);
    }
}
