package net.arcanum.spell.util;

import net.arcanum.spell.SpellContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Общие операции, которые нужны почти каждому заклинанию. */
public final class SpellUtil {
    private SpellUtil() {
    }

    // ------------------------------------------------------------------
    //  Наведение
    // ------------------------------------------------------------------

    /** Блок, на который смотрит игрок, либо {@code null}, если взгляд уходит в пустоту. */
    public static BlockHitResult raycastBlock(PlayerEntity player, double range, boolean includeFluids) {
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(range));
        RaycastContext.FluidHandling fluids = includeFluids
                ? RaycastContext.FluidHandling.ANY
                : RaycastContext.FluidHandling.NONE;
        HitResult hit = player.getWorld().raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, fluids, player));
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }

    /**
     * Существо в прицеле у произвольного игрока.
     *
     * <p>Заклинаниям нужен вариант с {@link SpellContext} — он учитывает
     * руну дальнобойности. Два имени вместо перегрузки взяты намеренно:
     * перегрузка по первому параметру тут только запутывает.
     *
     * <p>Реализовано вручную через пересечение хитбоксов с лучом взгляда —
     * так поведение не зависит от версии вспомогательных классов ванили.
     */
    public static LivingEntity raycastEntityFrom(PlayerEntity player, double range) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(range));

        // Луч мог упереться в стену раньше, чем в существо.
        BlockHitResult blockHit = raycastBlock(player, range, false);
        if (blockHit != null) {
            end = blockHit.getPos();
        }

        Box search = player.getBoundingBox().stretch(direction.multiply(range)).expand(1.0);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : player.getWorld().getOtherEntities(player, search, SpellUtil::isTargetable)) {
            Optional<Vec3d> point = entity.getBoundingBox().expand(0.3).raycast(start, end);
            if (point.isEmpty()) {
                continue;
            }
            double distance = start.squaredDistanceTo(point.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = (LivingEntity) entity;
            }
        }
        return best;
    }

    /** Цель в прицеле с поправкой на руну дальнобойности. */
    public static LivingEntity raycastEntity(SpellContext ctx, double baseRange) {
        return raycastEntityFrom(ctx.caster(), ctx.reach(baseRange));
    }

    /** Точка прицела с поправкой на руну дальнобойности. */
    public static Vec3d aimPoint(SpellContext ctx, double baseRange) {
        return aimPointFrom(ctx.caster(), ctx.reach(baseRange));
    }

    /** Точка, куда «смотрит» заклинатель: существо, блок или конец луча. */
    public static Vec3d aimPointFrom(PlayerEntity player, double range) {
        LivingEntity target = raycastEntityFrom(player, range);
        if (target != null) {
            return target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        }
        BlockHitResult hit = raycastBlock(player, range, false);
        if (hit != null) {
            return hit.getPos();
        }
        return player.getEyePos().add(player.getRotationVec(1.0f).multiply(range));
    }

    private static boolean isTargetable(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator();
    }

    // ------------------------------------------------------------------
    //  Поиск целей
    // ------------------------------------------------------------------

    public static List<LivingEntity> around(ServerWorld world, Vec3d center, double radius,
                                            Predicate<LivingEntity> filter) {
        Box box = Box.of(center, radius * 2, radius * 2, radius * 2);
        List<LivingEntity> out = new ArrayList<>();
        double radiusSq = radius * radius;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, e -> true)) {
            if (entity.squaredDistanceTo(center) <= radiusSq && filter.test(entity)) {
                out.add(entity);
            }
        }
        out.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(center)));
        return out;
    }

    /** Враги вокруг точки — всё живое, кроме заклинателя, его питомцев и союзников. */
    public static List<LivingEntity> enemiesAround(SpellContext ctx, Vec3d center, double radius) {
        return around(ctx.world(), center, radius, e -> isEnemy(ctx.caster(), e));
    }

    /** Союзники вокруг точки, включая самого заклинателя. */
    public static List<LivingEntity> alliesAround(SpellContext ctx, Vec3d center, double radius) {
        return around(ctx.world(), center, radius, e -> !isEnemy(ctx.caster(), e));
    }

    public static boolean isEnemy(PlayerEntity caster, LivingEntity target) {
        if (target == caster || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target instanceof TameableEntity tameable && tameable.isTamed() && tameable.getOwner() == caster) {
            return false;
        }
        if (target instanceof PlayerEntity other && caster.isTeammate(other)) {
            return false;
        }
        return !SummonManager.isSummonOf(target, caster);
    }

    // ------------------------------------------------------------------
    //  Воздействие
    // ------------------------------------------------------------------

    /**
     * Магический урон с привязкой к заклинателю (работает статистика и агро).
     *
     * <p>Здесь же срабатывают реакции школ: это единственная точка, через
     * которую заклинания бьют по целям, и держать логику меток в одном
     * месте надёжнее, чем вспоминать про неё в каждом заклинании.
     */
    public static boolean damage(SpellContext ctx, LivingEntity target, float amount) {
        boolean hit = target.damage(ctx.world(),
                ctx.world().getDamageSources().indirectMagic(ctx.caster(), ctx.caster()),
                amount);
        if (hit) {
            SpellReactions.onHit(ctx, target);
        }
        return hit;
    }

    /** Урон от огня: поджигает и бьёт магией — так работает и в воде, и по нежити. */
    public static boolean fireDamage(SpellContext ctx, LivingEntity target, float amount, int burnTicks) {
        boolean hit = damage(ctx, target, amount);
        if (hit && burnTicks > 0 && !target.isFireImmune()) {
            target.setOnFireForTicks(burnTicks);
        }
        return hit;
    }

    /** Урон холодом: подмораживает цель и замедляет её. */
    public static boolean frostDamage(SpellContext ctx, LivingEntity target, float amount, int freezeTicks) {
        boolean hit = damage(ctx, target, amount);
        if (hit && freezeTicks > 0) {
            target.setFrozenTicks(Math.min(target.getFrozenTicks() + freezeTicks, freezeTicks * 3));
            effect(target, StatusEffects.SLOWNESS, freezeTicks / 2, 1);
        }
        return hit;
    }

    public static void effect(LivingEntity target, RegistryEntry<StatusEffect> effect, int ticks, int amplifier) {
        if (ticks <= 0) {
            return;
        }
        target.addStatusEffect(new StatusEffectInstance(effect, ticks, amplifier, false, true, true));
    }

    /** Отбрасывание от точки. Отрицательная сила притягивает. */
    public static void knockback(LivingEntity target, Vec3d from, double strength) {
        Vec3d push = target.getPos().subtract(from);
        if (push.lengthSquared() < 1.0E-4) {
            push = new Vec3d(0.0, 1.0, 0.0);
        }
        push = push.normalize().multiply(strength);
        target.addVelocity(push.x, push.y * 0.6 + 0.15, push.z);
        target.velocityModified = true;
    }

    /** Телепортация без сброса ориентации; безопасна и для игроков, и для мобов. */
    public static void teleport(LivingEntity entity, Vec3d pos) {
        if (entity instanceof ServerPlayerEntity player) {
            player.networkHandler.requestTeleport(pos.x, pos.y, pos.z, player.getYaw(), player.getPitch());
        } else {
            entity.requestTeleport(pos.x, pos.y, pos.z);
        }
        entity.onLanding();
    }

    /**
     * Ищет безопасную точку для телепортации рядом с {@code target}:
     * два свободных блока по высоте и твёрдая опора снизу.
     */
    public static Vec3d safeSpot(ServerWorld world, Vec3d target) {
        BlockPos base = BlockPos.ofFloored(target);
        for (int dy = 0; dy <= 4; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos pos = base.up(dy * sign);
                if (isFree(world, pos) && isFree(world, pos.up())) {
                    return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                }
            }
        }
        return target;
    }

    private static boolean isFree(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    public static void message(SpellContext ctx, Text text) {
        ctx.caster().sendMessage(text, true);
    }

    // ------------------------------------------------------------------
    //  Снаряды и взрывы
    // ------------------------------------------------------------------

    /**
     * Запускает магический снаряд из глаз заклинателя.
     *
     * @param visual     предмет, который увидит игрок (рендерится ванилью)
     * @param speed      начальная скорость
     * @param gravity    притяжение: 0 — снаряд летит прямо
     * @param divergence разброс в градусах
     */
    public static net.arcanum.entity.SpellProjectileEntity shoot(
            SpellContext ctx,
            net.minecraft.item.Item visual,
            net.arcanum.spell.SpellSchool school,
            net.minecraft.particle.ParticleEffect trail,
            float speed, float gravity, float divergence,
            net.arcanum.entity.SpellProjectileEntity.Impact impact) {

        net.arcanum.entity.SpellProjectileEntity first = null;
        // Руна расщепления добавляет снаряды; чтобы они не летели одной
        // линией, лишним добавляется разброс.
        int shots = 1 + Math.max(0, ctx.extraShots());
        for (int index = 0; index < shots; index++) {
            net.arcanum.entity.SpellProjectileEntity projectile =
                    new net.arcanum.entity.SpellProjectileEntity(ctx.world(), ctx.caster(),
                            new net.minecraft.item.ItemStack(visual));
            projectile.configure(school, trail, gravity, impact);
            float spread = index == 0 ? divergence : Math.max(divergence, 8.0f);
            projectile.setVelocity(ctx.caster(), ctx.caster().getPitch(), ctx.caster().getYaw(),
                    0.0f, speed, spread);
            ctx.world().spawnEntity(projectile);
            if (first == null) {
                first = projectile;
            }
        }
        return first;
    }

    /**
     * Урон по площади с отбрасыванием. Блоки не разрушаются —
     * магия в этом моде не роет ландшафт.
     */
    public static int explode(SpellContext ctx, Vec3d center, double radius, float damage, double knockback) {
        int hits = 0;
        for (LivingEntity target : enemiesAround(ctx, center, radius)) {
            // Урон спадает к краю области.
            double distance = Math.sqrt(target.squaredDistanceTo(center));
            float falloff = (float) Math.max(0.25, 1.0 - distance / radius);
            if (damage(ctx, target, damage * falloff)) {
                hits++;
            }
            if (knockback != 0.0) {
                knockback(target, center, knockback * falloff);
            }
        }
        return hits;
    }

    /**
     * Временно ставит блок и убирает его через заданное время.
     * Ставится только вместо воздуха или заменяемых блоков (трава, вода).
     */
    public static boolean placeTemporary(net.minecraft.server.world.ServerWorld world, BlockPos pos,
                                         net.minecraft.block.BlockState state, int lifetimeTicks) {
        if (!world.getBlockState(pos).isReplaceable()) {
            return false;
        }
        world.setBlockState(pos, state);
        SpellTicker.schedule(world, lifetimeTicks, lifetimeTicks, elapsed -> {
            if (world.getBlockState(pos).isOf(state.getBlock())) {
                world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
            }
            return false;
        });
        return true;
    }
}
