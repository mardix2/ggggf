package net.ironman.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.ironman.net.payload.SuitActionPayload;
import net.ironman.net.payload.SuitStatePayload;
import net.ironman.suit.SuitAction;
import net.ironman.suit.SuitManager;
import net.minecraft.server.network.ServerPlayerEntity;

/** Регистрация пакетов и серверная сторона обмена с клиентом. */
public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(SuitStatePayload.ID, SuitStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SuitActionPayload.ID, SuitActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SuitActionPayload.ID, (payload, context) -> {
            // Клиенту не доверяем: сервер сам проверит костюм, энергию и перезарядку.
            SuitAction action = SuitAction.byOrdinal(payload.action());
            ServerPlayerEntity player = context.player();
            if (action != null) {
                context.server().execute(() -> SuitManager.handleAction(player, action));
            }
        });
    }

    public static void send(ServerPlayerEntity player, SuitStatePayload payload) {
        if (ServerPlayNetworking.canSend(player, SuitStatePayload.ID)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
