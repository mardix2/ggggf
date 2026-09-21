package net.arcanum.net;

import net.arcanum.mana.ManaData;
import net.arcanum.mana.ManaManager;
import net.arcanum.net.payload.CooldownPayload;
import net.arcanum.net.payload.KnownSpellsPayload;
import net.arcanum.net.payload.ManaSyncPayload;
import net.arcanum.net.payload.OpenSpellbookPayload;
import net.arcanum.net.payload.SelectSpellPayload;
import net.arcanum.spell.SpellRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Регистрация пакетов и серверная сторона обмена с клиентом. */
public final class ModNetworking {
    private ModNetworking() {
    }

    /**
     * Последняя отправленная каждому игроку пара «мана/максимум».
     * Нужна, чтобы не слать пакет каждый тик: полоса в HUD всё равно
     * показывает целые числа.
     */
    private static final Map<UUID, int[]> LAST_SENT = new HashMap<>();

    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(ManaSyncPayload.ID, ManaSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(KnownSpellsPayload.ID, KnownSpellsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CooldownPayload.ID, CooldownPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenSpellbookPayload.ID, OpenSpellbookPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectSpellPayload.ID, SelectSpellPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SelectSpellPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            MinecraftServer server = player.getServer();
            if (server != null) {
                server.execute(() -> selectSpell(player, payload.spell()));
            }
        });
    }

    /**
     * Клиент прислал выбор заклинания. Доверять ему нельзя, поэтому
     * проверяем, что заклинание существует и изучено.
     */
    private static void selectSpell(ServerPlayerEntity player, Identifier spell) {
        if (SpellRegistry.get(spell) == null) {
            return;
        }
        ManaData data = ManaManager.data(player);
        if (!data.knows(spell)) {
            return;
        }
        ManaManager.set(player, data.withSelected(spell));
        syncSpells(player);
    }

    public static void syncMana(ServerPlayerEntity player) {
        int mana = (int) ManaManager.current(player);
        int max = ManaManager.max(player);
        int[] last = LAST_SENT.get(player.getUuid());
        if (last != null && last[0] == mana && last[1] == max) {
            return;
        }
        LAST_SENT.put(player.getUuid(), new int[]{mana, max});
        ServerPlayNetworking.send(player, new ManaSyncPayload(mana, max));
    }

    public static void syncSpells(ServerPlayerEntity player) {
        ManaData data = ManaManager.data(player);
        ServerPlayNetworking.send(player, new KnownSpellsPayload(data.known(), data.selected()));
    }

    public static void syncCooldown(ServerPlayerEntity player, Identifier spell, int ticks) {
        ServerPlayNetworking.send(player, new CooldownPayload(spell, ticks));
    }

    public static void openSpellbook(ServerPlayerEntity player) {
        syncSpells(player);
        ServerPlayNetworking.send(player, OpenSpellbookPayload.INSTANCE);
    }

    /** Полная синхронизация при входе и возрождении. */
    public static void syncAll(ServerPlayerEntity player) {
        LAST_SENT.remove(player.getUuid());
        syncMana(player);
        syncSpells(player);
    }

    public static void forget(UUID player) {
        LAST_SENT.remove(player);
    }
}
