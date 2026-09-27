package net.ironman.registry;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.ironman.suit.SuitManager;
import net.minecraft.server.network.ServerPlayerEntity;

/** Серверные события: тик костюма, вход/выход, возрождение, урон. */
public final class ModEvents {
    private ModEvents() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                SuitManager.tick(player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                SuitManager.onJoin(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SuitManager.onDisconnect(handler.player));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                SuitManager.onRespawn(newPlayer));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SuitManager::allowDamage);
    }
}
