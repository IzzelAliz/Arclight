package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.bridge.core.util.IWorldPosCallableBridge;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryEnchanting;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentContainerMixin extends AbstractContainerMenuMixin implements PosContainerBridge {

    // @formatter:off
    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow(remap = false) protected abstract float getPower(Level world, BlockPos pos);
    @Shadow @Final private Random random;
    @Shadow @Final private DataSlot enchantmentSeed;
    @Shadow @Final public int[] costs;
    @Shadow @Final public int[] enchantClue;
    @Shadow @Final public int[] levelClue;
    @Shadow protected abstract List<EnchantmentInstance> getEnchantmentList(ItemStack stack, int enchantSlot, int level);
    // @formatter:on

    private CraftInventoryView bukkitEntity = null;
    private Inventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void arclight$init(int id, Inventory playerInventory, ContainerLevelAccess worldPosCallable, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(net.minecraft.world.entity.player.Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void slotsChanged(Container inventoryIn) {
        if (inventoryIn == this.enchantSlots) {
            ItemStack itemstack = inventoryIn.getItem(0);
            if (!itemstack.isEmpty()) {
                this.access.execute((p_217002_2_, p_217002_3_) -> {
                    float power = 0;

                    for (int k = -1; k <= 1; ++k) {
                        for (int l = -1; l <= 1; ++l) {
                            if ((k != 0 || l != 0) && p_217002_2_.isEmptyBlock(p_217002_3_.offset(l, 0, k)) && p_217002_2_.isEmptyBlock(p_217002_3_.offset(l, 1, k))) {
                                power += getPower(p_217002_2_, p_217002_3_.offset(l * 2, 0, k * 2));
                                power += getPower(p_217002_2_, p_217002_3_.offset(l * 2, 1, k * 2));

                                if (l != 0 && k != 0) {
                                    power += getPower(p_217002_2_, p_217002_3_.offset(l * 2, 0, k));
                                    power += getPower(p_217002_2_, p_217002_3_.offset(l * 2, 1, k));
                                    power += getPower(p_217002_2_, p_217002_3_.offset(l, 0, k * 2));
                                    power += getPower(p_217002_2_, p_217002_3_.offset(l, 1, k * 2));
                                }
                            }
                        }
                    }

                    this.random.setSeed(this.enchantmentSeed.get());

                    for (int i1 = 0; i1 < 3; ++i1) {
                        this.costs[i1] = EnchantmentHelper.getEnchantmentCost(this.random, i1, (int) power, itemstack);
                        this.enchantClue[i1] = -1;
                        this.levelClue[i1] = -1;
                        if (this.costs[i1] < i1 + 1) {
                            this.costs[i1] = 0;
                        }
                        this.costs[i1] = ForgeEventFactory.onEnchantmentLevelSet(p_217002_2_, p_217002_3_, i1, (int) power, itemstack, costs[i1]);
                    }

                    for (int j1 = 0; j1 < 3; ++j1) {
                        if (this.costs[j1] > 0) {
                            List<EnchantmentInstance> list = this.getEnchantmentList(itemstack, j1, this.costs[j1]);
                            if (list != null && !list.isEmpty()) {
                                EnchantmentInstance enchantmentdata = list.get(this.random.nextInt(list.size()));
                                this.enchantClue[j1] = Registry.ENCHANTMENT.getId(enchantmentdata.enchantment);
                                this.levelClue[j1] = enchantmentdata.level;
                            }
                        }
                    }


                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);
                    org.bukkit.enchantments.EnchantmentOffer[] offers = new EnchantmentOffer[3];
                    for (int j = 0; j < 3; ++j) {
                        org.bukkit.enchantments.Enchantment enchantment = (this.enchantClue[j] >= 0) ? org.bukkit.enchantments.Enchantment.getByKey(CraftNamespacedKey.fromMinecraft(ForgeRegistries.ENCHANTMENTS.getKey(Registry.ENCHANTMENT.byId(this.enchantClue[j])))) : null;
                        offers[j] = (enchantment != null) ? new EnchantmentOffer(enchantment, this.levelClue[j], this.costs[j]) : null;
                    }

                    PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(((ServerPlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), this.getBukkitView(), ((IWorldPosCallableBridge) this.access).bridge$getLocation().getBlock(), item, offers, (int) power);
                    event.setCancelled(!itemstack.isEnchantable());
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
                            this.enchantClue[j] = Registry.ENCHANTMENT.getId(ForgeRegistries.ENCHANTMENTS.getValue(CraftNamespacedKey.toMinecraft(offer.getEnchantment().getKey())));
                            this.levelClue[j] = offer.getEnchantmentLevel();
                        } else {
                            this.costs[j] = 0;
                            this.enchantClue[j] = -1;
                            this.levelClue[j] = -1;
                        }
                    }

                    this.broadcastChanges();
                });
            } else {
                for (int i = 0; i < 3; ++i) {
                    this.costs[i] = 0;
                    this.enchantClue[i] = -1;
                    this.levelClue[i] = -1;
                }
            }
        }

    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean clickMenuButton(net.minecraft.world.entity.player.Player playerIn, int id) {
        ItemStack itemstack = this.enchantSlots.getItem(0);
        ItemStack itemstack1 = this.enchantSlots.getItem(1);
        int i = id + 1;
        if ((itemstack1.isEmpty() || itemstack1.getCount() < i) && !playerIn.getAbilities().instabuild) {
            return false;
        } else if (this.costs[id] <= 0 || itemstack.isEmpty() || (playerIn.experienceLevel < i || playerIn.experienceLevel < this.costs[id]) && !playerIn.getAbilities().instabuild) {
            return false;
        } else {
            this.access.execute((p_217003_6_, p_217003_7_) -> {
                ItemStack itemstack2 = itemstack;
                List<EnchantmentInstance> list = this.getEnchantmentList(itemstack, id, this.costs[id]);
                if (true || !list.isEmpty()) {

                    //  playerIn.onEnchant(itemstack, i);
                    boolean flag = itemstack.getItem() == Items.BOOK;
                    Map<Enchantment, Integer> enchants = new java.util.HashMap<>();
                    for (EnchantmentInstance obj : list) {
                        enchants.put(org.bukkit.enchantments.Enchantment.getByKey(CraftNamespacedKey.fromMinecraft(ForgeRegistries.ENCHANTMENTS.getKey(obj.enchantment))), obj.level);
                    }
                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack2);

                    EnchantItemEvent event = new EnchantItemEvent(((Player) ((PlayerEntityBridge) playerIn).bridge$getBukkitEntity()), this.getBukkitView(), ((IWorldPosCallableBridge) this.access).bridge$getLocation().getBlock(), item, this.costs[id], enchants, id);
                    Bukkit.getPluginManager().callEvent(event);

                    int level = event.getExpLevelCost();
                    if (event.isCancelled() || (level > playerIn.experienceLevel && !playerIn.getAbilities().instabuild) || event.getEnchantsToAdd().isEmpty()) {
                        return;
                    }

                    if (flag) {
                        itemstack2 = new ItemStack(Items.ENCHANTED_BOOK);

                        CompoundTag tag = itemstack2.getTag();
                        if (tag != null) {
                            itemstack2.setTag(tag.copy());
                        }

                        this.enchantSlots.setItem(0, itemstack2);
                    }

                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
                        try {
                            if (flag) {
                                NamespacedKey enchantId = entry.getKey().getKey();
                                net.minecraft.world.item.enchantment.Enchantment nms = ForgeRegistries.ENCHANTMENTS.getValue(CraftNamespacedKey.toMinecraft(enchantId));
                                if (nms == null) {
                                    continue;
                                }

                                EnchantmentInstance weightedrandomenchant = new EnchantmentInstance(nms, entry.getValue());
                                EnchantedBookItem.addEnchantment(itemstack2, weightedrandomenchant);
                            } else {
                                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                            }
                        } catch (IllegalArgumentException e) {
                            /* Just swallow invalid enchantments */
                        }
                    }
                    playerIn.onEnchantmentPerformed(itemstack, i);

                    if (!playerIn.getAbilities().instabuild) {
                        itemstack1.shrink(i);
                        if (itemstack1.isEmpty()) {
                            this.enchantSlots.setItem(1, ItemStack.EMPTY);
                        }
                    }

                    playerIn.awardStat(Stats.ENCHANT_ITEM);
                    if (playerIn instanceof ServerPlayer) {
                        CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayer) playerIn, itemstack2, i);
                    }

                    this.enchantSlots.setChanged();
                    this.enchantmentSeed.set(playerIn.getEnchantmentSeed());
                    this.slotsChanged(this.enchantSlots);
                    p_217003_6_.playSound(null, p_217003_7_, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, p_217003_6_.random.nextFloat() * 0.1F + 0.9F);
                }

            });
            return true;
        }
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryEnchanting inventory = new CraftInventoryEnchanting(this.enchantSlots);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (AbstractContainerMenu) (Object) this);
        return bukkitEntity;
    }

    @Override
    public ContainerLevelAccess bridge$getWorldPos() {
        return this.access;
    }
}
