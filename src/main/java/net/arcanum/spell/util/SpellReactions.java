package net.arcanum.spell.util;

import net.arcanum.registry.ModEffects;
import net.arcanum.spell.SpellContext;
import net.arcanum.spell.SpellSchool;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Реакции школ.
 *
 * <p>Любое заклинание оставляет на цели «метку» своей школы. Если следующим
 * ударит другая школа — метка срабатывает. Это и есть главная причина
 * держать в запасе не одну школу, а две: связка бьёт заметно сильнее, чем
 * то же количество отдельных заклинаний.
 */
public final class SpellReactions {
    private SpellReactions() {
    }

    /** Сколько держится метка школы. */
    private static final int MARK_TICKS = 120;

    /**
     * Защита от рекурсии: реакция сама наносит урон, а урон снова зовёт
     * реакции. Сервер однопоточный, поэтому хватает обычного флага.
     */
    private static boolean reacting;

    /** Вызывается после каждого попадания заклинанием. */
    public static void onHit(SpellContext ctx, LivingEntity target) {
        if (reacting || !target.isAlive()) {
            return;
        }
        reacting = true;
        try {
            react(ctx, target);
        } finally {
            reacting = false;
        }
        mark(ctx, target);
    }

    // ------------------------------------------------------------------
    //  Метки
    // ------------------------------------------------------------------

    private static void mark(SpellContext ctx, LivingEntity target) {
        switch (ctx.school()) {
            case FIRE -> {
                if (!target.isFireImmune()) {
                    target.setOnFireForTicks(Math.max(target.getFireTicks(), 60));
                }
            }
            case FROST -> SpellUtil.effect(target, ModEffects.CHILLED, MARK_TICKS, 0);
            case STORM -> SpellUtil.effect(target, ModEffects.CHARGED, MARK_TICKS, 0);
            case ARCANE -> SpellUtil.effect(target, ModEffects.DESTABILIZED, MARK_TICKS, 0);
            case NATURE -> SpellUtil.effect(target, StatusEffects.POISON, MARK_TICKS / 2, 0);
            case SHADOW -> SpellUtil.effect(target, StatusEffects.WITHER, MARK_TICKS / 2, 0);
            case LIGHT -> SpellUtil.effect(target, StatusEffects.GLOWING, MARK_TICKS, 0);
        }
    }

    // ------------------------------------------------------------------
    //  Реакции
    // ------------------------------------------------------------------

    private static void react(SpellContext ctx, LivingEntity target) {
        SpellSchool school = ctx.school();
        Vec3d at = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);

        if (school == SpellSchool.FIRE && target.hasStatusEffect(ModEffects.CHILLED)) {
            target.removeStatusEffect(ModEffects.CHILLED);
            SpellUtil.damage(ctx, target, ctx.scaled(8.0f));
            Fx.sphere(ctx.world(), at, 1.2, ParticleTypes.EXPLOSION, 8);
            Fx.burst(ctx.world(), at, ParticleTypes.CLOUD, 25, 0.6, 0.05);
            announce(ctx, "thermal_shock", SoundEvents.ENTITY_GENERIC_EXPLODE, 1.4f);
            return;
        }

        if (school == SpellSchool.FROST && target.isOnFire()) {
            target.extinguish();
            SpellUtil.damage(ctx, target, ctx.scaled(6.0f));
            SpellUtil.effect(target, StatusEffects.SLOWNESS, 120, 2);
            Fx.burst(ctx.world(), at, ParticleTypes.CLOUD, 40, 0.7, 0.03);
            announce(ctx, "quench", SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.2f);
            return;
        }

        if (school == SpellSchool.STORM && target.hasStatusEffect(ModEffects.CHILLED)) {
            // Лёд проводит разряд дальше: бьёт по ближайшим целям.
            int hit = 0;
            for (LivingEntity other : SpellUtil.enemiesAround(ctx, at, 6.0)) {
                if (other == target || hit >= 3) {
                    continue;
                }
                Fx.beam(ctx.world(), at, other.getPos().add(0.0, other.getHeight() * 0.5, 0.0),
                        ParticleTypes.ELECTRIC_SPARK, 0.25);
                SpellUtil.damage(ctx, other, ctx.scaled(5.0f));
                hit++;
            }
            if (hit > 0) {
                announce(ctx, "conduction", SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.6f);
            }
            return;
        }

        if (school == SpellSchool.ARCANE && target.hasStatusEffect(ModEffects.CHARGED)) {
            target.removeStatusEffect(ModEffects.CHARGED);
            SpellUtil.explode(ctx, at, 3.5, ctx.scaled(7.0f), 0.8);
            Fx.sphere(ctx.world(), at, 2.0, Fx.dust(SpellSchool.ARCANE.color(), 1.3f), 40);
            announce(ctx, "resonance", SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f);
            return;
        }

        if (school == SpellSchool.SHADOW && target.hasStatusEffect(StatusEffects.POISON)) {
            target.removeStatusEffect(StatusEffects.POISON);
            SpellUtil.damage(ctx, target, ctx.scaled(5.0f));
            SpellUtil.effect(target, StatusEffects.WITHER, 200, 1);
            Fx.sphere(ctx.world(), at, 1.0, ParticleTypes.SQUID_INK, 25);
            announce(ctx, "rot", SoundEvents.ENTITY_WITHER_HURT, 1.0f);
            return;
        }

        if (school == SpellSchool.NATURE && target.hasStatusEffect(ModEffects.DESTABILIZED)) {
            target.removeStatusEffect(ModEffects.DESTABILIZED);
            SpellUtil.placeTemporary(ctx.world(), target.getBlockPos(),
                    Blocks.COBWEB.getDefaultState(), 200);
            SpellUtil.effect(target, StatusEffects.SLOWNESS, 160, 3);
            Fx.groundRing(ctx.world(), target.getPos(), 1.5, ParticleTypes.COMPOSTER, 16);
            announce(ctx, "overgrowth", SoundEvents.BLOCK_SWEET_BERRY_BUSH_PLACE, 0.8f);
            return;
        }

        if (school == SpellSchool.LIGHT && target.hasStatusEffect(ModEffects.DESTABILIZED)) {
            target.removeStatusEffect(ModEffects.DESTABILIZED);
            SpellUtil.damage(ctx, target, ctx.scaled(11.0f));
            Fx.burst(ctx.world(), at, ParticleTypes.FLASH, 1, 0.0, 0.0);
            Fx.sphere(ctx.world(), at, 1.6, ParticleTypes.END_ROD, 40);
            announce(ctx, "dispersion", SoundEvents.ITEM_TRIDENT_THUNDER, 1.4f);
        }
    }

    private static void announce(SpellContext ctx, String key,
                                 net.minecraft.sound.SoundEvent sound, float pitch) {
        Fx.sound(ctx.world(), ctx.caster().getPos(), sound, 0.9f, pitch);
        ctx.caster().sendMessage(Text.translatable("reaction.arcanum." + key)
                .formatted(Formatting.AQUA), true);
    }

    /** Список реакций для справки — используется командой и подсказками. */
    public static List<String> names() {
        return List.of("thermal_shock", "quench", "conduction", "resonance",
                "rot", "overgrowth", "dispersion");
    }
}
