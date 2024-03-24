package io.izzel.arclight.common.mod.server.entity;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.entity.vehicle.MinecartSpawner;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftEntity;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class EntityClassLookup {

    public static void init() {
        var allEntityClasses = new HashSet<Class<?>>();
        for (var bukkitType : org.bukkit.entity.EntityType.values()) {
            Class<? extends org.bukkit.entity.Entity> entityClass = bukkitType.getEntityClass();
            if (entityClass != null && !allEntityClasses.contains(entityClass)) {
                var next = new LinkedList<Class<?>>();
                next.add(entityClass);
                while (!next.isEmpty()) {
                    Class<?> cl = next.pollFirst();
                    if (!allEntityClasses.contains(cl)) {
                        allEntityClasses.add(cl);
                        for (Class<?> intf : cl.getInterfaces()) {
                            if (org.bukkit.entity.Entity.class.isAssignableFrom(intf)) {
                                next.addLast(intf);
                            }
                        }
                    }
                }
            }
        }
        Set<Class<?>> ignored = Set.of(
            org.bukkit.entity.Explosive.class,
            org.bukkit.entity.Damageable.class,
            org.bukkit.entity.NPC.class,
            org.bukkit.entity.Boss.class,
            org.bukkit.entity.Breedable.class,
            org.bukkit.entity.Steerable.class,
            org.bukkit.entity.Enemy.class,
            org.bukkit.entity.ComplexLivingEntity.class,
            org.bukkit.entity.Vehicle.class
        );
        boolean error = false;
        for (Class<?> entityClass : allEntityClasses) {
            if (ignored.contains(entityClass)) continue;
            var optional = NMS_TO_BUKKIT.values().stream().filter(c -> c.bukkitClass == entityClass).findAny();
            if (optional.isEmpty()) {
                error = true;
                ArclightMod.LOGGER.error(entityClass + " has no valid entity class mapping");
            }
        }
        if (error) {
            throw new RuntimeException("Missing valid entity class mapping");
        }
    }

    private static final Map<Class<?>, EntityClass<?>> nmsClassMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Entity> BiFunction<CraftServer, T, org.bukkit.entity.Entity> getConvert(T entity) {
        return (BiFunction<CraftServer, T, org.bukkit.entity.Entity>) nmsClassMap.computeIfAbsent(entity.getClass(), k -> getEntityTypeData(k, entity.getType())).convert;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> EntityClass<T> getEntityTypeData(Class<?> type, EntityType<T> entityType) {
        EntityClass<?> entityClass = null;
        for (Class<?> c = type; entityClass == null; c = c.getSuperclass()) {
            entityClass = NMS_TO_BUKKIT.get(c);
        }
        return (EntityClass<T>) Objects.requireNonNull(entityClass, "entityClass");
    }

    private record EntityClass<T extends Entity>(Class<? extends org.bukkit.entity.Entity> bukkitClass,
                                                 Class<? extends CraftEntity> implClass,
                                                 BiFunction<CraftServer, T, org.bukkit.entity.Entity> convert) {
        private EntityClass {
            if (!bukkitClass.isAssignableFrom(implClass)) {
                throw new IllegalArgumentException(bukkitClass + " " + implClass);
            }
        }
    }

    private static final Map<Class<?>, EntityClass<?>> NMS_TO_BUKKIT = new HashMap<>();

    private static <U extends V, V extends Entity> void add(Class<? super U> cl, EntityClass<? super V> entityClass) {
        if (NMS_TO_BUKKIT.put(cl, entityClass) != null) {
            throw new IllegalStateException("Duplicate " + cl + " mapping");
        }
    }

    private static Class<? extends CraftEntity> forName(String name) {
        try {
            return Class.forName(CraftEntity.class.getPackageName() + "." + name).asSubclass(CraftEntity.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> BiFunction<CraftServer, T, org.bukkit.entity.Entity> convert(String name) {
        try {
            Class<? extends CraftEntity> cl = forName(name);
            for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 2) {
                    var pTypes = constructor.getParameterTypes();
                    if (pTypes[0].equals(CraftServer.class) && Entity.class.isAssignableFrom(pTypes[1])) {
                        constructor.setAccessible(true);
                        var lookup = Unsafe.lookup().in(constructor.getDeclaringClass());
                        return (BiFunction<CraftServer, T, org.bukkit.entity.Entity>) LambdaMetafactory.metafactory(
                            lookup, "apply",
                            MethodType.methodType(BiFunction.class),
                            MethodType.methodType(Object.class, Object.class, Object.class),
                            lookup.unreflectConstructor(constructor),
                            lookup.unreflectConstructor(constructor).type()
                        ).dynamicInvoker().invoke();
                    }
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("convert");
    }

    static {
        // abstract types
        add(Entity.class, new EntityClass<>(org.bukkit.entity.Entity.class, ArclightModEntity.class, ArclightModEntity::new));
        add(AbstractSkeleton.class, new EntityClass<>(org.bukkit.entity.AbstractSkeleton.class, ArclightModAbstractSkeleton.class, ArclightModAbstractSkeleton::new));
        add(Mob.class, new EntityClass<>(org.bukkit.entity.Mob.class, org.bukkit.craftbukkit.v.entity.CraftMob.class, ArclightModMob::new));
        add(AbstractMinecart.class, new EntityClass<>(org.bukkit.entity.Minecart.class, ArclightModMinecart.class, ArclightModMinecart::new));
        add(AbstractMinecartContainer.class, new EntityClass<>(org.bukkit.entity.Minecart.class, ArclightModMinecartContainer.class, ArclightModMinecartContainer::new));
        add(AbstractHorse.class, new EntityClass<>(org.bukkit.entity.AbstractHorse.class, ArclightModHorse.class, ArclightModHorse::new));
        add(AbstractChestedHorse.class, new EntityClass<>(org.bukkit.entity.ChestedHorse.class, ArclightModChestedHorse.class, ArclightModChestedHorse::new));
        add(Projectile.class, new EntityClass<>(org.bukkit.entity.Projectile.class, ArclightModProjectile.class, ArclightModProjectile::new));
        add(Raider.class, new EntityClass<>(org.bukkit.entity.Raider.class, ArclightModRaider.class, ArclightModRaider::new));
        add(LivingEntity.class, new EntityClass<>(org.bukkit.entity.LivingEntity.class, org.bukkit.craftbukkit.v.entity.CraftLivingEntity.class, org.bukkit.craftbukkit.v.entity.CraftLivingEntity::new));
        add(Monster.class, new EntityClass<>(org.bukkit.entity.Monster.class, org.bukkit.craftbukkit.v.entity.CraftMonster.class, org.bukkit.craftbukkit.v.entity.CraftMonster::new));
        add(PathfinderMob.class, new EntityClass<>(org.bukkit.entity.Creature.class, org.bukkit.craftbukkit.v.entity.CraftCreature.class, org.bukkit.craftbukkit.v.entity.CraftCreature::new));
        add(AgeableMob.class, new EntityClass<>(org.bukkit.entity.Ageable.class, org.bukkit.craftbukkit.v.entity.CraftAgeable.class, org.bukkit.craftbukkit.v.entity.CraftAgeable::new));
        add(AbstractVillager.class, new EntityClass<>(org.bukkit.entity.AbstractVillager.class, org.bukkit.craftbukkit.v.entity.CraftAbstractVillager.class, org.bukkit.craftbukkit.v.entity.CraftAbstractVillager::new));
        add(AbstractArrow.class, new EntityClass<>(org.bukkit.entity.AbstractArrow.class, org.bukkit.craftbukkit.v.entity.CraftArrow.class, org.bukkit.craftbukkit.v.entity.CraftArrow::new));
        add(Animal.class, new EntityClass<>(org.bukkit.entity.Animals.class, org.bukkit.craftbukkit.v.entity.CraftAnimals.class, org.bukkit.craftbukkit.v.entity.CraftAnimals::new));
        add(Fireball.class, new EntityClass<>(org.bukkit.entity.SizedFireball.class, org.bukkit.craftbukkit.v.entity.CraftSizedFireball.class, org.bukkit.craftbukkit.v.entity.CraftSizedFireball::new));
        add(AbstractHurtingProjectile.class, new EntityClass<>(org.bukkit.entity.Fireball.class, org.bukkit.craftbukkit.v.entity.CraftFireball.class, org.bukkit.craftbukkit.v.entity.CraftFireball::new));
        add(Display.class, new EntityClass<>(org.bukkit.entity.Display.class, org.bukkit.craftbukkit.v.entity.CraftDisplay.class, org.bukkit.craftbukkit.v.entity.CraftDisplay::new));
        add(AbstractIllager.class, new EntityClass<>(org.bukkit.entity.Illager.class, org.bukkit.craftbukkit.v.entity.CraftIllager.class, org.bukkit.craftbukkit.v.entity.CraftIllager::new));
        add(ThrowableItemProjectile.class, new EntityClass<>(org.bukkit.entity.ThrowableProjectile.class, ArclightModThrowableProjectile.class, ArclightModThrowableProjectile::new));
        add(HangingEntity.class, new EntityClass<>(org.bukkit.entity.Hanging.class, org.bukkit.craftbukkit.v.entity.CraftHanging.class, org.bukkit.craftbukkit.v.entity.CraftHanging::new));
        add(SpellcasterIllager.class, new EntityClass<>(org.bukkit.entity.Spellcaster.class, org.bukkit.craftbukkit.v.entity.CraftSpellcaster.class, org.bukkit.craftbukkit.v.entity.CraftSpellcaster::new));
        add(AmbientCreature.class, new EntityClass<>(org.bukkit.entity.Ambient.class, org.bukkit.craftbukkit.v.entity.CraftAmbient.class, org.bukkit.craftbukkit.v.entity.CraftAmbient::new));
        add(TamableAnimal.class, new EntityClass<>(org.bukkit.entity.Tameable.class, org.bukkit.craftbukkit.v.entity.CraftTameableAnimal.class, org.bukkit.craftbukkit.v.entity.CraftTameableAnimal::new));
        add(AbstractPiglin.class, new EntityClass<>(org.bukkit.entity.PiglinAbstract.class, org.bukkit.craftbukkit.v.entity.CraftPiglinAbstract.class, org.bukkit.craftbukkit.v.entity.CraftPiglinAbstract::new));
        add(FlyingMob.class, new EntityClass<>(org.bukkit.entity.Flying.class, org.bukkit.craftbukkit.v.entity.CraftFlying.class, org.bukkit.craftbukkit.v.entity.CraftFlying::new));
        add(WaterAnimal.class, new EntityClass<>(org.bukkit.entity.WaterMob.class, org.bukkit.craftbukkit.v.entity.CraftWaterMob.class, org.bukkit.craftbukkit.v.entity.CraftWaterMob::new));
        add(AbstractGolem.class, new EntityClass<>(org.bukkit.entity.Golem.class, org.bukkit.craftbukkit.v.entity.CraftGolem.class, org.bukkit.craftbukkit.v.entity.CraftGolem::new));
        add(Player.class, new EntityClass<>(org.bukkit.entity.HumanEntity.class, org.bukkit.craftbukkit.v.entity.CraftHumanEntity.class, org.bukkit.craftbukkit.v.entity.CraftHumanEntity::new));
        add(AbstractFish.class, new EntityClass<>(org.bukkit.entity.Fish.class, org.bukkit.craftbukkit.v.entity.CraftFish.class, org.bukkit.craftbukkit.v.entity.CraftFish::new));
        add(EnderDragonPart.class, new EntityClass<>(org.bukkit.entity.EnderDragonPart.class, org.bukkit.craftbukkit.v.entity.CraftEnderDragonPart.class, org.bukkit.craftbukkit.v.entity.CraftEnderDragonPart::new));

        // vanilla mob types
        add(ElderGuardian.class, new EntityClass<>(org.bukkit.entity.ElderGuardian.class, org.bukkit.craftbukkit.v.entity.CraftElderGuardian.class, org.bukkit.craftbukkit.v.entity.CraftElderGuardian::new));
        add(WitherSkeleton.class, new EntityClass<>(org.bukkit.entity.WitherSkeleton.class, org.bukkit.craftbukkit.v.entity.CraftWitherSkeleton.class, org.bukkit.craftbukkit.v.entity.CraftWitherSkeleton::new));
        add(Stray.class, new EntityClass<>(org.bukkit.entity.Stray.class, org.bukkit.craftbukkit.v.entity.CraftStray.class, org.bukkit.craftbukkit.v.entity.CraftStray::new));
        add(Husk.class, new EntityClass<>(org.bukkit.entity.Husk.class, org.bukkit.craftbukkit.v.entity.CraftHusk.class, org.bukkit.craftbukkit.v.entity.CraftHusk::new));
        add(ZombieVillager.class, new EntityClass<>(org.bukkit.entity.ZombieVillager.class, org.bukkit.craftbukkit.v.entity.CraftVillagerZombie.class, org.bukkit.craftbukkit.v.entity.CraftVillagerZombie::new));
        add(SkeletonHorse.class, new EntityClass<>(org.bukkit.entity.SkeletonHorse.class, org.bukkit.craftbukkit.v.entity.CraftSkeletonHorse.class, org.bukkit.craftbukkit.v.entity.CraftSkeletonHorse::new));
        add(ZombieHorse.class, new EntityClass<>(org.bukkit.entity.ZombieHorse.class, org.bukkit.craftbukkit.v.entity.CraftZombieHorse.class, org.bukkit.craftbukkit.v.entity.CraftZombieHorse::new));
        add(ArmorStand.class, new EntityClass<>(org.bukkit.entity.ArmorStand.class, org.bukkit.craftbukkit.v.entity.CraftArmorStand.class, org.bukkit.craftbukkit.v.entity.CraftArmorStand::new));
        add(Donkey.class, new EntityClass<>(org.bukkit.entity.Donkey.class, org.bukkit.craftbukkit.v.entity.CraftDonkey.class, org.bukkit.craftbukkit.v.entity.CraftDonkey::new));
        add(Mule.class, new EntityClass<>(org.bukkit.entity.Mule.class, org.bukkit.craftbukkit.v.entity.CraftMule.class, org.bukkit.craftbukkit.v.entity.CraftMule::new));
        add(Evoker.class, new EntityClass<>(org.bukkit.entity.Evoker.class, org.bukkit.craftbukkit.v.entity.CraftEvoker.class, org.bukkit.craftbukkit.v.entity.CraftEvoker::new));
        add(Vex.class, new EntityClass<>(org.bukkit.entity.Vex.class, org.bukkit.craftbukkit.v.entity.CraftVex.class, org.bukkit.craftbukkit.v.entity.CraftVex::new));
        add(Vindicator.class, new EntityClass<>(org.bukkit.entity.Vindicator.class, org.bukkit.craftbukkit.v.entity.CraftVindicator.class, org.bukkit.craftbukkit.v.entity.CraftVindicator::new));
        add(Illusioner.class, new EntityClass<>(org.bukkit.entity.Illusioner.class, org.bukkit.craftbukkit.v.entity.CraftIllusioner.class, org.bukkit.craftbukkit.v.entity.CraftIllusioner::new));
        add(Creeper.class, new EntityClass<>(org.bukkit.entity.Creeper.class, org.bukkit.craftbukkit.v.entity.CraftCreeper.class, org.bukkit.craftbukkit.v.entity.CraftCreeper::new));
        add(Skeleton.class, new EntityClass<>(org.bukkit.entity.Skeleton.class, org.bukkit.craftbukkit.v.entity.CraftSkeleton.class, org.bukkit.craftbukkit.v.entity.CraftSkeleton::new));
        add(Spider.class, new EntityClass<>(org.bukkit.entity.Spider.class, org.bukkit.craftbukkit.v.entity.CraftSpider.class, org.bukkit.craftbukkit.v.entity.CraftSpider::new));
        add(Giant.class, new EntityClass<>(org.bukkit.entity.Giant.class, org.bukkit.craftbukkit.v.entity.CraftGiant.class, org.bukkit.craftbukkit.v.entity.CraftGiant::new));
        add(Zombie.class, new EntityClass<>(org.bukkit.entity.Zombie.class, org.bukkit.craftbukkit.v.entity.CraftZombie.class, org.bukkit.craftbukkit.v.entity.CraftZombie::new));
        add(Slime.class, new EntityClass<>(org.bukkit.entity.Slime.class, org.bukkit.craftbukkit.v.entity.CraftSlime.class, org.bukkit.craftbukkit.v.entity.CraftSlime::new));
        add(Ghast.class, new EntityClass<>(org.bukkit.entity.Ghast.class, org.bukkit.craftbukkit.v.entity.CraftGhast.class, org.bukkit.craftbukkit.v.entity.CraftGhast::new));
        add(ZombifiedPiglin.class, new EntityClass<>(org.bukkit.entity.PigZombie.class, org.bukkit.craftbukkit.v.entity.CraftPigZombie.class, org.bukkit.craftbukkit.v.entity.CraftPigZombie::new));
        add(EnderMan.class, new EntityClass<>(org.bukkit.entity.Enderman.class, org.bukkit.craftbukkit.v.entity.CraftEnderman.class, org.bukkit.craftbukkit.v.entity.CraftEnderman::new));
        add(CaveSpider.class, new EntityClass<>(org.bukkit.entity.CaveSpider.class, org.bukkit.craftbukkit.v.entity.CraftCaveSpider.class, org.bukkit.craftbukkit.v.entity.CraftCaveSpider::new));
        add(Silverfish.class, new EntityClass<>(org.bukkit.entity.Silverfish.class, org.bukkit.craftbukkit.v.entity.CraftSilverfish.class, org.bukkit.craftbukkit.v.entity.CraftSilverfish::new));
        add(Blaze.class, new EntityClass<>(org.bukkit.entity.Blaze.class, org.bukkit.craftbukkit.v.entity.CraftBlaze.class, org.bukkit.craftbukkit.v.entity.CraftBlaze::new));
        add(MagmaCube.class, new EntityClass<>(org.bukkit.entity.MagmaCube.class, org.bukkit.craftbukkit.v.entity.CraftMagmaCube.class, org.bukkit.craftbukkit.v.entity.CraftMagmaCube::new));
        add(WitherBoss.class, new EntityClass<>(org.bukkit.entity.Wither.class, org.bukkit.craftbukkit.v.entity.CraftWither.class, org.bukkit.craftbukkit.v.entity.CraftWither::new));
        add(Bat.class, new EntityClass<>(org.bukkit.entity.Bat.class, org.bukkit.craftbukkit.v.entity.CraftBat.class, org.bukkit.craftbukkit.v.entity.CraftBat::new));
        add(Witch.class, new EntityClass<>(org.bukkit.entity.Witch.class, org.bukkit.craftbukkit.v.entity.CraftWitch.class, org.bukkit.craftbukkit.v.entity.CraftWitch::new));
        add(Endermite.class, new EntityClass<>(org.bukkit.entity.Endermite.class, org.bukkit.craftbukkit.v.entity.CraftEndermite.class, org.bukkit.craftbukkit.v.entity.CraftEndermite::new));
        add(Guardian.class, new EntityClass<>(org.bukkit.entity.Guardian.class, org.bukkit.craftbukkit.v.entity.CraftGuardian.class, org.bukkit.craftbukkit.v.entity.CraftGuardian::new));
        add(Shulker.class, new EntityClass<>(org.bukkit.entity.Shulker.class, org.bukkit.craftbukkit.v.entity.CraftShulker.class, org.bukkit.craftbukkit.v.entity.CraftShulker::new));
        add(Pig.class, new EntityClass<>(org.bukkit.entity.Pig.class, org.bukkit.craftbukkit.v.entity.CraftPig.class, org.bukkit.craftbukkit.v.entity.CraftPig::new));
        add(Sheep.class, new EntityClass<>(org.bukkit.entity.Sheep.class, org.bukkit.craftbukkit.v.entity.CraftSheep.class, org.bukkit.craftbukkit.v.entity.CraftSheep::new));
        add(Cow.class, new EntityClass<>(org.bukkit.entity.Cow.class, org.bukkit.craftbukkit.v.entity.CraftCow.class, org.bukkit.craftbukkit.v.entity.CraftCow::new));
        add(Chicken.class, new EntityClass<>(org.bukkit.entity.Chicken.class, org.bukkit.craftbukkit.v.entity.CraftChicken.class, org.bukkit.craftbukkit.v.entity.CraftChicken::new));
        add(Squid.class, new EntityClass<>(org.bukkit.entity.Squid.class, org.bukkit.craftbukkit.v.entity.CraftSquid.class, org.bukkit.craftbukkit.v.entity.CraftSquid::new));
        add(Wolf.class, new EntityClass<>(org.bukkit.entity.Wolf.class, org.bukkit.craftbukkit.v.entity.CraftWolf.class, org.bukkit.craftbukkit.v.entity.CraftWolf::new));
        add(MushroomCow.class, new EntityClass<>(org.bukkit.entity.MushroomCow.class, org.bukkit.craftbukkit.v.entity.CraftMushroomCow.class, org.bukkit.craftbukkit.v.entity.CraftMushroomCow::new));
        add(SnowGolem.class, new EntityClass<>(org.bukkit.entity.Snowman.class, org.bukkit.craftbukkit.v.entity.CraftSnowman.class, org.bukkit.craftbukkit.v.entity.CraftSnowman::new));
        add(Ocelot.class, new EntityClass<>(org.bukkit.entity.Ocelot.class, org.bukkit.craftbukkit.v.entity.CraftOcelot.class, org.bukkit.craftbukkit.v.entity.CraftOcelot::new));
        add(IronGolem.class, new EntityClass<>(org.bukkit.entity.IronGolem.class, org.bukkit.craftbukkit.v.entity.CraftIronGolem.class, org.bukkit.craftbukkit.v.entity.CraftIronGolem::new));
        add(Horse.class, new EntityClass<>(org.bukkit.entity.Horse.class, org.bukkit.craftbukkit.v.entity.CraftHorse.class, org.bukkit.craftbukkit.v.entity.CraftHorse::new));
        add(Rabbit.class, new EntityClass<>(org.bukkit.entity.Rabbit.class, org.bukkit.craftbukkit.v.entity.CraftRabbit.class, org.bukkit.craftbukkit.v.entity.CraftRabbit::new));
        add(PolarBear.class, new EntityClass<>(org.bukkit.entity.PolarBear.class, org.bukkit.craftbukkit.v.entity.CraftPolarBear.class, org.bukkit.craftbukkit.v.entity.CraftPolarBear::new));
        add(Llama.class, new EntityClass<>(org.bukkit.entity.Llama.class, org.bukkit.craftbukkit.v.entity.CraftLlama.class, org.bukkit.craftbukkit.v.entity.CraftLlama::new));
        add(Parrot.class, new EntityClass<>(org.bukkit.entity.Parrot.class, org.bukkit.craftbukkit.v.entity.CraftParrot.class, org.bukkit.craftbukkit.v.entity.CraftParrot::new));
        add(Villager.class, new EntityClass<>(org.bukkit.entity.Villager.class, org.bukkit.craftbukkit.v.entity.CraftVillager.class, org.bukkit.craftbukkit.v.entity.CraftVillager::new));
        add(Turtle.class, new EntityClass<>(org.bukkit.entity.Turtle.class, org.bukkit.craftbukkit.v.entity.CraftTurtle.class, org.bukkit.craftbukkit.v.entity.CraftTurtle::new));
        add(Phantom.class, new EntityClass<>(org.bukkit.entity.Phantom.class, org.bukkit.craftbukkit.v.entity.CraftPhantom.class, org.bukkit.craftbukkit.v.entity.CraftPhantom::new));
        add(Cod.class, new EntityClass<>(org.bukkit.entity.Cod.class, org.bukkit.craftbukkit.v.entity.CraftCod.class, org.bukkit.craftbukkit.v.entity.CraftCod::new));
        add(Salmon.class, new EntityClass<>(org.bukkit.entity.Salmon.class, org.bukkit.craftbukkit.v.entity.CraftSalmon.class, org.bukkit.craftbukkit.v.entity.CraftSalmon::new));
        add(Pufferfish.class, new EntityClass<>(org.bukkit.entity.PufferFish.class, org.bukkit.craftbukkit.v.entity.CraftPufferFish.class, org.bukkit.craftbukkit.v.entity.CraftPufferFish::new));
        add(TropicalFish.class, new EntityClass<>(org.bukkit.entity.TropicalFish.class, org.bukkit.craftbukkit.v.entity.CraftTropicalFish.class, org.bukkit.craftbukkit.v.entity.CraftTropicalFish::new));
        add(Drowned.class, new EntityClass<>(org.bukkit.entity.Drowned.class, org.bukkit.craftbukkit.v.entity.CraftDrowned.class, org.bukkit.craftbukkit.v.entity.CraftDrowned::new));
        add(Dolphin.class, new EntityClass<>(org.bukkit.entity.Dolphin.class, org.bukkit.craftbukkit.v.entity.CraftDolphin.class, org.bukkit.craftbukkit.v.entity.CraftDolphin::new));
        add(Cat.class, new EntityClass<>(org.bukkit.entity.Cat.class, org.bukkit.craftbukkit.v.entity.CraftCat.class, org.bukkit.craftbukkit.v.entity.CraftCat::new));
        add(Panda.class, new EntityClass<>(org.bukkit.entity.Panda.class, org.bukkit.craftbukkit.v.entity.CraftPanda.class, org.bukkit.craftbukkit.v.entity.CraftPanda::new));
        add(Pillager.class, new EntityClass<>(org.bukkit.entity.Pillager.class, org.bukkit.craftbukkit.v.entity.CraftPillager.class, org.bukkit.craftbukkit.v.entity.CraftPillager::new));
        add(Ravager.class, new EntityClass<>(org.bukkit.entity.Ravager.class, org.bukkit.craftbukkit.v.entity.CraftRavager.class, org.bukkit.craftbukkit.v.entity.CraftRavager::new));
        add(TraderLlama.class, new EntityClass<>(org.bukkit.entity.TraderLlama.class, org.bukkit.craftbukkit.v.entity.CraftTraderLlama.class, org.bukkit.craftbukkit.v.entity.CraftTraderLlama::new));
        add(WanderingTrader.class, new EntityClass<>(org.bukkit.entity.WanderingTrader.class, org.bukkit.craftbukkit.v.entity.CraftWanderingTrader.class, org.bukkit.craftbukkit.v.entity.CraftWanderingTrader::new));
        add(Fox.class, new EntityClass<>(org.bukkit.entity.Fox.class, org.bukkit.craftbukkit.v.entity.CraftFox.class, org.bukkit.craftbukkit.v.entity.CraftFox::new));
        add(Bee.class, new EntityClass<>(org.bukkit.entity.Bee.class, org.bukkit.craftbukkit.v.entity.CraftBee.class, org.bukkit.craftbukkit.v.entity.CraftBee::new));
        add(Hoglin.class, new EntityClass<>(org.bukkit.entity.Hoglin.class, org.bukkit.craftbukkit.v.entity.CraftHoglin.class, org.bukkit.craftbukkit.v.entity.CraftHoglin::new));
        add(Piglin.class, new EntityClass<>(org.bukkit.entity.Piglin.class, org.bukkit.craftbukkit.v.entity.CraftPiglin.class, org.bukkit.craftbukkit.v.entity.CraftPiglin::new));
        add(Strider.class, new EntityClass<>(org.bukkit.entity.Strider.class, org.bukkit.craftbukkit.v.entity.CraftStrider.class, org.bukkit.craftbukkit.v.entity.CraftStrider::new));
        add(Zoglin.class, new EntityClass<>(org.bukkit.entity.Zoglin.class, org.bukkit.craftbukkit.v.entity.CraftZoglin.class, org.bukkit.craftbukkit.v.entity.CraftZoglin::new));
        add(PiglinBrute.class, new EntityClass<>(org.bukkit.entity.PiglinBrute.class, org.bukkit.craftbukkit.v.entity.CraftPiglinBrute.class, org.bukkit.craftbukkit.v.entity.CraftPiglinBrute::new));
        add(Axolotl.class, new EntityClass<>(org.bukkit.entity.Axolotl.class, org.bukkit.craftbukkit.v.entity.CraftAxolotl.class, org.bukkit.craftbukkit.v.entity.CraftAxolotl::new));
        add(GlowSquid.class, new EntityClass<>(org.bukkit.entity.GlowSquid.class, org.bukkit.craftbukkit.v.entity.CraftGlowSquid.class, org.bukkit.craftbukkit.v.entity.CraftGlowSquid::new));
        add(Goat.class, new EntityClass<>(org.bukkit.entity.Goat.class, org.bukkit.craftbukkit.v.entity.CraftGoat.class, org.bukkit.craftbukkit.v.entity.CraftGoat::new));
        add(Allay.class, new EntityClass<>(org.bukkit.entity.Allay.class, org.bukkit.craftbukkit.v.entity.CraftAllay.class, org.bukkit.craftbukkit.v.entity.CraftAllay::new));
        add(Frog.class, new EntityClass<>(org.bukkit.entity.Frog.class, org.bukkit.craftbukkit.v.entity.CraftFrog.class, org.bukkit.craftbukkit.v.entity.CraftFrog::new));
        add(Tadpole.class, new EntityClass<>(org.bukkit.entity.Tadpole.class, org.bukkit.craftbukkit.v.entity.CraftTadpole.class, org.bukkit.craftbukkit.v.entity.CraftTadpole::new));
        add(Warden.class, new EntityClass<>(org.bukkit.entity.Warden.class, org.bukkit.craftbukkit.v.entity.CraftWarden.class, org.bukkit.craftbukkit.v.entity.CraftWarden::new));
        add(Camel.class, new EntityClass<>(org.bukkit.entity.Camel.class, org.bukkit.craftbukkit.v.entity.CraftCamel.class, org.bukkit.craftbukkit.v.entity.CraftCamel::new));
        add(Sniffer.class, new EntityClass<>(org.bukkit.entity.Sniffer.class, org.bukkit.craftbukkit.v.entity.CraftSniffer.class, org.bukkit.craftbukkit.v.entity.CraftSniffer::new));
        add(EnderDragon.class, new EntityClass<>(org.bukkit.entity.EnderDragon.class, org.bukkit.craftbukkit.v.entity.CraftEnderDragon.class, org.bukkit.craftbukkit.v.entity.CraftEnderDragon::new));
        add(LargeFireball.class, new EntityClass<>(org.bukkit.entity.LargeFireball.class, org.bukkit.craftbukkit.v.entity.CraftLargeFireball.class, org.bukkit.craftbukkit.v.entity.CraftLargeFireball::new));
        add(SmallFireball.class, new EntityClass<>(org.bukkit.entity.SmallFireball.class, org.bukkit.craftbukkit.v.entity.CraftSmallFireball.class, org.bukkit.craftbukkit.v.entity.CraftSmallFireball::new));
        add(WitherSkull.class, new EntityClass<>(org.bukkit.entity.WitherSkull.class, org.bukkit.craftbukkit.v.entity.CraftWitherSkull.class, org.bukkit.craftbukkit.v.entity.CraftWitherSkull::new));
        add(DragonFireball.class, new EntityClass<>(org.bukkit.entity.DragonFireball.class, org.bukkit.craftbukkit.v.entity.CraftDragonFireball.class, org.bukkit.craftbukkit.v.entity.CraftDragonFireball::new));
        add(Painting.class, new EntityClass<>(org.bukkit.entity.Painting.class, org.bukkit.craftbukkit.v.entity.CraftPainting.class, org.bukkit.craftbukkit.v.entity.CraftPainting::new));
        add(ItemFrame.class, new EntityClass<>(org.bukkit.entity.ItemFrame.class, org.bukkit.craftbukkit.v.entity.CraftItemFrame.class, org.bukkit.craftbukkit.v.entity.CraftItemFrame::new));
        add(GlowItemFrame.class, new EntityClass<>(org.bukkit.entity.GlowItemFrame.class, org.bukkit.craftbukkit.v.entity.CraftGlowItemFrame.class, org.bukkit.craftbukkit.v.entity.CraftGlowItemFrame::new));
        add(Arrow.class, new EntityClass<>(org.bukkit.entity.Arrow.class, org.bukkit.craftbukkit.v.entity.CraftTippedArrow.class, org.bukkit.craftbukkit.v.entity.CraftTippedArrow::new));
        add(ThrownEnderpearl.class, new EntityClass<>(org.bukkit.entity.EnderPearl.class, org.bukkit.craftbukkit.v.entity.CraftEnderPearl.class, org.bukkit.craftbukkit.v.entity.CraftEnderPearl::new));
        add(ThrownExperienceBottle.class, new EntityClass<>(org.bukkit.entity.ThrownExpBottle.class, org.bukkit.craftbukkit.v.entity.CraftThrownExpBottle.class, org.bukkit.craftbukkit.v.entity.CraftThrownExpBottle::new));
        add(SpectralArrow.class, new EntityClass<>(org.bukkit.entity.SpectralArrow.class, org.bukkit.craftbukkit.v.entity.CraftSpectralArrow.class, org.bukkit.craftbukkit.v.entity.CraftSpectralArrow::new));
        add(EndCrystal.class, new EntityClass<>(org.bukkit.entity.EnderCrystal.class, org.bukkit.craftbukkit.v.entity.CraftEnderCrystal.class, org.bukkit.craftbukkit.v.entity.CraftEnderCrystal::new));
        add(ThrownTrident.class, new EntityClass<>(org.bukkit.entity.Trident.class, org.bukkit.craftbukkit.v.entity.CraftTrident.class, org.bukkit.craftbukkit.v.entity.CraftTrident::new));
        add(LightningBolt.class, new EntityClass<>(org.bukkit.entity.LightningStrike.class, org.bukkit.craftbukkit.v.entity.CraftLightningStrike.class, org.bukkit.craftbukkit.v.entity.CraftLightningStrike::new));
        add(ShulkerBullet.class, new EntityClass<>(org.bukkit.entity.ShulkerBullet.class, org.bukkit.craftbukkit.v.entity.CraftShulkerBullet.class, org.bukkit.craftbukkit.v.entity.CraftShulkerBullet::new));
        add(Boat.class, new EntityClass<>(org.bukkit.entity.Boat.class, org.bukkit.craftbukkit.v.entity.CraftBoat.class, org.bukkit.craftbukkit.v.entity.CraftBoat::new));
        add(LlamaSpit.class, new EntityClass<>(org.bukkit.entity.LlamaSpit.class, org.bukkit.craftbukkit.v.entity.CraftLlamaSpit.class, org.bukkit.craftbukkit.v.entity.CraftLlamaSpit::new));
        add(ChestBoat.class, new EntityClass<>(org.bukkit.entity.ChestBoat.class, org.bukkit.craftbukkit.v.entity.CraftChestBoat.class, org.bukkit.craftbukkit.v.entity.CraftChestBoat::new));
        add(Marker.class, new EntityClass<>(org.bukkit.entity.Marker.class, org.bukkit.craftbukkit.v.entity.CraftMarker.class, org.bukkit.craftbukkit.v.entity.CraftMarker::new));
        add(Display.BlockDisplay.class, new EntityClass<>(org.bukkit.entity.BlockDisplay.class, org.bukkit.craftbukkit.v.entity.CraftBlockDisplay.class, org.bukkit.craftbukkit.v.entity.CraftBlockDisplay::new));
        add(Interaction.class, new EntityClass<>(org.bukkit.entity.Interaction.class, org.bukkit.craftbukkit.v.entity.CraftInteraction.class, org.bukkit.craftbukkit.v.entity.CraftInteraction::new));
        add(Display.ItemDisplay.class, new EntityClass<>(org.bukkit.entity.ItemDisplay.class, org.bukkit.craftbukkit.v.entity.CraftItemDisplay.class, org.bukkit.craftbukkit.v.entity.CraftItemDisplay::new));
        add(Display.TextDisplay.class, new EntityClass<>(org.bukkit.entity.TextDisplay.class, org.bukkit.craftbukkit.v.entity.CraftTextDisplay.class, org.bukkit.craftbukkit.v.entity.CraftTextDisplay::new));
        add(ItemEntity.class, new EntityClass<>(org.bukkit.entity.Item.class, org.bukkit.craftbukkit.v.entity.CraftItem.class, convert("CraftItem")));
        add(ExperienceOrb.class, new EntityClass<>(org.bukkit.entity.ExperienceOrb.class, org.bukkit.craftbukkit.v.entity.CraftExperienceOrb.class, org.bukkit.craftbukkit.v.entity.CraftExperienceOrb::new));
        add(AreaEffectCloud.class, new EntityClass<>(org.bukkit.entity.AreaEffectCloud.class, org.bukkit.craftbukkit.v.entity.CraftAreaEffectCloud.class, org.bukkit.craftbukkit.v.entity.CraftAreaEffectCloud::new));
        add(ThrownEgg.class, new EntityClass<>(org.bukkit.entity.Egg.class, org.bukkit.craftbukkit.v.entity.CraftEgg.class, org.bukkit.craftbukkit.v.entity.CraftEgg::new));
        add(LeashFenceKnotEntity.class, new EntityClass<>(org.bukkit.entity.LeashHitch.class, org.bukkit.craftbukkit.v.entity.CraftLeash.class, org.bukkit.craftbukkit.v.entity.CraftLeash::new));
        add(Snowball.class, new EntityClass<>(org.bukkit.entity.Snowball.class, org.bukkit.craftbukkit.v.entity.CraftSnowball.class, org.bukkit.craftbukkit.v.entity.CraftSnowball::new));
        add(EyeOfEnder.class, new EntityClass<>(org.bukkit.entity.EnderSignal.class, org.bukkit.craftbukkit.v.entity.CraftEnderSignal.class, org.bukkit.craftbukkit.v.entity.CraftEnderSignal::new));
        add(ThrownPotion.class, new EntityClass<>(org.bukkit.entity.ThrownPotion.class, org.bukkit.craftbukkit.v.entity.CraftThrownPotion.class, org.bukkit.craftbukkit.v.entity.CraftThrownPotion::new));
        add(PrimedTnt.class, new EntityClass<>(org.bukkit.entity.TNTPrimed.class, org.bukkit.craftbukkit.v.entity.CraftTNTPrimed.class, org.bukkit.craftbukkit.v.entity.CraftTNTPrimed::new));
        add(FallingBlockEntity.class, new EntityClass<>(org.bukkit.entity.FallingBlock.class, org.bukkit.craftbukkit.v.entity.CraftFallingBlock.class, org.bukkit.craftbukkit.v.entity.CraftFallingBlock::new));
        add(FireworkRocketEntity.class, new EntityClass<>(org.bukkit.entity.Firework.class, org.bukkit.craftbukkit.v.entity.CraftFirework.class, org.bukkit.craftbukkit.v.entity.CraftFirework::new));
        add(EvokerFangs.class, new EntityClass<>(org.bukkit.entity.EvokerFangs.class, org.bukkit.craftbukkit.v.entity.CraftEvokerFangs.class, org.bukkit.craftbukkit.v.entity.CraftEvokerFangs::new));
        add(MinecartCommandBlock.class, new EntityClass<>(org.bukkit.entity.minecart.CommandMinecart.class, org.bukkit.craftbukkit.v.entity.CraftMinecartCommand.class, org.bukkit.craftbukkit.v.entity.CraftMinecartCommand::new));
        add(Minecart.class, new EntityClass<>(org.bukkit.entity.minecart.RideableMinecart.class, org.bukkit.craftbukkit.v.entity.CraftMinecartRideable.class, org.bukkit.craftbukkit.v.entity.CraftMinecartRideable::new));
        add(MinecartChest.class, new EntityClass<>(org.bukkit.entity.minecart.StorageMinecart.class, org.bukkit.craftbukkit.v.entity.CraftMinecartChest.class, org.bukkit.craftbukkit.v.entity.CraftMinecartChest::new));
        add(MinecartFurnace.class, new EntityClass<>(org.bukkit.entity.minecart.PoweredMinecart.class, org.bukkit.craftbukkit.v.entity.CraftMinecartFurnace.class, org.bukkit.craftbukkit.v.entity.CraftMinecartFurnace::new));
        add(MinecartTNT.class, new EntityClass<>(org.bukkit.entity.minecart.ExplosiveMinecart.class, org.bukkit.craftbukkit.v.entity.CraftMinecartTNT.class, convert("CraftMinecartTNT")));
        add(MinecartHopper.class, new EntityClass<>(org.bukkit.entity.minecart.HopperMinecart.class, org.bukkit.craftbukkit.v.entity.CraftMinecartHopper.class, org.bukkit.craftbukkit.v.entity.CraftMinecartHopper::new));
        add(MinecartSpawner.class, new EntityClass<>(org.bukkit.entity.minecart.SpawnerMinecart.class, forName("CraftMinecartMobSpawner"), convert("CraftMinecartMobSpawner")));
        add(FishingHook.class, new EntityClass<>(org.bukkit.entity.FishHook.class, org.bukkit.craftbukkit.v.entity.CraftFishHook.class, org.bukkit.craftbukkit.v.entity.CraftFishHook::new));
        add(ServerPlayer.class, new EntityClass<>(org.bukkit.entity.Player.class, org.bukkit.craftbukkit.v.entity.CraftPlayer.class, org.bukkit.craftbukkit.v.entity.CraftPlayer::new));
    }
}
