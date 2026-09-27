package net.ironman.suit;

import net.ironman.entity.MicroMissileEntity;
import net.ironman.registry.ModDamageTypes;
import net.ironman.registry.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Оружие костюма. Всё мгновенное оружие (репульсор, огнемёт, юнибим)
 * работает лучом: никаких сущностей-снарядов, только рейкаст и частицы.
 */
public final class Weapons {
    private Weapons() {
    }

    private static final double REPULSOR_RANGE = 48.0;
    private static final float REPULSOR_COST = 60.0f;
    private static final int REPULSOR_COOLDOWN = 10;

    private static final double FLAME_RANGE = 7.0;
    private static final float FLAME_COST = 45.0f;
    private static final int FLAME_COOLDOWN = 6;

    private static final double UNIBEAM_RANGE = 64.0;
    private static final double UNIBEAM_WIDTH = 1.4;
    private static final float UNIBEAM_COST = 700.0f;
    private static final int UNIBEAM_COOLDOWN = 200;

    private static final double MISSILE_SEARCH = 40.0;
    private static final float MISSILE_COST = 60.0f;
    private static final int MISSILE_COOLDOWN = 120;

    private static final ParticleEffect REPULSOR_GLOW = new DustParticleEffect(0xBFF6FF, 1.1f);
    private static final ParticleEffect UNIBEAM_GLOW = new DustParticleEffect(0xE8FDFF, 2.2f);

    // ---- основное оружие -------------------------------------------------------

    static void firePrimary(ServerPlayerEntity player, SuitManager.Runtime rt) {
        SuitTier tier = requireOnline(player);
        if (tier == null || rt.primaryCooldown > 0) {
            return;
        }
        boolean flamethrower = tier.primary() == SuitTier.PrimaryWeapon.FLAMETHROWER;
        if (!SuitManager.consume(player, (flamethrower ? FLAME_COST : REPULSOR_COST) * tier.costMultiplier())) {
            noPower(player);
            return;
        }
        rt.primaryCooldown = flamethrower ? FLAME_COOLDOWN : REPULSOR_COOLDOWN;
        player.swingHand(Hand.MAIN_HAND, true);

        if (flamethrower) {
            flamethrower(player, tier);
        } else {
            repulsor(player, tier);
        }
    }

    private static void repulsor(ServerPlayerEntity player, SuitTier tier) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVector();
        Vec3d end = blockLimit(player, eye, look, REPULSOR_RANGE);

        EntityHitResult hit = firstEntity(player, eye, end);
        Vec3d impact = hit != null ? hit.getPos() : end;

