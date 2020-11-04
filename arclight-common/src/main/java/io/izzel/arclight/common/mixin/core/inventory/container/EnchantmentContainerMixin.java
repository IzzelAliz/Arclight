package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.bridge.util.IWorldPosCallableBridge;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.EnchantmentContainer;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
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

@Mixin(EnchantmentContainer.class)
public abstract class EnchantmentContainerMixin extends ContainerMixin implements PosContainerBridge {

    // @formatter:off
    @Shadow @Final private IInventory tableInventory;
    @Shadow @Final private IWorldPosCallable worldPosCallable;
    @Shadow(remap = false) protected abstract float getPower(World world, BlockPos pos);
    @Shadow @Final private Random rand;
    @Shadow @Final private IntReferenceHolder xpSeed;
    @Shadow @Final public int[] enchantLevels;
    @Shadow @Final public int[] enchantClue;
    @Shadow @Final public int[] worldClue;
    @Shadow protected abstract List<EnchantmentData> getEnchantmentList(ItemStack stack, int enchantSlot, int level);
    // @formatter:on

    private CraftInventoryView bukkitEntity = null;
    private PlayerInventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/util/IWorldPosCallable;)V", at = @At("RETURN"))
    public void arclight$init(int id, PlayerInventory playerInventory, IWorldPosCallable worldPosCallable, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Inject(method = "canInteractWith", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(PlayerEntity playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        if (inventoryIn == this.tableInventory) {
            ItemStack itemstack = inventoryIn.getStackInSlot(0);
            if (!itemstack.isEmpty()) {
                this.worldPosCallable.consume((p_217002_2_, p_217002_3_) -> {
                    float power = 0;

                    for (int k = -1; k <= 1; ++k) {
                        for (int l = -1; l <= 1; ++l) {
                            if ((k != 0 || l != 0) && p_217002_2_.isAirBlock(p_217002_3_.add(l, 0, k)) && p_217002_2_.isAirBlock(p_217002_3_.add(l, 1, k))) {
                                power += getPower(p_217002_2_, p_217002_3_.add(l * 2, 0, k * 2));
                                power += getPower(p_217002_2_, p_217002_3_.add(l * 2, 1, k * 2));

                                if (l != 0 && k != 0) {
                                    power += getPower(p_217002_2_, p_217002_3_.add(l * 2, 0, k));
                                    power += getPower(p_217002_2_, p_217002_3_.add(l * 2, 1, k));
                                    power += getPower(p_217002_2_, p_217002_3_.add(l, 0, k * 2));
                                    power += getPower(p_217002_2_, p_217002_3_.add(l, 1, k * 2));
                                }
                            }
                        }
                    }

                    this.rand.setSeed(this.xpSeed.get());

                    for (int i1 = 0; i1 < 3; ++i1) {
                        this.enchantLevels[i1] = EnchantmentHelper.calcItemStackEnchantability(this.rand, i1, (int) power, itemstack);
                        this.enchantClue[i1] = -1;
                        this.worldClue[i1] = -1;
                        if (this.enchantLevels[i1] < i1 + 1) {
                            this.enchantLevels[i1] = 0;
                        }
                        this.enchantLevels[i1] = ForgeEventFactory.onEnchantmentLevelSet(p_217002_2_, p_217002_3_, i1, (int) power, itemstack, enchantLevels[i1]);
                    }

                    for (int j1 = 0; j1 < 3; ++j1) {
                        if (this.enchantLevels[j1] > 0) {
                            List<EnchantmentData> list = this.getEnchantmentList(itemstack, j1, this.enchantLevels[j1]);
                            if (list != null && !list.isEmpty()) {
                                EnchantmentData enchantmentdata = list.get(this.rand.nextInt(list.size()));
                                this.enchantClue[j1] = Registry.ENCHANTMENT.getId(enchantmentdata.enchantment);
                                this.worldClue[j1] = enchantmentdata.enchantmentLevel;
                            }
                        }
                    }


                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);
                    org.bukkit.enchantments.EnchantmentOffer[] offers = new EnchantmentOffer[3];
                    for (int j = 0; j < 3; ++j) {
                        org.bukkit.enchantments.Enchantment enchantment = (this.enchantClue[j] >= 0) ? org.bukkit.enchantments.Enchantment.getByKey(CraftNamespacedKey.fromMinecraft(ForgeRegistries.ENCHANTMENTS.getKey(Registry.ENCHANTMENT.getByValue(this.enchantClue[j])))) : null;
                        offers[j] = (enchantment != null) ? new EnchantmentOffer(enchantment, this.worldClue[j], this.enchantLevels[j]) : null;
                    }

                    PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(((ServerPlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), this.getBukkitView(), ((IWorldPosCallableBridge) this.worldPosCallable).bridge$getLocation().getBlock(), item, offers, (int) power);
                    event.setCancelled(!itemstack.isEnchantable());
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        for (int j = 0; j < 3; ++j) {
                            this.enchantLevels[j] = 0;
                            this.enchantClue[j] = -1;
                            this.worldClue[j] = -1;
                        }
                        return;
                    }

                    for (int j = 0; j < 3; j++) {
                        EnchantmentOffer offer = event.getOffers()[j];
                        if (offer != null) {
                            this.enchantLevels[j] = offer.getCost();
                            this.enchantClue[j] = Registry.ENCHANTMENT.getId(ForgeRegistries.ENCHANTMENTS.getValue(CraftNamespacedKey.toMinecraft(offer.getEnchantment().getKey())));
                            this.worldClue[j] = offer.getEnchantmentLevel();
                        } else {
                            this.enchantLevels[j] = 0;
                            this.enchantClue[j] = -1;
                            this.worldClue[j] = -1;
                        }
                    }

                    this.detectAndSendChanges();
                });
            } else {
                for (int i = 0; i < 3; ++i) {
                    this.enchantLevels[i] = 0;
                    this.enchantClue[i] = -1;
                    this.worldClue[i] = -1;
                }
            }
        }

    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean enchantItem(PlayerEntity playerIn, int id) {
        ItemStack itemstack = this.tableInventory.getStackInSlot(0);
        ItemStack itemstack1 = this.tableInventory.getStackInSlot(1);
        int i = id + 1;
        if ((itemstack1.isEmpty() || itemstack1.getCount() < i) && !playerIn.abilities.isCreativeMode) {
            return false;
        } else if (this.enchantLevels[id] <= 0 || itemstack.isEmpty() || (playerIn.experienceLevel < i || playerIn.experienceLevel < this.enchantLevels[id]) && !playerIn.abilities.isCreativeMode) {
            return false;
        } else {
            this.worldPosCallable.consume((p_217003_6_, p_217003_7_) -> {
                ItemStack itemstack2 = itemstack;
                List<EnchantmentData> list = this.getEnchantmentList(itemstack, id, this.enchantLevels[id]);
                if (true || !list.isEmpty()) {

                    //  playerIn.onEnchant(itemstack, i);
                    boolean flag = itemstack.getItem() == Items.BOOK;
                    Map<Enchantment, Integer> enchants = new java.util.HashMap<>();
                    for (EnchantmentData obj : list) {
                        enchants.put(org.bukkit.enchantments.Enchantment.getByKey(CraftNamespacedKey.fromMinecraft(ForgeRegistries.ENCHANTMENTS.getKey(obj.enchantment))), obj.enchantmentLevel);
                    }
                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack2);

                    EnchantItemEvent event = new EnchantItemEvent(((Player) ((PlayerEntityBridge) playerIn).bridge$getBukkitEntity()), this.getBukkitView(), ((IWorldPosCallableBridge) this.worldPosCallable).bridge$getLocation().getBlock(), item, this.enchantLevels[id], enchants, id);
                    Bukkit.getPluginManager().callEvent(event);

                    int level = event.getExpLevelCost();
                    if (event.isCancelled() || (level > playerIn.experienceLevel && !playerIn.abilities.isCreativeMode) || event.getEnchantsToAdd().isEmpty()) {
                        return;
                    }

                    if (flag) {
                        itemstack2 = new ItemStack(Items.ENCHANTED_BOOK);

                        CompoundNBT tag = itemstack2.getTag();
                        if (tag != null) {
                            itemstack2.setTag(tag.copy());
                        }

                        this.tableInventory.setInventorySlotContents(0, itemstack2);
                    }

                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
                        try {
                            if (flag) {
                                NamespacedKey enchantId = entry.getKey().getKey();
                                net.minecraft.enchantment.Enchantment nms = ForgeRegistries.ENCHANTMENTS.getValue(CraftNamespacedKey.toMinecraft(enchantId));
                                if (nms == null) {
                                    continue;
                                }

                                EnchantmentData weightedrandomenchant = new EnchantmentData(nms, entry.getValue());
                                EnchantedBookItem.addEnchantment(itemstack2, weightedrandomenchant);
                            } else {
                                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                            }
                        } catch (IllegalArgumentException e) {
                            /* Just swallow invalid enchantments */
                        }
                    }
                    playerIn.onEnchant(itemstack, i);

                    if (!playerIn.abilities.isCreativeMode) {
                        itemstack1.shrink(i);
                        if (itemstack1.isEmpty()) {
                            this.tableInventory.setInventorySlotContents(1, ItemStack.EMPTY);
                        }
                    }

                    playerIn.addStat(Stats.ENCHANT_ITEM);
                    if (playerIn instanceof ServerPlayerEntity) {
                        CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayerEntity) playerIn, itemstack2, i);
                    }

                    this.tableInventory.markDirty();
                    this.xpSeed.set(playerIn.getXPSeed());
                    this.onCraftMatrixChanged(this.tableInventory);
                    p_217003_6_.playSound(null, p_217003_7_, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, p_217003_6_.rand.nextFloat() * 0.1F + 0.9F);
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

        CraftInventoryEnchanting inventory = new CraftInventoryEnchanting(this.tableInventory);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (Container) (Object) this);
        return bukkitEntity;
    }

    @Override
    public IWorldPosCallable bridge$getWorldPos() {
        return this.worldPosCallable;
    }
}
