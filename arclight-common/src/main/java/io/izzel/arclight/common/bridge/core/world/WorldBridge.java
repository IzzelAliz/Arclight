package io.izzel.arclight.common.bridge.core.world;

import io.izzel.arclight.common.bridge.inject.InjectLevelBridge;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CapturedBlockState;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.generator.ChunkGenerator;
import org.spigotmc.SpigotWorldConfig;

import java.util.Map;

public interface WorldBridge extends IWorldWriterBridge, IWorldBridge, InjectLevelBridge {

    CraftServer bridge$getServer();

    CraftWorld bridge$getWorld();

    boolean bridge$isPvpMode();

    boolean bridge$isKeepSpawnInMemory();

    boolean bridge$isPopulating();

    void bridge$setPopulating(boolean populating);

    ChunkGenerator bridge$getGenerator();

    BlockEntity bridge$getTileEntity(BlockPos pos, boolean validate);

    SpigotWorldConfig bridge$spigotConfig();

    Object2LongOpenHashMap<SpawnCategory> bridge$ticksPerSpawnCategory();

    ResourceKey<LevelStem> bridge$getTypeKey();

    void bridge$setLastPhysicsProblem(BlockPos pos);

    boolean bridge$preventPoiUpdated();

    void bridge$preventPoiUpdated(boolean b);

    void bridge$forge$notifyAndUpdatePhysics(BlockPos pos, LevelChunk chunk, BlockState oldBlock, BlockState newBlock, int i, int j);

    default boolean bridge$forge$onBlockPlace(BlockPos pos, LivingEntity livingEntity, Direction direction) {
        return false;
    }

    default boolean bridge$forge$mobGriefing(Entity entity) {
        return ((Level) this).getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    default ItemStack bridge$forge$potionBrewMix(ItemStack a, ItemStack b) {
        return PotionBrewing.mix(a, b);
    }

    default void bridge$forge$onPotionBrewed(NonNullList<ItemStack> stacks) {}

    default boolean bridge$forge$restoringBlockSnapshots() {
        return false;
    }

    Map<BlockPos, CapturedBlockState> bridge$getCapturedBlockState();

    Map<BlockPos, BlockEntity> bridge$getCapturedBlockEntity();

    default void bridge$platform$startCaptureBlockBreak() {}

    default boolean bridge$isCapturingBlockBreak() {
        return false;
    }

    default void bridge$platform$endCaptureBlockBreak() {}
}
