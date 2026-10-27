package io.papermc.paper.entity.activation;

import net.minecraft.core.BlockPos;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.common.bridge.core.world.entity.ai.goal.GoalSelectorBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spigotmc.SpigotWorldConfig;
import java.util.List;
import java.util.Set;

public final class ActivationRange {

    private static int currentTick(Entity entity) {
        return ((MinecraftServerBridge) entity.level().getServer()).bridge$getCurrentTick();
    }

    private ActivationRange() {
    }

    static Activity[] VILLAGER_PANIC_IMMUNITIES = {
        Activity.HIDE,
        Activity.PRE_RAID,
        Activity.RAID,
        Activity.PANIC
    };

    private static int checkInactiveWakeup(final Entity entity) {
        final Level world = entity.level();
        final SpigotWorldConfig config = ((WorldBridge) world).bridge$spigotConfig();
        final long inactiveFor = currentTick(entity) - ((EntityBridge) entity).bridge$getActivatedTick();
        if (((EntityBridge) entity).bridge$getActivationType() == ActivationType.VILLAGER) {
            if (inactiveFor > config.wakeUpInactiveVillagersEvery && ((WorldBridge) world).bridge$getWakeupInactiveVillagers() > 0) {
                ((WorldBridge) world).bridge$setWakeupInactiveVillagers(((WorldBridge) world).bridge$getWakeupInactiveVillagers() - 1);
                return config.wakeUpInactiveVillagersFor;
            }
        } else if (((EntityBridge) entity).bridge$getActivationType() == ActivationType.ANIMAL) {
            if (inactiveFor > config.wakeUpInactiveAnimalsEvery && ((WorldBridge) world).bridge$getWakeupInactiveAnimals() > 0) {
                ((WorldBridge) world).bridge$setWakeupInactiveAnimals(((WorldBridge) world).bridge$getWakeupInactiveAnimals() - 1);
                return config.wakeUpInactiveAnimalsFor;
            }
        } else if (((EntityBridge) entity).bridge$getActivationType() == ActivationType.FLYING_MONSTER) {
            if (inactiveFor > config.wakeUpInactiveFlyingEvery && ((WorldBridge) world).bridge$getWakeupInactiveFlying() > 0) {
                ((WorldBridge) world).bridge$setWakeupInactiveFlying(((WorldBridge) world).bridge$getWakeupInactiveFlying() - 1);
                return config.wakeUpInactiveFlyingFor;
            }
        } else if (((EntityBridge) entity).bridge$getActivationType() == ActivationType.MONSTER || ((EntityBridge) entity).bridge$getActivationType() == ActivationType.RAIDER) {
            if (inactiveFor > config.wakeUpInactiveMonstersEvery && ((WorldBridge) world).bridge$getWakeupInactiveMonsters() > 0) {
                ((WorldBridge) world).bridge$setWakeupInactiveMonsters(((WorldBridge) world).bridge$getWakeupInactiveMonsters() - 1);
                return config.wakeUpInactiveMonstersFor;
            }
        }
        return -1;
    }

    static AABB maxBB = new AABB(0, 0, 0, 0, 0, 0);