        beam(world, handPos(player), impact, REPULSOR_GLOW, 3.0);
        world.spawnParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 6, 0.1, 0.1, 0.1, 0.08);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z, 10, 0.2, 0.2, 0.2, 0.3);
        sound(world, player.getEyePos(), SoundEvents.ENTITY_BREEZE_SHOOT, 1.0f, 1.4f);
        sound(world, impact, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.2f);

        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            target.damage(world, ModDamageTypes.source(world, ModDamageTypes.REPULSOR, player, player),
                    tier.primaryDamage());
            target.takeKnockback(1.0, -look.x, -look.z);
        }
    }

    /** Огнемёт Mark I: конус пламени перед игроком, всех в нём поджигает. */
    private static void flamethrower(ServerPlayerEntity player, SuitTier tier) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVector();
        Vec3d origin = handPos(player);

        // count = 0: частица летит с заданной скоростью в направлении (dx, dy, dz).
        for (int i = 0; i < 24; i++) {
            double spread = 0.18;
            double dx = look.x + (world.random.nextDouble() - 0.5) * spread;
            double dy = look.y + (world.random.nextDouble() - 0.5) * spread;
            double dz = look.z + (world.random.nextDouble() - 0.5) * spread;
            world.spawnParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z, 0, dx, dy, dz,
                    0.35 + world.random.nextDouble() * 0.2);
        }
        sound(world, origin, SoundEvents.ITEM_FIRECHARGE_USE, 0.8f, 0.8f);

        Box area = player.getBoundingBox().stretch(look.multiply(FLAME_RANGE)).expand(1.5);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
            Vec3d to = target.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            if (distance > FLAME_RANGE || to.normalize().dotProduct(look) < 0.85 || !player.canSee(target)) {
                continue;
            }
            target.damage(world, ModDamageTypes.source(world, ModDamageTypes.FLAMETHROWER, player, player),
                    tier.primaryDamage());
            target.setOnFireFor(5.0f);
        }
    }

    // ---- юнибим --------------------------------------------------------------------

    /** Луч из реактора в груди: пробивает всех на линии. */
    static void fireUnibeam(ServerPlayerEntity player, SuitManager.Runtime rt) {
        SuitTier tier = requireOnline(player);
        if (tier == null) {
            return;
        }
        if (!tier.hasUnibeam()) {
            player.sendMessage(Text.translatable("message.ironman.no_unibeam")
                    .formatted(Formatting.GRAY), true);
            return;
        }
        if (rt.unibeamCooldown > 0) {
            return;
        }
        if (!SuitManager.consume(player, UNIBEAM_COST * tier.costMultiplier())) {
            noPower(player);
            return;
        }
        rt.unibeamCooldown = UNIBEAM_COOLDOWN;

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d look = player.getRotationVector();
        Vec3d chest = new Vec3d(player.getX(), player.getY() + player.getHeight() * 0.62, player.getZ())
                .add(look.multiply(0.4));
        Vec3d end = blockLimit(player, chest, look, UNIBEAM_RANGE);
        double length = chest.distanceTo(end);

        beam(world, chest, end, UNIBEAM_GLOW, 4.0);
        beam(world, chest, end, ParticleTypes.END_ROD, 1.5);
        world.spawnParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 2, 0.3, 0.3, 0.3, 0.0);
        sound(world, chest, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.5f, 2.0f);
        sound(world, chest, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.6f);
        sound(world, end, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.8f, 1.3f);

        Box area = new Box(chest, end).expand(UNIBEAM_WIDTH);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
            Vec3d center = target.getBoundingBox().getCenter();
            double along = center.subtract(chest).dotProduct(look);
            if (along < 0.0 || along > length) {
                continue;
            }
            Vec3d closest = chest.add(look.multiply(along));
            if (closest.distanceTo(center) > UNIBEAM_WIDTH + target.getWidth() / 2.0) {
                continue;
            }
            target.damage(world, ModDamageTypes.source(world, ModDamageTypes.UNIBEAM, player, player),
                    tier.unibeamDamage());
            target.setOnFireFor(4.0f);
            target.takeKnockback(1.5, -look.x, -look.z);
        }
    }

    // ---- микроракеты -----------------------------------------------------------------

    static void fireMissiles(ServerPlayerEntity player, SuitManager.Runtime rt) {
        SuitTier tier = requireOnline(player);
        if (tier == null) {
            return;
        }
        if (tier.missiles() <= 0) {
            player.sendMessage(Text.translatable("message.ironman.no_missiles")
                    .formatted(Formatting.GRAY), true);
            return;
        }
        if (rt.missileCooldown > 0) {
            return;
        }

        int count = tier.missiles();
        if (!player.isCreative()) {
            count = Math.min(count, countAmmo(player));
            if (count <= 0) {
                player.sendMessage(Text.translatable("message.ironman.no_ammo")
                        .formatted(Formatting.RED), true);
                return;
            }
        }
        float cost = MISSILE_COST * count * tier.costMultiplier();
        if (!SuitManager.consume(player, cost)) {
            noPower(player);
            return;
        }
        if (!player.isCreative()) {
            takeAmmo(player, count);
        }
        rt.missileCooldown = MISSILE_COOLDOWN;

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        List<LivingEntity> targets = findTargets(player);
        Vec3d look = player.getRotationVector();
        float yaw = player.getBodyYaw() * MathHelper.RADIANS_PER_DEGREE;
        Vec3d side = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw));

        for (int i = 0; i < count; i++) {
            double offset = (i % 2 == 0 ? 1 : -1) * (0.35 + 0.1 * (i / 2));
            Vec3d pos = new Vec3d(player.getX(), player.getY() + player.getHeight() * 0.85, player.getZ())
                    .add(side.multiply(offset));
            LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());

            MicroMissileEntity missile = new MicroMissileEntity(world, player, pos, target, tier);
            Vec3d launch = look.multiply(0.5).add(0.0, 0.35, 0.0).add(side.multiply(offset * 0.4));
            missile.setVelocity(launch);
            world.spawnEntity(missile);
        }
        sound(world, player.getEyePos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2f, 1.4f);
        player.sendMessage(Text.translatable("message.ironman.missiles_away", count,
                targets.size()).formatted(Formatting.GOLD), true);
    }

    /**
     * Цели ракет: сначала то, на что смотрит игрок, затем все видимые
     * враждебные существа вокруг — от ближних к дальним.
     */
    private static List<LivingEntity> findTargets(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        List<LivingEntity> result = new ArrayList<>();

        Vec3d eye = player.getEyePos();
        Vec3d end = blockLimit(player, eye, player.getRotationVector(), 64.0);
        EntityHitResult aimed = firstEntity(player, eye, end);
        if (aimed != null && aimed.getEntity() instanceof LivingEntity living) {
            result.add(living);
        }

        Box area = player.getBoundingBox().expand(MISSILE_SEARCH);
        List<LivingEntity> hostiles = world.getEntitiesByClass(LivingEntity.class, area,
                entity -> entity instanceof Monster && entity.isAlive() && !result.contains(entity)
                        && player.canSee(entity));
        hostiles.sort(Comparator.comparingDouble(player::squaredDistanceTo));
        result.addAll(hostiles);
        return result;
    }

    public static int countAmmo(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(ModItems.MICRO_MISSILE)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void takeAmmo(ServerPlayerEntity player, int amount) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size() && amount > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(ModItems.MICRO_MISSILE)) {
                int taken = Math.min(amount, stack.getCount());
                stack.decrement(taken);
                amount -= taken;
            }
        }
    }

    // ---- общее ------------------------------------------------------------------------

    private static SuitTier requireOnline(ServerPlayerEntity player) {
        SuitTier tier = SuitManager.onlineTier(player);
        if (tier == null) {
            if (Suit.tierOf(player) == null) {
                player.sendMessage(Text.translatable("message.ironman.suit_incomplete")
                        .formatted(Formatting.RED), true);
            } else {
                noPower(player);
            }
        }
        return tier;
    }

    private static void noPower(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable(SuitManager.reactor(player).installed()
                ? "message.ironman.not_enough_power" : "message.ironman.no_reactor")
                .formatted(Formatting.RED), true);
    }

    /** Точка, где луч упирается в блок (или конец дальности). */
    private static Vec3d blockLimit(ServerPlayerEntity player, Vec3d from, Vec3d direction, double range) {
        Vec3d to = from.add(direction.multiply(range));
        BlockHitResult hit = player.getEntityWorld().raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getPos();
    }

    private static EntityHitResult firstEntity(ServerPlayerEntity player, Vec3d from, Vec3d to) {
        Box box = new Box(from, to).expand(1.0);
        return ProjectileUtil.raycast(player, from, to, box,
                entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator()
                        && entity != player,
                from.squaredDistanceTo(to));
    }

    /** Правая ладонь: немного вправо и вниз от глаз. */
    private static Vec3d handPos(ServerPlayerEntity player) {
        float yaw = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        return player.getEyePos().add(right.multiply(0.35)).add(0.0, -0.35, 0.0)
                .add(player.getRotationVector().multiply(0.5));
    }

    /** Луч частиц от {@code from} до {@code to}, {@code density} частиц на блок. */
    private static void beam(ServerWorld world, Vec3d from, Vec3d to, ParticleEffect particle, double density) {
        Vec3d delta = to.subtract(from);
        int steps = Math.max(1, (int) (delta.length() * density));
        for (int i = 0; i <= steps; i++) {
            Vec3d point = from.add(delta.multiply(i / (double) steps));
            world.spawnParticles(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void sound(ServerWorld world, Vec3d pos, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
