package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.collect.BiMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.bukkit.craftbukkit.v.inventory.CraftItemFlag;
import org.bukkit.inventory.ItemFlag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Locale;

@Mixin(value = CraftItemFlag.class, remap = false)
public class CraftItemFlagMixin {

    // @formatter:off
    @Shadow @Final private static BiMap<ItemFlag, DataComponentType<?>> BUKKIT_TO_NMS;
    // @formatter:on

    /**
     * @author Unfaths
     * @reason Return null on unknown components instead of throwing IllegalArgumentException
     */
    @Overwrite
    public static ItemFlag nmsToBukkit(DataComponentType<?> nms) {
        ItemFlag inverse = BUKKIT_TO_NMS.inverse().get(nms);

        if (inverse == null) {
            Identifier key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(nms);
            if (key == null) {
                return null;
            }

            try {
                inverse = ItemFlag.valueOf("HIDE_" + key.getPath().toUpperCase(Locale.ROOT).replace('/', '_'));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        return inverse;
    }
}