    /**
     * These entities are excluded from Activation range checks.
     *
     * @param entity
     * @param config
     * @return boolean If it should always tick.
     */
    public static boolean initializeEntityActivationState(final Entity entity, final SpigotWorldConfig config) {
        return (((EntityBridge) entity).bridge$getActivationType() == ActivationType.MISC && config.miscActivationRange <= 0)
            || (((EntityBridge) entity).bridge$getActivationType() == ActivationType.RAIDER && config.raiderActivationRange <= 0)
            || (((EntityBridge) entity).bridge$getActivationType() == ActivationType.ANIMAL && config.animalActivationRange <= 0)
            || (((EntityBridge) entity).bridge$getActivationType() == ActivationType.MONSTER && config.monsterActivationRange <= 0)
            || (((EntityBridge) entity).bridge$getActivationType() == ActivationType.VILLAGER && config.villagerActivationRange <= 0)
            || (((EntityBridge) entity).bridge$getActivationType() == ActivationType.WATER && config.waterActivationRange <= 0)
            || (((EntityBridge) entity).bridge$getActivationType() == ActivationType.FLYING_MONSTER && config.flyingMonsterActivationRange <= 0)
            || entity instanceof EyeOfEnder
            || entity instanceof Player
            || entity instanceof ThrowableProjectile
            || entity instanceof EnderDragon
            || entity instanceof EnderDragonPart
            || entity instanceof WitherBoss
            || entity instanceof AbstractHurtingProjectile
            || entity instanceof LightningBolt
            || entity instanceof PrimedTnt
            || entity instanceof FallingBlockEntity
            || entity instanceof AbstractMinecart
            || entity instanceof AbstractBoat
            || entity instanceof EndCrystal
            || entity instanceof FireworkRocketEntity
            || entity instanceof ThrownTrident;
    }

    /**
     * Find what entities are in range of the players in the world and set
     * active if in range.
     *
     * @param world
     */
    public static void activateEntities(final Level world) {
        final int miscActivationRange = ((WorldBridge) world).bridge$spigotConfig().miscActivationRange;
        final int raiderActivationRange = ((WorldBridge) world).bridge$spigotConfig().raiderActivationRange;
        final int animalActivationRange = ((WorldBridge) world).bridge$spigotConfig().animalActivationRange;
        final int monsterActivationRange = ((WorldBridge) world).bridge$spigotConfig().monsterActivationRange;
        final int waterActivationRange = ((WorldBridge) world).bridge$spigotConfig().waterActivationRange;
        final int flyingActivationRange = ((WorldBridge) world).bridge$spigotConfig().flyingMonsterActivationRange;
        final int villagerActivationRange = ((WorldBridge) world).bridge$spigotConfig().villagerActivationRange;
        ((WorldBridge) world).bridge$setWakeupInactiveAnimals(Math.min(((WorldBridge) world).bridge$getWakeupInactiveAnimals() + 1, ((WorldBridge) world).bridge$spigotConfig().wakeUpInactiveAnimals));
        ((WorldBridge) world).bridge$setWakeupInactiveVillagers(Math.min(((WorldBridge) world).bridge$getWakeupInactiveVillagers() + 1, ((WorldBridge) world).bridge$spigotConfig().wakeUpInactiveVillagers));
        ((WorldBridge) world).bridge$setWakeupInactiveMonsters(Math.min(((WorldBridge) world).bridge$getWakeupInactiveMonsters() + 1, ((WorldBridge) world).bridge$spigotConfig().wakeUpInactiveMonsters));
        ((WorldBridge) world).bridge$setWakeupInactiveFlying(Math.min(((WorldBridge) world).bridge$getWakeupInactiveFlying() + 1, ((WorldBridge) world).bridge$spigotConfig().wakeUpInactiveFlying));

        int maxRange = Math.max(monsterActivationRange, animalActivationRange);
        maxRange = Math.max(maxRange, raiderActivationRange);
        maxRange = Math.max(maxRange, miscActivationRange);
        maxRange = Math.max(maxRange, flyingActivationRange);
        maxRange = Math.max(maxRange, waterActivationRange);
        maxRange = Math.max(maxRange, villagerActivationRange);
        maxRange = Math.min((((WorldBridge) world).bridge$spigotConfig().simulationDistance << 4) - 8, maxRange);

        for (final Player player : world.players()) {
            ((EntityBridge) player).bridge$setActivatedTick(((MinecraftServerBridge) world.getServer()).bridge$getCurrentTick());
            if (((WorldBridge) world).bridge$spigotConfig().ignoreSpectatorActivation && player.isSpectator()) {
                continue;
            }

            final int worldHeight = world.getHeight();
            ActivationRange.maxBB = player.getBoundingBox().inflate(maxRange, worldHeight, maxRange);
            ActivationType.MISC.boundingBox = player.getBoundingBox().inflate(miscActivationRange, worldHeight, miscActivationRange);
            ActivationType.RAIDER.boundingBox = player.getBoundingBox().inflate(raiderActivationRange, worldHeight, raiderActivationRange);
            ActivationType.ANIMAL.boundingBox = player.getBoundingBox().inflate(animalActivationRange, worldHeight, animalActivationRange);
            ActivationType.MONSTER.boundingBox = player.getBoundingBox().inflate(monsterActivationRange, worldHeight, monsterActivationRange);
            ActivationType.WATER.boundingBox = player.getBoundingBox().inflate(waterActivationRange, worldHeight, waterActivationRange);
            ActivationType.FLYING_MONSTER.boundingBox = player.getBoundingBox().inflate(flyingActivationRange, worldHeight, flyingActivationRange);
            ActivationType.VILLAGER.boundingBox = player.getBoundingBox().inflate(villagerActivationRange, worldHeight, villagerActivationRange);

            final List<Entity> entities = world.getEntities((Entity) null, ActivationRange.maxBB, e -> true);
            final boolean tickMarkers = world.paperConfig().entities.markers.tick;
            for (final Entity entity : entities) {
                if (!tickMarkers && entity instanceof Marker) {
                    continue;
                }

                ActivationRange.activateEntity(entity);
            }
        }
    }

