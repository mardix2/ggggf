package net.ironman.entity;

import net.ironman.registry.ModDamageTypes;
import net.ironman.registry.ModEntities;
import net.ironman.registry.ModItems;
import net.ironman.suit.SuitTier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Самонаводящаяся микроракета из наплечных пусковых.
 *
 * <p>Первые тики ракета просто летит вверх-вперёд, как при настоящем
 * запуске, затем плавно разворачивается к цели. Взрыв не ломает блоки
 * и не задевает владельца.
 */
public class MicroMissileEntity extends ThrownItemEntity {

    private static final double SPEED = 1.3;
    /** Сколько тиков ракета летит без наведения после пуска. */
    private static final int LAUNCH_TICKS = 6;
    /** Насколько резко ракета доворачивает к цели (0..1). */
    private static final double TURN_RATE = 0.28;
    private static final int MAX_AGE = 100;
    private static final double BLAST_RADIUS = 2.5;

    private LivingEntity target;
    private float damage = 10.0f;

    public MicroMissileEntity(EntityType<? extends MicroMissileEntity> type, World world) {
        super(type, world);
    }

    public MicroMissileEntity(World world, LivingEntity owner, Vec3d pos, LivingEntity target, SuitTier tier) {
        super(ModEntities.MICRO_MISSILE, pos.x, pos.y, pos.z, world, new ItemStack(ModItems.MICRO_MISSILE));
        setOwner(owner);
        this.target = target;
        this.damage = tier == SuitTier.MARK_85 ? 12.0f : 9.0f;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MICRO_MISSILE;
    }

    @Override
    protected double getGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        if (!getEntityWorld().isClient()) {
            steer();
        }
        super.tick();

        if (getEntityWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.0);
            world.spawnParticles(ParticleTypes.SMALL_FLAME, getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            if (age > MAX_AGE && !isRemoved()) {
                explode(world, getEntityPos());
            }
        }
    }

    private void steer() {
        Vec3d velocity = getVelocity();
        if (age < LAUNCH_TICKS) {
            return;
        }
        Vec3d direction = velocity.lengthSquared() > 1.0E-4 ? velocity.normalize() : new Vec3d(0.0, 1.0, 0.0);
        if (target != null && target.isAlive() && !target.isRemoved()) {
            Vec3d toTarget = target.getBoundingBox().getCenter().subtract(getEntityPos()).normalize();
            direction = direction.lerp(toTarget, TURN_RATE).normalize();
        }
        setVelocity(direction.multiply(SPEED));
    }

    @Override
    protected void onCollision(HitResult result) {
        super.onCollision(result);
        if (getEntityWorld() instanceof ServerWorld world && !isRemoved()) {
            explode(world, result.getPos());
        }
    }

    private void explode(ServerWorld world, Vec3d pos) {
        Entity owner = getOwner();
        DamageSource source = ModDamageTypes.source(world, ModDamageTypes.MICRO_MISSILE, this, owner);
        Box area = new Box(pos, pos).expand(BLAST_RADIUS);
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area,
                entity -> entity != owner && entity.isAlive() && !entity.isSpectator())) {
            double distance = victim.getBoundingBox().getCenter().distanceTo(pos);
            if (distance > BLAST_RADIUS + 1.0) {
                continue;
            }
            float falloff = (float) Math.max(0.35, 1.0 - distance / (BLAST_RADIUS + 1.0));
            victim.damage(world, source, damage * falloff);
            Vec3d push = victim.getEntityPos().subtract(pos);
            victim.takeKnockback(0.6 * falloff, -push.x, -push.z);
        }

        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 12, 0.3, 0.3, 0.3, 0.08);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 6, 0.3, 0.3, 0.3, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                SoundCategory.PLAYERS, 0.9f, 1.4f + world.random.nextFloat() * 0.2f);
        discard();
    }
}
