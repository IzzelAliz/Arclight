package io.izzel.arclight.common.mixin.core.world.level.storage.loot;

import io.izzel.arclight.common.bridge.core.world.storage.loot.LootTableBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.v.CraftLootTable;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LootDataManager.class)
public class LootDataManagerMixin {

    // @formatter:off
    @Shadow private Map<LootDataId<?>, ?> elements;
    // @formatter:on

    @Inject(method = "apply", at = @At("RETURN"))
    private void arclight$buildRev(Map<LootDataType<?>, Map<ResourceLocation, ?>> p_279426_, CallbackInfo ci) {
        this.elements.forEach((key, value) -> {
            if (value instanceof LootTable lootTable) {
                ((LootTableBridge) lootTable).bridge$setCraftLootTable(new CraftLootTable(CraftNamespacedKey.fromMinecraft(key.location()), lootTable));
            }
        });
    }
}
