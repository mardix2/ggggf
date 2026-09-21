package net.arcanum.client;

import net.arcanum.net.payload.CooldownPayload;
import net.arcanum.net.payload.KnownSpellsPayload;
import net.arcanum.net.payload.ManaSyncPayload;
import net.arcanum.net.payload.OpenSpellbookPayload;
import net.arcanum.net.payload.SelectSpellPayload;
import net.arcanum.registry.ModBlocks;
import net.arcanum.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.util.Identifier;

import java.util.List;

/** Точка входа клиентской части. */
public class ArcanumClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ArcanumKeys.init();
        ArcanumHud.register();
        ArcanumTooltips.register();

        // Снаряд рисуется ванильным рендерером летящих предметов.
        EntityRendererRegistry.register(ModEntities.SPELL_PROJECTILE, FlyingItemEntityRenderer::new);

        registerCutoutBlocks();
        registerReceivers();
        registerKeyHandling();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientSpellState.reset());
    }

    /**
     * Блоки с прозрачными участками. Если в вашей версии Fabric API
     * {@code BlockRenderLayerMap} переименован, правка нужна только здесь.
     */
    private void registerCutoutBlocks() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
                ModBlocks.ARCANE_CRYSTAL_CLUSTER,
                ModBlocks.MOONPETAL,
                ModBlocks.INFUSION_ALTAR,
                ModBlocks.MANA_FONT,
                ModBlocks.TELEPORT_ANCHOR,
                ModBlocks.ARCANE_PEDESTAL);
    }

    private void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ManaSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientSpellState.setMana(payload.mana(), payload.max())));

        ClientPlayNetworking.registerGlobalReceiver(KnownSpellsPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientSpellState.setSpells(payload.known(), payload.selected())));

        ClientPlayNetworking.registerGlobalReceiver(CooldownPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientSpellState.startCooldown(payload.spell(), payload.ticks())));

        ClientPlayNetworking.registerGlobalReceiver(OpenSpellbookPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new SpellbookScreen())));
    }

    private void registerKeyHandling() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            while (ArcanumKeys.OPEN_SPELLBOOK.wasPressed()) {
                client.setScreen(new SpellbookScreen());
            }
            while (ArcanumKeys.NEXT_SPELL.wasPressed()) {
                cycleSpell(client, 1);
            }
            while (ArcanumKeys.PREV_SPELL.wasPressed()) {
                cycleSpell(client, -1);
            }
        });
    }

    /** Переключение заклинания без открытия книги. */
    private static void cycleSpell(MinecraftClient client, int direction) {
        List<Identifier> known = ClientSpellState.known();
        if (known.isEmpty() || client.player == null) {
            return;
        }
        int current = ClientSpellState.selectedId().map(known::indexOf).orElse(-1);
        int next = Math.floorMod(current + direction, known.size());
        Identifier id = known.get(next);

        ClientSpellState.setSelectedLocally(id);
        ClientPlayNetworking.send(new SelectSpellPayload(id));

        var spell = net.arcanum.spell.SpellRegistry.get(id);
        if (spell != null) {
            client.player.sendMessage(spell.displayName(), true);
        }
    }
}
