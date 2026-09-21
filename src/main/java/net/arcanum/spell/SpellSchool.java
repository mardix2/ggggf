package net.arcanum.spell;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;

/**
 * Семь школ магии. Школа задаёт цвет в интерфейсе, рунный реагент для
 * создания свитков и то, какой фокус усиливает заклинание.
 */
public enum SpellSchool implements StringIdentifiable {
    FIRE("fire", 0xFF7A2A, Formatting.GOLD),
    FROST("frost", 0x7FD8FF, Formatting.AQUA),
    STORM("storm", 0xE8E45C, Formatting.YELLOW),
    ARCANE("arcane", 0xC77DFF, Formatting.LIGHT_PURPLE),
    NATURE("nature", 0x74D14C, Formatting.GREEN),
    SHADOW("shadow", 0x6E4A8E, Formatting.DARK_PURPLE),
    LIGHT("light", 0xFFF3B0, Formatting.WHITE);

    private final String name;
    private final int color;
    private final Formatting formatting;

    SpellSchool(String name, int color, Formatting formatting) {
        this.name = name;
        this.color = color;
        this.formatting = formatting;
    }

    /** Цвет 0xRRGGBB — для частиц и текста. */
    public int color() {
        return color;
    }

    /** Непрозрачный цвет 0xAARRGGBB — для отрисовки HUD. */
    public int argb() {
        return 0xFF000000 | color;
    }

    public Formatting formatting() {
        return formatting;
    }

    public String translationKey() {
        return "school.arcanum." + name;
    }

    public Text displayName() {
        return Text.translatable(translationKey()).formatted(formatting);
    }

    @Override
    public String asString() {
        return name;
    }
}
