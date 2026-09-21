package net.arcanum.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcanum.ArcanumConfig;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Постоянные магические данные игрока: запас маны, настройка (постоянные
 * улучшения ёмкости), список изученных заклинаний и выбранное заклинание.
 *
 * <p>Хранится как Fabric-attachment, поэтому это неизменяемая запись:
 * любое изменение создаёт новый экземпляр и кладётся обратно через
 * {@link ManaManager#set}.
 */
public record ManaData(float mana, int attunement, List<Identifier> known, Optional<Identifier> selected) {

    public static final ManaData DEFAULT =
            new ManaData(ArcanumConfig.BASE_MANA, 0, List.of(), Optional.empty());

    public static final Codec<ManaData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("mana", (float) ArcanumConfig.BASE_MANA).forGetter(ManaData::mana),
            Codec.INT.optionalFieldOf("attunement", 0).forGetter(ManaData::attunement),
            Identifier.CODEC.listOf().optionalFieldOf("known", List.of()).forGetter(ManaData::known),
            Identifier.CODEC.optionalFieldOf("selected").forGetter(ManaData::selected)
    ).apply(instance, ManaData::new));

    public ManaData withMana(float value) {
        return new ManaData(value, attunement, known, selected);
    }

    public ManaData withAttunement(int value) {
        return new ManaData(mana, Math.min(ArcanumConfig.MAX_ATTUNEMENT, Math.max(0, value)), known, selected);
    }

    public ManaData withSelected(Identifier spell) {
        return new ManaData(mana, attunement, known, Optional.ofNullable(spell));
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
        return new ManaData(mana, attunement, List.copyOf(next), sel);
    }

    public ManaData forget(Identifier spell) {
        if (!known.contains(spell)) {
            return this;
        }
        List<Identifier> next = new ArrayList<>(known);
        next.remove(spell);
        Optional<Identifier> sel = selected.filter(s -> !s.equals(spell));
        return new ManaData(mana, attunement, List.copyOf(next), sel);
    }
}
