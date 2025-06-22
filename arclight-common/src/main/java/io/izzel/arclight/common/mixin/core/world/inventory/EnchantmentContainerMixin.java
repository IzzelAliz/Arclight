package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.EnchantmentMenuBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.bridge.core.util.IWorldPosCallableBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryEnchanting;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.view.CraftEnchantmentView;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.view.EnchantmentView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// morejs https://github.com/AlmostReliable/morejs/blob/fd738a28a054d780031c7666fc8a01533c86f63b/Common/src/main/java/com/almostreliable/morejs/mixin/enchanting/EnchantmentMenuMixin.java
@Mixin(value = EnchantmentMenu.class, priority = 39)
public abstract class EnchantmentContainerMixin extends AbstractContainerMenuMixin implements PosContainerBridge, EnchantmentMenuBridge {

    // @formatter:off
    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final public int[] costs;
    @Shadow @Final public int[] enchantClue;
    @Shadow @Final public int[] levelClue;
    // @formatter:on

    private CraftEnchantmentView bukkitEntity = null;
    private Inventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void arclight$init(int id, Inventory playerInventory, ContainerLevelAccess worldPosCallable, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(net.minecraft.world.entity.player.Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    private transient boolean arclight$enchantable;

    @Decorate(method = "slotsChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEnchantable()Z"))
    private boolean arclight$relaxCondition(ItemStack instance) throws Throwable {
        arclight$enchantable = (boolean) DecorationOps.callsite().invoke(instance);
        return true;
    }

    private transient int arclight$power;

    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"))
    private int arclight$lastPower(RandomSource random, int i, int power, ItemStack arg) throws Throwable {
        return (int) DecorationOps.callsite().invoke(random, i, arclight$power = power, arg);
    }

    @Decorate(method = "*", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;broadcastChanges()V"))
    private void arclight$prepareEnchantEvent(@Local(ordinal = 0) ItemStack itemstack,
                                              @Local(ordinal = 0) Level level) {
        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);
        var offers = new EnchantmentOffer[3];
        for (int j = 0; j < 3; ++j) {
            var enchantment = (this.enchantClue[j] >= 0) ? org.bukkit.enchantments.Enchantment.getByKey(CraftNamespacedKey.fromMinecraft(registry.getKey(registry.byId(this.enchantClue[j])))) : null;
            offers[j] = (enchantment != null) ? new EnchantmentOffer(enchantment, this.levelClue[j], this.costs[j]) : null;
        }

        PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(((ServerPlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), (EnchantmentView) this.getBukkitView(), ((IWorldPosCallableBridge) this.access).bridge$getLocation().getBlock(), item, offers, arclight$power);
        event.setCancelled(!arclight$enchantable);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            for (int j = 0; j < 3; ++j) {
                this.costs[j] = 0;
                this.enchantClue[j] = -1;
                this.levelClue[j] = -1;
            }
            return;
        }

        for (int j = 0; j < 3; j++) {
            EnchantmentOffer offer = event.getOffers()[j];
            if (offer != null) {
                this.costs[j] = offer.getCost();
                this.enchantClue[j] = registry.getId(registry.get(CraftNamespacedKey.toMinecraft(offer.getEnchantment().getKey())));
                this.levelClue[j] = offer.getEnchantmentLevel();
            } else {
                this.costs[j] = 0;
                this.enchantClue[j] = -1;
                this.levelClue[j] = -1;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;getEnchantmentList(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;"),
        slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V")))
    private List<EnchantmentInstance> arclight$itemEnchantEvent(EnchantmentMenu instance, RegistryAccess registryAccess, ItemStack itemStack, int i, int j,
                                                              @Local(ordinal = -1) Level level,
                                                              @Local(ordinal = -1) net.minecraft.world.entity.player.Player playerIn) throws Throwable {
        var list = (List<EnchantmentInstance>) DecorationOps.callsite().invoke(instance, registryAccess, itemStack, i, j);
        IdMap<Holder<net.minecraft.world.item.enchantment.Enchantment>> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).asHolderIdMap();

        Map<Enchantment, Integer> enchants = new java.util.HashMap<>();
        for (EnchantmentInstance enchantmentInstance : list) {
            enchants.put(CraftEnchantment.minecraftHolderToBukkit(enchantmentInstance.enchantment), enchantmentInstance.level);
        }
        CraftItemStack item = CraftItemStack.asCraftMirror(itemStack);

        var hintedEnchantment = CraftEnchantment.minecraftHolderToBukkit(registry.byId(enchantClue[i]));
        int hintedEnchantmentLevel = levelClue[i];
        EnchantItemEvent event = new EnchantItemEvent(((Player) ((PlayerEntityBridge) playerIn).bridge$getBukkitEntity()), this.getBukkitView(), ((IWorldPosCallableBridge) this.access).bridge$getLocation().getBlock(), item, this.costs[i], enchants, hintedEnchantment, hintedEnchantmentLevel, i);
        Bukkit.getPluginManager().callEvent(event);

        int levelCost = event.getExpLevelCost();
        if (event.isCancelled() || (levelCost > playerIn.experienceLevel && !playerIn.getAbilities().instabuild) || event.getEnchantsToAdd().isEmpty()) {
            return (List<EnchantmentInstance>) DecorationOps.cancel().invoke();
        }
        var newList = new ArrayList<EnchantmentInstance>();
        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
            Holder<net.minecraft.world.item.enchantment.Enchantment> nms = CraftEnchantment.bukkitToMinecraftHolder(entry.getKey());
            if (nms == null) {
                continue;
            }

            EnchantmentInstance enchantmentInstance = new EnchantmentInstance(nms, entry.getValue());
            newList.add(enchantmentInstance);
        }
        return newList;
    }

    @Override
    public CraftEnchantmentView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryEnchanting inventory = new CraftInventoryEnchanting(this.enchantSlots);
        bukkitEntity = new CraftEnchantmentView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (EnchantmentMenu) (Object) this);
        return bukkitEntity;
    }

    @Override
    public ContainerLevelAccess bridge$getWorldPos() {
        return this.access;
    }
}
