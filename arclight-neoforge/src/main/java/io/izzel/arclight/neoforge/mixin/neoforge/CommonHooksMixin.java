package io.izzel.arclight.neoforge.mixin.neoforge;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.mod.server.event.EntityEventHandler;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.neoforge.mod.util.BlockDropsScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.block.BlockBreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import io.izzel.arclight.neoforge.mod.util.RecipeContentSubscriptions;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommonHooks.class)
public abstract class CommonHooksMixin {

    @Inject(method = "sendRecipes", remap = false, at = @At("RETURN"))
    private static void arclight$rememberRecipeContentTypes(ServerPlayer player, Set<RecipeType<?>> types,
                                                           RecipeMap recipes, CallbackInfo ci) {
        RecipeContentSubscriptions.capture(player, types);
    }


    private static final ThreadLocal<Deque<BlockDropsScope>> arclight$blockDropsScopes = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "onPlaceItemIntoWorld", remap = false, at = @At("HEAD"))
    private static void arclight$captureHand(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ArclightCaptures.capturePlaceEventHand(context.getHand());
    }

    @Inject(method = "onPlaceItemIntoWorld", remap = false, at = @At("RETURN"))
    private static void arclight$removeHand(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ArclightCaptures.getPlaceEventHand(InteractionHand.MAIN_HAND);
    }

    @Decorate(method = "onLivingDrops", remap = false, at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/entity/living/LivingDropsEvent;isCanceled()Z"))
    private static boolean arclight$monitorLivingDrops(LivingDropsEvent instance) throws Throwable {
        instance.setCanceled(EntityEventHandler.monitorLivingDrops(instance.getEntity(), instance.getSource(), (List<ItemEntity>) instance.getDrops(), instance.isCanceled()));
        return (boolean) DecorationOps.callsite().invoke(instance);
    }

    @WrapMethod(method = "handleBlockDrops", remap = false)
    private static void arclight$handleBlockDrops(ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, List<ItemEntity> drops, Entity breaker, ItemStack tool, Operation<Void> original) {
        Deque<BlockDropsScope> scopes = arclight$blockDropsScopes.get();
        BlockDropsScope scope = new BlockDropsScope(level, arclight$getBlockBreakContext(level, pos, breaker));
        scopes.push(scope);
        try {
            original.call(level, pos, state, blockEntity, drops, breaker, tool);
        } finally {
            if (scopes.peek() == scope) {
                scopes.pop();
            } else {
                scopes.removeFirstOccurrence(scope);
            }
            if (scopes.isEmpty()) {
                arclight$blockDropsScopes.remove();
            }
        }
    }

    @Decorate(method = "handleBlockDrops", remap = false, at = @At(value = "INVOKE", target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"))
    private static Event arclight$bridgeBlockDropsEvent(IEventBus bus, Event event) throws Throwable {
        BlockDropsScope scope = arclight$currentBlockDropsScope();
        if (scope != null && scope.context() != null && event instanceof BlockDropsEvent drops) {
            drops.setDroppedExperience(scope.context().getEvent().getExpToDrop());
        }
        Event result = (Event) DecorationOps.callsite().invoke(bus, event);
        if (scope != null && scope.context() != null && event instanceof BlockDropsEvent drops) {
            scope.capture(drops.getDrops());
        }
        return result;
    }

    @Decorate(method = "handleBlockDrops", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$captureBlockDrops(ServerLevel level, Entity entity) throws Throwable {
        BlockDropsScope scope = arclight$currentBlockDropsScope();
        if (scope != null && scope.context() != null && scope.level() == level && entity instanceof ItemEntity itemEntity && scope.consume(itemEntity)) {
            return scope.context().getBlockDrops().add(itemEntity);
        }
        return (boolean) DecorationOps.callsite().invoke(level, entity);
    }
    private static ArclightCaptures.BlockBreakEventContext arclight$getBlockBreakContext(ServerLevel level, BlockPos pos, Entity breaker) {
        if (!(breaker instanceof ServerPlayer player) || ArclightCaptures.blockBreakEventStack.empty()) {
            return null;
        }
        ArclightCaptures.BlockBreakEventContext context = ArclightCaptures.blockBreakEventStack.peek();
        BlockBreakEvent event = context.getEvent();
        if (event.getPlayer().getUniqueId().equals(player.getUUID())
            && ((CraftWorld) event.getBlock().getWorld()).getHandle() == level
            && event.getBlock().getX() == pos.getX()
            && event.getBlock().getY() == pos.getY()
            && event.getBlock().getZ() == pos.getZ()) {
            return context;
        }
        return null;
    }

    private static BlockDropsScope arclight$currentBlockDropsScope() {
        Deque<BlockDropsScope> scopes = arclight$blockDropsScopes.get();
        return scopes.isEmpty() ? null : scopes.peek();
    }

}
