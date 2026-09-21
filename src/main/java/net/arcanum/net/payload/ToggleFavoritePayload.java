package net.arcanum.net.payload;

import net.arcanum.Arcanum;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Клиент → сервер: добавить или убрать заклинание из колеса быстрого выбора. */
public record ToggleFavoritePayload(Identifier spell) implements CustomPayload {

    public static final CustomPayload.Id<ToggleFavoritePayload> ID =
            new CustomPayload.Id<>(Arcanum.id("toggle_favorite"));

    public static final PacketCodec<RegistryByteBuf, ToggleFavoritePayload> CODEC =
            PacketCodec.tuple(Identifier.PACKET_CODEC, ToggleFavoritePayload::spell,
                    ToggleFavoritePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
