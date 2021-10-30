package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.base.Function;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(value = CraftEventFactory.class, remap = false)
public class CraftEventFactoryMixin {

    @Shadow public static Entity entityDamage;
    @Shadow public static Block blockDamage;

    @Inject(method = "handleEntityDamageEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Ljava/util/Map;Ljava/util/Map;Z)Lorg/bukkit/event/entity/EntityDamageEvent;", at = @At("HEAD"))
    private static void arclight$captureSource(Entity entity, DamageSource source, Map<EntityDamageEvent.DamageModifier, Double> modifiers, Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> modifierFunctions, boolean cancelled, CallbackInfoReturnable<EntityDamageEvent> cir) {
        Entity damageEventEntity = ArclightCaptures.getDamageEventEntity();
        BlockPos damageEventBlock = ArclightCaptures.getDamageEventBlock();
        if (damageEventEntity != null && entityDamage == null) {
            if (source.msgId.equals(DamageSource.LIGHTNING_BOLT.msgId)) {
                entityDamage = damageEventEntity;
            }
        }
        if (damageEventBlock != null && blockDamage == null) {
            if (source.msgId.equals(DamageSource.CACTUS.msgId)
                || source.msgId.equals(DamageSource.SWEET_BERRY_BUSH.msgId)
                || source.msgId.equals(DamageSource.HOT_FLOOR.msgId)) {
                blockDamage = CraftBlock.at(entity.getCommandSenderWorld(), damageEventBlock);
            }
        }
    }

    @Inject(method = "handleEntityDamageEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Ljava/util/Map;Ljava/util/Map;Z)Lorg/bukkit/event/entity/EntityDamageEvent;", cancellable = true, at = @At(value = "NEW", target = "java/lang/IllegalStateException"))
    private static void arclight$unhandledDamage(Entity entity, DamageSource source, Map<EntityDamageEvent.DamageModifier, Double> modifiers, Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> modifierFunctions, boolean cancelled, CallbackInfoReturnable<EntityDamageEvent> cir) {
        // todo blockDamage is lost
        EntityDamageEvent event;
        if (source.getEntity() != null) {
            ArclightMod.LOGGER.debug("Unhandled damage of {} by {} from {}", entity, source.getEntity(), source.msgId);
            event = new EntityDamageByEntityEvent(((EntityBridge) source.getEntity()).bridge$getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), EntityDamageEvent.DamageCause.CUSTOM, modifiers, modifierFunctions);
        } else {
            ArclightMod.LOGGER.debug("Unhandled damage of {} from {}", entity, source.msgId);
            event = new EntityDamageEvent(((EntityBridge) entity).bridge$getBukkitEntity(), EntityDamageEvent.DamageCause.CUSTOM, modifiers, modifierFunctions);
        }
        event.setCancelled(cancelled);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            ((EntityBridge) entity).bridge$getBukkitEntity().setLastDamageCause(event);
        }
        cir.setReturnValue(event);
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
        Block block = ((WorldBridge) world).bridge$getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
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
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static boolean handleBlockFormEvent(Level world, BlockPos pos, net.minecraft.world.level.block.state.BlockState block, int flag, @Nullable Entity entity) {
        // Suppress during worldgen
        if (!DistValidate.isValid(world)) {
            world.setBlock(pos, block, flag);
            return true;
        }
        CraftBlockState blockState = CraftBlockStates.getBlockState(world, pos, flag);
        blockState.setData(block);

        BlockFormEvent event = (entity == null) ? new BlockFormEvent(blockState.getBlock(), blockState) : new EntityBlockFormEvent(((EntityBridge) entity).bridge$getBukkitEntity(), blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            blockState.update(true);
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
            return new BlockFadeEvent(CraftBlock.at(world, pos), CraftBlockStates.getBlockState(CraftMagicNumbers.getMaterial(newBlock.getBlock()), null));
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
    public static EntityChangeBlockEvent callEntityChangeBlockEvent(Entity entity, BlockPos position, net.minecraft.world.level.block.state.BlockState newBlock, boolean cancelled) {
        Block block = CraftBlock.at(entity.level, position);
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(((EntityBridge) entity).bridge$getBukkitEntity(), block, CraftBlockData.fromData(newBlock));
        event.setCancelled(cancelled);
        // Suppress during worldgen
        if (DistValidate.isValid(entity.level)) {
            Bukkit.getPluginManager().callEvent(event);
        }
        return event;
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
}
