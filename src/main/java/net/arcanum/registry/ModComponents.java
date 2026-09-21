package net.arcanum.registry;

import com.mojang.serialization.Codec;
import net.arcanum.Arcanum;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Компоненты предметов: привязанные заклинания посоха, выбранный слот,
 * заклинание свитка и внутренний запас маны.
 */
public final class ModComponents {
    private ModComponents() {
    }

    /**
     * Заклинания, «впечатанные» в посох: их можно применять, даже если игрок
     * их не изучил. Так работают посохи из сокровищниц.
     */
    public static final ComponentType<List<Identifier>> BOUND_SPELLS = register("bound_spells",
            builder -> builder
                    .codec(Identifier.CODEC.listOf())
                    .packetCodec(Identifier.PACKET_CODEC.collect(PacketCodecs.toList())));

    /** Заклинание, записанное в свиток. */
    public static final ComponentType<Identifier> SCROLL_SPELL = register("scroll_spell",
            builder -> builder.codec(Identifier.CODEC).packetCodec(Identifier.PACKET_CODEC));

    /** Мана, запасённая внутри предмета (кристалл маны). */
    public static final ComponentType<Integer> STORED_MANA = register("stored_mana",
            builder -> builder.codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT));

    private static <T> ComponentType<T> register(String name, UnaryOperator<ComponentType.Builder<T>> builder) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Arcanum.id(name),
                builder.apply(ComponentType.builder()).build());
    }

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
