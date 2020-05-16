package io.izzel.arclight.mod.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.profiler.IProfiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.NetworkTagManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameRules;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.bukkit.craftbukkit.v1_14_R1.util.BlockStateListPopulator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class ArclightBlockPopulator extends BlockStateListPopulator {

    public ArclightBlockPopulator(World world) {
        super(world);
    }

    public Biome getBiome(BlockPos pos) {return getWorld().getBiome(pos);}

    public Biome getBiomeBody(BlockPos pos) {return getWorld().getBiomeBody(pos);}

    public boolean isRemote() {return getWorld().isRemote();}

    @Nullable
    public MinecraftServer getServer() {return getWorld().getServer();}

    @OnlyIn(Dist.CLIENT)
    public void setInitialSpawnLocation() {getWorld().setInitialSpawnLocation();}

    public BlockState getGroundAboveSeaLevel(BlockPos pos) {return getWorld().getGroundAboveSeaLevel(pos);}

    public static boolean isValid(BlockPos pos) {return World.isValid(pos);}

    public static boolean isOutsideBuildHeight(BlockPos pos) {return World.isOutsideBuildHeight(pos);}

    public static boolean isYOutOfBounds(int y) {return World.isYOutOfBounds(y);}

    public Chunk getChunkAt(BlockPos pos) {return getWorld().getChunkAt(pos);}

    @Override
    public Chunk getChunk(int chunkX, int chunkZ) {return getWorld().getChunk(chunkX, chunkZ);}

    public IChunk getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull) {return getWorld().getChunk(x, z, requiredStatus, nonnull);}

    public void markAndNotifyBlock(BlockPos pos, @Nullable Chunk chunk, BlockState blockstate, BlockState newState, int flags) {getWorld().markAndNotifyBlock(pos, chunk, blockstate, newState, flags);}

    public void onBlockStateChange(BlockPos p_217393_1_, BlockState p_217393_2_, BlockState p_217393_3_) {getWorld().onBlockStateChange(p_217393_1_, p_217393_2_, p_217393_3_);}

    public boolean removeBlock(BlockPos pos, boolean isMoving) {return getWorld().removeBlock(pos, isMoving);}

    public boolean destroyBlock(BlockPos pos, boolean dropBlock) {return getWorld().destroyBlock(pos, dropBlock);}

    public boolean setBlockState(BlockPos pos, BlockState state) {return getWorld().setBlockState(pos, state);}

    public void notifyBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState, int flags) {getWorld().notifyBlockUpdate(pos, oldState, newState, flags);}

    public void notifyNeighbors(BlockPos pos, Block blockIn) {getWorld().notifyNeighbors(pos, blockIn);}

    public void func_225319_b(BlockPos p_225319_1_, BlockState p_225319_2_, BlockState p_225319_3_) {getWorld().func_225319_b(p_225319_1_, p_225319_2_, p_225319_3_);}

    public void notifyNeighborsOfStateChange(BlockPos pos, Block blockIn) {getWorld().notifyNeighborsOfStateChange(pos, blockIn);}

    public void notifyNeighborsOfStateExcept(BlockPos pos, Block blockType, Direction skipSide) {getWorld().notifyNeighborsOfStateExcept(pos, blockType, skipSide);}

    public void neighborChanged(BlockPos pos, Block blockIn, BlockPos fromPos) {getWorld().neighborChanged(pos, blockIn, fromPos);}

    public int getLightSubtracted(BlockPos pos, int amount) {return getWorld().getLightSubtracted(pos, amount);}

    public int getHeight(Heightmap.Type heightmapType, int x, int z) {return getWorld().getHeight(heightmapType, x, z);}

    public int getLightFor(LightType type, BlockPos pos) {return getWorld().getLightFor(type, pos);}

    public boolean isDaytime() {return getWorld().isDaytime();}

    public void playSound(@Nullable PlayerEntity player, BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume, float pitch) {getWorld().playSound(player, pos, soundIn, category, volume, pitch);}

    public void playSound(@Nullable PlayerEntity player, double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch) {getWorld().playSound(player, x, y, z, soundIn, category, volume, pitch);}

    public void playMovingSound(@Nullable PlayerEntity p_217384_1_, Entity p_217384_2_, SoundEvent p_217384_3_, SoundCategory p_217384_4_, float p_217384_5_, float p_217384_6_) {getWorld().playMovingSound(p_217384_1_, p_217384_2_, p_217384_3_, p_217384_4_, p_217384_5_, p_217384_6_);}

    public void playSound(double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch, boolean distanceDelay) {getWorld().playSound(x, y, z, soundIn, category, volume, pitch, distanceDelay);}

    public void addParticle(IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {getWorld().addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);}

    @OnlyIn(Dist.CLIENT)
    public void addParticle(IParticleData particleData, boolean forceAlwaysRender, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {getWorld().addParticle(particleData, forceAlwaysRender, x, y, z, xSpeed, ySpeed, zSpeed);}

    public void addOptionalParticle(IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {getWorld().addOptionalParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);}

    public void addOptionalParticle(IParticleData particleData, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {getWorld().addOptionalParticle(particleData, ignoreRange, x, y, z, xSpeed, ySpeed, zSpeed);}

    public float getSunBrightness(float partialTicks) {return getWorld().getSunBrightness(partialTicks);}

    public float getSunBrightnessBody(float partialTicks) {return getWorld().getSunBrightnessBody(partialTicks);}

    @OnlyIn(Dist.CLIENT)
    public Vec3d getSkyColor(BlockPos p_217382_1_, float p_217382_2_) {return getWorld().getSkyColor(p_217382_1_, p_217382_2_);}

    @OnlyIn(Dist.CLIENT)
    public Vec3d getSkyColorBody(BlockPos p_217382_1_, float p_217382_2_) {return getWorld().getSkyColorBody(p_217382_1_, p_217382_2_);}

    public float getCelestialAngleRadians(float partialTicks) {return getWorld().getCelestialAngleRadians(partialTicks);}

    @OnlyIn(Dist.CLIENT)
    public Vec3d getCloudColour(float partialTicks) {return getWorld().getCloudColour(partialTicks);}

    @OnlyIn(Dist.CLIENT)
    public Vec3d getCloudColorBody(float partialTicks) {return getWorld().getCloudColorBody(partialTicks);}

    @OnlyIn(Dist.CLIENT)
    public Vec3d getFogColor(float partialTicks) {return getWorld().getFogColor(partialTicks);}

    @OnlyIn(Dist.CLIENT)
    public float getStarBrightness(float partialTicks) {return getWorld().getStarBrightness(partialTicks);}

    @OnlyIn(Dist.CLIENT)
    public float getStarBrightnessBody(float partialTicks) {return getWorld().getStarBrightnessBody(partialTicks);}

    public boolean addTileEntity(TileEntity tile) {return getWorld().addTileEntity(tile);}

    public void addTileEntities(Collection<TileEntity> tileEntityCollection) {getWorld().addTileEntities(tileEntityCollection);}

    public void tickBlockEntities() {getWorld().tickBlockEntities();}

    public void guardEntityTick(Consumer<Entity> p_217390_1_, Entity p_217390_2_) {getWorld().guardEntityTick(p_217390_1_, p_217390_2_);}

    public boolean checkBlockCollision(AxisAlignedBB bb) {return getWorld().checkBlockCollision(bb);}

    public boolean isFlammableWithin(AxisAlignedBB bb) {return getWorld().isFlammableWithin(bb);}

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public BlockState findBlockstateInArea(AxisAlignedBB area, Block blockIn) {return getWorld().findBlockstateInArea(area, blockIn);}

    public boolean isMaterialInBB(AxisAlignedBB bb, Material materialIn) {return getWorld().isMaterialInBB(bb, materialIn);}

    public Explosion createExplosion(@Nullable Entity entityIn, double xIn, double yIn, double zIn, float explosionRadius, Explosion.Mode modeIn) {return getWorld().createExplosion(entityIn, xIn, yIn, zIn, explosionRadius, modeIn);}

    public Explosion createExplosion(@Nullable Entity entityIn, double xIn, double yIn, double zIn, float explosionRadius, boolean causesFire, Explosion.Mode modeIn) {return getWorld().createExplosion(entityIn, xIn, yIn, zIn, explosionRadius, causesFire, modeIn);}

    public Explosion createExplosion(@Nullable Entity entityIn, @Nullable DamageSource damageSourceIn, double xIn, double yIn, double zIn, float explosionRadius, boolean causesFire, Explosion.Mode modeIn) {return getWorld().createExplosion(entityIn, damageSourceIn, xIn, yIn, zIn, explosionRadius, causesFire, modeIn);}

    public boolean extinguishFire(@Nullable PlayerEntity player, BlockPos pos, Direction side) {return getWorld().extinguishFire(player, pos, side);}

    @OnlyIn(Dist.CLIENT)
    public String getProviderName() {return getWorld().getProviderName();}

    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {return getWorld().getTileEntity(pos);}

    public void setTileEntity(BlockPos pos, @Nullable TileEntity tileEntityIn) {getWorld().setTileEntity(pos, tileEntityIn);}

    public void removeTileEntity(BlockPos pos) {getWorld().removeTileEntity(pos);}

    public boolean isBlockPresent(BlockPos pos) {return getWorld().isBlockPresent(pos);}

    public boolean isTopSolid(BlockPos p_217400_1_, Entity p_217400_2_) {return getWorld().isTopSolid(p_217400_1_, p_217400_2_);}

    public void calculateInitialSkylight() {getWorld().calculateInitialSkylight();}

    public void setAllowedSpawnTypes(boolean hostile, boolean peaceful) {getWorld().setAllowedSpawnTypes(hostile, peaceful);}

    public void calculateInitialWeatherBody() {getWorld().calculateInitialWeatherBody();}

    public void close() throws IOException {getWorld().close();}

    @Override
    public ChunkStatus getChunkStatus() {return getWorld().getChunkStatus();}

    public List<Entity> getEntitiesInAABBexcluding(@Nullable Entity entityIn, AxisAlignedBB boundingBox, @Nullable Predicate<? super Entity> predicate) {return getWorld().getEntitiesInAABBexcluding(entityIn, boundingBox, predicate);}

    public List<Entity> getEntitiesWithinAABB(@Nullable EntityType<?> type, AxisAlignedBB boundingBox, Predicate<? super Entity> predicate) {return getWorld().getEntitiesWithinAABB(type, boundingBox, predicate);}

    public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> clazz, AxisAlignedBB aabb, @Nullable Predicate<? super T> filter) {return getWorld().getEntitiesWithinAABB(clazz, aabb, filter);}

    @Override
    public <T extends Entity> List<T> func_225316_b(Class<? extends T> p_225316_1_, AxisAlignedBB p_225316_2_, @Nullable Predicate<? super T> p_225316_3_) {return getWorld().func_225316_b(p_225316_1_, p_225316_2_, p_225316_3_);}

    @Nullable
    public Entity getEntityByID(int id) {return getWorld().getEntityByID(id);}

    public void markChunkDirty(BlockPos pos, TileEntity unusedTileEntity) {getWorld().markChunkDirty(pos, unusedTileEntity);}

    public int getSeaLevel() {return getWorld().getSeaLevel();}

    public WorldType getWorldType() {return getWorld().getWorldType();}

    public int getStrongPower(BlockPos pos) {return getWorld().getStrongPower(pos);}

    public boolean isSidePowered(BlockPos pos, Direction side) {return getWorld().isSidePowered(pos, side);}

    public int getRedstonePower(BlockPos pos, Direction facing) {return getWorld().getRedstonePower(pos, facing);}

    public boolean isBlockPowered(BlockPos pos) {return getWorld().isBlockPowered(pos);}

    public int getRedstonePowerFromNeighbors(BlockPos pos) {return getWorld().getRedstonePowerFromNeighbors(pos);}

    @OnlyIn(Dist.CLIENT)
    public void sendQuittingDisconnectingPacket() {getWorld().sendQuittingDisconnectingPacket();}

    public void setGameTime(long worldTime) {getWorld().setGameTime(worldTime);}

    public long getSeed() {return getWorld().getSeed();}

    public long getGameTime() {return getWorld().getGameTime();}

    public long getDayTime() {return getWorld().getDayTime();}

    public void setDayTime(long time) {getWorld().setDayTime(time);}

    @Override
    public BlockPos getSpawnPoint() {return getWorld().getSpawnPoint();}

    public void setSpawnPoint(BlockPos pos) {getWorld().setSpawnPoint(pos);}

    public boolean isBlockModifiable(PlayerEntity player, BlockPos pos) {return getWorld().isBlockModifiable(player, pos);}

    public boolean canMineBlockBody(PlayerEntity player, BlockPos pos) {return getWorld().canMineBlockBody(player, pos);}

    public void setEntityState(Entity entityIn, byte state) {getWorld().setEntityState(entityIn, state);}

    public AbstractChunkProvider getChunkProvider() {return getWorld().getChunkProvider();}

    public void addBlockEvent(BlockPos pos, Block blockIn, int eventID, int eventParam) {getWorld().addBlockEvent(pos, blockIn, eventID, eventParam);}

    public WorldInfo getWorldInfo() {return getWorld().getWorldInfo();}

    public GameRules getGameRules() {return getWorld().getGameRules();}

    public float getThunderStrength(float delta) {return getWorld().getThunderStrength(delta);}

    @OnlyIn(Dist.CLIENT)
    public void setThunderStrength(float strength) {getWorld().setThunderStrength(strength);}

    public float getRainStrength(float delta) {return getWorld().getRainStrength(delta);}

    @OnlyIn(Dist.CLIENT)
    public void setRainStrength(float strength) {getWorld().setRainStrength(strength);}

    public boolean isThundering() {return getWorld().isThundering();}

    public boolean isRaining() {return getWorld().isRaining();}

    public boolean isRainingAt(BlockPos position) {return getWorld().isRainingAt(position);}

    public boolean isBlockinHighHumidity(BlockPos pos) {return getWorld().isBlockinHighHumidity(pos);}

    @Nullable
    public MapData getMapData(String mapName) {return getWorld().getMapData(mapName);}

    public void registerMapData(MapData mapDataIn) {getWorld().registerMapData(mapDataIn);}

    public int getNextMapId() {return getWorld().getNextMapId();}

    public void playBroadcastSound(int id, BlockPos pos, int data) {getWorld().playBroadcastSound(id, pos, data);}

    public int getActualHeight() {return getWorld().getActualHeight();}

    public double getHorizon() {return getWorld().getHorizon();}

    public CrashReportCategory fillCrashReport(CrashReport report) {return getWorld().fillCrashReport(report);}

    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {getWorld().sendBlockBreakProgress(breakerId, pos, progress);}

    @OnlyIn(Dist.CLIENT)
    public void makeFireworks(double x, double y, double z, double motionX, double motionY, double motionZ, @Nullable CompoundNBT compound) {getWorld().makeFireworks(x, y, z, motionX, motionY, motionZ, compound);}

    public Scoreboard getScoreboard() {return getWorld().getScoreboard();}

    public void updateComparatorOutputLevel(BlockPos pos, Block blockIn) {getWorld().updateComparatorOutputLevel(pos, blockIn);}

    public DifficultyInstance getDifficultyForLocation(BlockPos pos) {return getWorld().getDifficultyForLocation(pos);}

    public int getSkylightSubtracted() {return getWorld().getSkylightSubtracted();}

    @OnlyIn(Dist.CLIENT)
    public int getLastLightningBolt() {return getWorld().getLastLightningBolt();}

    public void setLastLightningBolt(int lastLightningBoltIn) {getWorld().setLastLightningBolt(lastLightningBoltIn);}

    public WorldBorder getWorldBorder() {return getWorld().getWorldBorder();}

    public void sendPacketToServer(IPacket<?> packetIn) {getWorld().sendPacketToServer(packetIn);}

    @Nullable
    public BlockPos findNearestStructure(String name, BlockPos pos, int radius, boolean p_211157_4_) {return getWorld().findNearestStructure(name, pos, radius, p_211157_4_);}

    public Dimension getDimension() {return getWorld().getDimension();}

    public Random getRandom() {return getWorld().getRandom();}

    public boolean hasBlockState(BlockPos p_217375_1_, Predicate<BlockState> p_217375_2_) {return getWorld().hasBlockState(p_217375_1_, p_217375_2_);}

    public RecipeManager getRecipeManager() {return getWorld().getRecipeManager();}

    public NetworkTagManager getTags() {return getWorld().getTags();}

    public BlockPos getBlockRandomPos(int p_217383_1_, int p_217383_2_, int p_217383_3_, int p_217383_4_) {return getWorld().getBlockRandomPos(p_217383_1_, p_217383_2_, p_217383_3_, p_217383_4_);}

    public boolean isSaveDisabled() {return getWorld().isSaveDisabled();}

    public IProfiler getProfiler() {return getWorld().getProfiler();}

    public BlockPos getHeight(Heightmap.Type heightmapType, BlockPos pos) {return getWorld().getHeight(heightmapType, pos);}

    public double getMaxEntityRadius() {return getWorld().getMaxEntityRadius();}

    public double increaseMaxEntityRadius(double value) {return getWorld().increaseMaxEntityRadius(value);}

    public boolean areCapsCompatible(CapabilityProvider<World> other) {return getWorld().areCapsCompatible(other);}

    public boolean areCapsCompatible(@Nullable CapabilityDispatcher other) {return getWorld().areCapsCompatible(other);}

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {return getWorld().getCapability(cap, side);}

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {return getWorld().getCapability(cap);}

    @Override
    public boolean isSkyLightMax(BlockPos pos) {return getWorld().isSkyLightMax(pos);}

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getCombinedLight(BlockPos pos, int minLight) {return getWorld().getCombinedLight(pos, minLight);}

    @Override
    public int getLightValue(BlockPos pos) {return getWorld().getLightValue(pos);}

    @Override
    public int getMaxLightLevel() {return getWorld().getMaxLightLevel();}

    @Override
    public int getHeight() {return getWorld().getHeight();}

    @Override
    public BlockRayTraceResult rayTraceBlocks(RayTraceContext context) {return getWorld().rayTraceBlocks(context);}

    @Override
    @Nullable
    public BlockRayTraceResult rayTraceBlocks(Vec3d p_217296_1_, Vec3d p_217296_2_, BlockPos p_217296_3_, VoxelShape p_217296_4_, BlockState p_217296_5_) {return getWorld().rayTraceBlocks(p_217296_1_, p_217296_2_, p_217296_3_, p_217296_4_, p_217296_5_);}

    public static <T> T func_217300_a(RayTraceContext p_217300_0_, BiFunction<RayTraceContext, BlockPos, T> p_217300_1_, Function<RayTraceContext, T> p_217300_2_) {return IBlockReader.func_217300_a(p_217300_0_, p_217300_1_, p_217300_2_);}

    @Override
    public float getCurrentMoonPhaseFactor() {return getWorld().getCurrentMoonPhaseFactor();}

    @Override
    public float getCelestialAngle(float partialTicks) {return getWorld().getCelestialAngle(partialTicks);}

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getMoonPhase() {return getWorld().getMoonPhase();}

    @Override
    public Difficulty getDifficulty() {return getWorld().getDifficulty();}

    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {return getWorld().chunkExists(chunkX, chunkZ);}

    @Override
    public void playEvent(int type, BlockPos pos, int data) {getWorld().playEvent(type, pos, data);}

    @Override
    public Stream<VoxelShape> getEmptyCollisionShapes(@Nullable Entity entityIn, AxisAlignedBB aabb, Set<Entity> entitiesToIgnore) {return getWorld().getEmptyCollisionShapes(entityIn, aabb, entitiesToIgnore);}

    @Override
    public boolean checkNoEntityCollision(@Nullable Entity entityIn, VoxelShape shape) {return getWorld().checkNoEntityCollision(entityIn, shape);}

    @Override
    public List<Entity> getEntitiesWithinAABBExcludingEntity(@Nullable Entity entityIn, AxisAlignedBB bb) {return getWorld().getEntitiesWithinAABBExcludingEntity(entityIn, bb);}

    @Override
    public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> p_217357_1_, AxisAlignedBB p_217357_2_) {return getWorld().getEntitiesWithinAABB(p_217357_1_, p_217357_2_);}

    @Override
    public <T extends Entity> List<T> func_225317_b(Class<? extends T> p_225317_1_, AxisAlignedBB p_225317_2_) {return getWorld().func_225317_b(p_225317_1_, p_225317_2_);}

    @Override
    @Nullable
    public PlayerEntity getClosestPlayer(double x, double y, double z, double distance, @Nullable Predicate<Entity> predicate) {return getWorld().getClosestPlayer(x, y, z, distance, predicate);}

    @Override
    @Nullable
    public PlayerEntity getClosestPlayer(Entity entityIn, double distance) {return getWorld().getClosestPlayer(entityIn, distance);}

    @Override
    @Nullable
    public PlayerEntity getClosestPlayer(double x, double y, double z, double distance, boolean creativePlayers) {return getWorld().getClosestPlayer(x, y, z, distance, creativePlayers);}

    @Override
    @Nullable
    public PlayerEntity getClosestPlayer(double x, double y, double z) {return getWorld().getClosestPlayer(x, y, z);}

    @Override
    public boolean isPlayerWithin(double x, double y, double z, double distance) {return getWorld().isPlayerWithin(x, y, z, distance);}

    @Override
    @Nullable
    public PlayerEntity getClosestPlayer(EntityPredicate predicate, LivingEntity target) {return getWorld().getClosestPlayer(predicate, target);}

    @Override
    @Nullable
    public PlayerEntity getClosestPlayer(EntityPredicate predicate, LivingEntity target, double p_217372_3_, double p_217372_5_, double p_217372_7_) {return getWorld().getClosestPlayer(predicate, target, p_217372_3_, p_217372_5_, p_217372_7_);}

    @Override
    @Nullable
    public PlayerEntity getClosestPlayer(EntityPredicate predicate, double x, double y, double z) {return getWorld().getClosestPlayer(predicate, x, y, z);}

    @Override
    @Nullable
    public <T extends LivingEntity> T getClosestEntityWithinAABB(Class<? extends T> entityClazz, EntityPredicate p_217360_2_, @Nullable LivingEntity target, double x, double y, double z, AxisAlignedBB boundingBox) {return getWorld().getClosestEntityWithinAABB(entityClazz, p_217360_2_, target, x, y, z, boundingBox);}

    @Override
    @Nullable
    public <T extends LivingEntity> T func_225318_b(Class<? extends T> p_225318_1_, EntityPredicate p_225318_2_, @Nullable LivingEntity p_225318_3_, double p_225318_4_, double p_225318_6_, double p_225318_8_, AxisAlignedBB p_225318_10_) {return getWorld().func_225318_b(p_225318_1_, p_225318_2_, p_225318_3_, p_225318_4_, p_225318_6_, p_225318_8_, p_225318_10_);}

    @Override
    @Nullable
    public <T extends LivingEntity> T getClosestEntity(List<? extends T> entities, EntityPredicate predicate, @Nullable LivingEntity target, double x, double y, double z) {return getWorld().getClosestEntity(entities, predicate, target, x, y, z);}

    @Override
    public List<PlayerEntity> getTargettablePlayersWithinAABB(EntityPredicate predicate, LivingEntity target, AxisAlignedBB box) {return getWorld().getTargettablePlayersWithinAABB(predicate, target, box);}

    @Override
    public <T extends LivingEntity> List<T> getTargettableEntitiesWithinAABB(Class<? extends T> p_217374_1_, EntityPredicate p_217374_2_, LivingEntity p_217374_3_, AxisAlignedBB p_217374_4_) {return getWorld().getTargettableEntitiesWithinAABB(p_217374_1_, p_217374_2_, p_217374_3_, p_217374_4_);}

    @Override
    @Nullable
    public PlayerEntity getPlayerByUuid(UUID uniqueIdIn) {return getWorld().getPlayerByUuid(uniqueIdIn);}

    @Override
    public boolean isAirBlock(BlockPos pos) {return getWorld().isAirBlock(pos);}

    @Override
    public boolean canBlockSeeSky(BlockPos pos) {return getWorld().canBlockSeeSky(pos);}

    @Override
    public float getBrightness(BlockPos pos) {return getWorld().getBrightness(pos);}

    @Override
    public int getStrongPower(BlockPos pos, Direction direction) {return getWorld().getStrongPower(pos, direction);}

    @Override
    public IChunk getChunk(BlockPos pos) {return getWorld().getChunk(pos);}

    @Override
    public IChunk getChunk(int chunkX, int chunkZ, ChunkStatus requiredStatus) {return getWorld().getChunk(chunkX, chunkZ, requiredStatus);}

    @Override
    public boolean func_217350_a(BlockState blockStateIn, BlockPos pos, ISelectionContext selectionContext) {return getWorld().func_217350_a(blockStateIn, pos, selectionContext);}

    @Override
    public boolean checkNoEntityCollision(Entity entityIn) {return getWorld().checkNoEntityCollision(entityIn);}

    @Override
    public boolean areCollisionShapesEmpty(AxisAlignedBB aabb) {return getWorld().areCollisionShapesEmpty(aabb);}

    @Override
    public boolean areCollisionShapesEmpty(Entity entityIn) {return getWorld().areCollisionShapesEmpty(entityIn);}

    @Override
    public boolean isCollisionBoxesEmpty(Entity entityIn, AxisAlignedBB aabb) {return getWorld().isCollisionBoxesEmpty(entityIn, aabb);}

    @Override
    public boolean isCollisionBoxesEmpty(@Nullable Entity entityIn, AxisAlignedBB aabb, Set<Entity> entitiesToIgnore) {return getWorld().isCollisionBoxesEmpty(entityIn, aabb, entitiesToIgnore);}

    @Override
    public Stream<VoxelShape> getCollisionShapes(@Nullable Entity entityIn, AxisAlignedBB aabb, Set<Entity> entitiesToIgnore) {return getWorld().getCollisionShapes(entityIn, aabb, entitiesToIgnore);}

    @Override
    public Stream<VoxelShape> getCollisionShapes(@Nullable Entity entityIn, AxisAlignedBB aabb) {return getWorld().getCollisionShapes(entityIn, aabb);}

    @Override
    public boolean hasWater(BlockPos pos) {return getWorld().hasWater(pos);}

    @Override
    public boolean containsAnyLiquid(AxisAlignedBB bb) {return getWorld().containsAnyLiquid(bb);}

    @Override
    public int getLight(BlockPos pos) {return getWorld().getLight(pos);}

    @Override
    public int getNeighborAwareLightSubtracted(BlockPos pos, int amount) {return getWorld().getNeighborAwareLightSubtracted(pos, amount);}

    @Override
    @Deprecated
    public boolean isBlockLoaded(BlockPos pos) {return getWorld().isBlockLoaded(pos);}

    @Override
    public boolean isAreaLoaded(BlockPos center, int range) {return getWorld().isAreaLoaded(center, range);}

    @Override
    @Deprecated
    public boolean isAreaLoaded(BlockPos from, BlockPos to) {return getWorld().isAreaLoaded(from, to);}

    @Override
    @Deprecated
    public boolean isAreaLoaded(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {return getWorld().isAreaLoaded(fromX, fromY, fromZ, toX, toY, toZ);}

    @Override
    public int getMaxHeight() {return getWorld().getMaxHeight();}

    @Override
    public boolean addEntity(Entity entityIn) {return getWorld().addEntity(entityIn);}
}
