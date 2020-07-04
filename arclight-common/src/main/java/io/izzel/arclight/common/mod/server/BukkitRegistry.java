package io.izzel.arclight.common.mod.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.potion.ArclightPotionEffect;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "ConstantConditions"})
public class BukkitRegistry {

    private static final List<Class<?>> MAT_CTOR = ImmutableList.of(int.class);
    private static final Map<String, Material> BY_NAME = getStatic(Material.class, "BY_NAME");
    private static final Map<Block, Material> BLOCK_MATERIAL = getStatic(CraftMagicNumbers.class, "BLOCK_MATERIAL");
    private static final Map<Item, Material> ITEM_MATERIAL = getStatic(CraftMagicNumbers.class, "ITEM_MATERIAL");
    private static final Map<Material, Item> MATERIAL_ITEM = getStatic(CraftMagicNumbers.class, "MATERIAL_ITEM");
    private static final Map<Material, Block> MATERIAL_BLOCK = getStatic(CraftMagicNumbers.class, "MATERIAL_BLOCK");

    public static void registerAll() {
        loadMaterials();
        loadPotions();
        loadEnchantments();
    }

    private static void loadEnchantments() {
        int origin = Enchantment.values().length;
        int size = ForgeRegistries.ENCHANTMENTS.getEntries().size();
        putBool(Enchantment.class, "acceptingNew", true);
        for (Map.Entry<ResourceLocation, net.minecraft.enchantment.Enchantment> entry : ForgeRegistries.ENCHANTMENTS.getEntries()) {
            Enchantment.registerEnchantment(new CraftEnchantment(entry.getValue()));
        }
        Enchantment.stopAcceptingRegistrations();
        ArclightMod.LOGGER.info("registry.enchantment", size - origin);
    }

    private static void loadPotions() {
        int origin = PotionEffectType.values().length;
        int size = ForgeRegistries.POTIONS.getEntries().size();
        PotionEffectType[] types = new PotionEffectType[size + 1];
        putStatic(PotionEffectType.class, "byId", types);
        putBool(PotionEffectType.class, "acceptingNew", true);
        for (Map.Entry<ResourceLocation, Effect> entry : ForgeRegistries.POTIONS.getEntries()) {
            String name = toName(entry.getKey());
            ArclightPotionEffect effect = new ArclightPotionEffect(entry.getValue(), name);
            PotionEffectType.registerPotionEffectType(effect);
            ArclightMod.LOGGER.debug("Registered {}: {} as potion", entry.getKey(), effect);
        }
        PotionEffectType.stopAcceptingRegistrations();
        ArclightMod.LOGGER.info("registry.potion", size - origin);
    }

    private static void loadMaterials() {
        int blocks = 0, items = 0;
        int i = Material.values().length;
        int origin = i;
        for (Map.Entry<ResourceLocation, Block> entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation location = entry.getKey();
            Block block = entry.getValue();
            Material material = Material.matchMaterial(location.toString());
            if (material == null) {
                String name = toName(location);
                material = EnumHelper.makeEnum(Material.class, name, i, MAT_CTOR, ImmutableList.of(i));
                ((MaterialBridge) (Object) material).bridge$setupBlock(location, block, spec(location));
                BY_NAME.put(name, material);
                i++;
                blocks++;
                ArclightMod.LOGGER.debug("Registered {} as block {}", location, material);
            }
            BLOCK_MATERIAL.put(block, material);
            MATERIAL_BLOCK.put(material, block);
        }
        for (Map.Entry<ResourceLocation, Item> entry : ForgeRegistries.ITEMS.getEntries()) {
            ResourceLocation location = entry.getKey();
            Item item = entry.getValue();
            Material material = Material.matchMaterial(location.toString());
            if (material == null) {
                String name = toName(location);
                material = EnumHelper.makeEnum(Material.class, name, i, MAT_CTOR, ImmutableList.of(i));
                ((MaterialBridge) (Object) material).bridge$setupItem(location, item, spec(location));
                BY_NAME.put(name, material);
                i++;
                items++;
                ArclightMod.LOGGER.debug("Registered {} as item {}", location, material);
            }
            ITEM_MATERIAL.put(item, material);
            MATERIAL_ITEM.put(material, item);
        }
        ArclightMod.LOGGER.info("registry.material", i - origin, blocks, items);
    }

    private static String toName(ResourceLocation location) {
        return location.toString()
            .replace(':', '_')
            .replaceAll("\\s+", "_")
            .replaceAll("\\W", "")
            .toUpperCase(Locale.ENGLISH);
    }

    private static MaterialPropertySpec spec(ResourceLocation location) {
        return ArclightConfig.spec().getCompat().getOverride(location.toString()).orElse(MaterialPropertySpec.EMPTY);
    }

    private static <T> T getStatic(Class<?> cl, String name) {
        try {
            Unsafe.ensureClassInitialized(cl);
            Field field = cl.getDeclaredField(name);
            Object materialByNameBase = Unsafe.staticFieldBase(field);
            long materialByNameOffset = Unsafe.staticFieldOffset(field);
            return (T) Unsafe.getObject(materialByNameBase, materialByNameOffset);
        } catch (Exception e) {
            return null;
        }
    }

    private static void putStatic(Class<?> cl, String name, Object o) {
        try {
            Unsafe.ensureClassInitialized(cl);
            Field field = cl.getDeclaredField(name);
            Object materialByNameBase = Unsafe.staticFieldBase(field);
            long materialByNameOffset = Unsafe.staticFieldOffset(field);
            Unsafe.putObject(materialByNameBase, materialByNameOffset, o);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void putBool(Class<?> cl, String name, boolean b) {
        try {
            Unsafe.ensureClassInitialized(cl);
            Field field = cl.getDeclaredField(name);
            Object materialByNameBase = Unsafe.staticFieldBase(field);
            long materialByNameOffset = Unsafe.staticFieldOffset(field);
            Unsafe.putBoolean(materialByNameBase, materialByNameOffset, b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Set<IForgeRegistry<?>> registries() {
        return ImmutableSet.of(ForgeRegistries.BLOCKS, ForgeRegistries.ITEMS,
            ForgeRegistries.POTION_TYPES, ForgeRegistries.POTIONS,
            ForgeRegistries.ENTITIES, ForgeRegistries.TILE_ENTITIES,
            ForgeRegistries.BIOMES);
    }

    public static void unlockRegistries() {
        for (IForgeRegistry<?> registry : registries()) {
            if (registry instanceof ForgeRegistry) {
                ((ForgeRegistry<?>) registry).unfreeze();
            }
        }
    }

    public static void lockRegistries() {
        for (IForgeRegistry<?> registry : registries()) {
            if (registry instanceof ForgeRegistry) {
                ((ForgeRegistry<?>) registry).freeze();
            }
        }
    }
}
