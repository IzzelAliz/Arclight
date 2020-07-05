package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.collect.ImmutableMap;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.inventory.CraftItemFactory;
import org.bukkit.craftbukkit.v.inventory.CraftMetaArmorStand;
import org.bukkit.craftbukkit.v.inventory.CraftMetaBanner;
import org.bukkit.craftbukkit.v.inventory.CraftMetaBlockState;
import org.bukkit.craftbukkit.v.inventory.CraftMetaBook;
import org.bukkit.craftbukkit.v.inventory.CraftMetaBookSigned;
import org.bukkit.craftbukkit.v.inventory.CraftMetaCharge;
import org.bukkit.craftbukkit.v.inventory.CraftMetaCrossbow;
import org.bukkit.craftbukkit.v.inventory.CraftMetaEnchantedBook;
import org.bukkit.craftbukkit.v.inventory.CraftMetaFirework;
import org.bukkit.craftbukkit.v.inventory.CraftMetaItem;
import org.bukkit.craftbukkit.v.inventory.CraftMetaKnowledgeBook;
import org.bukkit.craftbukkit.v.inventory.CraftMetaLeatherArmor;
import org.bukkit.craftbukkit.v.inventory.CraftMetaMap;
import org.bukkit.craftbukkit.v.inventory.CraftMetaPotion;
import org.bukkit.craftbukkit.v.inventory.CraftMetaSkull;
import org.bukkit.craftbukkit.v.inventory.CraftMetaSpawnEgg;
import org.bukkit.craftbukkit.v.inventory.CraftMetaSuspiciousStew;
import org.bukkit.craftbukkit.v.inventory.CraftMetaTropicalFishBucket;
import org.bukkit.craftbukkit.v.util.CraftLegacy;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.BiFunction;

@Mixin(value = CraftItemFactory.class, remap = false)
public class CraftItemFactoryMixin {

    private static final Map<String, BiFunction<Material, CraftMetaItem, ItemMeta>> TYPES = ImmutableMap
        .<String, BiFunction<Material, CraftMetaItem, ItemMeta>>builder()
        .put("ARMOR_STAND", (a, b) -> new CraftMetaArmorStand(b))
        .put("BANNER", (a, b) -> new CraftMetaBanner(b))
        .put("TILE_ENTITY", (a, b) -> new CraftMetaBlockState(b, a))
        .put("BOOK", (a, b) -> new CraftMetaBook(b))
        .put("BOOK_SIGNED", (a, b) -> new CraftMetaBookSigned(b))
        .put("SKULL", (a, b) -> new CraftMetaSkull(b))
        .put("LEATHER_ARMOR", (a, b) -> new CraftMetaLeatherArmor(b))
        .put("MAP", (a, b) -> new CraftMetaMap(b))
        .put("POTION", (a, b) -> new CraftMetaPotion(b))
        .put("SPAWN_EGG", (a, b) -> new CraftMetaSpawnEgg(b))
        .put("ENCHANTED", (a, b) -> new CraftMetaEnchantedBook(b))
        .put("FIREWORK", (a, b) -> new CraftMetaFirework(b))
        .put("FIREWORK_EFFECT", (a, b) -> new CraftMetaCharge(b))
        .put("KNOWLEDGE_BOOK", (a, b) -> new CraftMetaKnowledgeBook(b))
        .put("TROPICAL_FISH_BUCKET", (a, b) -> new CraftMetaTropicalFishBucket(b))
        .put("CROSSBOW", (a, b) -> new CraftMetaCrossbow(b))
        .put("SUSPICIOUS_STEW", (a, b) -> new CraftMetaSuspiciousStew(b))
        .put("UNSPECIFIC", (a, b) -> new CraftMetaItem(b))
        .build();

    @SuppressWarnings("AmbiguousMixinReference")
    @Inject(method = "getItemMeta*", require = 0, expect = 0, cancellable = true, at = @At("HEAD"))
    private void arclight$getItemMeta(Material material, CraftMetaItem meta, CallbackInfoReturnable<ItemMeta> cir) {
        MaterialBridge bridge = (MaterialBridge) (Object) CraftLegacy.fromLegacy(material);
        if (bridge.bridge$getType() != MaterialPropertySpec.MaterialType.VANILLA) {
            MaterialPropertySpec spec = bridge.bridge$getSpec();
            BiFunction<Material, CraftMetaItem, ItemMeta> func;
            if (spec == null || spec.itemMetaType == null) {
                func = (a, b) -> new CraftMetaItem(b);
            } else {
                func = TYPES.get(spec.itemMetaType);
                if (func == null) {
                    func = (a, b) -> dynamicCreate(spec.itemMetaType, a, b);
                }
            }
            cir.setReturnValue(func.apply(material, meta));
        }
    }

    private ItemMeta dynamicCreate(String type, Material material, CraftMetaItem meta) {
        try {
            Class<?> cl = Class.forName(type);
            if (!CraftMetaItem.class.isAssignableFrom(cl)) {
                throw new IllegalArgumentException("" + cl + " is not assignable from " + CraftMetaItem.class);
            }
            for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1) {
                    if (parameterTypes[0] == Material.class) {
                        constructor.setAccessible(true);
                        return (ItemMeta) constructor.newInstance(material);
                    } else if (CraftMetaItem.class.isAssignableFrom(parameterTypes[0])) {
                        constructor.setAccessible(true);
                        return (ItemMeta) constructor.newInstance(meta);
                    }
                } else if (parameterTypes.length == 2) {
                    if (parameterTypes[0] == Material.class && CraftMetaItem.class.isAssignableFrom(parameterTypes[1])) {
                        constructor.setAccessible(true);
                        return (ItemMeta) constructor.newInstance(material, meta);
                    } else if (parameterTypes[1] == Material.class && CraftMetaItem.class.isAssignableFrom(parameterTypes[0])) {
                        constructor.setAccessible(true);
                        return (ItemMeta) constructor.newInstance(meta, material);
                    }
                }
            }
        } catch (Exception e) {
            ArclightMod.LOGGER.warn("Bad itemMetaType {} for {}: {}", type, material, e);
        }
        return new CraftMetaItem(meta);
    }
}
