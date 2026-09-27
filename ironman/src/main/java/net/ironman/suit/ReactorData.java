package net.ironman.suit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Постоянные данные игрока: какой реактор установлен и сколько в нём энергии.
 *
 * <p>Неизменяемая запись: каждое изменение создаёт новый экземпляр.
 */
public record ReactorData(ReactorTier reactor, float energy) {

    public static final ReactorData EMPTY = new ReactorData(ReactorTier.NONE, 0.0f);

    public static final Codec<ReactorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("reactor", 0).forGetter(data -> data.reactor().ordinal()),
            Codec.FLOAT.optionalFieldOf("energy", 0.0f).forGetter(ReactorData::energy)
    ).apply(instance, (reactor, energy) -> new ReactorData(ReactorTier.byOrdinal(reactor), energy)));

    public int capacity() {
        return reactor.capacity();
    }

    public boolean installed() {
        return reactor != ReactorTier.NONE;
    }

    public ReactorData withEnergy(float value) {
        return new ReactorData(reactor, Math.max(0.0f, Math.min(capacity(), value)));
    }

    public ReactorData withReactor(ReactorTier tier) {
        return new ReactorData(tier, Math.min(energy, tier.capacity()));
    }

    public float fraction() {
        return capacity() > 0 ? energy / capacity() : 0.0f;
    }
}
