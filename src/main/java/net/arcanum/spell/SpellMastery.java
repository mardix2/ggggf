package net.arcanum.spell;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Мастерство заклинания.
 *
 * <p>Каждое успешное применение засчитывается, и с опытом одно и то же
 * заклинание становится дешевле, сильнее и быстрее перезаряжается. Это
 * поощряет выбрать «своё» заклинание вместо того, чтобы дежурно перебирать
 * весь список.
 */
public final class SpellMastery {
    private SpellMastery() {
    }

    public static final int MAX_LEVEL = 5;

    /** Сколько раз нужно применить заклинание, чтобы взять уровень. */
    private static final int[] THRESHOLDS = {0, 10, 30, 75, 150, 300};

    private static final float COST_PER_LEVEL = 0.05f;
    private static final float POWER_PER_LEVEL = 0.06f;
    private static final float COOLDOWN_PER_LEVEL = 0.04f;

    public static int level(int casts) {
        int level = 0;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (casts >= THRESHOLDS[i]) {
                level = i;
            }
        }
        return level;
    }

    /** Сколько применений нужно до следующего уровня; {@code -1} на максимуме. */
    public static int nextThreshold(int level) {
        return level >= MAX_LEVEL ? -1 : THRESHOLDS[level + 1];
    }

    /** Множитель стоимости: на пятом уровне заклинание дешевле на четверть. */
    public static float costFactor(int level) {
        return 1.0f - COST_PER_LEVEL * level;
    }

    /** Прибавка к силе: до +30 % на максимуме. */
    public static float powerBonus(int level) {
        return POWER_PER_LEVEL * level;
    }

    public static float cooldownFactor(int level) {
        return 1.0f - COOLDOWN_PER_LEVEL * level;
    }

    /** Полоска звёзд для интерфейса: ★★★☆☆. */
    public static Text stars(int level) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < MAX_LEVEL; i++) {
            builder.append(i < level ? '★' : '☆');
        }
        return Text.literal(builder.toString())
                .formatted(level >= MAX_LEVEL ? Formatting.GOLD : Formatting.YELLOW);
    }
}
