package net.arcanum.spell;

import net.arcanum.spell.impl.ArcaneSpells;
import net.arcanum.spell.impl.FireSpells;
import net.arcanum.spell.impl.FrostSpells;
import net.arcanum.spell.impl.LightSpells;
import net.arcanum.spell.impl.NatureSpells;
import net.arcanum.spell.impl.ShadowSpells;
import net.arcanum.spell.impl.StormSpells;

/**
 * Сборка всех заклинаний.
 *
 * <p>Порядок вызовов задаёт порядок школ в книге заклинаний и в колесе выбора.
 */
public final class Spells {
    private Spells() {
    }

    public static void init() {
        FireSpells.init();
        FrostSpells.init();
        StormSpells.init();
        ArcaneSpells.init();
        NatureSpells.init();
        ShadowSpells.init();
        LightSpells.init();
    }
}
