package net.arcanum.spell.impl;

import net.arcanum.registry.ModEffects;
import net.arcanum.registry.ModItems;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCastType;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.arcanum.spell.util.SpellTicker;
import net.arcanum.spell.util.SpellUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Пиромантия: прямой урон, поджог и работа по площади. */
public final class FireSpells {
    private FireSpells() {
    }

    public static void init() {
        fireBolt();
        flameWave();
        fireShield();
        combustion();
        meteor();
        inferno();
    }

    /** Дешёвый снаряд — рабочая лошадка любого пироманта. */
    private static void fireBolt() {
        Spell.builder("fire_bolt", SpellSchool.FIRE)
                .type(SpellCastType.PROJECTILE)
                .tier(1).cost(8.0f).cooldown(10)
                .action(ctx -> {
                    SpellUtil.shoot(ctx, ModItems.RUNES.get(SpellSchool.FIRE), SpellSchool.FIRE,
                            ParticleTypes.FLAME, 1.6f, 0.02f, 0.0f,
                            (world, projectile, pos, hit) -> {
                                if (hit != null) {
                                    SpellUtil.fireDamage(ctx, hit, ctx.scaled(5.0f), 60);
                                } else {
                                    SpellUtil.explode(ctx, pos, 1.5, ctx.scaled(3.0f), 0.2);
                                }
                                Fx.burst(world, pos, ParticleTypes.FLAME, 20, 0.3, 0.05);
                                Fx.sound(world, pos, SoundEvents.ENTITY_BLAZE_HURT, 0.7f, 1.4f);
                            });
                    Fx.sound(ctx.world(), ctx.eyes(), SoundEvents.ITEM_FIRECHARGE_USE, 0.7f, 1.5f);
                    return true;
                })
                .register();
    }

    /** Конус пламени перед заклинателем: бьёт нескольких и поджигает. */
    private static void flameWave() {
        Spell.builder("flame_wave", SpellSchool.FIRE)
                .type(SpellCastType.AREA)
                .tier(1).cost(20.0f).cooldown(40)
                .action(ctx -> {
                    Vec3d origin = ctx.eyes();
                    Vec3d direction = ctx.look();
                    double range = 7.0;

                    List<LivingEntity> targets =
                            SpellUtil.enemiesAround(ctx, origin.add(direction.multiply(range / 2)), range);
                    for (LivingEntity target : targets) {
                        // Только те, кто действительно перед лицом заклинателя.
                        Vec3d toTarget = target.getPos().subtract(origin).normalize();
                        if (toTarget.dotProduct(direction) < 0.5) {
                            continue;
                        }
                        SpellUtil.fireDamage(ctx, target, ctx.scaled(6.0f), 100);
                        SpellUtil.knockback(target, origin, 0.4);
                    }

                    Fx.cone(ctx.world(), origin, direction, range, 45.0, ParticleTypes.FLAME, 80);
                    Fx.sound(ctx.world(), origin, SoundEvents.ENTITY_GHAST_SHOOT, 1.0f, 0.9f);
                    return true;
                })
                .register();
    }

    /** Огненный доспех: защищает от огня и жжёт всех, кто подойдёт вплотную. */
    private static void fireShield() {
        Spell.builder("fire_shield", SpellSchool.FIRE)
                .type(SpellCastType.SELF)
                .tier(2).cost(35.0f).cooldown(300)
                .action(ctx -> {
                    int duration = 12 * 20;
                    SpellUtil.effect(ctx.caster(), StatusEffects.FIRE_RESISTANCE, duration, 0);

                    SpellTicker.schedule(ctx.world(), duration, 10, elapsed -> {
                        if (ctx.caster().isRemoved() || !ctx.caster().isAlive()) {
                            return false;
                        }
                        Vec3d center = ctx.caster().getPos().add(0.0, 1.0, 0.0);
                        Fx.ring(ctx.world(), center, 1.3, ParticleTypes.FLAME, 10);
                        for (LivingEntity target : SpellUtil.enemiesAround(ctx, center, 2.5)) {
                            SpellUtil.fireDamage(ctx, target, ctx.scaled(2.0f), 40);
                        }
                        return true;
                    });

                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
                    return true;
                })
                .register();
    }

