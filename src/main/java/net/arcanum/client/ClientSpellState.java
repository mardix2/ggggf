package net.arcanum.client;

import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellMastery;
import net.arcanum.spell.SpellRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Зеркало серверного состояния на клиенте: только для отрисовки.
 *
 * <p>Ничего здесь не решает исход игры — сервер всё равно перепроверяет
 * и ману, и знание заклинания.
 */
public final class ClientSpellState {
    private ClientSpellState() {
    }

    private static float mana;
    private static int maxMana = 1;
    private static List<Identifier> known = List.of();
    private static Optional<Identifier> selected = Optional.empty();
    private static final Map<Identifier, Long> COOLDOWN_END = new HashMap<>();
    private static Map<Identifier, Integer> casts = Map.of();
    private static List<Identifier> favorites = List.of();

    public static void setMana(float value, int max) {
        mana = value;
        maxMana = Math.max(1, max);
    }

    public static float mana() {
        return mana;
    }

    public static int maxMana() {
        return maxMana;
    }

    public static float manaFraction() {
        return Math.min(1.0f, mana / maxMana);
    }

    public static void setSpells(List<Identifier> knownSpells, Optional<Identifier> selectedSpell,
                                 Map<Identifier, Integer> castCounts,
                                 List<Identifier> favoriteSpells) {
        known = List.copyOf(knownSpells);
        selected = selectedSpell;
        casts = Map.copyOf(castCounts);
        favorites = List.copyOf(favoriteSpells);
    }

    public static boolean isFavorite(Identifier spell) {
        return favorites.contains(spell);
    }

    /**
     * Что показать в колесе быстрого выбора.
     *
     * <p>Пока игрок ничего не отметил, колесо показывает первые изученные
     * заклинания — иначе при первом нажатии он увидел бы пустоту и решил,
     * что колесо сломано.
     */
    public static List<Identifier> wheel() {
        if (!favorites.isEmpty()) {
            return favorites;
        }
        return known.size() <= 8 ? known : known.subList(0, 8);
    }

    public static int casts(Identifier spell) {
        return casts.getOrDefault(spell, 0);
    }

    public static int mastery(Identifier spell) {
        return SpellMastery.level(casts(spell));
    }

    public static List<Identifier> known() {
        return known;
    }

    public static boolean knows(Identifier id) {
        return known.contains(id);
    }

    public static Optional<Identifier> selectedId() {
        return selected;
    }

    public static Spell selectedSpell() {
        return selected.map(SpellRegistry::get).orElse(null);
    }

    /** Локальный выбор до ответа сервера — интерфейс не должен «залипать». */
    public static void setSelectedLocally(Identifier id) {
        selected = Optional.ofNullable(id);
    }

    public static void startCooldown(Identifier spell, int ticks) {
        COOLDOWN_END.put(spell, worldTime() + ticks);
    }

    /** Остаток перезарядки в тиках; 0 — готово. */
    public static int cooldown(Identifier spell) {
        Long end = COOLDOWN_END.get(spell);
        if (end == null) {
            return 0;
        }
        long left = end - worldTime();
        if (left <= 0) {
            COOLDOWN_END.remove(spell);
            return 0;
        }
        return (int) left;
    }

    private static long worldTime() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? 0L : client.world.getTime();
    }

    /** Сброс при выходе из мира: иначе в новом мире висят чужие перезарядки. */
    public static void reset() {
        mana = 0.0f;
        maxMana = 1;
        known = List.of();
        selected = Optional.empty();
        casts = Map.of();
        favorites = List.of();
        COOLDOWN_END.clear();
    }
}
