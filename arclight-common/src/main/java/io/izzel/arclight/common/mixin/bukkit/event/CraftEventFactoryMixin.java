package io.izzel.arclight.common.mixin.bukkit.event;

import com.google.common.base.Function;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.block.CraftSign;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v.damage.CraftDamageSource;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerSignOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Mixin(value = CraftEventFactory.class, remap = false)
public abstract class CraftEventFactoryMixin {

    // @formatter:off
    @Shadow private static EntityDamageEvent callEntityDamageEvent(Entity damager, Entity damagee, EntityDamageEvent.DamageCause cause, org.bukkit.damage.DamageSource bukkitDamageSource, Map<EntityDamageEvent.DamageModifier, Double> modifiers, Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> modifierFunctions, boolean cancelled) { return null; }
    // @formatter:on

    @ModifyVariable(method = "handleEntityDamageEvent*", at = @At("HEAD"), index = 1, argsOnly = true)
    private static DamageSource arclight$captureSource(DamageSource source, Entity entity) {
        Entity damageEventEntity = ArclightCaptures.getDamageEventEntity();
        BlockPos damageEventBlock = ArclightCaptures.getDamageEventBlock();
        if (damageEventEntity != null && ((DamageSourceBridge) source).bridge$getCausingEntity() == null) {
            if (source.is(DamageTypes.LIGHTNING_BOLT)) {
                source = ((DamageSourceBridge) source).bridge$customCausingEntity(damageEventEntity);
            }
        }
        if (damageEventBlock != null && ((DamageSourceBridge) source).bridge$directBlock() == null) {
            if (source.is(DamageTypes.CACTUS)
                    || source.is(DamageTypes.SWEET_BERRY_BUSH)
                    || source.is(DamageTypes.HOT_FLOOR)) {
                source = ((DamageSourceBridge) source).bridge$directBlock(CraftBlock.at(entity.getCommandSenderWorld(), damageEventBlock));
            }
        }
        return source;
    }

    @Inject(method = "handleEntityDamageEvent*", cancellable = true, at = @At(value = "NEW", target = "java/lang/IllegalStateException"))
    private static void arclight$unhandledDamage(Entity entity, DamageSource source, Map<EntityDamageEvent.DamageModifier, Double> modifiers, Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> modifierFunctions, boolean cancelled, CallbackInfoReturnable<EntityDamageEvent> cir) {
        // todo blockDamage is lost
        CraftDamageSource bukkitDamageSource = new CraftDamageSource(source);
        EntityDamageEvent event = callEntityDamageEvent(((DamageSourceBridge) source).bridge$getCausingEntity(), entity, EntityDamageEvent.DamageCause.CUSTOM, bukkitDamageSource, modifiers, modifierFunctions, cancelled);
        cir.setReturnValue(event);
    }

    @Decorate(method = "callPlayerInteractEvent*", at = @At(value = "INVOKE", target = "Lorg/bukkit/plugin/PluginManager;callEvent(Lorg/bukkit/event/Event;)V"))
    private static void arclight$cancelPlayerInteractIfNecessary(PluginManager instance, Event event) throws Throwable {
        if (ArclightCaptures.shouldCancelPlayerInteract()) {
            ((Cancellable) event).setCancelled(true);
        }
        DecorationOps.callsite().invoke(instance, event);
    }

    /**
     * @author InitAuther97
     * @reason
     */
    @Overwrite
    public static EntityDeathEvent callEntityDeathEvent(net.minecraft.world.entity.LivingEntity victim, DamageSource damageSource, List<ItemStack> drops) {
        LivingEntityBridge living = (LivingEntityBridge) victim;
        CraftLivingEntity craft = living.bridge$getBukkitEntity();
        EntityDeathEvent event = ArclightEventFactory.callEntityDeathEvent(victim, damageSource, drops);

        CraftWorld world = (CraftWorld) craft.getWorld();
        living.bridge$setExpToDrop(event.getDroppedExp());
        for(org.bukkit.inventory.ItemStack stack : event.getDrops()) {
            if (stack != null && stack.getType() != Material.AIR && stack.getAmount() != 0) {
                world.dropItem(craft.getLocation(), stack);
            }
        }
        return event;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static boolean handleBlockSpreadEvent(LevelAccessor world, BlockPos source, BlockPos target, net.minecraft.world.level.block.state.BlockState block, int flag) {
        // Suppress during worldgen
        if (!(world instanceof Level) || !DistValidate.isValid(world)) {
            world.setBlock(target, block, flag);
            return true;
        }

        CraftBlockState state = CraftBlockStates.getBlockState(world, target, flag);
        state.setData(block);

        BlockSpreadEvent event = new BlockSpreadEvent(state.getBlock(), CraftBlock.at(world, source), state);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            state.update(true);
        }
        return !event.isCancelled();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static boolean handleBlockGrowEvent(Level world, BlockPos pos, net.minecraft.world.level.block.state.BlockState newData, int flag) {
        // Suppress during worldgen
        if (!DistValidate.isValid(world)) {
            world.setBlock(pos, newData, flag);
            return true;
        }
        Block block = world.bridge$getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
        CraftBlockState state = (CraftBlockState) block.getState();
        state.setData(newData);

        BlockGrowEvent event = new BlockGrowEvent(block, state);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            state.update(true);
        }

        return !event.isCancelled();
    }

