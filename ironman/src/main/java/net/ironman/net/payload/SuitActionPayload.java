package net.ironman.net.payload;

import net.ironman.IronMan;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Клиент → сервер: игрок нажал клавишу костюма.
 *
 * @param action порядковый номер {@link net.ironman.suit.SuitAction}
 */
public record SuitActionPayload(int action) implements CustomPayload {

    public static final CustomPayload.Id<SuitActionPayload> ID =
            new CustomPayload.Id<>(IronMan.id("suit_action"));

    public static final PacketCodec<RegistryByteBuf, SuitActionPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, SuitActionPayload::action, SuitActionPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
