package net.arcanum.net.payload;

import net.arcanum.Arcanum;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Сервер → клиент: у заклинания началась перезарядка. */
public record CooldownPayload(Identifier spell, int ticks) implements CustomPayload {

    public static final CustomPayload.Id<CooldownPayload> ID =
            new CustomPayload.Id<>(Arcanum.id("cooldown"));

    public static final PacketCodec<RegistryByteBuf, CooldownPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, CooldownPayload::spell,
            PacketCodecs.VAR_INT, CooldownPayload::ticks,
            CooldownPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
