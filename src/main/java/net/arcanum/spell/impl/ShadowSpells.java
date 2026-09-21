package net.arcanum.spell.impl;

import net.arcanum.registry.ModEffects;
import net.arcanum.registry.ModItems;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCastType;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.arcanum.spell.util.SpellTicker;
import net.arcanum.spell.util.SpellUtil;
import net.arcanum.spell.util.SummonManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Магия тени: чужая жизнь как ресурс. */
public final class ShadowSpells {
    private ShadowSpells() {
    }

    public static void init() {
        shadowBolt();
        lifeDrain();
        curseOfDecay();
        veil();
        summonShades();
        soulHarvest();
    }

    /** Теневой сгусток — снаряд, накладывающий иссушение. */
    private static void shadowBolt() {
        Spell.builder("shadow_bolt", SpellSchool.SHADOW)
                .type(SpellCastType.PROJECTILE)
                .tier(1).cost(10.0f).cooldown(14)
                .action(ctx -> {
                    SpellUtil.shoot(ctx, ModItems.RUNES.get(SpellSchool.SHADOW), SpellSchool.SHADOW,
                            ParticleTypes.SMOKE, 1.5f, 0.02f, 0.0f,
                            (world, projectile, pos, hit) -> {
                                if (hit != null) {
                                    SpellUtil.damage(ctx, hit, ctx.scaled(5.0f));
                                    SpellUtil.effect(hit, StatusEffects.WITHER, 5 * 20, 0);
                                }
                                Fx.burst(world, pos, ParticleTypes.LARGE_SMOKE, 15, 0.3, 0.02);
                                Fx.sound(world, pos, SoundEvents.ENTITY_WITHER_SHOOT, 0.5f, 1.6f);
                            });
                    return true;
                })
                .register();
    }

    /** Высасывание жизни: урон цели превращается в здоровье заклинателя. */
    private static void lifeDrain() {
        Spell.builder("life_drain", SpellSchool.SHADOW)
                .type(SpellCastType.TARGET)
                .tier(2).cost(28.0f).cooldown(80)
                .action(ctx -> {
                    LivingEntity target = SpellUtil.raycastEntity(ctx, 20.0);
                    if (target == null) {
                        return false;
                    }
                    float damage = ctx.scaled(7.0f);
                    if (!SpellUtil.damage(ctx, target, damage)) {
                        return false;
                    }
                    ctx.caster().heal(damage * 0.6f);

                    Fx.beam(ctx.world(), target.getPos().add(0.0, target.getHeight() * 0.5, 0.0),
                            ctx.eyes(), Fx.dust(SpellSchool.SHADOW.color(), 1.1f), 0.2);
                    Fx.sound(ctx.world(), target.getPos(), SoundEvents.ENTITY_WITHER_HURT, 0.7f, 1.4f);
                    return true;
                })
                .register();
    }

    /** Проклятие распада: слабость, иссушение и выжигание маны. */
    private static void curseOfDecay() {
        Spell.builder("curse_of_decay", SpellSchool.SHADOW)
                .type(SpellCastType.TARGET)
                .tier(3).cost(42.0f).cooldown(180)
                .action(ctx -> {
                    LivingEntity target = SpellUtil.raycastEntity(ctx, 24.0);
                    if (target == null) {
                        return false;
                    }
                    int duration = 15 * 20;
                    SpellUtil.effect(target, StatusEffects.WITHER, duration, 1);
                    SpellUtil.effect(target, StatusEffects.WEAKNESS, duration, 1);
                    SpellUtil.effect(target, ModEffects.MANA_BURN, duration, 1);
                    SpellUtil.effect(target, ModEffects.SILENCE, duration / 2, 0);

                    SpellTicker.schedule(ctx.world(), duration, 10, elapsed -> {
                        if (!target.isAlive()) {
                            return false;
                        }
                        Fx.sphere(ctx.world(), target.getPos().add(0.0, target.getHeight() * 0.5, 0.0),
                                0.8, ParticleTypes.SQUID_INK, 6);
                        return true;
                    });
                    Fx.sound(ctx.world(), target.getPos(), SoundEvents.ENTITY_WITHER_AMBIENT, 0.8f, 1.6f);
                    return true;
                })
                .register();
    }

    /** Покров тьмы: невидимость и скорость, пока не ударишь. */
    private static void veil() {
        Spell.builder("veil", SpellSchool.SHADOW)
                .type(SpellCastType.SELF)
                .tier(2).cost(30.0f).cooldown(300)
                .action(ctx -> {
                    int duration = 20 * 20;
                    SpellUtil.effect(ctx.caster(), StatusEffects.INVISIBILITY, duration, 0);
                    SpellUtil.effect(ctx.caster(), StatusEffects.SPEED, duration, 1);
                    Fx.burst(ctx.world(), ctx.feet().add(0.0, 1.0, 0.0),
                            ParticleTypes.LARGE_SMOKE, 40, 0.6, 0.02);
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 0.9f);
                    return true;
                })
                .register();
    }

    /** Призыв теней: три призрака на минуту. */
    private static void summonShades() {
        Spell.builder("summon_shades", SpellSchool.SHADOW)
                .type(SpellCastType.SUMMON)
                .tier(3).cost(60.0f).cooldown(500)
                .action(ctx -> {
                    int summoned = 0;
                    for (int i = 0; i < 3; i++) {
                        double angle = i * Math.PI * 2 / 3;
                        Vec3d spot = ctx.feet().add(Math.cos(angle) * 2.0, 1.0, Math.sin(angle) * 2.0);
                        if (SummonManager.summon(ctx.world(), EntityType.VEX, ctx.caster(), spot,
                                60 * 20, Text.translatable("entity.arcanum.shade"), 1.8f) != null) {
                            summoned++;
                        }
                    }
                    if (summoned == 0) {
                        return false;
                    }
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.ENTITY_VEX_CHARGE, 1.2f, 0.8f);
                    return true;
                })
                .register();
    }

    /** Жатва душ: бьёт всех вокруг и лечит за каждую задетую цель. */
    private static void soulHarvest() {
        Spell.builder("soul_harvest", SpellSchool.SHADOW)
                .type(SpellCastType.AREA)
                .tier(4).cost(95.0f).cooldown(500)
                .action(ctx -> {
                    Vec3d center = ctx.feet().add(0.0, 1.0, 0.0);
                    List<LivingEntity> targets = SpellUtil.enemiesAround(ctx, center, 8.0);
                    if (targets.isEmpty()) {
                        return false;
                    }

                    int hits = 0;
                    for (LivingEntity target : targets) {
                        if (SpellUtil.damage(ctx, target, ctx.scaled(9.0f))) {
                            SpellUtil.effect(target, StatusEffects.WITHER, 8 * 20, 1);
                            Fx.beam(ctx.world(), target.getPos().add(0.0, target.getHeight() * 0.5, 0.0),
                                    center, ParticleTypes.SOUL, 0.3);
                            hits++;
                        }
                    }
                    ctx.caster().heal(hits * 2.5f);
                    SpellUtil.effect(ctx.caster(), StatusEffects.ABSORPTION, 20 * 20, Math.min(3, hits));

                    Fx.sphere(ctx.world(), center, 2.5, ParticleTypes.SOUL_FIRE_FLAME, 60);
                    Fx.sound(ctx.world(), center, SoundEvents.PARTICLE_SOUL_ESCAPE, 1.4f, 0.6f);
                    return true;
                })
                .register();
    }
}
