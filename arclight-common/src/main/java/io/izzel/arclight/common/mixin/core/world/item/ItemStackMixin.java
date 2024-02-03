package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.item.ItemStackBridge;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin extends CapabilityProvider<ItemStack> implements ItemStackBridge {

    // @formatter:off
    @Shadow @Deprecated private Item item;
    @Shadow private int count;
    @Shadow(remap = false) private CompoundTag capNBT;
    @Mutable @Shadow(remap = false) @Final private net.minecraft.core.Holder.Reference<Item> delegate;
    // @formatter:on

    @Shadow
    public abstract boolean is(Item p_150931_);

    @Shadow
    public abstract Item getItem();

    @Shadow @Nullable private CompoundTag tag;

    protected ItemStackMixin(Class<ItemStack> baseClass) {
        super(baseClass);
    }

    @Override
    public CompoundTag bridge$getForgeCaps() {
        return this.serializeCaps();
    }

    @Override
    public void bridge$setForgeCaps(CompoundTag caps) {
        this.capNBT = caps;
        if (caps != null) {
            this.deserializeCaps(caps);
        }
    }

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

    @ModifyVariable(method = "hurt", index = 1, at = @At(value = "JUMP", opcode = Opcodes.IFGT, ordinal = 0))
    private int arclight$itemDamage(int i, int amount, RandomSource rand, ServerPlayer damager) {
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

    @Inject(method = "hurtAndBreak", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private <T extends LivingEntity> void arclight$itemBreak(int amount, T entityIn, Consumer<T> onBroken, CallbackInfo ci) {
        if (this.count == 1 && entityIn instanceof Player) {
            CraftEventFactory.callPlayerItemBreakEvent(((Player) entityIn), (ItemStack) (Object) this);
        }
    }

    @Deprecated
    public void setItem(Item item) {
        this.item = item;
        this.delegate = ForgeRegistries.ITEMS.getDelegateOrThrow(item);
    }

    @Unique
    private static boolean arclight$lenientItemMatch(Object a, Object b) {
        if (ArclightConfig.spec().getCompat().isLenientItemTagMatch()) {
            var tagA = (CompoundTag) a;
            var tagB = (CompoundTag) b;
            if (tagB != null) {
                var tmp = tagA;
                tagA = tagB;
                tagB = tmp;
            }
            return tagA == null || (tagA.isEmpty() ? (tagB == null || tagB.isEmpty()) : tagA.equals(tagB));
        } else {
            return Objects.equals(a, b);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static boolean tagMatches(ItemStack itemStack, ItemStack itemStack2) {
        return arclight$lenientItemMatch(itemStack.getTag(), itemStack2.getTag())
            && itemStack.areCapsCompatible(itemStack2);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private boolean matches(ItemStack other) {
        if (this.count != other.getCount()) {
            return false;
        } else if (!this.is(other.getItem())) {
            return false;
        } else return tagMatches((ItemStack) (Object) this, other);
    }
}
