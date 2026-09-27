package net.ironman.suit;

import net.minecraft.util.Formatting;

/**
 * Дуговой реактор в груди игрока.
 *
 * <p>Реактор — источник энергии для любого костюма. Сам костюм без реактора
 * не работает: это просто очень дорогие доспехи.
 */
public enum ReactorTier {
    NONE("none", 0, 0.0f, Formatting.GRAY),
    /** Первый реактор из пещеры: палладий, маленькая ёмкость. */
    MK_I("mk_i", 1000, 1.5f, Formatting.AQUA),
    /** Миниатюрный реактор из мастерской. */
    MK_II("mk_ii", 2500, 3.0f, Formatting.AQUA),
    /** Реактор на новом элементе: огромная ёмкость и быстрый заряд. */
    STARKIUM("starkium", 6000, 7.0f, Formatting.LIGHT_PURPLE);

    private final String id;
    private final int capacity;
    private final float regen;
    private final Formatting color;

    ReactorTier(String id, int capacity, float regen, Formatting color) {
        this.id = id;
        this.capacity = capacity;
        this.regen = regen;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int capacity() {
        return capacity;
    }

    /** Восстановление энергии за тик, когда костюм не тратит её. */
    public float regen() {
        return regen;
    }

    public Formatting color() {
        return color;
    }

    public String translationKey() {
        return "reactor.ironman." + id;
    }

    public static ReactorTier byOrdinal(int ordinal) {
        ReactorTier[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }
}
