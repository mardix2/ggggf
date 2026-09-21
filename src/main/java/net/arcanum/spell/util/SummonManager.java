package net.arcanum.spell.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Призванные существа.
 *
 * <p>Вместо собственных типов сущностей мод берёт ванильных мобов и ведёт их
 * снаружи: раз в секунду наводит на ближайшего врага и через заданное время
 * развеивает. Так призыв не тянет за собой ни моделей, ни рендереров, а
 * поведение остаётся предсказуемым.
 */
public final class SummonManager {
    private SummonManager() {
    }

    /** Тег, который получают все призванные существа. */
    public static final String SUMMON_TAG = "arcanum_summon";

    private record Summon(UUID entity, UUID owner, RegistryKey<World> world, long expiry) {
    }

    private static final List<Summon> ACTIVE = new ArrayList<>();
    private static final Map<UUID, UUID> OWNERS = new HashMap<>();

    private static final int RETARGET_INTERVAL = 20;
    private static final double TARGET_RANGE = 16.0;
    private static final double LEASH_RANGE = 32.0;

    /**
     * Создаёт моба заданного типа рядом с игроком и берёт его под контроль.
     *
     * @return призванное существо либо {@code null}, если тип не удалось создать
     */
    public static MobEntity summon(ServerWorld world, EntityType<? extends MobEntity> type,
                                   ServerPlayerEntity owner, Vec3d pos, int lifetimeTicks,
                                   Text name, float healthMultiplier) {
        MobEntity mob = type.create(world, SpawnReason.MOB_SUMMONED);
        if (mob == null) {
            return null;
        }
        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, owner.getYaw(), 0.0f);
        mob.setPersistent();
        mob.addCommandTag(SUMMON_TAG);
        if (name != null) {
            mob.setCustomName(name);
        }
        if (healthMultiplier != 1.0f) {
            // Поднимаем именно максимум: setHealth сам по себе обрезается по нему.
            EntityAttributeInstance maxHealth = mob.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(maxHealth.getBaseValue() * healthMultiplier);
            }
            mob.setHealth(mob.getMaxHealth());
        }
        if (!world.spawnEntity(mob)) {
            return null;
        }

        register(mob, owner, lifetimeTicks);
        Fx.sphere(world, mob.getPos().add(0.0, 0.5, 0.0), 0.8, ParticleTypes.SOUL_FIRE_FLAME, 20);
        return mob;
    }

    private static void register(MobEntity mob, ServerPlayerEntity owner, int lifetimeTicks) {
        ACTIVE.add(new Summon(mob.getUuid(), owner.getUuid(), mob.getWorld().getRegistryKey(),
                mob.getWorld().getTime() + lifetimeTicks));
        OWNERS.put(mob.getUuid(), owner.getUuid());
    }

    public static boolean isSummonOf(LivingEntity entity, PlayerEntity owner) {
        UUID ownerId = OWNERS.get(entity.getUuid());
        return ownerId != null && ownerId.equals(owner.getUuid());
    }

    public static boolean isSummon(Entity entity) {
        return OWNERS.containsKey(entity.getUuid());
    }

    /** Вызывается раз в тик из общего серверного цикла. */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        boolean retarget = server.getTicks() % RETARGET_INTERVAL == 0;

        Iterator<Summon> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Summon summon = iterator.next();
            ServerWorld world = server.getWorld(summon.world());
            if (world == null) {
                iterator.remove();
                OWNERS.remove(summon.entity());
                continue;
            }
            Entity entity = world.getEntity(summon.entity());
            if (!(entity instanceof MobEntity mob) || !mob.isAlive()) {
                iterator.remove();
                OWNERS.remove(summon.entity());
                continue;
            }
            if (world.getTime() >= summon.expiry()) {
                dispel(world, mob);
                iterator.remove();
                OWNERS.remove(summon.entity());
                continue;
            }
            if (retarget) {
                PlayerEntity owner = world.getPlayerByUuid(summon.owner());
                steer(world, mob, owner);
            }
        }
    }

    /** Наводит призванного на ближайшего врага и не даёт уйти далеко от хозяина. */
    private static void steer(ServerWorld world, MobEntity mob, PlayerEntity owner) {
        if (owner == null) {
            return;
        }
        // Призванный никогда не бьёт хозяина, что бы ни говорил его ИИ.
        LivingEntity current = mob.getTarget();
        if (current == owner) {
            mob.setTarget(null);
            current = null;
        }

        if (mob.squaredDistanceTo(owner) > LEASH_RANGE * LEASH_RANGE) {
            SpellUtil.teleport(mob, SpellUtil.safeSpot(world, owner.getPos()));
            return;
        }

        if (current != null && current.isAlive()) {
            return;
        }
        List<LivingEntity> candidates = SpellUtil.around(world, mob.getPos(), TARGET_RANGE,
                e -> SpellUtil.isEnemy(owner, e) && !isSummon(e) && !(e instanceof PlayerEntity));
        if (!candidates.isEmpty()) {
            mob.setTarget(candidates.get(0));
        }
    }

    private static void dispel(ServerWorld world, MobEntity mob) {
        Fx.sphere(world, mob.getPos().add(0.0, mob.getHeight() * 0.5, 0.0), 0.7, ParticleTypes.SMOKE, 25);
        mob.discard();
    }

    /** Развеивает всех призванных конкретного игрока. Возвращает их число. */
    public static int dispelAll(MinecraftServer server, PlayerEntity owner) {
        int count = 0;
        Iterator<Summon> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Summon summon = iterator.next();
            if (!summon.owner().equals(owner.getUuid())) {
                continue;
            }
            ServerWorld world = server.getWorld(summon.world());
            if (world != null && world.getEntity(summon.entity()) instanceof MobEntity mob) {
                dispel(world, mob);
                count++;
            }
            iterator.remove();
            OWNERS.remove(summon.entity());
        }
        return count;
    }
}
