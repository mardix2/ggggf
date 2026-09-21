package net.arcanum.spell.impl;

import net.arcanum.registry.ModEffects;
import net.arcanum.registry.ModItems;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCastType;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.arcanum.spell.util.SpellTicker;
import net.arcanum.spell.util.SpellUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Криомантия: контроль над полем боя, а не голый урон. */
public final class FrostSpells {
    private FrostSpells() {
    }

    /** Промёрзшая цель получает от школы льда в полтора раза больше. */
    private static float chillBonus(LivingEntity target, float damage) {
        return target.hasStatusEffect(ModEffects.CHILLED) ? damage * 1.5f : damage;
    }

    public static void init() {
        iceShard();
        frostNova();
        iceWall();
        glacialArmor();
        deepFreeze();
        blizzard();
    }

    /** Ледяная игла — базовый снаряд школы. */
    private static void iceShard() {
        Spell.builder("ice_shard", SpellSchool.FROST)
                .type(SpellCastType.PROJECTILE)
                .tier(1).cost(9.0f).cooldown(12)
                .action(ctx -> {
                    SpellUtil.shoot(ctx, ModItems.RUNES.get(SpellSchool.FROST), SpellSchool.FROST,
                            ParticleTypes.SNOWFLAKE, 1.8f, 0.03f, 0.0f,
                            (world, projectile, pos, hit) -> {
                                if (hit != null) {
                                    SpellUtil.frostDamage(ctx, hit, chillBonus(hit, ctx.scaled(5.0f)), 80);
                                    SpellUtil.effect(hit, ModEffects.CHILLED, 100, 0);
                                }
                                Fx.burst(world, pos, ParticleTypes.SNOWFLAKE, 18, 0.3, 0.03);
                                Fx.sound(world, pos, SoundEvents.BLOCK_GLASS_BREAK, 0.7f, 1.6f);
                            });
                    Fx.sound(ctx.world(), ctx.eyes(), SoundEvents.BLOCK_POWDER_SNOW_BREAK, 0.7f, 1.5f);
                    return true;
                })
                .register();
    }

    /** Вспышка мороза вокруг заклинателя — способ разорвать дистанцию. */
    private static void frostNova() {
        Spell.builder("frost_nova", SpellSchool.FROST)
                .type(SpellCastType.AREA)
                .tier(1).cost(22.0f).cooldown(60)
                .action(ctx -> {
                    Vec3d center = ctx.feet().add(0.0, 0.5, 0.0);
                    for (LivingEntity target : SpellUtil.enemiesAround(ctx, center, 6.0)) {
                        SpellUtil.frostDamage(ctx, target, chillBonus(target, ctx.scaled(4.0f)), 120);
                        SpellUtil.effect(target, ModEffects.CHILLED, 160, 0);
                        SpellUtil.effect(target, StatusEffects.SLOWNESS, 160, 2);
                        SpellUtil.knockback(target, center, 0.3);
                    }
                    for (int r = 1; r <= 6; r++) {
                        Fx.groundRing(ctx.world(), center, r, ParticleTypes.SNOWFLAKE, r * 8);
                    }
                    Fx.sound(ctx.world(), center, SoundEvents.BLOCK_GLASS_BREAK, 1.2f, 0.7f);
                    return true;
                })
                .register();
    }

    /** Ледяная стена: временное укрытие прямо перед собой. */
    private static void iceWall() {
        Spell.builder("ice_wall", SpellSchool.FROST)
                .type(SpellCastType.UTILITY)
                .tier(2).cost(28.0f).cooldown(100)
                .action(ctx -> {
                    Vec3d look = ctx.look();
                    Vec3d base = ctx.feet().add(look.x * 2.5, 0.0, look.z * 2.5);
                    // Стена строится поперёк направления взгляда.
                    Vec3d side = new Vec3d(-look.z, 0.0, look.x).normalize();

                    int placed = 0;
                    for (int offset = -2; offset <= 2; offset++) {
                        for (int height = 0; height < 3; height++) {
                            BlockPos pos = BlockPos.ofFloored(base.add(side.multiply(offset)))
                                    .up(height);
                            if (SpellUtil.placeTemporary(ctx.world(), pos,
                                    Blocks.PACKED_ICE.getDefaultState(), 20 * 20)) {
                                placed++;
                            }
                        }
                    }
                    if (placed == 0) {
                        return false;
                    }
                    Fx.sound(ctx.world(), base, SoundEvents.BLOCK_GLASS_PLACE, 1.0f, 0.6f);
                    Fx.burst(ctx.world(), base.add(0.0, 1.0, 0.0), ParticleTypes.SNOWFLAKE, 40, 1.5, 0.02);
                    return true;
                })
                .register();
    }

