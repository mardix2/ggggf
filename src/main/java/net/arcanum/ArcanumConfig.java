package net.arcanum;

/**
 * Числовой баланс мода в одном месте.
 *
 * <p>Намеренно сделан статическими константами, а не файлом конфигурации:
 * значения читаются и на клиенте, и на сервере, и должны совпадать.
 */
public final class ArcanumConfig {
    private ArcanumConfig() {
    }

    /** Базовый запас маны игрока без каких-либо улучшений. */
    public static final int BASE_MANA = 100;
    /** Сколько маны добавляет одна «Печать разума» (максимум {@link #MAX_ATTUNEMENT}). */
    public static final int MANA_PER_ATTUNEMENT = 25;
    public static final int MAX_ATTUNEMENT = 12;

    /** Базовая регенерация маны в единицах за тик (20 тиков = 1 секунда). */
    public static final float BASE_REGEN_PER_TICK = 0.25f;
    /** Множитель регенерации, пока игрок стоит рядом с «Источником маны». */
    public static final float FONT_REGEN_MULTIPLIER = 4.0f;
    /** Радиус действия «Источника маны» в блоках. */
    public static final double FONT_RADIUS = 8.0;

    /** Сколько тиков после траты маны регенерация стоит на паузе. */
    public static final int REGEN_DELAY_TICKS = 40;

    /** Базовый радиус захвата предметов алтарём превращения. */
    public static final double ALTAR_RADIUS = 3.0;
    /** Длительность ритуала на алтаре в тиках. */
    public static final int ALTAR_RITUAL_TICKS = 60;
}