    /**
     * Tries to activate an entity.
     *
     * @param entity
     */
    private static void activateEntity(final Entity entity) {
        if (currentTick(entity) > ((EntityBridge) entity).bridge$getActivatedTick()) {
            if (((EntityBridge) entity).bridge$getDefaultActivationState()) {
                ((EntityBridge) entity).bridge$setActivatedTick(currentTick(entity));
                return;
            }
            if (((EntityBridge) entity).bridge$getActivationType().boundingBox.intersects(entity.getBoundingBox())) {
                ((EntityBridge) entity).bridge$setActivatedTick(currentTick(entity));
            }
        }
    }

    /**
     * If an entity is not in range, do some more checks to see if we should
     * give it a shot.
     *
     * @param entity
     * @return
     */
    public static int checkEntityImmunities(final Entity entity) { // return # of ticks to get immunity
        final SpigotWorldConfig config = ((WorldBridge) entity.level()).bridge$spigotConfig();
        final int inactiveWakeUpImmunity = checkInactiveWakeup(entity);
        if (inactiveWakeUpImmunity > -1) {
            return inactiveWakeUpImmunity;
        }
        if (entity.getRemainingFireTicks() > 0) {
            return 2;
        }
        if (((EntityBridge) entity).bridge$getActivatedImmunityTick() >= currentTick(entity)) {
            return 1;
        }
        final long inactiveFor = currentTick(entity) - ((EntityBridge) entity).bridge$getActivatedTick();
        if ((((EntityBridge) entity).bridge$getActivationType() != ActivationType.WATER && entity.isInWater() && entity.isPushedByFluid())) {
            return 100;
        }
        if (!entity.onGround() || entity.getDeltaMovement().horizontalDistanceSqr() > 9.999999747378752E-6D) {
            return 100;
        }
        if (!(entity instanceof final AbstractArrow arrow)) {
            if ((!entity.onGround() && !isEntityThatFlies(entity))) {
                return 10;
            }
        } else if (!arrow.isInGround()) {
            return 1;
        }
        // special cases.
        if (entity instanceof final LivingEntity living) {
            if (living.blockPosition().equals(living.getLastClimbablePos().orElse(null)) || living.jumping || living.hurtTime > 0 || !living.getActiveEffects().isEmpty() || living.isFreezing()) {
                return 1;
            }
            if (entity instanceof final Mob mob && mob.getTarget() != null) {
                return 20;
            }
            if (entity instanceof final Bee bee) {
                final BlockPos movingTarget = ((io.izzel.arclight.common.bridge.core.world.entity.PathfinderMobBridge) bee).bridge$getMovingTarget();
                if (bee.isAngry() ||
                    (bee.getHivePos() != null && bee.getHivePos().equals(movingTarget)) ||
                    (bee.getSavedFlowerPos() != null && bee.getSavedFlowerPos().equals(movingTarget))
                ) {
                    return 20;
                }
            }
            if (entity instanceof final Villager villager) {
                final Brain<Villager> behaviorController = villager.getBrain();

                if (config.villagersActiveForPanic) {
                    for (final Activity activity : VILLAGER_PANIC_IMMUNITIES) {
                        if (behaviorController.isActive(activity)) {
                            return 20 * 5;
                        }
                    }
                }

                if (config.villagersWorkImmunityAfter > 0 && inactiveFor >= config.villagersWorkImmunityAfter) {
                    if (behaviorController.isActive(Activity.WORK)) {
                        return config.villagersWorkImmunityFor;
                    }
                }
            }
            if (entity instanceof final Llama llama && llama.inCaravan()) {
                return 1;
            }
            if (entity instanceof final Animal animal) {
                if (animal.isBaby() || animal.isInLove()) {
                    return 5;
                }
                if (entity instanceof final Sheep sheep && sheep.isSheared()) {
                    return 1;
                }
            }
            if (entity instanceof final Creeper creeper && creeper.isIgnited()) { // isExplosive
                return 20;
            }
            if (entity instanceof final Mob mob && ((GoalSelectorBridge) ((MobBridge) mob).bridge$targetSelector()).bridge$hasTasks()) {
                return 0;
            }
            if (entity instanceof final Pillager pillager) {
                // TODO:?
            }
        }
        // SPIGOT-6644: Otherwise the target refresh tick will be missed
        if (entity instanceof ExperienceOrb) {
            return 20;
        }
        return -1;
    }