    /** Ледяной доспех: сопротивление урону и иммунитет к заморозке. */
    private static void glacialArmor() {
        Spell.builder("glacial_armor", SpellSchool.FROST)
                .type(SpellCastType.SELF)
                .tier(2).cost(38.0f).cooldown(360)
                .action(ctx -> {
                    int duration = 20 * 20;
                    SpellUtil.effect(ctx.caster(), StatusEffects.RESISTANCE, duration, 1);
                    SpellUtil.effect(ctx.caster(), StatusEffects.ABSORPTION, duration, 2);
                    ctx.caster().setFrozenTicks(0);

                    SpellTicker.schedule(ctx.world(), duration, 20, elapsed -> {
                        if (!ctx.caster().isAlive()) {
                            return false;
                        }
                        // Иней стряхивает заморозку и подмораживает тех, кто рядом.
                        ctx.caster().setFrozenTicks(0);
                        Fx.sphere(ctx.world(), ctx.caster().getPos().add(0.0, 1.0, 0.0), 1.1,
                                ParticleTypes.SNOWFLAKE, 12);
                        for (LivingEntity target : SpellUtil.enemiesAround(ctx,
                                ctx.caster().getPos(), 2.5)) {
                            SpellUtil.effect(target, ModEffects.CHILLED, 60, 0);
                        }
                        return true;
                    });
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.BLOCK_POWDER_SNOW_PLACE, 1.0f, 0.7f);
                    return true;
                })
                .register();
    }

    /** Глубокая заморозка: цель не может сдвинуться с места. */
    private static void deepFreeze() {
        Spell.builder("deep_freeze", SpellSchool.FROST)
                .type(SpellCastType.TARGET)
                .tier(3).cost(45.0f).cooldown(160)
                .action(ctx -> {
                    LivingEntity target = SpellUtil.raycastEntity(ctx, 24.0);
                    if (target == null) {
                        return false;
                    }
                    int duration = 5 * 20;
                    SpellUtil.frostDamage(ctx, target, chillBonus(target, ctx.scaled(7.0f)), 200);
                    SpellUtil.effect(target, StatusEffects.SLOWNESS, duration, 6);
                    SpellUtil.effect(target, StatusEffects.MINING_FATIGUE, duration, 3);
                    SpellUtil.effect(target, ModEffects.CHILLED, duration * 2, 1);

                    SpellTicker.schedule(ctx.world(), duration, 2, elapsed -> {
                        if (!target.isAlive()) {
                            return false;
                        }
                        // Удерживаем цель на месте: скорость обнуляется каждый тик.
                        target.setVelocity(0.0, target.getVelocity().y, 0.0);
                        target.velocityModified = true;
                        Fx.sphere(ctx.world(), target.getPos().add(0.0, target.getHeight() * 0.5, 0.0),
                                0.7, ParticleTypes.SNOWFLAKE, 6);
                        return true;
                    });
                    Fx.sound(ctx.world(), target.getPos(), SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                    return true;
                })
                .register();
    }

    /** Метель: двенадцать секунд холода на большой площади. */
    private static void blizzard() {
        Spell.builder("blizzard", SpellSchool.FROST)
                .type(SpellCastType.LINGERING)
                .tier(4).cost(100.0f).cooldown(600)
                .action(ctx -> {
                    Vec3d center = SpellUtil.aimPoint(ctx, 24.0);
                    int duration = 12 * 20;
                    Fx.sound(ctx.world(), center, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, 1.6f, 0.6f);

                    SpellTicker.schedule(ctx.world(), duration, 10, elapsed -> {
                        Fx.burst(ctx.world(), center.add(0.0, 2.0, 0.0),
                                ParticleTypes.SNOWFLAKE, 40, 4.0, 0.05);
                        Fx.ring(ctx.world(), center, 8.0, ParticleTypes.ITEM_SNOWBALL, 24);
                        for (LivingEntity target : SpellUtil.enemiesAround(ctx, center, 8.0)) {
                            SpellUtil.frostDamage(ctx, target, chillBonus(target, ctx.scaled(2.5f)), 60);
                            SpellUtil.effect(target, ModEffects.CHILLED, 60, 0);
                            SpellUtil.effect(target, StatusEffects.SLOWNESS, 60, 1);
                        }
                        return true;
                    });
                    return true;
                })
                .register();
    }
}
