package io.izzel.arclight.common.mixin.core.entity.monster.piglin;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.piglin.PiglinEntity;
import net.minecraft.entity.monster.piglin.PiglinTasks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PiglinTasks.class)
public abstract class PiglinTasksMixin {

    // @formatter:off
    @Shadow private static void func_234531_r_(PiglinEntity p_234531_0_) { }
    @Shadow private static ItemStack func_234465_a_(ItemEntity p_234465_0_) { return null; }
    @Shadow protected static boolean func_234480_a_(Item p_234480_0_) { return false; }
    @Shadow private static void func_241427_c_(PiglinEntity p_241427_0_, ItemStack p_241427_1_) { }
    @Shadow private static void func_234501_d_(LivingEntity p_234501_0_) { }
    @Shadow private static boolean func_234499_c_(Item p_234499_0_) { return false; }
    @Shadow private static boolean func_234538_z_(PiglinEntity p_234538_0_) { return false; }
    @Shadow private static void func_234536_x_(PiglinEntity p_234536_0_) { }
    @Shadow private static void func_234498_c_(PiglinEntity p_234498_0_, ItemStack p_234498_1_) { }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected static void func_234470_a_(PiglinEntity piglinEntity, ItemEntity itemEntity) {
        ItemStack itemstack;
        func_234531_r_(piglinEntity);
        if (itemEntity.getItem().getItem() == Items.GOLD_NUGGET && !CraftEventFactory.callEntityPickupItemEvent(piglinEntity, itemEntity, 0, false).isCancelled()) {
            piglinEntity.onItemPickup(itemEntity, itemEntity.getItem().getCount());
            itemstack = itemEntity.getItem();
            itemEntity.remove();
        } else if (!CraftEventFactory.callEntityPickupItemEvent(piglinEntity, itemEntity, itemEntity.getItem().getCount() - 1, false).isCancelled()) {
            piglinEntity.onItemPickup(itemEntity, 1);
            itemstack = func_234465_a_(itemEntity);
        } else {
            return;
        }
        Item item = itemstack.getItem();
        if (func_234480_a_(item)) {
            piglinEntity.getBrain().removeMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            func_241427_c_(piglinEntity, itemstack);
            func_234501_d_(piglinEntity);
        } else if (func_234499_c_(item) && !func_234538_z_(piglinEntity)) {
            func_234536_x_(piglinEntity);
        } else {
            ((MobEntityBridge) piglinEntity).bridge$captureItemDrop(itemEntity);
            boolean flag = piglinEntity.func_233665_g_(itemstack);
            if (!flag) {
                func_234498_c_(piglinEntity, itemstack);
            }
        }
    }
}
