package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.item.ItemStackBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackBridge {

    // @formatter:off
    @Shadow @Deprecated private Item item;
    @Shadow private int count;
    // @formatter:on

    private static final Logger LOG = LogManager.getLogger("Arclight");

    public void convertStack(int version) {
        if (0 < version && version < CraftMagicNumbers.INSTANCE.getDataVersion()) {
            LOG.warn("Legacy ItemStack being used, updates will not applied: {}", this);
        }
    }

    @Override
    public void bridge$convertStack(int version) {
        this.convertStack(version);
    }

    @ModifyVariable(method = "attemptDamageItem", index = 1, name = "amount", at = @At(value = "JUMP", opcode = Opcodes.IFGT, ordinal = 0))
    private int arclight$itemDamage(int i, int amount, Random rand, ServerPlayerEntity damager) {
        if (damager != null) {
            PlayerItemDamageEvent event = new PlayerItemDamageEvent(((ServerPlayerEntityBridge) damager).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror((ItemStack) (Object) this), i);
            event.getPlayer().getServer().getPluginManager().callEvent(event);

            if (i != event.getDamage() || event.isCancelled()) {
                event.getPlayer().updateInventory();
            }
            if (event.isCancelled()) {
                return -1;
            }
            return event.getDamage();
        }
        return i;
    }

    @Inject(method = "damageItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;shrink(I)V"))
    private <T extends LivingEntity> void arclight$itemBreak(int amount, T entityIn, Consumer<T> onBroken, CallbackInfo ci) {
        if (this.count == 1 && entityIn instanceof PlayerEntity) {
            CraftEventFactory.callPlayerItemBreakEvent(((PlayerEntity) entityIn), (ItemStack) (Object) this);
        }
    }

    @Deprecated
    public void setItem(Item item) {
        this.item = item;
    }
}
