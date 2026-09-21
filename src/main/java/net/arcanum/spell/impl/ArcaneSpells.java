package net.arcanum.spell.impl;

import net.arcanum.mana.ManaAttachments;
import net.arcanum.mana.ManaManager;
import net.arcanum.registry.ModEffects;
import net.arcanum.registry.ModItems;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCastType;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.arcanum.spell.util.SpellTicker;
import net.arcanum.spell.util.SpellUtil;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Чистая магия: перемещение, сила, защита и знание. */
public final class ArcaneSpells {
    private ArcaneSpells() {
    }

    /** Больше искр разом — и кадр проседает, и в глазах рябит. */
    private static final int MAX_PROSPECT_HITS = 64;

    public static void init() {
        magicMissile();
        blink();
        arcaneExplosion();
        telekinesis();
        manaShield();
        arcaneSight();
        prospect();
        recall();
    }

    /** Цвет искры над рудой: по названию блока, чтобы не заводить таблицу на каждый мод. */
    private static int oreColor(String path) {
        if (path.contains("diamond")) {
            return 0x5CE6E0;
        }
        if (path.contains("emerald")) {
            return 0x3CE06A;
        }
        if (path.contains("gold")) {
            return 0xF2C13B;
        }
        if (path.contains("redstone")) {
            return 0xE23B3B;
        }
        if (path.contains("lapis")) {
            return 0x2F5BE0;
        }
        if (path.contains("copper")) {
            return 0xE08A3B;
        }
        if (path.contains("coal")) {
            return 0x4A4A55;
        }
        if (path.contains("arcane")) {
            return SpellSchool.ARCANE.color();
        }
        return 0xD8D8E0;
    }

