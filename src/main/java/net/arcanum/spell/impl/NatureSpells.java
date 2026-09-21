package net.arcanum.spell.impl;

import net.arcanum.registry.ModItems;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCastType;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.arcanum.spell.util.SpellUtil;
import net.arcanum.spell.util.SummonManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Магия природы: лечение, рост и помощь зверей. */
public final class NatureSpells {
    private NatureSpells() {
    }

    public static void init() {
        mend();
        healingBloom();
        entangle();
        verdantGrowth();
        thornVolley();
        callOfTheWild();
    }

    /** Простое лечение себя. */
    private static void mend() {
        Spell.builder("mend", SpellSchool.NATURE)
                .type(SpellCastType.SELF)
                .tier(1).cost(18.0f).cooldown(60)
                .action(ctx -> {
                    if (ctx.caster().getHealth() >= ctx.caster().getMaxHealth()) {
                        return false;
                    }
                    ctx.caster().heal(ctx.scaled(6.0f));
                    Fx.helix(ctx.world(), ctx.feet(), 0.7, 2.0, ParticleTypes.HAPPY_VILLAGER, 30, 2.0);
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.6f);
                    return true;
                })
                .register();
    }

    /** Цветение жизни: лечит всех союзников вокруг и даёт регенерацию. */
    private static void healingBloom() {
        Spell.builder("healing_bloom", SpellSchool.NATURE)
                .type(SpellCastType.AREA)
                .tier(2).cost(40.0f).cooldown(200)
                .action(ctx -> {
                    Vec3d center = ctx.feet();
                    for (LivingEntity ally : SpellUtil.alliesAround(ctx, center, 8.0)) {
                        ally.heal(ctx.scaled(5.0f));
                        SpellUtil.effect(ally, StatusEffects.REGENERATION, 8 * 20, 1);
                    }
                    ctx.caster().heal(ctx.scaled(5.0f));
                    SpellUtil.effect(ctx.caster(), StatusEffects.REGENERATION, 8 * 20, 1);

                    for (int r = 1; r <= 8; r += 2) {
                        Fx.groundRing(ctx.world(), center, r, ParticleTypes.HAPPY_VILLAGER, r * 6);
                    }
                    Fx.sound(ctx.world(), center, SoundEvents.BLOCK_BEEHIVE_EXIT, 1.2f, 1.2f);
                    return true;
                })
                .register();
    }

    /** Опутывание: паутина под ногами у всех врагов поблизости. */
    private static void entangle() {
        Spell.builder("entangle", SpellSchool.NATURE)
                .type(SpellCastType.AREA)
                .tier(2).cost(26.0f).cooldown(120)
                .action(ctx -> {
                    Vec3d center = SpellUtil.aimPoint(ctx.caster(), 20.0);
                    boolean caught = false;
                    for (LivingEntity target : SpellUtil.enemiesAround(ctx, center, 6.0)) {
                        BlockPos pos = target.getBlockPos();
                        SpellUtil.placeTemporary(ctx.world(), pos, Blocks.COBWEB.getDefaultState(), 12 * 20);
                        SpellUtil.effect(target, StatusEffects.SLOWNESS, 6 * 20, 2);
                        SpellUtil.damage(ctx, target, ctx.scaled(2.0f));
                        caught = true;
                    }
                    Fx.groundRing(ctx.world(), center, 6.0, ParticleTypes.COMPOSTER, 30);
                    Fx.sound(ctx.world(), center, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PLACE, 1.0f, 0.7f);
                    return caught;
                })
                .register();
    }

    /** Буйный рост: как мешок костной муки, но по площади. */
    private static void verdantGrowth() {
        Spell.builder("verdant_growth", SpellSchool.NATURE)
                .type(SpellCastType.UTILITY)
                .tier(1).cost(24.0f).cooldown(100)
                .action(ctx -> {
                    BlockPos center = BlockPos.ofFloored(SpellUtil.aimPoint(ctx.caster(), 16.0));
                    ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
                    int grown = 0;

                    for (BlockPos pos : BlockPos.iterate(center.add(-4, -2, -4), center.add(4, 2, 4))) {
                        // useOnFertilizable сам проверит, что блок вообще растёт.
                        if (BoneMealItem.useOnFertilizable(boneMeal, ctx.world(), pos.toImmutable())) {
                            grown++;
                        }
                    }
                    if (grown == 0) {
                        return false;
                    }

                    Fx.burst(ctx.world(), Vec3d.ofCenter(center), ParticleTypes.HAPPY_VILLAGER,
                            60, 3.0, 0.05);
                    Fx.sound(ctx.world(), Vec3d.ofCenter(center), SoundEvents.ITEM_BONE_MEAL_USE, 1.2f, 1.0f);
                    SpellUtil.message(ctx, Text.translatable("message.arcanum.growth", grown));
                    return true;
                })
                .register();
    }

    /** Залп шипов: пять снарядов веером. */
    private static void thornVolley() {
        Spell.builder("thorn_volley", SpellSchool.NATURE)
                .type(SpellCastType.PROJECTILE)
                .tier(2).cost(22.0f).cooldown(40)
                .action(ctx -> {
                    for (int i = 0; i < 5; i++) {
                        SpellUtil.shoot(ctx, ModItems.RUNES.get(SpellSchool.NATURE), SpellSchool.NATURE,
                                ParticleTypes.COMPOSTER, 1.7f, 0.05f, 14.0f,
                                (world, projectile, pos, hit) -> {
                                    if (hit != null) {
                                        SpellUtil.damage(ctx, hit, ctx.scaled(3.0f));
                                        SpellUtil.effect(hit, StatusEffects.POISON, 5 * 20, 0);
                                    }
                                    Fx.burst(world, pos, ParticleTypes.COMPOSTER, 8, 0.2, 0.05);
                                });
                    }
                    Fx.sound(ctx.world(), ctx.eyes(), SoundEvents.ENTITY_ARROW_SHOOT, 1.0f, 0.8f);
                    return true;
                })
                .register();
    }

    /** Зов дикой природы: два волка на полторы минуты. */
    private static void callOfTheWild() {
        Spell.builder("call_of_the_wild", SpellSchool.NATURE)
                .type(SpellCastType.SUMMON)
                .tier(3).cost(55.0f).cooldown(400)
                .action(ctx -> {
                    int summoned = 0;
                    for (int i = 0; i < 2; i++) {
                        Vec3d spot = SpellUtil.safeSpot(ctx.world(),
                                ctx.feet().add(i == 0 ? 1.5 : -1.5, 0.0, i == 0 ? 1.5 : -1.5));
                        if (SummonManager.summon(ctx.world(), EntityType.WOLF, ctx.caster(), spot,
                                90 * 20, Text.translatable("entity.arcanum.summoned_wolf"), 1.5f) != null) {
                            summoned++;
                        }
                    }
                    if (summoned == 0) {
                        return false;
                    }
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.ENTITY_WOLF_HOWL, 1.2f, 1.0f);
                    return true;
                })
                .register();
    }
}
