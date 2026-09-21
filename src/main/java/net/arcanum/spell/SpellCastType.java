package net.arcanum.spell;

import net.minecraft.text.Text;

/**
 * Вид заклинания — чисто описательная категория для подсказок и интерфейса.
 *
 * <p>Все заклинания применяются одинаково (щелчок посохом), а длительные
 * эффекты держит серверный планировщик
 * {@link net.arcanum.spell.util.SpellTicker}.
 */
public enum SpellCastType {
    /** Летящий снаряд. */
    PROJECTILE("projectile"),
    /** Мгновенный эффект по площади вокруг заклинателя или точки прицела. */
    AREA("area"),
    /** Бьёт по одной цели в прицеле. */
    TARGET("target"),
    /** Действует на самого заклинателя. */
    SELF("self"),
    /** Оставляет область, которая работает несколько секунд. */
    LINGERING("lingering"),
    /** Призывает существ. */
    SUMMON("summon"),
    /** Прочее: перемещение, работа с блоками, свет. */
    UTILITY("utility");

    private final String name;

    SpellCastType(String name) {
        this.name = name;
    }

    public String asString() {
        return name;
    }

    public Text displayName() {
        return Text.translatable("casttype.arcanum." + asString());
    }
}
