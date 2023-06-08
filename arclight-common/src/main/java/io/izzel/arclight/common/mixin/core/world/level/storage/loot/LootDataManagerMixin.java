package io.izzel.arclight.common.mixin.core.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
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

    public Map<?, ResourceLocation> lootTableToKey = ImmutableMap.of();

    @Inject(method = "apply", at = @At("RETURN"))
    private void arclight$buildRev(Map<LootDataType<?>, Map<ResourceLocation, ?>> p_279426_, CallbackInfo ci) {
        ImmutableMap.Builder<Object, ResourceLocation> lootTableToKeyBuilder = ImmutableMap.builder();
        this.elements.forEach((key, lootTable) -> lootTableToKeyBuilder.put(lootTable, key.location()));
        this.lootTableToKey = lootTableToKeyBuilder.build();
    }
}
