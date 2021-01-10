package io.izzel.arclight.common.mod.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.bukkit.EntityTypeBridge;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import io.izzel.arclight.common.mod.util.types.ArclightEnchantment;
import io.izzel.arclight.common.mod.util.types.ArclightPotionEffect;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.conf.EntityPropertySpec;
import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraftforge.fml.CrashReportExtender;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v.CraftCrashReport;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "ConstantConditions"})
public class BukkitRegistry {

    private static final List<Class<?>> MAT_CTOR = ImmutableList.of(int.class);
    private static final List<Class<?>> ENTITY_CTOR = ImmutableList.of(String.class, Class.class, int.class);
    private static final List<Class<?>> ENV_CTOR = ImmutableList.of(int.class);
    private static final Map<String, Material> BY_NAME = Unsafe.getStatic(Material.class, "BY_NAME");
    private static final Map<Block, Material> BLOCK_MATERIAL = Unsafe.getStatic(CraftMagicNumbers.class, "BLOCK_MATERIAL");
    private static final Map<Item, Material> ITEM_MATERIAL = Unsafe.getStatic(CraftMagicNumbers.class, "ITEM_MATERIAL");
    private static final Map<Material, Item> MATERIAL_ITEM = Unsafe.getStatic(CraftMagicNumbers.class, "MATERIAL_ITEM");
    private static final Map<Material, Block> MATERIAL_BLOCK = Unsafe.getStatic(CraftMagicNumbers.class, "MATERIAL_BLOCK");
    private static final Map<String, EntityType> ENTITY_NAME_MAP = Unsafe.getStatic(EntityType.class, "NAME_MAP");
    private static final Map<Integer, World.Environment> ENVIRONMENT_MAP = Unsafe.getStatic(World.Environment.class, "lookup");
    static final BiMap<RegistryKey<DimensionType>, World.Environment> DIM_MAP =
        HashBiMap.create(ImmutableMap.<RegistryKey<DimensionType>, World.Environment>builder()
            .put(DimensionType.OVERWORLD, World.Environment.NORMAL)
            .put(DimensionType.THE_NETHER, World.Environment.NETHER)
            .put(DimensionType.THE_END, World.Environment.THE_END)
            .build());

    public static void registerAll() {
        CrashReportExtender.registerCrashCallable("Arclight", () -> new CraftCrashReport().call().toString());
        loadMaterials();
        loadPotions();
        loadEnchantments();
        loadEntities();
        loadVillagerProfessions();
        loadBiomes();
    }

    private static void loadBiomes() {
        int i = Biome.values().length;
        List<Biome> newTypes = new ArrayList<>();
        Field key = Arrays.stream(Biome.class.getDeclaredFields()).filter(it -> it.getName().equals("key")).findAny().orElse(null);
        long keyOffset = Unsafe.objectFieldOffset(key);
        for (net.minecraft.world.biome.Biome biome : ForgeRegistries.BIOMES) {
            String name = ResourceLocationUtil.standardize(biome.getRegistryName());
            Biome bukkit;
            try {
                bukkit = Biome.valueOf(name);
            } catch (Throwable t) {
                bukkit = null;
            }
            if (bukkit == null) {
                bukkit = EnumHelper.makeEnum(Biome.class, name, i++, ImmutableList.of(), ImmutableList.of());
                newTypes.add(bukkit);
                Unsafe.putObject(bukkit, keyOffset, CraftNamespacedKey.fromMinecraft(biome.getRegistryName()));
                ArclightMod.LOGGER.debug("Registered {} as biome {}", biome.getRegistryName(), bukkit);
            }
        }
        EnumHelper.addEnums(Biome.class, newTypes);
        ArclightMod.LOGGER.info("registry.biome", newTypes.size());
    }

    private static void loadVillagerProfessions() {
        int i = Villager.Profession.values().length;
        List<Villager.Profession> newTypes = new ArrayList<>();
        Field key = Arrays.stream(Villager.Profession.class.getDeclaredFields()).filter(it -> it.getName().equals("key")).findAny().orElse(null);
        long keyOffset = Unsafe.objectFieldOffset(key);
        for (VillagerProfession villagerProfession : ForgeRegistries.PROFESSIONS) {
            String name = ResourceLocationUtil.standardize(villagerProfession.getRegistryName());
            Villager.Profession profession;
            try {
                profession = Villager.Profession.valueOf(name);
            } catch (Throwable t) {
                profession = null;
            }
            if (profession == null) {
                profession = EnumHelper.makeEnum(Villager.Profession.class, name, i++, ImmutableList.of(), ImmutableList.of());
                newTypes.add(profession);
                Unsafe.putObject(profession, keyOffset, CraftNamespacedKey.fromMinecraft(villagerProfession.getRegistryName()));
                ArclightMod.LOGGER.debug("Registered {} as villager profession {}", villagerProfession.getRegistryName(), profession);
            }
        }
        EnumHelper.addEnums(Villager.Profession.class, newTypes);
        ArclightMod.LOGGER.info("registry.villager-profession", newTypes.size());
    }

    public static void registerEnvironments() {
        int i = World.Environment.values().length;
        List<World.Environment> newTypes = new ArrayList<>();
        Registry<DimensionType> registry = ArclightServer.getMinecraftServer().func_244267_aX().getRegistry(Registry.DIMENSION_TYPE_KEY);
        for (Map.Entry<RegistryKey<DimensionType>, DimensionType> entry : registry.getEntries()) {
            RegistryKey<DimensionType> key = entry.getKey();
            World.Environment environment = DIM_MAP.get(key);
            if (environment == null) {
                String name = ResourceLocationUtil.standardize(key.getLocation());
                environment = EnumHelper.makeEnum(World.Environment.class, name, i, ENV_CTOR, ImmutableList.of(i - 1));
                newTypes.add(environment);
                ENVIRONMENT_MAP.put(i - 1, environment);
                DIM_MAP.put(key, environment);
                ArclightMod.LOGGER.debug("Registered {} as environment {}", key.getLocation(), environment);
                i++;
            }
        }
        EnumHelper.addEnums(World.Environment.class, newTypes);
        ArclightMod.LOGGER.info("registry.environment", newTypes.size());
    }

