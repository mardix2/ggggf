package net.ironman.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.ironman.net.payload.SuitActionPayload;
import net.ironman.net.payload.SuitStatePayload;
import net.ironman.registry.ModEntities;
import net.ironman.suit.SuitAction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Точка входа клиентской части. */
public class IronManClient implements ClientModInitializer {

    /** Как часто повторяется выстрел, пока клавиша основного оружия зажата. */
    private static final int AUTO_FIRE_INTERVAL = 4;

    private int primaryHeld;

    @Override
    public void onInitializeClient() {
        IronManKeys.init();
        JarvisHud.register();
        IronManTooltips.register();

        // Ракету рисует ванильный рендерер летящих предметов.
        EntityRendererRegistry.register(ModEntities.MICRO_MISSILE, FlyingItemEntityRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(SuitStatePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientSuitState.update(payload)));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientSuitState.reset());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            handleKeys(client);
            FlightController.tick(client);
        });
    }

    private void handleKeys(MinecraftClient client) {
        while (IronManKeys.FLIGHT.wasPressed()) {
            send(SuitAction.TOGGLE_FLIGHT);
        }
        while (IronManKeys.UNIBEAM.wasPressed()) {
            send(SuitAction.UNIBEAM);
        }
        while (IronManKeys.MISSILES.wasPressed()) {
            send(SuitAction.MISSILES);
        }
        while (IronManKeys.NIGHT_VISION.wasPressed()) {
            send(SuitAction.TOGGLE_NIGHT_VISION);
        }
        while (IronManKeys.HUD.wasPressed()) {
            ClientSuitState.toggleHud();
            client.player.sendMessage(Text.translatable(ClientSuitState.hudEnabled()
                    ? "message.ironman.hud_on" : "message.ironman.hud_off").formatted(Formatting.AQUA), true);
        }

        // Основное оружие стреляет и по нажатию, и очередью при удержании.
        boolean fired = false;
        while (IronManKeys.PRIMARY.wasPressed()) {
            fired = true;
        }
        if (IronManKeys.PRIMARY.isPressed()) {
            primaryHeld++;
        } else {
            primaryHeld = 0;
        }
        if (fired || (primaryHeld > 0 && primaryHeld % AUTO_FIRE_INTERVAL == 0
                && ClientSuitState.primaryCooldown() == 0)) {
            send(SuitAction.PRIMARY);
        }
    }

    private static void send(SuitAction action) {
        if (ClientPlayNetworking.canSend(SuitActionPayload.ID)) {
            ClientPlayNetworking.send(new SuitActionPayload(action.ordinal()));
        }
    }
}
