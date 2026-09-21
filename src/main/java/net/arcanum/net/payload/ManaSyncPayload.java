package net.arcanum.net.payload;

import net.arcanum.Arcanum;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Сервер → клиент: текущая и максимальная мана для полосы в HUD. */
public record ManaSyncPayload(float mana, int max) implements CustomPayload {

    public static final CustomPayload.Id<ManaSyncPayload> ID =
            new CustomPayload.Id<>(Arcanum.id("mana_sync"));

    public static final PacketCodec<RegistryByteBuf, ManaSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, ManaSyncPayload::mana,
            PacketCodecs.VAR_INT, ManaSyncPayload::max,
            ManaSyncPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