    /**
     * Checks if the entity is active for this tick.
     *
     * @param entity
     * @return
     */
    public static boolean checkIfActive(final Entity entity) {
        // Never safe to skip fireworks or item gravity
        if (entity instanceof FireworkRocketEntity || (entity instanceof ItemEntity && (entity.tickCount + entity.getId()) % 4 == 0)) { // Needed for item gravity, see ItemEntity tick
            return true;
        }
        // special case always immunities
        // immunize brand-new entities, dead entities, and portal scenarios
        if (((EntityBridge) entity).bridge$getDefaultActivationState() || entity.tickCount < 20 * 10 || !entity.isAlive() || (entity.portalProcess != null && !entity.portalProcess.hasExpired()) || entity.portalCooldown > 0) {
            return true;
        }
        // immunize leashed entities
        if (entity instanceof final Mob mob && mob.getLeashHolder() instanceof Player) {
            return true;
        }

        boolean isActive = ((EntityBridge) entity).bridge$getActivatedTick() >= currentTick(entity);
        ((EntityBridge) entity).bridge$setTemporarilyActive(false);

        // Should this entity tick?
        if (!isActive) {
            if ((currentTick(entity) - ((EntityBridge) entity).bridge$getActivatedTick() - 1) % 20 == 0) {
                // Check immunities every 20 ticks.
                final int immunity = checkEntityImmunities(entity);
                if (immunity >= 0) {
                    ((EntityBridge) entity).bridge$setActivatedTick(currentTick(entity) + immunity);
                } else {
                    ((EntityBridge) entity).bridge$setTemporarilyActive(true);
                }
                isActive = true;
            }
        }
        // removed the original's dumb tick skipping for active entities
        return isActive;
    }

    private static Set<EntityType<?>> ENTITIES_THAT_FLY = Set.of(
        EntityType.GHAST,
        EntityType.HAPPY_GHAST,
        EntityType.PHANTOM
    );

    private static boolean isEntityThatFlies(final Entity entity) {
        return ENTITIES_THAT_FLY.contains(entity.getType());
    }
}