    private static void loadEntities() {
        int origin = EntityType.values().length;
        int i = origin;
        List<EntityType> newTypes = new ArrayList<>(ForgeRegistries.ENTITIES.getEntries().size() - origin + 1); // UNKNOWN
        for (net.minecraft.entity.EntityType<?> type : ForgeRegistries.ENTITIES) {
            ResourceLocation location = type.getRegistryName();
            EntityType entityType = null;
            boolean found = false;
            if (location.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                entityType = EntityType.fromName(location.getPath());
                if (entityType != null) found = true;
                else ArclightMod.LOGGER.warn("Not found {} in {}", location, EntityType.class);
            }
            if (!found) {
                String name = ResourceLocationUtil.standardize(location);
                entityType = EnumHelper.makeEnum(EntityType.class, name, i++, ENTITY_CTOR, ImmutableList.of(location.getPath(), Entity.class, -1));
                ((EntityTypeBridge) (Object) entityType).bridge$setup(location, type, entitySpec(location));
                newTypes.add(entityType);
                ArclightMod.LOGGER.debug("Registered {} as entity {}", location, entityType);
            }
            ENTITY_NAME_MAP.put(location.toString(), entityType);
        }
        EnumHelper.addEnums(EntityType.class, newTypes);
        ArclightMod.LOGGER.info("registry.entity-type", newTypes.size());
    }

    private static void loadEnchantments() {
        int origin = Enchantment.values().length;
        int size = ForgeRegistries.ENCHANTMENTS.getEntries().size();
        putBool(Enchantment.class, "acceptingNew", true);
        for (net.minecraft.enchantment.Enchantment enc : ForgeRegistries.ENCHANTMENTS) {
            String name = ResourceLocationUtil.standardize(enc.getRegistryName());
            ArclightEnchantment enchantment = new ArclightEnchantment(enc, name);
            Enchantment.registerEnchantment(enchantment);
            ArclightMod.LOGGER.debug("Registered {} as enchantment {}", enc.getRegistryName(), enchantment);
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
        for (Effect eff : ForgeRegistries.POTIONS) {
            String name = ResourceLocationUtil.standardize(eff.getRegistryName());
            ArclightPotionEffect effect = new ArclightPotionEffect(eff, name);
            PotionEffectType.registerPotionEffectType(effect);
            ArclightMod.LOGGER.debug("Registered {} as potion {}", eff.getRegistryName(), effect);
        }
        PotionEffectType.stopAcceptingRegistrations();
        ArclightMod.LOGGER.info("registry.potion", size - origin);
    }

    private static void loadMaterials() {
        int blocks = 0, items = 0;
        int i = Material.values().length;
        int origin = i;
        List<Material> list = new ArrayList<>();
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation location = block.getRegistryName();
            String name = ResourceLocationUtil.standardize(location);
            Material material = BY_NAME.get(name);
            if (material == null) {
                material = EnumHelper.makeEnum(Material.class, name, i, MAT_CTOR, ImmutableList.of(i));
                ((MaterialBridge) (Object) material).bridge$setupBlock(location, block, matSpec(location));
                BY_NAME.put(name, material);
                i++;
                blocks++;
                ArclightMod.LOGGER.debug("Registered {} as block {}", location, material);
                list.add(material);
            } else {
                ((MaterialBridge) (Object) material).bridge$setupVanillaBlock(matSpec(location));
            }
            BLOCK_MATERIAL.put(block, material);
            MATERIAL_BLOCK.put(material, block);
            Item value = ForgeRegistries.ITEMS.getValue(location);
            if (value != null && value != Items.AIR) {
                ((MaterialBridge) (Object) material).bridge$setItem();
                ITEM_MATERIAL.put(value, material);
                MATERIAL_ITEM.put(material, value);
            }
        }
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation location = item.getRegistryName();
            String name = ResourceLocationUtil.standardize(location);
            Material material = BY_NAME.get(name);
            if (material == null) {
                material = EnumHelper.makeEnum(Material.class, name, i, MAT_CTOR, ImmutableList.of(i));
                ((MaterialBridge) (Object) material).bridge$setupItem(location, item, matSpec(location));
                BY_NAME.put(name, material);
                i++;
                items++;
                ArclightMod.LOGGER.debug("Registered {} as item {}", location, material);
                list.add(material);
            }
            ITEM_MATERIAL.put(item, material);
            MATERIAL_ITEM.put(material, item);
            Block value = ForgeRegistries.BLOCKS.getValue(location);
            if (value != null && value != Blocks.AIR) {
                ((MaterialBridge) (Object) material).bridge$setBlock();
                BLOCK_MATERIAL.put(value, material);
                MATERIAL_BLOCK.put(material, value);
            }
        }
        EnumHelper.addEnums(Material.class, list);
        ArclightMod.LOGGER.info("registry.material", i - origin, blocks, items);
    }

    private static MaterialPropertySpec matSpec(ResourceLocation location) {
        return ArclightConfig.spec().getCompat().getMaterial(location.toString()).orElse(MaterialPropertySpec.EMPTY);
    }

    private static EntityPropertySpec entitySpec(ResourceLocation location) {
        return ArclightConfig.spec().getCompat().getEntity(location.toString()).orElse(EntityPropertySpec.EMPTY);
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
