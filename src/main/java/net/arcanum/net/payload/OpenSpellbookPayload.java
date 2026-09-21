package net.arcanum.net.payload;

import net.arcanum.Arcanum;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Сервер → клиент: открыть книгу заклинаний. */
public record OpenSpellbookPayload() implements CustomPayload {

    public static final OpenSpellbookPayload INSTANCE = new OpenSpellbookPayload();

    public static final CustomPayload.Id<OpenSpellbookPayload> ID =
            new CustomPayload.Id<>(Arcanum.id("open_spellbook"));

    public static final PacketCodec<RegistryByteBuf, OpenSpellbookPayload> CODEC =
            PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
