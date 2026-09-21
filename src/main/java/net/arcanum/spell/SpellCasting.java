package net.arcanum.spell;

import net.arcanum.mana.ManaData;
import net.arcanum.mana.ManaManager;
import net.arcanum.mana.SpellCooldowns;
import net.arcanum.registry.ModComponents;
import net.arcanum.net.ModNetworking;
import net.arcanum.registry.ModEffects;
import net.arcanum.spell.util.Fx;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Единственная точка, через которую применяются заклинания.
 *
 * <p>Здесь собраны все проверки (знание, ступень посоха, перезарядка, мана,
 * безмолвие) — заклинания в {@code spell.impl} занимаются только эффектом.
 */
public final class SpellCasting {
    private SpellCasting() {
    }

    /** Почему заклинание не сработало. {@link #OK} — сработало. */
    public enum Result {
        OK(null),
        NO_SPELL("message.arcanum.cast.no_spell"),
        NOT_LEARNED("message.arcanum.cast.not_learned"),
        TIER_TOO_LOW("message.arcanum.cast.tier_too_low"),
        ON_COOLDOWN("message.arcanum.cast.cooldown"),
        NOT_ENOUGH_MANA("message.arcanum.cast.no_mana"),
        SILENCED("message.arcanum.cast.silenced"),
        FIZZLED("message.arcanum.cast.fizzled");

        private final String key;

        Result(String key) {
            this.key = key;
        }

        public boolean ok() {
            return this == OK;
        }

        public Text message(Object... args) {
            return key == null ? Text.empty() : Text.translatable(key, args).formatted(Formatting.RED);
        }
    }

    /**
     * Готовое сообщение об ошибке — с подставленными числами
     * (секунды перезарядки, требуемая ступень, нехватка маны).
     */
    public static Text failureMessage(ServerPlayerEntity player, Spell spell, Result result) {
        if (spell == null) {
            return result.message();
        }
        return switch (result) {
            case ON_COOLDOWN -> result.message(
                    String.format("%.1f", SpellCooldowns.remaining(player, spell.id()) / 20.0f));
            case TIER_TOO_LOW -> result.message(spell.tier());
            case NOT_ENOUGH_MANA -> result.message(
                    Math.round(spell.manaCost() * ManaManager.costMultiplier(player)
                            * SpellMastery.costFactor(
                                    SpellMastery.level(ManaManager.data(player).casts(spell.id())))),
                    (int) ManaManager.current(player));
            case NOT_LEARNED -> result.message(spell.displayName());
            default -> result.message();
        };
    }

    /** Выбранное игроком заклинание либо {@code null}. */
    public static Spell selected(ServerPlayerEntity player) {
        ManaData data = ManaManager.data(player);
        return data.selected().map(SpellRegistry::get).orElse(null);
    }

    /** Доступно ли заклинание: изучено игроком или впечатано в посох. */
    public static boolean hasAccess(ServerPlayerEntity player, ItemStack tool, Spell spell) {
        if (ManaManager.data(player).knows(spell.id())) {
            return true;
        }
        List<Identifier> bound = tool.get(ModComponents.BOUND_SPELLS);
        return bound != null && bound.contains(spell.id());
    }

    /**
     * Применяет заклинание со всеми проверками.
     *
     * @param tier ступень посоха в руке (для свитков — ступень самого заклинания)
     */
    public static Result cast(ServerPlayerEntity player, ItemStack tool, Spell spell, int tier) {
        if (spell == null) {
            return Result.NO_SPELL;
        }
        if (player.hasStatusEffect(ModEffects.SILENCE)) {
            return Result.SILENCED;
        }
        if (!hasAccess(player, tool, spell)) {
            return Result.NOT_LEARNED;
        }
        if (tier < spell.tier()) {
            return Result.TIER_TOO_LOW;
        }
        if (!SpellCooldowns.ready(player, spell.id())) {
            return Result.ON_COOLDOWN;
        }

        int casts = ManaManager.data(player).casts(spell.id());
        int mastery = SpellMastery.level(casts);

        float cost = spell.manaCost() * ManaManager.costMultiplier(player)
                * SpellMastery.costFactor(mastery);
        if (!ManaManager.has(player, cost)) {
            return Result.NOT_ENOUGH_MANA;
        }

        ServerWorld world = (ServerWorld) player.getWorld();
        float power = 1.0f + ManaManager.powerBonus(player, spell.school())
                + SpellMastery.powerBonus(mastery);
        SpellContext ctx = new SpellContext(world, player, tool, power, tier);

        if (!spell.cast(ctx)) {
            // Заклинание само решило, что применять его не к чему — мана цела.
            return Result.FIZZLED;
        }

        ManaManager.consume(player, cost);
        int cooldown = Math.round(spell.cooldownTicks() * ManaManager.cooldownMultiplier(player)
                * SpellMastery.cooldownFactor(mastery));
        SpellCooldowns.start(player, spell.id(), cooldown);
        Fx.soundVaried(world, player.getPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 0.5f, 1.2f);

        recordMastery(player, spell, casts, mastery);
        return Result.OK;
    }

    /**
     * Засчитывает применение и сообщает игроку о новом уровне мастерства.
     *
     * <p>Данные перечитываются заново: {@link ManaManager#consume} уже успел
     * записать новую ману, и старый снимок её бы затёр.
     */
    private static void recordMastery(ServerPlayerEntity player, Spell spell, int casts, int mastery) {
        ManaManager.set(player, ManaManager.data(player).recordCast(spell.id()));
        ModNetworking.syncSpells(player);

        int next = SpellMastery.level(casts + 1);
        if (next <= mastery) {
            return;
        }
        player.sendMessage(Text.translatable("message.arcanum.mastery_up",
                spell.displayName(), SpellMastery.stars(next)), false);
        Fx.sound((ServerWorld) player.getWorld(), player.getPos(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
    }

    /**
     * Применение со свитка: знание и ступень не проверяются — свиток
     * «думает» за игрока, но мана всё равно нужна.
     */
    public static Result castFromScroll(ServerPlayerEntity player, ItemStack scroll, Spell spell) {
        if (spell == null) {
            return Result.NO_SPELL;
        }
        if (player.hasStatusEffect(ModEffects.SILENCE)) {
            return Result.SILENCED;
        }
        float cost = spell.manaCost() * 0.5f * ManaManager.costMultiplier(player);
        if (!ManaManager.has(player, cost)) {
            return Result.NOT_ENOUGH_MANA;
        }

        ServerWorld world = (ServerWorld) player.getWorld();
        float power = 1.0f + ManaManager.powerBonus(player, spell.school());
        if (!spell.cast(new SpellContext(world, player, scroll, power, spell.tier()))) {
            return Result.FIZZLED;
        }
        ManaManager.consume(player, cost);
        Fx.soundVaried(world, player.getPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        return Result.OK;
    }
}
