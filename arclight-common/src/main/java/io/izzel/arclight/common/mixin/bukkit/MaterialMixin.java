package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.collect.ImmutableMap;
import io.izzel.arclight.common.bridge.block.FireBlockBridge;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.server.block.ArclightTileInventory;
import io.izzel.arclight.i18n.LocalizedException;
import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
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
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(value = Material.class, remap = false)
public abstract class MaterialMixin implements MaterialBridge {

    // @formatter:off
    @Shadow @Mutable @Final private NamespacedKey key;
    @Shadow @Mutable @Final private Constructor<? extends MaterialData> ctor;
    @Shadow @Mutable @Final public Class<?> data;
    @Shadow public abstract boolean isBlock();
    // @formatter:on

    private static final Map<String, BiFunction<Material, CraftMetaItem, ItemMeta>> TYPES = ImmutableMap
        .<String, BiFunction<Material, CraftMetaItem, ItemMeta>>builder()
        .put("ARMOR_STAND", (a, b) -> b instanceof CraftMetaArmorStand ? b : new CraftMetaArmorStand(b))
        .put("BANNER", (a, b) -> b instanceof CraftMetaBanner ? b : new CraftMetaBanner(b))
        .put("TILE_ENTITY", (a, b) -> new CraftMetaBlockState(b, a))
        .put("BOOK", (a, b) -> b != null && b.getClass().equals(CraftMetaBook.class) ? b : new CraftMetaBook(b))
        .put("BOOK_SIGNED", (a, b) -> b instanceof CraftMetaBookSigned ? b : new CraftMetaBookSigned(b))
        .put("SKULL", (a, b) -> b instanceof CraftMetaSkull ? b : new CraftMetaSkull(b))
        .put("LEATHER_ARMOR", (a, b) -> b instanceof CraftMetaLeatherArmor ? b : new CraftMetaLeatherArmor(b))
        .put("MAP", (a, b) -> b instanceof CraftMetaMap ? b : new CraftMetaMap(b))
        .put("POTION", (a, b) -> b instanceof CraftMetaPotion ? b : new CraftMetaPotion(b))
        .put("SPAWN_EGG", (a, b) -> b instanceof CraftMetaSpawnEgg ? b : new CraftMetaSpawnEgg(b))
        .put("ENCHANTED", (a, b) -> b instanceof CraftMetaEnchantedBook ? b : new CraftMetaEnchantedBook(b))
        .put("FIREWORK", (a, b) -> b instanceof CraftMetaFirework ? b : new CraftMetaFirework(b))
        .put("FIREWORK_EFFECT", (a, b) -> b instanceof CraftMetaCharge ? b : new CraftMetaCharge(b))
        .put("KNOWLEDGE_BOOK", (a, b) -> b instanceof CraftMetaKnowledgeBook ? b : new CraftMetaKnowledgeBook(b))
        .put("TROPICAL_FISH_BUCKET", (a, b) -> b instanceof CraftMetaTropicalFishBucket ? b : new CraftMetaTropicalFishBucket(b))
        .put("CROSSBOW", (a, b) -> b instanceof CraftMetaCrossbow ? b : new CraftMetaCrossbow(b))
        .put("SUSPICIOUS_STEW", (a, b) -> b instanceof CraftMetaSuspiciousStew ? b : new CraftMetaSuspiciousStew(b))
        .put("UNSPECIFIC", (a, b) -> new CraftMetaItem(b))
        .put("NULL", (a, b) -> null)
        .build();

    private MaterialPropertySpec.MaterialType arclight$type = MaterialPropertySpec.MaterialType.VANILLA;
    private MaterialPropertySpec arclight$spec;
    private boolean arclight$block = false, arclight$item = false;

    @Override
    public void bridge$setBlock() {
        this.arclight$block = true;
    }

    @Override
    public void bridge$setItem() {
        this.arclight$item = true;
    }

