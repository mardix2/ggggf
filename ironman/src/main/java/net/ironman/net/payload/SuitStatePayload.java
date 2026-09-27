package net.ironman.net.payload;

import net.ironman.IronMan;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Сервер → клиент: всё, что нужно HUD-у J.A.R.V.I.S.
 *
 * <p>Энергия передаётся целым числом: на экране дробная часть не видна,
 * а равенство записей позволяет не слать одинаковые пакеты каждый тик.
 */
public record SuitStatePayload(int reactor, int energy, int capacity, boolean flightArmed,
                               boolean nightVision, int primaryCooldown, int unibeamCooldown,
                               int missileCooldown) implements CustomPayload {

    public static final CustomPayload.Id<SuitStatePayload> ID =
            new CustomPayload.Id<>(IronMan.id("suit_state"));

    public static final PacketCodec<RegistryByteBuf, SuitStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SuitStatePayload::reactor,
            PacketCodecs.VAR_INT, SuitStatePayload::energy,
            PacketCodecs.VAR_INT, SuitStatePayload::capacity,
            PacketCodecs.BOOLEAN, SuitStatePayload::flightArmed,
            PacketCodecs.BOOLEAN, SuitStatePayload::nightVision,
            PacketCodecs.VAR_INT, SuitStatePayload::primaryCooldown,
            PacketCodecs.VAR_INT, SuitStatePayload::unibeamCooldown,
            PacketCodecs.VAR_INT, SuitStatePayload::missileCooldown,
            SuitStatePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
