package net.arcanum.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcanum.ArcanumConfig;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Постоянные магические данные игрока: запас маны, настройка (постоянные
 * улучшения ёмкости), список изученных заклинаний и выбранное заклинание.
 *
 * <p>Хранится как Fabric-attachment, поэтому это неизменяемая запись:
 * любое изменение создаёт новый экземпляр и кладётся обратно через
 * {@link ManaManager#set}.
 *
 * @param casts     сколько раз игрок применил каждое заклинание — из этого
 *                  считается мастерство ({@link net.arcanum.spell.SpellMastery})
 * @param favorites заклинания в колесе быстрого выбора, в порядке добавления
 */
public record ManaData(float mana, int attunement, List<Identifier> known,
                       Optional<Identifier> selected, Map<Identifier, Integer> casts,
                       List<Identifier> favorites) {

    public static final ManaData DEFAULT =
            new ManaData(ArcanumConfig.BASE_MANA, 0, List.of(), Optional.empty(), Map.of(), List.of());

    public static final Codec<ManaData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("mana", (float) ArcanumConfig.BASE_MANA).forGetter(ManaData::mana),
            Codec.INT.optionalFieldOf("attunement", 0).forGetter(ManaData::attunement),
            Identifier.CODEC.listOf().optionalFieldOf("known", List.of()).forGetter(ManaData::known),
            Identifier.CODEC.optionalFieldOf("selected").forGetter(ManaData::selected),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                    .optionalFieldOf("casts", Map.of()).forGetter(ManaData::casts),
            Identifier.CODEC.listOf().optionalFieldOf("favorites", List.of())
                    .forGetter(ManaData::favorites)
    ).apply(instance, ManaData::new));

    public ManaData withMana(float value) {
        return new ManaData(value, attunement, known, selected, casts, favorites);
    }

    public ManaData withAttunement(int value) {
        return new ManaData(mana, Math.min(ArcanumConfig.MAX_ATTUNEMENT, Math.max(0, value)),
                known, selected, casts, favorites);
    }

    public ManaData withSelected(Identifier spell) {
        return new ManaData(mana, attunement, known, Optional.ofNullable(spell), casts, favorites);
    }

    /** Сколько раз игрок применял это заклинание. */
    public int casts(Identifier spell) {
        return casts.getOrDefault(spell, 0);
    }

    /** Засчитывает одно применение. */
    public ManaData recordCast(Identifier spell) {
        Map<Identifier, Integer> next = new HashMap<>(casts);
        next.merge(spell, 1, Integer::sum);
        return new ManaData(mana, attunement, known, selected, Map.copyOf(next), favorites);
    }

    public boolean isFavorite(Identifier spell) {
        return favorites.contains(spell);
    }

    /**
     * Добавляет или убирает заклинание из колеса.
     *
     * <p>Когда мест больше нет, самое старое уступает место новому —
     * так игроку не приходится сначала что-то выбрасывать вручную.
     */
    public ManaData toggleFavorite(Identifier spell, int limit) {
        List<Identifier> next = new ArrayList<>(favorites);
        if (!next.remove(spell)) {
            next.add(spell);
            while (next.size() > Math.max(1, limit)) {
                next.remove(0);
            }
        }
        return new ManaData(mana, attunement, known, selected, casts, List.copyOf(next));
    }

    public boolean knows(Identifier spell) {
        return known.contains(spell);
    }

    public ManaData learn(Identifier spell) {
        if (known.contains(spell)) {
            return this;
        }
        List<Identifier> next = new ArrayList<>(known);
        next.add(spell);
        // Первое выученное заклинание сразу становится активным — иначе игрок
        // получает посох, который «ничего не делает».
        Optional<Identifier> sel = selected.isPresent() ? selected : Optional.of(spell);
        return new ManaData(mana, attunement, List.copyOf(next), sel, casts, favorites);
    }

    public ManaData forget(Identifier spell) {
        if (!known.contains(spell)) {
            return this;
        }
        List<Identifier> next = new ArrayList<>(known);
        next.remove(spell);
        Optional<Identifier> sel = selected.filter(s -> !s.equals(spell));
        List<Identifier> stillFavorite = new ArrayList<>(favorites);
        stillFavorite.remove(spell);
        return new ManaData(mana, attunement, List.copyOf(next), sel, casts,
                List.copyOf(stillFavorite));
    }
}
