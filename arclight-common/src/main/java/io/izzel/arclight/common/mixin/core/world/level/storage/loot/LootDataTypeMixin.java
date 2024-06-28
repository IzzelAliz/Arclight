package io.izzel.arclight.common.mixin.core.world.level.storage.loot;

import io.izzel.arclight.common.bridge.core.world.storage.loot.LootTableBridge;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.v.CraftLootTable;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootDataType.class)
public class LootDataTypeMixin {

    @Inject(method = "createLootTableValidator", cancellable = true, at = @At("RETURN"))
    private static void arclight$setHandle(CallbackInfoReturnable<LootDataType.Validator<LootTable>> cir) {
        var validator = cir.getReturnValue();
        cir.setReturnValue((validationContext, resourceKey, object) -> {
            validator.run(validationContext, resourceKey, object);
            ((LootTableBridge) object).bridge$setCraftLootTable(new CraftLootTable(CraftNamespacedKey.fromMinecraft(resourceKey.location()), object));
        });
    }
}
