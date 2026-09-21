package net.arcanum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Числовой баланс мода в одном месте.
 *
 * <p>Значения читаются и на клиенте, и на сервере; в сетевой игре решает
 * сервер, а клиентские поля влияют только на отрисовку. Файл
 * {@code config/arcanum.json} создаётся при первом запуске и содержит
 * все ключи со значениями по умолчанию — менять можно любые.
 */
public final class ArcanumConfig {
    private ArcanumConfig() {
    }

    /** Базовый запас маны игрока без каких-либо улучшений. */
    public static int BASE_MANA = 100;
    /** Сколько маны добавляет одна «Печать разума» (максимум {@link #MAX_ATTUNEMENT}). */
    public static int MANA_PER_ATTUNEMENT = 25;
    public static int MAX_ATTUNEMENT = 12;

    /** Базовая регенерация маны в единицах за тик (20 тиков = 1 секунда). */
    public static float BASE_REGEN_PER_TICK = 0.25f;
    /** Множитель регенерации, пока игрок стоит рядом с «Источником маны». */
    public static float FONT_REGEN_MULTIPLIER = 4.0f;
    /** Радиус действия «Источника маны» в блоках. */
    public static double FONT_RADIUS = 8.0;

    /** Сколько тиков после траты маны регенерация стоит на паузе. */
    public static int REGEN_DELAY_TICKS = 40;

    /** Базовый радиус захвата предметов алтарём превращения. */
    public static double ALTAR_RADIUS = 3.0;
    /** Длительность ритуала на алтаре в тиках. */
    public static int ALTAR_RITUAL_TICKS = 60;

    /** Сколько заклинаний помещается в колесо быстрого выбора. */
    public static int MAX_FAVORITES = 8;

    /** Положение полосы маны на экране. */
    public static int HUD_X = 10;
    public static int HUD_Y = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Читает конфигурацию, создавая файл при первом запуске.
     *
     * <p>Любая ошибка чтения — не повод падать: мод просто остаётся на
     * значениях по умолчанию и пишет об этом в лог.
     */
    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("arcanum.json");
        try {
            if (Files.exists(path)) {
                JsonObject json = GSON.fromJson(Files.readString(path), JsonObject.class);
                if (json != null) {
                    apply(json);
                }
            }
            Files.writeString(path, GSON.toJson(snapshot()));
        } catch (IOException | RuntimeException e) {
            Arcanum.LOGGER.warn("Не удалось прочитать config/arcanum.json, используются значения по умолчанию", e);
        }
    }

    private static void apply(JsonObject json) {
        BASE_MANA = readInt(json, "base_mana", BASE_MANA, 1, 10000);
        MANA_PER_ATTUNEMENT = readInt(json, "mana_per_attunement", MANA_PER_ATTUNEMENT, 0, 1000);
        MAX_ATTUNEMENT = readInt(json, "max_attunement", MAX_ATTUNEMENT, 0, 64);
        BASE_REGEN_PER_TICK = (float) readDouble(json, "base_regen_per_tick", BASE_REGEN_PER_TICK, 0.0, 100.0);
        FONT_REGEN_MULTIPLIER = (float) readDouble(json, "font_regen_multiplier", FONT_REGEN_MULTIPLIER, 1.0, 100.0);
        FONT_RADIUS = readDouble(json, "font_radius", FONT_RADIUS, 1.0, 32.0);
        REGEN_DELAY_TICKS = readInt(json, "regen_delay_ticks", REGEN_DELAY_TICKS, 0, 1200);
        ALTAR_RADIUS = readDouble(json, "altar_radius", ALTAR_RADIUS, 1.0, 16.0);
        ALTAR_RITUAL_TICKS = readInt(json, "altar_ritual_ticks", ALTAR_RITUAL_TICKS, 1, 1200);
        MAX_FAVORITES = readInt(json, "max_favorites", MAX_FAVORITES, 1, 12);
        HUD_X = readInt(json, "hud_x", HUD_X, -4096, 4096);
        HUD_Y = readInt(json, "hud_y", HUD_Y, -4096, 4096);
    }

    private static JsonObject snapshot() {
        JsonObject json = new JsonObject();
        json.addProperty("base_mana", BASE_MANA);
        json.addProperty("mana_per_attunement", MANA_PER_ATTUNEMENT);
        json.addProperty("max_attunement", MAX_ATTUNEMENT);
        json.addProperty("base_regen_per_tick", BASE_REGEN_PER_TICK);
        json.addProperty("font_regen_multiplier", FONT_REGEN_MULTIPLIER);
        json.addProperty("font_radius", FONT_RADIUS);
        json.addProperty("regen_delay_ticks", REGEN_DELAY_TICKS);
        json.addProperty("altar_radius", ALTAR_RADIUS);
        json.addProperty("altar_ritual_ticks", ALTAR_RITUAL_TICKS);
        json.addProperty("max_favorites", MAX_FAVORITES);
        json.addProperty("hud_x", HUD_X);
        json.addProperty("hud_y", HUD_Y);
        return json;
    }

    private static int readInt(JsonObject json, String key, int fallback, int min, int max) {
        if (!json.has(key)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, json.get(key).getAsInt()));
    }

    private static double readDouble(JsonObject json, String key, double fallback, double min, double max) {
        if (!json.has(key)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, json.get(key).getAsDouble()));
    }
}