    @Inject(method = "isBlock", cancellable = true, at = @At("HEAD"))
    private void arclight$isBlock(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$type != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(arclight$block);
        }
    }

    @Inject(method = "isItem", cancellable = true, at = @At("HEAD"))
    private void arclight$isItem(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$type != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(arclight$item);
        }
    }

    @Inject(method = "isEdible", cancellable = true, at = @At("HEAD"))
    private void arclight$isEdible(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.edible);
        }
    }

    @Inject(method = "isRecord", cancellable = true, at = @At("HEAD"))
    private void arclight$isRecord(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.record);
        }
    }

    @Inject(method = "isSolid", cancellable = true, at = @At("HEAD"))
    private void arclight$isSolid(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.solid);
        }
    }

    @Inject(method = "isAir", cancellable = true, at = @At("HEAD"))
    private void arclight$isAir(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.air);
        }
    }

    @Inject(method = "isTransparent", cancellable = true, at = @At("HEAD"))
    private void arclight$isTransparent(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.transparent);
        }
    }

    @Inject(method = "isFlammable", cancellable = true, at = @At("HEAD"))
    private void arclight$isFlammable(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.flammable);
        }
    }

    @Inject(method = "isBurnable", cancellable = true, at = @At("HEAD"))
    private void arclight$isBurnable(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.burnable);
        }
    }

    @Inject(method = "isFuel", cancellable = true, at = @At("HEAD"))
    private void arclight$isFuel(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.fuel);
        }
    }

    @Inject(method = "isOccluding", cancellable = true, at = @At("HEAD"))
    private void arclight$isOccluding(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.occluding);
        }
    }

    @Inject(method = "hasGravity", cancellable = true, at = @At("HEAD"))
    private void arclight$hasGravity(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.gravity);
        }
    }

    @Inject(method = "isInteractable", cancellable = true, at = @At("HEAD"))
    private void arclight$isInteractable(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.interactable);
        }
    }

    @Inject(method = "getHardness", cancellable = true, at = @At("HEAD"))
    private void arclight$getHardness(CallbackInfoReturnable<Float> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.hardness);
        }
    }

    @Inject(method = "getBlastResistance", cancellable = true, at = @At("HEAD"))
    private void arclight$getBlastResistance(CallbackInfoReturnable<Float> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.blastResistance);
        }
    }

    @Inject(method = "getCraftingRemainingItem", cancellable = true, at = @At("HEAD"))
    private void arclight$getCraftingRemainingItem(CallbackInfoReturnable<Material> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(Material.getMaterial(arclight$spec.craftingRemainingItem));
        }
    }

    @Override
    public MaterialPropertySpec bridge$getSpec() {
        return arclight$spec;
    }

    @Override
    public MaterialPropertySpec.MaterialType bridge$getType() {
        return arclight$type;
    }

    private Function<CraftMetaItem, ItemMeta> arclight$metaFunc;

    @Override
    public Function<CraftMetaItem, ItemMeta> bridge$itemMetaFactory() {
        return arclight$metaFunc;
    }

    @Override
    public void bridge$setItemMetaFactory(Function<CraftMetaItem, ItemMeta> func) {
        this.arclight$metaFunc = func;
    }

    private Function<CraftBlock, BlockState> arclight$stateFunc;

    @Override
    public Function<CraftBlock, BlockState> bridge$blockStateFactory() {
        return arclight$stateFunc;
    }

    @Override
    public void bridge$setBlockStateFactory(Function<CraftBlock, BlockState> func) {
        this.arclight$stateFunc = func;
    }

    @Override
    public void bridge$setupBlock(ResourceLocation key, Block block, MaterialPropertySpec spec) {
        this.arclight$spec = spec.clone();
        arclight$type = MaterialPropertySpec.MaterialType.FORGE;
        arclight$block = true;
        arclight$setupCommon(key, block, block.asItem());
    }

    @Override
    public void bridge$setupVanillaBlock(MaterialPropertySpec spec) {
        if (spec != MaterialPropertySpec.EMPTY) {
            this.arclight$spec = spec.clone();
            this.setupBlockStateFunc();
        }
    }

    @Override
    public void bridge$setupItem(ResourceLocation key, Item item, MaterialPropertySpec spec) {
        this.arclight$spec = spec.clone();
        arclight$type = MaterialPropertySpec.MaterialType.FORGE;
        arclight$item = true;
        arclight$setupCommon(key, null, item);
    }

    @Override
    public boolean bridge$shouldApplyStateFactory() {
        return this.arclight$type != MaterialPropertySpec.MaterialType.VANILLA ||
            (this.arclight$spec != null && this.arclight$spec.blockStateClass != null);
    }

    @SuppressWarnings("unchecked")
    private void arclight$setupCommon(ResourceLocation key, Block block, Item item) {
        this.key = CraftNamespacedKey.fromMinecraft(key);
        if (arclight$spec.materialDataClass != null) {
            try {
                Class<?> data = Class.forName(arclight$spec.materialDataClass);
                if (MaterialData.class.isAssignableFrom(data)) {
                    this.data = data;
                    this.ctor = (Constructor<? extends MaterialData>) data.getConstructor(Material.class, byte.class);
                }
            } catch (Exception e) {
                ArclightMod.LOGGER.warn("Bad material data class {} for {}", arclight$spec.materialDataClass, this);
                ArclightMod.LOGGER.warn(e);
            }
        }
        if (arclight$spec.maxStack == null) {
            arclight$spec.maxStack = tryGetMaxStackSize(item);
        }
        if (arclight$spec.maxDurability == null) {
            arclight$spec.maxDurability = tryGetDurability(item);
        }
        if (arclight$spec.edible == null) {
            arclight$spec.edible = false;
        }
        if (arclight$spec.record == null) {
            arclight$spec.record = false;
        }
        if (arclight$spec.solid == null) {
            arclight$spec.solid = block != null && block.getDefaultState().isSolid();
        }
        if (arclight$spec.air == null) {
            arclight$spec.air = block != null && block.getDefaultState().isAir();
        }
        if (arclight$spec.transparent == null) {
            arclight$spec.transparent = block != null && block.getDefaultState().isTransparent();
        }
        if (arclight$spec.flammable == null) {
            arclight$spec.flammable = block != null && ((FireBlockBridge) Blocks.FIRE).bridge$canBurn(block);
        }
        if (arclight$spec.burnable == null) {
            arclight$spec.burnable = block != null && ((FireBlockBridge) Blocks.FIRE).bridge$canBurn(block);
        }
        if (arclight$spec.fuel == null) {
            arclight$spec.fuel = item != null && new ItemStack(item).getBurnTime() > 0;
        }
        if (arclight$spec.occluding == null) {
            arclight$spec.occluding = arclight$spec.solid;
        }
        if (arclight$spec.gravity == null) {
            arclight$spec.gravity = block instanceof FallingBlock;
        }
        if (arclight$spec.interactable == null) {
            arclight$spec.interactable = true;
        }
        if (arclight$spec.hardness == null) {
            arclight$spec.hardness = block != null ? block.getDefaultState().hardness : 0;
        }
        if (arclight$spec.blastResistance == null) {
            arclight$spec.blastResistance = block != null ? block.getExplosionResistance() : 0;
        }
        if (arclight$spec.itemMetaType == null) {
            arclight$spec.itemMetaType = "UNSPECIFIC";
        }
        BiFunction<Material, CraftMetaItem, ItemMeta> function = TYPES.get(arclight$spec.itemMetaType);
        if (function != null) {
            this.arclight$metaFunc = meta -> function.apply((Material) (Object) this, meta);
        } else {
            this.arclight$metaFunc = dynamicMetaCreator(arclight$spec.itemMetaType);
        }
        this.setupBlockStateFunc();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupBlockStateFunc() {
        if (arclight$spec.blockStateClass != null && !arclight$spec.blockStateClass.equalsIgnoreCase("auto")) {
            try {
                Class<?> cl = Class.forName(arclight$spec.blockStateClass);
                if (!CraftBlockState.class.isAssignableFrom(cl)) {
                    throw LocalizedException.checked("registry.block-state.not-subclass", cl, CraftBlockState.class);
                }
                for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                    if (constructor.getParameterTypes().length == 1
                        && org.bukkit.block.Block.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
                        constructor.setAccessible(true);
                        this.arclight$stateFunc = b -> {
                            try {
                                return (BlockState) constructor.newInstance(b);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                    }
                }
            } catch (Exception e) {
                if (e instanceof LocalizedException) {
                    ArclightMod.LOGGER.warn(((LocalizedException) e).node(), ((LocalizedException) e).args());
                } else {
                    ArclightMod.LOGGER.warn("registry.block-state.error", this, arclight$spec.blockStateClass, e);
                }
            }
            if (this.arclight$stateFunc == null) {
                ArclightMod.LOGGER.warn("registry.block-state.no-candidate", this, arclight$spec.blockStateClass);
            }
        }
        if (this.arclight$stateFunc == null) {
            this.arclight$stateFunc = b -> {
                TileEntity tileEntity = b.getCraftWorld().getHandle().getTileEntity(b.getPosition());
                if (tileEntity instanceof IInventory) {
                    return new ArclightTileInventory(b, tileEntity.getClass());
                }
                return tileEntity == null ? new CraftBlockState(b) : new CraftBlockEntityState<>(b, tileEntity.getClass());
            };
        }
    }

    private Function<CraftMetaItem, ItemMeta> dynamicMetaCreator(String type) {
        Function<CraftMetaItem, ItemMeta> candidate = null;
        try {
            Class<?> cl = Class.forName(type);
            if (!CraftMetaItem.class.isAssignableFrom(cl)) {
                throw LocalizedException.checked("registry.meta-type.not-subclass", cl, CraftMetaItem.class);
            }
            for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1) {
                    if (parameterTypes[0] == Material.class) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(this);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    } else if (CraftMetaItem.class.isAssignableFrom(parameterTypes[0])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(meta);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    }
                } else if (parameterTypes.length == 2) {
                    if (parameterTypes[0] == Material.class && CraftMetaItem.class.isAssignableFrom(parameterTypes[1])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(this, meta);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    } else if (parameterTypes[1] == Material.class && CraftMetaItem.class.isAssignableFrom(parameterTypes[0])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(meta, this);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof LocalizedException) {
                ArclightMod.LOGGER.warn(((LocalizedException) e).node(), ((LocalizedException) e).args());
            } else {
                ArclightMod.LOGGER.warn("registry.meta-type.error", this, type, e);
            }
        }
        if (candidate == null) {
            ArclightMod.LOGGER.warn("registry.meta-type.no-candidate", this, type);
            candidate = CraftMetaItem::new;
        }
        return candidate;
    }

    private static int tryGetMaxStackSize(Item item) {
        try {
            return item.getItemStackLimit(new ItemStack(item));
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
            return item.getMaxDamage(new ItemStack(item));
        } catch (Throwable t) {
            try {
                return item.getMaxDamage();
            } catch (Throwable t1) {
                return 0;
            }
        }
    }
}
