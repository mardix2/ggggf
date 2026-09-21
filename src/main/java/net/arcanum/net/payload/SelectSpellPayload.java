package net.arcanum.net.payload;

import net.arcanum.Arcanum;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Клиент → сервер: игрок выбрал заклинание в колесе или книге. */
public record SelectSpellPayload(Identifier spell) implements CustomPayload {

    public static final CustomPayload.Id<SelectSpellPayload> ID =
            new CustomPayload.Id<>(Arcanum.id("select_spell"));

    public static final PacketCodec<RegistryByteBuf, SelectSpellPayload> CODEC =
            PacketCodec.tuple(Identifier.PACKET_CODEC, SelectSpellPayload::spell, SelectSpellPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