    /** Три самонаводящихся снаряда — надёжный, хоть и небыстрый урон. */
    private static void magicMissile() {
        Spell.builder("magic_missile", SpellSchool.ARCANE)
                .type(SpellCastType.PROJECTILE)
                .tier(1).cost(12.0f).cooldown(20)
                .action(ctx -> {
                    for (int i = 0; i < 3; i++) {
                        SpellUtil.shoot(ctx, ModItems.RUNES.get(SpellSchool.ARCANE), SpellSchool.ARCANE,
                                Fx.dust(SpellSchool.ARCANE.color(), 1.0f), 1.5f, 0.0f, 6.0f,
                                (world, projectile, pos, hit) -> {
                                    if (hit != null) {
                                        SpellUtil.damage(ctx, hit, ctx.scaled(3.5f));
                                    }
                                    Fx.burst(world, pos, ParticleTypes.WITCH, 10, 0.2, 0.05);
                                });
                    }
                    Fx.sound(ctx.world(), ctx.eyes(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 0.7f, 1.6f);
                    return true;
                })
                .register();
    }

    /** Рывок сквозь пространство на несколько блоков вперёд. */
    private static void blink() {
        Spell.builder("blink", SpellSchool.ARCANE)
                .type(SpellCastType.UTILITY)
                .tier(1).cost(16.0f).cooldown(40)
                .action(ctx -> {
                    Vec3d from = ctx.feet();
                    Vec3d target = SpellUtil.aimPoint(ctx, 16.0);
                    // Отходим от стены, в которую упёрся взгляд.
                    Vec3d adjusted = target.subtract(ctx.look().multiply(0.8));
                    Vec3d safe = SpellUtil.safeSpot(ctx.world(), adjusted);

                    SpellUtil.teleport(ctx.caster(), safe);
                    Fx.beam(ctx.world(), from.add(0.0, 1.0, 0.0), safe.add(0.0, 1.0, 0.0),
                            ParticleTypes.PORTAL, 0.3);
                    Fx.burst(ctx.world(), from.add(0.0, 1.0, 0.0), ParticleTypes.PORTAL, 30, 0.4, 0.1);
                    Fx.sound(ctx.world(), safe, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                    return true;
                })
                .register();
    }

    /** Разрыв чистой магии вокруг себя. */
    private static void arcaneExplosion() {
        Spell.builder("arcane_explosion", SpellSchool.ARCANE)
                .type(SpellCastType.AREA)
                .tier(2).cost(32.0f).cooldown(90)
                .action(ctx -> {
                    Vec3d center = ctx.feet().add(0.0, 1.0, 0.0);
                    SpellUtil.explode(ctx, center, 6.5, ctx.scaled(11.0f), 0.9);
                    Fx.sphere(ctx.world(), center, 3.0, Fx.dust(SpellSchool.ARCANE.color(), 1.4f), 80);
                    Fx.sphere(ctx.world(), center, 1.5, ParticleTypes.WITCH, 40);
                    Fx.sound(ctx.world(), center, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.4f, 0.8f);
                    return true;
                })
                .register();
    }

    /** Телекинез: притягивает цель к себе, а в приседе — отталкивает. */
    private static void telekinesis() {
        Spell.builder("telekinesis", SpellSchool.ARCANE)
                .type(SpellCastType.TARGET)
                .tier(2).cost(20.0f).cooldown(50)
                .action(ctx -> {
                    LivingEntity target = SpellUtil.raycastEntity(ctx, 24.0);
                    if (target == null) {
                        return false;
                    }
                    boolean push = ctx.caster().isSneaking();
                    Vec3d caster = ctx.feet();
                    if (push) {
                        SpellUtil.knockback(target, caster, 2.2);
                    } else {
                        Vec3d pull = caster.subtract(target.getPos()).normalize().multiply(1.1);
                        target.addVelocity(pull.x, 0.35, pull.z);
                        target.velocityModified = true;
                    }
                    SpellUtil.damage(ctx, target, ctx.scaled(2.0f));
                    Fx.beam(ctx.world(), ctx.eyes(),
                            target.getPos().add(0.0, target.getHeight() * 0.5, 0.0),
                            Fx.dust(SpellSchool.ARCANE.color(), 0.9f), 0.25);
                    Fx.sound(ctx.world(), target.getPos(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS,
                            0.8f, push ? 0.8f : 1.5f);
                    return true;
                })
                .register();
    }

    /** Магический щит: урон списывается с маны, пока она есть. */
    private static void manaShield() {
        Spell.builder("mana_shield", SpellSchool.ARCANE)
                .type(SpellCastType.SELF)
                .tier(3).cost(45.0f).cooldown(400)
                .action(ctx -> {
                    SpellUtil.effect(ctx.caster(), ModEffects.ARCANE_SHIELD, 30 * 20, 1);
                    Fx.sphere(ctx.world(), ctx.feet().add(0.0, 1.0, 0.0), 1.2,
                            Fx.dust(SpellSchool.ARCANE.color(), 1.2f), 60);
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.4f);
                    return true;
                })
                .register();
    }

    /** Взор мага: подсвечивает всё живое вокруг и даёт ночное зрение. */
    private static void arcaneSight() {
        Spell.builder("arcane_sight", SpellSchool.ARCANE)
                .type(SpellCastType.SELF)
                .tier(1).cost(20.0f).cooldown(200)
                .action(ctx -> {
                    SpellUtil.effect(ctx.caster(), ModEffects.ARCANE_SIGHT, 45 * 20, 0);
                    SpellUtil.effect(ctx.caster(), StatusEffects.NIGHT_VISION, 45 * 20, 0);
                    Fx.helix(ctx.world(), ctx.feet(), 0.8, 2.0, ParticleTypes.END_ROD, 40, 2.0);
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.5f);
                    return true;
                })
                .register();
    }

    /**
     * Взор недр: на десять секунд подсвечивает руду вокруг.
     *
     * <p>Блоки в Minecraft подсветить нельзя, поэтому над каждой жилой
     * висит искра цвета самой руды — сквозь камень её видно так же плохо,
     * как и любые частицы, зато у поверхности пласта читается отлично.
     */
    private static void prospect() {
        Spell.builder("prospect", SpellSchool.ARCANE)
                .type(SpellCastType.UTILITY)
                .tier(2).cost(28.0f).cooldown(300)
                .action(ctx -> {
                    int radius = (int) ctx.reach(10.0);
                    BlockPos origin = ctx.caster().getBlockPos();
                    List<BlockPos> found = new ArrayList<>();
                    List<Integer> colors = new ArrayList<>();

                    for (BlockPos pos : BlockPos.iterate(origin.add(-radius, -radius, -radius),
                            origin.add(radius, radius, radius))) {
                        if (found.size() >= MAX_PROSPECT_HITS) {
                            break;
                        }
                        BlockState state = ctx.world().getBlockState(pos);
                        if (!state.isIn(ConventionalBlockTags.ORES)) {
                            continue;
                        }
                        found.add(pos.toImmutable());
                        colors.add(oreColor(Registries.BLOCK.getId(state.getBlock()).getPath()));
                    }

                    if (found.isEmpty()) {
                        SpellUtil.message(ctx, Text.translatable("message.arcanum.no_ore")
                                .formatted(Formatting.GRAY));
                        return false;
                    }

                    SpellTicker.schedule(ctx.world(), 200, 20, elapsed -> {
                        for (int i = 0; i < found.size(); i++) {
                            Vec3d at = Vec3d.ofCenter(found.get(i));
                            Fx.burst(ctx.world(), at, Fx.dust(colors.get(i), 1.0f), 2, 0.25, 0.0);
                        }
                        return true;
                    });

                    SpellUtil.message(ctx, Text.translatable("message.arcanum.ore_found", found.size())
                            .formatted(Formatting.AQUA));
                    Fx.sound(ctx.world(), ctx.feet(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
                    return true;
                })
                .register();
    }

    /** Возврат к установленному «Якорю перемещения». */
    private static void recall() {
        Spell.builder("recall", SpellSchool.ARCANE)
                .type(SpellCastType.UTILITY)
                .tier(3).cost(70.0f).cooldown(600)
                .action(ctx -> {
                    GlobalPos anchor = ctx.caster().getAttached(ManaAttachments.ANCHOR);
                    if (anchor == null) {
                        SpellUtil.message(ctx, Text.translatable("message.arcanum.no_anchor")
                                .formatted(Formatting.GRAY));
                        return false;
                    }
                    if (!anchor.dimension().equals(ctx.world().getRegistryKey())) {
                        SpellUtil.message(ctx, Text.translatable("message.arcanum.anchor_other_world")
                                .formatted(Formatting.GRAY));
                        return false;
                    }

                    ServerWorld world = ctx.world();
                    Vec3d from = ctx.feet();
                    Vec3d to = SpellUtil.safeSpot(world, Vec3d.ofBottomCenter(anchor.pos()));
                    SpellUtil.teleport(ctx.caster(), to);

                    Fx.helix(world, from, 1.0, 3.0, ParticleTypes.PORTAL, 60, 3.0);
                    Fx.helix(world, to, 1.0, 3.0, ParticleTypes.PORTAL, 60, 3.0);
                    Fx.sound(world, to, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                    // Возвращение в убежище восстанавливает немного маны.
                    ManaManager.give(ctx.caster(), 20.0f);
                    return true;
                })
                .register();
    }
}
