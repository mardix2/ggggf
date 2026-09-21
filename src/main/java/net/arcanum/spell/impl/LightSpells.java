package net.arcanum.spell.impl;

import net.arcanum.registry.ModEffects;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCastType;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.arcanum.spell.util.SpellUtil;
import net.arcanum.spell.util.SummonManager;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Магия света: защита, поддержка и кара для нежити. */
public final class LightSpells {
    private LightSpells() {
    }

    /** По нежити свет бьёт вдвое сильнее — это основной приём школы. */
    private static float undeadBonus(LivingEntity target, float damage) {
        return target.getType().isIn(EntityTypeTags.UNDEAD) ? damage * 2.0f : damage;
    }

    public static void init() {
        smite();
        holyLight();
        radiance();
        blessing();
        guardian();
        purify();
    }

    /** Кара: точечный удар, для нежити — смертельный. */
    private static void smite() {
        Spell.builder("smite", SpellSchool.LIGHT)
                .type(SpellCastType.TARGET)
                .tier(1).cost(16.0f).cooldown(30)
                .action(ctx -> {
                    LivingEntity target = SpellUtil.raycastEntity(ctx, 24.0);
                    if (target == null) {
                        return false;
                    }
                    SpellUtil.damage(ctx, target, undeadBonus(target, ctx.scaled(6.0f)));

                    Vec3d top = target.getPos().add(0.0, target.getHeight() + 3.0, 0.0);
                    Fx.beam(ctx.world(), top, target.getPos(), ParticleTypes.END_ROD, 0.2);
                    Fx.burst(ctx.world(), target.getPos(), ParticleTypes.FLASH, 1, 0.0, 0.0);
                    Fx.sound(ctx.world(), target.getPos(), SoundEvents.ITEM_TRIDENT_THUNDER, 0.8f, 1.6f);
                    return true;
                })
                .register();
    }

    /** Священный свет: лечит союзников и жжёт нежить вокруг. */
    private static void holyLight() {
        Spell.builder("holy_light", SpellSchool.LIGHT)
                .type(SpellCastType.AREA)
                .tier(2).cost(38.0f).cooldown(160)
                .action(ctx -> {
                    Vec3d center = ctx.feet().add(0.0, 1.0, 0.0);
                    for (LivingEntity ally : SpellUtil.alliesAround(ctx, center, 8.0)) {
                        ally.heal(ctx.scaled(4.0f));
                    }
                    ctx.caster().heal(ctx.scaled(4.0f));

                    for (LivingEntity target : SpellUtil.enemiesAround(ctx, center, 8.0)) {
                        if (target.getType().isIn(EntityTypeTags.UNDEAD)) {
                            SpellUtil.damage(ctx, target, ctx.scaled(10.0f));
                            target.setOnFireForTicks(100);
                        } else {
                            SpellUtil.effect(target, StatusEffects.BLINDNESS, 5 * 20, 0);
                        }
                    }

                    Fx.sphere(ctx.world(), center, 3.0, ParticleTypes.END_ROD, 80);
                    Fx.burst(ctx.world(), center, ParticleTypes.FLASH, 1, 0.0, 0.0);
                    Fx.sound(ctx.world(), center, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.4f, 1.6f);
                    return true;
                })
                .register();
    }

    /** Сияние: расставляет вокруг источники света на полминуты. */
    private static void radiance() {
        Spell.builder("radiance", SpellSchool.LIGHT)
                .type(SpellCastType.UTILITY)
                .tier(1).cost(14.0f).cooldown(80)
                .action(ctx -> {
                    BlockPos center = ctx.caster().getBlockPos();
                    List<BlockPos> spots = new ArrayList<>();
                    spots.add(center.up(2));
                    spots.add(center.add(4, 2, 0));
                    spots.add(center.add(-4, 2, 0));
                    spots.add(center.add(0, 2, 4));
                    spots.add(center.add(0, 2, -4));

                    int placed = 0;
                    for (BlockPos pos : spots) {
                        if (SpellUtil.placeTemporary(ctx.world(), pos,
                                Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15), 30 * 20)) {
                            placed++;
                        }
                    }
                    if (placed == 0) {
                        return false;
                    }
                    Fx.sphere(ctx.world(), ctx.feet().add(0.0, 1.5, 0.0), 2.0, ParticleTypes.END_ROD, 40);
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
                    return true;
                })
                .register();
    }

    /** Благословение: усиливает магию и защищает — себя и союзников рядом. */
    private static void blessing() {
        Spell.builder("blessing", SpellSchool.LIGHT)
                .type(SpellCastType.SELF)
                .tier(3).cost(50.0f).cooldown(400)
                .action(ctx -> {
                    int duration = 30 * 20;
                    List<LivingEntity> allies = SpellUtil.alliesAround(ctx, ctx.feet(), 10.0);
                    allies.add(ctx.caster());
                    for (LivingEntity ally : allies) {
                        SpellUtil.effect(ally, ModEffects.BLESSED, duration, 1);
                        SpellUtil.effect(ally, StatusEffects.RESISTANCE, duration, 0);
                        SpellUtil.effect(ally, StatusEffects.STRENGTH, duration, 0);
                    }
                    Fx.helix(ctx.world(), ctx.feet(), 1.5, 3.0, ParticleTypes.END_ROD, 70, 3.0);
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.BLOCK_BEACON_ACTIVATE, 1.4f, 1.4f);
                    return true;
                })
                .register();
    }

    /** Страж: железный голем на две минуты. */
    private static void guardian() {
        Spell.builder("guardian", SpellSchool.LIGHT)
                .type(SpellCastType.SUMMON)
                .tier(4).cost(90.0f).cooldown(700)
                .action(ctx -> {
                    Vec3d spot = SpellUtil.safeSpot(ctx.world(), ctx.feet().add(ctx.look().x * 2.0, 0.0,
                            ctx.look().z * 2.0));
                    if (SummonManager.summon(ctx.world(), EntityType.IRON_GOLEM, ctx.caster(), spot,
                            120 * 20, Text.translatable("entity.arcanum.guardian"), 1.2f) == null) {
                        return false;
                    }
                    Fx.sphere(ctx.world(), spot.add(0.0, 1.0, 0.0), 1.5, ParticleTypes.END_ROD, 60);
                    Fx.sound(ctx.world(), spot, SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
                    return true;
                })
                .register();
    }

    /** Очищение: снимает все вредные эффекты и тушит огонь. */
    private static void purify() {
        Spell.builder("purify", SpellSchool.LIGHT)
                .type(SpellCastType.SELF)
                .tier(2).cost(26.0f).cooldown(200)
                .action(ctx -> {
                    List<StatusEffectInstance> harmful = new ArrayList<>();
                    for (StatusEffectInstance instance : ctx.caster().getStatusEffects()) {
                        if (instance.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                            harmful.add(instance);
                        }
                    }
                    boolean burning = ctx.caster().isOnFire();
                    if (harmful.isEmpty() && !burning) {
                        return false;
                    }

                    for (StatusEffectInstance instance : harmful) {
                        ctx.caster().removeStatusEffect(instance.getEffectType());
                    }
                    ctx.caster().extinguish();
                    ctx.caster().setFrozenTicks(0);
                    ctx.caster().heal(ctx.scaled(3.0f));

                    Fx.helix(ctx.world(), ctx.feet(), 0.8, 2.2, ParticleTypes.END_ROD, 40, 2.0);
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.BLOCK_CONDUIT_DEACTIVATE, 1.0f, 1.8f);
                    return true;
                })
                .register();
    }
}
