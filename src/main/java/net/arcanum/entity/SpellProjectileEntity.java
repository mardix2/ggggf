package net.arcanum.entity;

import net.arcanum.registry.ModEntities;
import net.arcanum.registry.ModItems;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Универсальный магический снаряд.
 *
 * <p>Один тип сущности на все школы: внешний вид задаётся предметом внутри
 * (его рисует ванильный рендерер летящих предметов), а что произойдёт при
 * попадании — коллбэком {@link Impact}, который выставляет заклинание.
 * Поэтому мод не нуждается ни в собственных моделях, ни в рендерерах.
 */
public class SpellProjectileEntity extends ThrownItemEntity {

    /** Что снаряд делает при попадании. {@code directHit} = {@code null} при попадании в блок. */
    @FunctionalInterface
    public interface Impact {
        void apply(ServerWorld world, SpellProjectileEntity projectile, Vec3d pos, LivingEntity directHit);
    }

    /** Через сколько тиков «забытый» снаряд исчезает сам. */
    private static final int MAX_AGE = 120;

    private SpellSchool school = SpellSchool.ARCANE;
    private ParticleEffect trail;
    private Impact impact;
    private float gravity;

    public SpellProjectileEntity(net.minecraft.entity.EntityType<? extends SpellProjectileEntity> type, World world) {
        super(type, world);
    }

    public SpellProjectileEntity(World world, LivingEntity owner, ItemStack visual) {
        super(ModEntities.SPELL_PROJECTILE, owner, world, visual);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ARCANE_CRYSTAL;
    }

    public SpellProjectileEntity configure(SpellSchool school, ParticleEffect trail, float gravity, Impact impact) {
        this.school = school;
        this.trail = trail;
        this.gravity = gravity;
        this.impact = impact;
        return this;
    }

    public SpellSchool school() {
        return school;
    }

    @Override
    protected double getGravity() {
        return gravity;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld() instanceof ServerWorld serverWorld) {
            if (trail != null) {
                serverWorld.spawnParticles(trail, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.0);
            }
            // Снаряд, переживший перезагрузку чанка, теряет свой эффект —
            // пусть он тихо исчезнет, а не висит в мире навсегда.
            if (age > MAX_AGE) {
                discard();
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        super.onEntityHit(result);
        Entity hit = result.getEntity();
        if (getWorld() instanceof ServerWorld serverWorld && impact != null) {
            impact.apply(serverWorld, this, result.getPos(),
                    hit instanceof LivingEntity living ? living : null);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult result) {
        super.onBlockHit(result);
        if (getWorld() instanceof ServerWorld serverWorld && impact != null) {
            impact.apply(serverWorld, this, result.getPos(), null);
        }
    }

    @Override
    protected void onCollision(HitResult result) {
        super.onCollision(result);
        if (!getWorld().isClient()) {
            if (getWorld() instanceof ServerWorld serverWorld) {
                Fx.burst(serverWorld, result.getPos(), Fx.dust(school.color(), 1.2f), 12, 0.2, 0.05);
            }
            discard();
        }
    }
}
