package net.arcanum.net.payload;

import net.arcanum.Arcanum;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервер → клиент: список изученных заклинаний, выбранное, счётчики
 * применений (из них клиент сам считает уровень мастерства) и избранное
 * для колеса быстрого выбора.
 */
public record KnownSpellsPayload(List<Identifier> known, Optional<Identifier> selected,
                                 Map<Identifier, Integer> casts,
                                 List<Identifier> favorites) implements CustomPayload {

    public static final CustomPayload.Id<KnownSpellsPayload> ID =
            new CustomPayload.Id<>(Arcanum.id("known_spells"));

    public static final PacketCodec<RegistryByteBuf, KnownSpellsPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC.collect(PacketCodecs.toList()), KnownSpellsPayload::known,
            PacketCodecs.optional(Identifier.PACKET_CODEC), KnownSpellsPayload::selected,
            PacketCodecs.map(HashMap::new, Identifier.PACKET_CODEC, PacketCodecs.VAR_INT),
            KnownSpellsPayload::casts,
            Identifier.PACKET_CODEC.collect(PacketCodecs.toList()), KnownSpellsPayload::favorites,
            KnownSpellsPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
