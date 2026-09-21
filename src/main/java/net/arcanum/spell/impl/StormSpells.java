package net.arcanum.spell.impl;

import net.arcanum.registry.ModItems;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCastType;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.arcanum.spell.util.SpellTicker;
import net.arcanum.spell.util.SpellUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Магия бури: цепные разряды, ветер и погода. */
public final class StormSpells {
    private StormSpells() {
    }

    public static void init() {
        spark();
        chainLightning();
        thunderstrike();
        gale();
        staticField();
        stormCall();
    }

    /** Быстрая искра: мало урона, но почти без перезарядки. */
    private static void spark() {
        Spell.builder("spark", SpellSchool.STORM)
                .type(SpellCastType.PROJECTILE)
                .tier(1).cost(6.0f).cooldown(6)
                .action(ctx -> {
                    SpellUtil.shoot(ctx, ModItems.RUNES.get(SpellSchool.STORM), SpellSchool.STORM,
                            ParticleTypes.ELECTRIC_SPARK, 2.4f, 0.0f, 0.0f,
                            (world, projectile, pos, hit) -> {
                                if (hit != null) {
                                    SpellUtil.damage(ctx, hit, ctx.scaled(4.0f));
                                    SpellUtil.effect(hit, StatusEffects.WEAKNESS, 60, 0);
                                }
                                Fx.burst(world, pos, ParticleTypes.ELECTRIC_SPARK, 15, 0.2, 0.2);
                                Fx.sound(world, pos, SoundEvents.ENTITY_BEE_STING, 0.6f, 1.8f);
                            });
                    return true;
                })
                .register();
    }

    /** Цепная молния перепрыгивает с цели на цель, слабея с каждым скачком. */
    private static void chainLightning() {
        Spell.builder("chain_lightning", SpellSchool.STORM)
                .type(SpellCastType.TARGET)
                .tier(2).cost(30.0f).cooldown(70)
                .action(ctx -> {
                    LivingEntity first = SpellUtil.raycastEntity(ctx, 24.0);
                    if (first == null) {
                        return false;
                    }

                    List<LivingEntity> chain = new ArrayList<>();
                    LivingEntity current = first;
                    Vec3d from = ctx.eyes();
                    float damage = ctx.scaled(8.0f);

                    for (int jump = 0; jump < 5 && current != null; jump++) {
                        Fx.beam(ctx.world(), from, current.getPos().add(0.0, current.getHeight() * 0.5, 0.0),
                                ParticleTypes.ELECTRIC_SPARK, 0.25);
                        SpellUtil.damage(ctx, current, damage);
                        Fx.sound(ctx.world(), current.getPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,
                                0.5f, 1.8f);
                        chain.add(current);

                        from = current.getPos().add(0.0, current.getHeight() * 0.5, 0.0);
                        damage *= 0.72f;
                        LivingEntity next = null;
                        for (LivingEntity candidate : SpellUtil.enemiesAround(ctx, from, 6.0)) {
                            if (!chain.contains(candidate)) {
                                next = candidate;
                                break;
                            }
                        }
                        current = next;
                    }
                    return true;
                })
                .register();
    }

    /** Настоящая молния в точку прицела. Под открытым небом бьёт больнее. */
    private static void thunderstrike() {
        Spell.builder("thunderstrike", SpellSchool.STORM)
                .type(SpellCastType.AREA)
                .tier(3).cost(55.0f).cooldown(200)
                .action(ctx -> {
                    Vec3d target = SpellUtil.aimPoint(ctx, 32.0);
                    ServerWorld world = ctx.world();

                    LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
                    if (bolt != null) {
                        bolt.refreshPositionAfterTeleport(target);
                        bolt.setChanneler(ctx.caster());
                        world.spawnEntity(bolt);
                    }

                    SpellUtil.explode(ctx, target, 4.0, ctx.scaled(10.0f), 0.5);
                    Fx.burst(world, target, ParticleTypes.ELECTRIC_SPARK, 60, 1.5, 0.3);
                    return true;
                })
                .register();
    }

    /** Порыв ветра: расшвыривает всех вокруг и подбрасывает заклинателя. */
    private static void gale() {
        Spell.builder("gale", SpellSchool.STORM)
                .type(SpellCastType.AREA)
                .tier(1).cost(18.0f).cooldown(80)
                .action(ctx -> {
                    Vec3d center = ctx.feet();
                    for (LivingEntity target : SpellUtil.enemiesAround(ctx, center, 7.0)) {
                        SpellUtil.damage(ctx, target, ctx.scaled(2.0f));
                        SpellUtil.knockback(target, center, 1.6);
                    }
                    // Заклинателя подбрасывает вверх — заодно это способ уйти от удара.
                    ctx.caster().addVelocity(0.0, 0.9, 0.0);
                    ctx.caster().velocityModified = true;
                    ctx.caster().onLanding();

                    Fx.sphere(ctx.world(), center.add(0.0, 1.0, 0.0), 2.5, ParticleTypes.CLOUD, 50);
                    Fx.sound(ctx.world(), center, SoundEvents.ENTITY_BREEZE_WIND_BURST, 1.2f, 1.0f);
                    return true;
                })
                .register();
    }

    /** Статическое поле держит врагов на месте и бьёт током. */
    private static void staticField() {
        Spell.builder("static_field", SpellSchool.STORM)
                .type(SpellCastType.LINGERING)
                .tier(2).cost(40.0f).cooldown(220)
                .action(ctx -> {
                    Vec3d center = SpellUtil.aimPoint(ctx, 20.0);
                    int duration = 8 * 20;
                    Fx.sound(ctx.world(), center, SoundEvents.BLOCK_BEACON_AMBIENT, 1.0f, 1.6f);

                    SpellTicker.schedule(ctx.world(), duration, 10, elapsed -> {
                        Fx.ring(ctx.world(), center, 5.0, ParticleTypes.ELECTRIC_SPARK, 20);
                        for (LivingEntity target : SpellUtil.enemiesAround(ctx, center, 5.0)) {
                            SpellUtil.damage(ctx, target, ctx.scaled(2.0f));
                            SpellUtil.effect(target, StatusEffects.SLOWNESS, 30, 2);
                            Fx.beam(ctx.world(), center.add(0.0, 2.0, 0.0),
                                    target.getPos().add(0.0, target.getHeight() * 0.5, 0.0),
                                    ParticleTypes.ELECTRIC_SPARK, 0.4);
                        }
                        return true;
                    });
                    return true;
                })
                .register();
    }

    /** Зов бури: меняет погоду и бьёт молнией по всем врагам поблизости. */
    private static void stormCall() {
        Spell.builder("storm_call", SpellSchool.STORM)
                .type(SpellCastType.UTILITY)
                .tier(4).cost(120.0f).cooldown(1200)
                .action(ctx -> {
                    ServerWorld world = ctx.world();
                    world.setWeather(0, 12000, true, true);

                    List<LivingEntity> targets = SpellUtil.enemiesAround(ctx, ctx.feet(), 20.0);
                    int struck = 0;
                    for (LivingEntity target : targets) {
                        if (struck >= 6) {
                            break;
                        }
                        Entity bolt = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
                        if (bolt instanceof LightningEntity lightning) {
                            lightning.refreshPositionAfterTeleport(target.getPos());
                            lightning.setChanneler(ctx.caster());
                            world.spawnEntity(lightning);
                            struck++;
                        }
                    }

                    Fx.helix(world, ctx.feet(), 2.0, 6.0, ParticleTypes.ELECTRIC_SPARK, 80, 3.0);
                    Fx.sound(world, ctx.feet(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.8f);
                    return true;
                })
                .register();
    }
}
