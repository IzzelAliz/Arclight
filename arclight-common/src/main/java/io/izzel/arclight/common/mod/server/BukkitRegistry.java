package io.izzel.arclight.common.mod.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.potion.ArclightPotionEffect;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "ConstantConditions"})
public class BukkitRegistry {

    private static List<Class<?>> MAT_CTOR = ImmutableList.of(int.class, int.class, int.class);
    private static Map<String, Material> BY_NAME = getStatic(Material.class, "BY_NAME");
    private static Map<Block, Material> BLOCK_MATERIAL = getStatic(CraftMagicNumbers.class, "BLOCK_MATERIAL");
    private static Map<Item, Material> ITEM_MATERIAL = getStatic(CraftMagicNumbers.class, "ITEM_MATERIAL");
    private static Map<Material, Item> MATERIAL_ITEM = getStatic(CraftMagicNumbers.class, "MATERIAL_ITEM");
    private static Map<Material, Block> MATERIAL_BLOCK = getStatic(CraftMagicNumbers.class, "MATERIAL_BLOCK");

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
        ArclightMod.LOGGER.info("Registered {} new enchantments", size - origin);
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
        ArclightMod.LOGGER.info("Registered {} new potion effect types", size - origin);
    }

    private static void loadMaterials() {
        List<Material> newMats = new ArrayList<>(ForgeRegistries.BLOCKS.getKeys().size() + ForgeRegistries.ITEMS.getKeys().size());
        int blocks = 0, items = 0;
        int i = Material.values().length;
        int origin = i;
        for (Map.Entry<ResourceLocation, Block> entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation location = entry.getKey();
            Block block = entry.getValue();
            Material material = Material.matchMaterial(location.toString());
            if (material == null) {
                Item item = block.asItem();
                int maxStack = tryGetMaxStackSize(item);
                int durability = tryGetDurability(item);
                String name = toName(location);
                material = EnumHelper.makeEnum(Material.class, name, i, MAT_CTOR, ImmutableList.of(i, maxStack, durability));
                if (!newMats.contains(material)) {
                    newMats.add(material);
                }
                BY_NAME.put(name, material);
                ((MaterialBridge) (Object) material).bridge$setInternal(block.getMaterial(block.getDefaultState()));
                i++;
                blocks++;
                ArclightMod.LOGGER.debug("Registered {}: {} as block", location, material);
            }
            ((MaterialBridge) (Object) material).bridge$setKey(CraftNamespacedKey.fromMinecraft(location));
            ((MaterialBridge) (Object) material).bridge$setBlock();
            BLOCK_MATERIAL.put(block, material);
            MATERIAL_BLOCK.put(material, block);
        }
        for (Map.Entry<ResourceLocation, Item> entry : ForgeRegistries.ITEMS.getEntries()) {
            ResourceLocation location = entry.getKey();
            Item item = entry.getValue();
            Material material = Material.matchMaterial(location.toString());
            if (material == null) {
                int maxStack = tryGetMaxStackSize(item);
                int durability = tryGetDurability(item);
                String name = toName(location);
                material = EnumHelper.makeEnum(Material.class, name, i, MAT_CTOR, ImmutableList.of(i, maxStack, durability));
                if (!newMats.contains(material)) {
                    newMats.add(material);
                }
                BY_NAME.put(name, material);
                i++;
                items++;
                ArclightMod.LOGGER.debug("Registered {}: {} as item", location, material);
            }
            ((MaterialBridge) (Object) material).bridge$setKey(CraftNamespacedKey.fromMinecraft(location));
            ((MaterialBridge) (Object) material).bridge$setItem();
            ITEM_MATERIAL.put(item, material);
            MATERIAL_ITEM.put(material, item);
        }
        ArclightMod.LOGGER.info("Registered {} new materials, with {} blocks and {} items", i - origin, blocks, items);
    }

    private static String toName(ResourceLocation location) {
        return location.toString().replace(':', '_').toUpperCase(Locale.ENGLISH);
    }

    private static int tryGetMaxStackSize(Item item) {
        try {
            return item.getItemStackLimit(item.getDefaultInstance());
        } catch (Throwable t) {
            try {
                return item.getMaxStackSize();
            } catch (Throwable t1) {
                return 64;
            }
        }
    }

    private static int tryGetDurability(Item item) {
        try {
            return item.getMaxDamage(item.getDefaultInstance());
        } catch (Throwable t) {
            try {
                return item.getMaxDamage();
            } catch (Throwable t1) {
                return 0;
            }
        }
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