    /**
     * @author IzzelAliz, InitAuther97
     * @reason IzzelAliz: suppress during world generation; InitAuther97: use extracted logic
     */
    @Overwrite
    public static boolean handleBlockFormEvent(Level world, BlockPos pos, net.minecraft.world.level.block.state.BlockState block, int flag, @Nullable Entity entity) {
        // Suppressed in callBlockFormEvent
        final var event = ArclightEventFactory.callBlockFormEvent(world, pos, block, flag, entity);
        if (event == null) {
            world.setBlock(pos, block, flag);
            return true;
        }

        if (!event.isCancelled()) {
            event.getNewState().update(true);
        }

        return !event.isCancelled();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static BlockFadeEvent callBlockFadeEvent(LevelAccessor world, BlockPos pos, net.minecraft.world.level.block.state.BlockState newBlock) {
        // Suppress during worldgen
        if (!(world instanceof Level) || !DistValidate.isValid(world)) {
            return new BlockFadeEvent(CraftBlock.at(world, pos), null);
        }
        CraftBlockState state = CraftBlockStates.getBlockState(world, pos);
        state.setData(newBlock);

        BlockFadeEvent event = new BlockFadeEvent(state.getBlock(), state);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static BlockPhysicsEvent callBlockPhysicsEvent(LevelAccessor world, BlockPos blockposition) {
        org.bukkit.block.Block block = CraftBlock.at(world, blockposition);
        BlockPhysicsEvent event = new BlockPhysicsEvent(block, block.getBlockData());
        // Suppress during worldgen
        if (world instanceof Level && DistValidate.isValid(world)) {
            Bukkit.getPluginManager().callEvent(event);
        }
        return event;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static boolean callEntityChangeBlockEvent(Entity entity, BlockPos position, net.minecraft.world.level.block.state.BlockState newBlock, boolean cancelled) {
        Block block = CraftBlock.at(entity.level(), position);
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(entity.bridge$getBukkitEntity(), block, CraftBlockData.fromData(newBlock));
        event.setCancelled(cancelled);
        // Suppress during worldgen
        if (DistValidate.isValid(entity.level())) {
            Bukkit.getPluginManager().callEvent(event);
        }
        return !event.isCancelled();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static BlockRedstoneEvent callRedstoneChange(Level world, BlockPos pos, int oldCurrent, int newCurrent) {
        BlockRedstoneEvent event = new BlockRedstoneEvent(CraftBlock.at(world, pos), oldCurrent, newCurrent);
        // Suppress during worldgen
        if (DistValidate.isValid(world)) {
            Bukkit.getPluginManager().callEvent(event);
        }
        return event;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static NotePlayEvent callNotePlayEvent(Level world, BlockPos pos, NoteBlockInstrument instrument, int note) {
        NotePlayEvent event = new NotePlayEvent(CraftBlock.at(world, pos), org.bukkit.Instrument.getByType((byte) instrument.ordinal()), new org.bukkit.Note(note));
        // Suppress during worldgen
        if (DistValidate.isValid(world)) {
            Bukkit.getPluginManager().callEvent(event);
        }
        return event;
    }

    @Inject(method = "callItemSpawnEvent", cancellable = true, at = @At("HEAD"))
    private static void arclight$noAirDrops(ItemEntity itemEntity, CallbackInfoReturnable<ItemSpawnEvent> cir) {
        if (itemEntity.getItem().isEmpty()) {
            Item entity = (Item) itemEntity.bridge$getBukkitEntity();
            ItemSpawnEvent event = new ItemSpawnEvent(entity);
            event.setCancelled(true);
            cir.setReturnValue(event);
        }
    }

    /**
     * @author sj-hub9796
     * @reason
     */
    @Overwrite
    public static boolean callPlayerSignOpenEvent(net.minecraft.world.entity.player.Player player, SignBlockEntity tileEntitySign, boolean front, PlayerSignOpenEvent.Cause cause) {
        Block block = CraftBlock.at(tileEntitySign.getLevel(), tileEntitySign.getBlockPos());
        Sign sign;
        if (CraftBlockStates.getBlockState(block) instanceof Sign sign1) {
            sign = sign1;
        } else {
            sign = new CraftSign<>(((WorldBridge) tileEntitySign.getLevel()).bridge$getWorld(), tileEntitySign);
        }
        Side side = front ? Side.FRONT : Side.BACK;
        return callPlayerSignOpenEvent(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity(), sign, side, cause);
    }

    /**
     * @author sj-hub9796
     * @reason
     */
    @Overwrite
    public static boolean callPlayerSignOpenEvent(Player player, Sign sign, Side side, PlayerSignOpenEvent.Cause cause) {
        PlayerSignOpenEvent event = new PlayerSignOpenEvent(player, sign, side, cause);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
