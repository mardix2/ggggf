package net.arcanum.mana;

import net.arcanum.net.ModNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Перезарядка отдельных заклинаний.
 *
 * <p>Живёт только в памяти (перезаход в мир сбрасывает перезарядку) —
 * сохранять её смысла нет, а так структура остаётся простой. Сервер —
 * источник истины, клиент держит собственную копию для отрисовки HUD.
 */
public final class SpellCooldowns {
    private SpellCooldowns() {
    }

    private static final Map<UUID, Map<Identifier, Long>> SERVER = new HashMap<>();

    public static void start(PlayerEntity player, Identifier spell, int ticks) {
        if (ticks <= 0) {
            return;
        }
        long end = player.getWorld().getTime() + ticks;
        SERVER.computeIfAbsent(player.getUuid(), k -> new HashMap<>()).put(spell, end);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ModNetworking.syncCooldown(serverPlayer, spell, ticks);
        }
    }

    public static int remaining(PlayerEntity player, Identifier spell) {
        Map<Identifier, Long> map = SERVER.get(player.getUuid());
        if (map == null) {
            return 0;
        }
        Long end = map.get(spell);
        if (end == null) {
            return 0;
        }
        long left = end - player.getWorld().getTime();
        if (left <= 0) {
            map.remove(spell);
            return 0;
        }
        return (int) left;
    }

    public static boolean ready(PlayerEntity player, Identifier spell) {
        return remaining(player, spell) <= 0;
    }

    public static void clear(UUID player) {
        SERVER.remove(player);
    }
}