    /** Метка возгорания: через три секунды цель взрывается изнутри. */
    private static void combustion() {
        Spell.builder("combustion", SpellSchool.FIRE)
                .type(SpellCastType.TARGET)
                .tier(2).cost(25.0f).cooldown(80)
                .action(ctx -> {
                    LivingEntity target = SpellUtil.raycastEntity(ctx, 24.0);
                    if (target == null) {
                        return false;
                    }
                    SpellUtil.effect(target, ModEffects.COMBUSTION, 60, 0);
                    Fx.sound(ctx.world(), target.getPos(), SoundEvents.BLOCK_FIRE_AMBIENT, 1.0f, 0.6f);

                    SpellTicker.schedule(ctx.world(), 60, 5, elapsed -> {
                        if (!target.isAlive()) {
                            return false;
                        }
                        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
                        if (elapsed < 60) {
                            Fx.sphere(ctx.world(), pos, 0.8, ParticleTypes.SMALL_FLAME, 8);
                            return true;
                        }
                        // Срок вышел — взрыв.
                        SpellUtil.explode(ctx, pos, 3.5, ctx.scaled(9.0f), 0.6);
                        Fx.burst(ctx.world(), pos, ParticleTypes.EXPLOSION, 6, 0.6, 0.0);
                        Fx.sound(ctx.world(), pos, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
                        return false;
                    });
                    return true;
                })
                .register();
    }

    /** Метеор падает в точку прицела через полторы секунды. */
    private static void meteor() {
        Spell.builder("meteor", SpellSchool.FIRE)
                .type(SpellCastType.AREA)
                .tier(3).cost(65.0f).cooldown(240)
                .action(ctx -> {
                    Vec3d impact = SpellUtil.aimPoint(ctx, 32.0);
                    Vec3d start = impact.add(0.0, 14.0, 0.0);
                    Fx.sound(ctx.world(), impact, SoundEvents.ENTITY_WITHER_SHOOT, 1.4f, 0.6f);

                    SpellTicker.schedule(ctx.world(), 30, 2, elapsed -> {
                        double progress = elapsed / 30.0;
                        Vec3d position = start.lerp(impact, progress);
                        Fx.sphere(ctx.world(), position, 1.1, ParticleTypes.FLAME, 25);
                        Fx.sphere(ctx.world(), position, 0.6, ParticleTypes.LARGE_SMOKE, 10);

                        if (elapsed < 30) {
                            return true;
                        }
                        SpellUtil.explode(ctx, impact, 6.0, ctx.scaled(18.0f), 1.1);
                        for (LivingEntity target : SpellUtil.enemiesAround(ctx, impact, 6.0)) {
                            if (!target.isFireImmune()) {
                                target.setOnFireForTicks(120);
                            }
                        }
                        Fx.burst(ctx.world(), impact, ParticleTypes.EXPLOSION_EMITTER, 3, 1.5, 0.0);
                        Fx.sound(ctx.world(), impact, SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
                        return false;
                    });
                    return true;
                })
                .register();
    }

    /** Огненная буря: десять секунд ада в радиусе семи блоков. */
    private static void inferno() {
        Spell.builder("inferno", SpellSchool.FIRE)
                .type(SpellCastType.LINGERING)
                .tier(4).cost(110.0f).cooldown(600)
                .action(ctx -> {
                    Vec3d center = SpellUtil.aimPoint(ctx, 24.0);
                    int duration = 10 * 20;
                    Fx.sound(ctx.world(), center, SoundEvents.ITEM_FIRECHARGE_USE, 2.0f, 0.5f);

                    SpellTicker.schedule(ctx.world(), duration, 10, elapsed -> {
                        Fx.ring(ctx.world(), center, 7.0, ParticleTypes.FLAME, 30);
                        Fx.burst(ctx.world(), center.add(0.0, 1.0, 0.0), ParticleTypes.LAVA, 6, 3.0, 0.0);
                        for (LivingEntity target : SpellUtil.enemiesAround(ctx, center, 7.0)) {
                            SpellUtil.fireDamage(ctx, target, ctx.scaled(3.5f), 60);
                        }
                        Fx.sound(ctx.world(), center, SoundEvents.BLOCK_FIRE_AMBIENT, 1.2f, 0.8f);
                        return true;
                    });
                    return true;
                })
                .register();
    }
}
